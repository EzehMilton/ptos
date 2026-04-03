package com.ptos.service;

import com.ptos.domain.AssignmentStatus;
import com.ptos.domain.CheckIn;
import com.ptos.domain.ClientProfile;
import com.ptos.domain.ClientRecord;
import com.ptos.domain.Conversation;
import com.ptos.domain.RiskLevel;
import com.ptos.dto.ClientHealthScoreResult;
import com.ptos.repository.CheckInRepository;
import com.ptos.repository.ClientProfileRepository;
import com.ptos.repository.ConversationRepository;
import com.ptos.repository.WorkoutAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class HealthScoreService {

    private final ClientProfileRepository clientProfileRepository;
    private final CheckInRepository checkInRepository;
    private final WorkoutAssignmentRepository workoutAssignmentRepository;
    private final ConversationRepository conversationRepository;

    public ClientHealthScoreResult calculateHealthScore(ClientRecord clientRecord) {
        return calculateHealthScore(clientRecord, LocalDateTime.now());
    }

    public ClientHealthScoreResult calculateHealthScore(ClientRecord clientRecord, LocalDateTime asOf) {
        Optional<ClientProfile> profileOpt = clientProfileRepository.findByUserId(clientRecord.getClientUser().getId());
        List<CheckIn> checkIns = checkInRepository.findByClientRecordOrderBySubmittedAtDesc(clientRecord).stream()
                .filter(checkIn -> !checkIn.getSubmittedAt().isAfter(asOf))
                .toList();

        int completedProfileFields = countProfileCompletion(profileOpt.orElse(null));
        int profileCompletionScore = scaleScore(completedProfileFields, 6, 10);

        Integer daysSinceLastCheckIn = checkIns.stream()
                .findFirst()
                .map(checkIn -> daysBetween(checkIn.getSubmittedAt(), asOf))
                .orElse(null);
        int checkInActivityScore = scoreCheckInActivity(daysSinceLastCheckIn);

        List<com.ptos.domain.WorkoutAssignment> recentAssignments =
                workoutAssignmentRepository.findByClientRecordOrderByAssignedDateDesc(clientRecord).stream()
                        .filter(assignment -> !assignment.getAssignedDate().isAfter(asOf.toLocalDate()))
                        .filter(assignment -> !assignment.getAssignedDate().isBefore(asOf.toLocalDate().minusDays(27)))
                        .toList();
        Integer daysSinceLastWorkout = findDaysSinceLastWorkoutActivity(clientRecord, asOf);
        int workoutActivityScore = scoreWorkoutActivity(recentAssignments, asOf);

        int goalProgressScore = scoreGoalProgress(profileOpt.orElse(null), checkIns);

        Optional<Conversation> conversationOpt = conversationRepository.findByPtUserAndClientUser(
                clientRecord.getPtUser(), clientRecord.getClientUser()
        );
        Integer daysSinceLastMessage = conversationOpt
                .map(Conversation::getLastMessageAt)
                .filter(lastMessageAt -> !lastMessageAt.isAfter(asOf))
                .map(lastMessageAt -> daysBetween(lastMessageAt, asOf))
                .orElse(null);
        int communicationScore = scoreCommunication(daysSinceLastMessage);

        int overallScore = profileCompletionScore + checkInActivityScore + workoutActivityScore
                + goalProgressScore + communicationScore;

        return ClientHealthScoreResult.builder()
                .clientRecordId(clientRecord.getId())
                .clientName(clientRecord.getClientUser().getFullName())
                .overallScore(overallScore)
                .profileCompletionScore(profileCompletionScore)
                .checkInActivityScore(checkInActivityScore)
                .workoutActivityScore(workoutActivityScore)
                .goalProgressScore(goalProgressScore)
                .communicationScore(communicationScore)
                .riskLevel(resolveRiskLevel(overallScore))
                .daysSinceLastCheckIn(daysSinceLastCheckIn)
                .daysSinceLastWorkout(daysSinceLastWorkout)
                .daysSinceLastMessage(daysSinceLastMessage)
                .build();
    }

    private int countProfileCompletion(ClientProfile profile) {
        if (profile == null) {
            return 0;
        }

        int completion = 0;
        if (profile.getAge() != null) completion++;
        if (profile.getHeightCm() != null) completion++;
        if (profile.getCurrentWeightKg() != null) completion++;
        if (profile.getGoalType() != null) completion++;
        if (profile.getTargetWeightKg() != null) completion++;
        if (profile.getTrainingExperience() != null) completion++;
        return completion;
    }

    private int scaleScore(int value, int maxValue, int maxScore) {
        if (value <= 0) {
            return 0;
        }
        return (int) Math.round((value * (double) maxScore) / maxValue);
    }

    private int scoreCheckInActivity(Integer daysSinceLastCheckIn) {
        if (daysSinceLastCheckIn == null) {
            return 0;
        }
        if (daysSinceLastCheckIn <= 7) {
            return 30;
        }
        if (daysSinceLastCheckIn <= 14) {
            return 20;
        }
        if (daysSinceLastCheckIn <= 21) {
            return 10;
        }
        return 0;
    }

    private int scoreWorkoutActivity(List<com.ptos.domain.WorkoutAssignment> assignments, LocalDateTime asOf) {
        if (assignments.isEmpty()) {
            return 0;
        }

        long completedCount = assignments.stream()
                .filter(assignment -> assignment.getStatus() == AssignmentStatus.COMPLETED)
                .filter(assignment -> assignment.getCompletedAt() != null)
                .filter(assignment -> !assignment.getCompletedAt().isAfter(asOf))
                .count();
        double completionRate = (completedCount * 100.0) / assignments.size();

        if (completionRate >= 80) {
            return 30;
        }
        if (completionRate >= 50) {
            return 20;
        }
        if (completionRate > 0) {
            return 10;
        }
        return 0;
    }

    private Integer findDaysSinceLastWorkoutActivity(ClientRecord clientRecord, LocalDateTime asOf) {
        return workoutAssignmentRepository.findByClientRecordOrderByAssignedDateDesc(clientRecord).stream()
                .map(assignment -> StreamCandidate.builder()
                        .assignedDate(assignment.getAssignedDate().atStartOfDay())
                        .startedAt(assignment.getStartedAt())
                        .completedAt(assignment.getCompletedAt())
                        .build())
                .map(candidate -> candidate.latestAtOrBefore(asOf))
                .filter(value -> value != null)
                .max(Comparator.naturalOrder())
                .map(value -> daysBetween(value, asOf))
                .orElse(null);
    }

    private int scoreGoalProgress(ClientProfile profile, List<CheckIn> checkIns) {
        if (profile == null || profile.getTargetWeightKg() == null) {
            return 8;
        }
        if (checkIns.size() < 2) {
            return 8;
        }

        CheckIn latest = checkIns.get(0);
        CheckIn earliest = checkIns.get(checkIns.size() - 1);
        double firstDistance = Math.abs(earliest.getCurrentWeightKg() - profile.getTargetWeightKg());
        double latestDistance = Math.abs(latest.getCurrentWeightKg() - profile.getTargetWeightKg());
        double improvement = firstDistance - latestDistance;

        if (improvement > 0.5) {
            return 15;
        }
        if (Math.abs(improvement) <= 0.5) {
            return 8;
        }
        return 0;
    }

    private int scoreCommunication(Integer daysSinceLastMessage) {
        if (daysSinceLastMessage == null) {
            return 0;
        }
        if (daysSinceLastMessage <= 7) {
            return 15;
        }
        if (daysSinceLastMessage <= 14) {
            return 10;
        }
        if (daysSinceLastMessage <= 21) {
            return 5;
        }
        return 0;
    }

    private RiskLevel resolveRiskLevel(int overallScore) {
        if (overallScore >= 70) {
            return RiskLevel.HEALTHY;
        }
        if (overallScore >= 50) {
            return RiskLevel.WATCH;
        }
        if (overallScore >= 30) {
            return RiskLevel.AT_RISK;
        }
        return RiskLevel.CHURNING;
    }

    private int daysBetween(LocalDateTime earlier, LocalDateTime later) {
        return Math.max(0, (int) ChronoUnit.DAYS.between(earlier.toLocalDate(), later.toLocalDate()));
    }

    private static class StreamCandidate {
        private LocalDateTime assignedDate;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;

        public static StreamCandidateBuilder builder() {
            return new StreamCandidateBuilder();
        }

        public LocalDateTime latestAtOrBefore(LocalDateTime asOf) {
            return Stream.of(assignedDate, startedAt, completedAt)
                    .filter(value -> value != null && !value.isAfter(asOf))
                    .max(Comparator.naturalOrder())
                    .orElse(null);
        }

        private static class StreamCandidateBuilder {
            private final StreamCandidate target = new StreamCandidate();

            public StreamCandidateBuilder assignedDate(LocalDateTime assignedDate) {
                target.assignedDate = assignedDate;
                return this;
            }

            public StreamCandidateBuilder startedAt(LocalDateTime startedAt) {
                target.startedAt = startedAt;
                return this;
            }

            public StreamCandidateBuilder completedAt(LocalDateTime completedAt) {
                target.completedAt = completedAt;
                return this;
            }

            public StreamCandidate build() {
                return target;
            }
        }
    }
}

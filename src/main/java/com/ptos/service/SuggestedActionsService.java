package com.ptos.service;

import com.ptos.domain.AssignmentStatus;
import com.ptos.domain.CheckIn;
import com.ptos.domain.CheckInStatus;
import com.ptos.domain.ClientProfile;
import com.ptos.domain.ClientRecord;
import com.ptos.domain.ClientStatus;
import com.ptos.domain.User;
import com.ptos.domain.RiskLevel;
import com.ptos.dto.SuggestedAction;
import com.ptos.dto.ClientHealthScoreResult;
import com.ptos.repository.CheckInRepository;
import com.ptos.repository.ClientProfileRepository;
import com.ptos.repository.ClientRecordRepository;
import com.ptos.repository.WorkoutAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SuggestedActionsService {

    private final CheckInRepository checkInRepository;
    private final ClientRecordRepository clientRecordRepository;
    private final ClientProfileRepository clientProfileRepository;
    private final WorkoutAssignmentRepository workoutAssignmentRepository;
    private final HealthScoreService healthScoreService;

    public List<SuggestedAction> getActions(User ptUser) {
        List<SuggestedAction> actions = new ArrayList<>();
        List<ClientRecord> records = clientRecordRepository.findByPtUser(ptUser);
        LocalDate today = LocalDate.now();

        checkInRepository.findByClientRecord_PtUserAndStatusOrderBySubmittedAtDesc(ptUser, CheckInStatus.PENDING_REVIEW)
                .forEach(checkIn -> actions.add(SuggestedAction.builder()
                        .priority(SuggestedAction.Priority.HIGH)
                        .icon("⚠️")
                        .message(checkIn.getClientRecord().getClientUser().getFullName()
                                + " submitted a check-in "
                                + daysAgo(checkIn.getSubmittedAt().toLocalDate(), today)
                                + " days ago")
                        .actionUrl("/pt/checkins/" + checkIn.getId())
                        .actionLabel("Review")
                        .build()));

        records.stream()
                .filter(record -> record.getStatus() == com.ptos.domain.ClientStatus.ACTIVE)
                .map(record -> java.util.Map.entry(record, healthScoreService.calculateHealthScore(record)))
                .filter(entry -> entry.getValue().getRiskLevel() == RiskLevel.CHURNING)
                .sorted(Comparator.comparing(entry -> entry.getKey().getClientUser().getFullName(), String.CASE_INSENSITIVE_ORDER))
                .forEach(entry -> actions.add(SuggestedAction.builder()
                        .priority(SuggestedAction.Priority.HIGH)
                        .icon("⚠️")
                        .message(entry.getKey().getClientUser().getFullName() + " is churning and needs urgent re-engagement")
                        .actionUrl("/pt/messages/new/" + entry.getKey().getId())
                        .actionLabel("Send Message")
                        .build()));

        records.stream()
                .filter(record -> record.getStatus() == com.ptos.domain.ClientStatus.ACTIVE)
                .map(record -> java.util.Map.entry(record, healthScoreService.calculateHealthScore(record)))
                .filter(entry -> entry.getValue().getRiskLevel() == RiskLevel.AT_RISK)
                .sorted(Comparator.comparing(entry -> entry.getKey().getClientUser().getFullName(), String.CASE_INSENSITIVE_ORDER))
                .forEach(entry -> actions.add(SuggestedAction.builder()
                        .priority(SuggestedAction.Priority.MEDIUM)
                        .icon("⚠️")
                        .message(entry.getKey().getClientUser().getFullName() + " is at risk and needs attention")
                        .actionUrl("/pt/messages/new/" + entry.getKey().getId())
                        .actionLabel("Send Message")
                        .build()));

        records.stream()
                .filter(record -> record.getStatus() == ClientStatus.ACTIVE)
                .filter(record -> workoutAssignmentRepository.findByClientRecordOrderByAssignedDateDesc(record).isEmpty())
                .sorted(Comparator.comparing(record -> record.getClientUser().getFullName(), String.CASE_INSENSITIVE_ORDER))
                .forEach(record -> actions.add(SuggestedAction.builder()
                        .priority(SuggestedAction.Priority.MEDIUM)
                        .icon("💪")
                        .message(record.getClientUser().getFullName() + " has no assigned workouts")
                        .actionUrl("/pt/workouts")
                        .actionLabel("Assign Workout")
                        .build()));

        records.stream()
                .filter(record -> record.getStatus() == ClientStatus.ACTIVE)
                .filter(record -> profileCompletion(record) < 3)
                .sorted(Comparator.comparing(record -> record.getClientUser().getFullName(), String.CASE_INSENSITIVE_ORDER))
                .forEach(record -> actions.add(SuggestedAction.builder()
                        .priority(SuggestedAction.Priority.MEDIUM)
                        .icon("📋")
                        .message(record.getClientUser().getFullName() + " has an incomplete profile")
                        .actionUrl("/pt/clients/" + record.getId())
                        .actionLabel("View Client")
                        .build()));

        records.stream()
                .filter(record -> record.getStatus() == ClientStatus.ACTIVE)
                .map(record -> buildCheckInRecencyAction(record, today))
                .flatMap(Optional::stream)
                .forEach(actions::add);

        records.stream()
                .filter(record -> record.getStatus() == ClientStatus.ACTIVE)
                .filter(record -> workoutAssignmentRepository.countByClientRecordAndStatus(record, AssignmentStatus.COMPLETED) == 0)
                .sorted(Comparator.comparing(record -> record.getClientUser().getFullName(), String.CASE_INSENSITIVE_ORDER))
                .forEach(record -> actions.add(SuggestedAction.builder()
                        .priority(SuggestedAction.Priority.LOW)
                        .icon("📝")
                        .message(record.getClientUser().getFullName() + " hasn't completed any workouts yet")
                        .actionUrl("/pt/clients/" + record.getId())
                        .actionLabel("View Client")
                        .build()));

        return actions.stream()
                .sorted(Comparator.comparing((SuggestedAction action) -> action.getPriority().ordinal()))
                .limit(10)
                .toList();
    }

    private Optional<SuggestedAction> buildCheckInRecencyAction(ClientRecord record, LocalDate today) {
        ClientHealthScoreResult healthScore = healthScoreService.calculateHealthScore(record);
        long daysSinceCheckIn = healthScore.getDaysSinceLastCheckIn() != null
                ? healthScore.getDaysSinceLastCheckIn()
                : daysAgo(record.getStartDate(), today);

        if (daysSinceCheckIn < 14) {
            return Optional.empty();
        }

        return Optional.of(SuggestedAction.builder()
                .priority(SuggestedAction.Priority.MEDIUM)
                .icon("📋")
                .message(record.getClientUser().getFullName() + " hasn't checked in for " + daysSinceCheckIn + " days")
                .actionUrl("/pt/clients/" + record.getId())
                .actionLabel("View Client")
                .build());
    }

    private int profileCompletion(ClientRecord record) {
        Optional<ClientProfile> profileOpt = clientProfileRepository.findByUserId(record.getClientUser().getId());
        if (profileOpt.isEmpty()) {
            return 0;
        }

        ClientProfile profile = profileOpt.get();
        int completion = 0;
        if (profile.getAge() != null) completion++;
        if (profile.getHeightCm() != null) completion++;
        if (profile.getCurrentWeightKg() != null) completion++;
        if (profile.getGoalType() != null) completion++;
        if (profile.getTargetWeightKg() != null) completion++;
        if (profile.getTrainingExperience() != null) completion++;
        return completion;
    }

    private long daysAgo(LocalDate date, LocalDate today) {
        return Math.max(0, ChronoUnit.DAYS.between(date, today));
    }
}

package com.ptos.service;

import com.ptos.domain.CheckIn;
import com.ptos.domain.ClientRecord;
import com.ptos.domain.ClientStatus;
import com.ptos.domain.Conversation;
import com.ptos.domain.User;
import com.ptos.domain.WorkoutAssignment;
import com.ptos.dto.ClientHealthScoreResult;
import com.ptos.dto.WeeklyClientSummaryView;
import com.ptos.integration.ClientInsightGateway;
import com.ptos.repository.CheckInRepository;
import com.ptos.repository.ClientRecordRepository;
import com.ptos.repository.ConversationRepository;
import com.ptos.repository.WorkoutAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class InsightService {

    private final ClientInsightGateway clientInsightGateway;
    private final CheckInRepository checkInRepository;
    private final WorkoutAssignmentRepository workoutAssignmentRepository;
    private final ClientRecordRepository clientRecordRepository;
    private final ConversationRepository conversationRepository;
    private final HealthScoreService healthScoreService;

    public String getCheckInInsight(ClientRecord cr, CheckIn current) {
        CheckIn previous = checkInRepository.findTopByClientRecordAndSubmittedAtBeforeOrderBySubmittedAtDesc(
                cr, current.getSubmittedAt()
        ).orElse(null);
        return clientInsightGateway.generateCheckInSummary(cr, current, previous);
    }

    public String getAtRiskInsight(ClientRecord cr) {
        ClientHealthScoreResult healthScore = healthScoreService.calculateHealthScore(cr);
        return clientInsightGateway.generateAtRiskInsight(cr, healthScore);
    }

    public String getWeeklySummary(ClientRecord cr) {
        LocalDate today = LocalDate.now();
        List<CheckIn> recentCheckIns = checkInRepository.findByClientRecordOrderBySubmittedAtDesc(cr).stream()
                .filter(checkIn -> !checkIn.getSubmittedAt().toLocalDate().isBefore(today.minusDays(6)))
                .toList();
        List<WorkoutAssignment> recentAssignments = workoutAssignmentRepository.findByClientRecordOrderByAssignedDateDesc(cr).stream()
                .filter(assignment -> assignment.getAssignedDate() != null)
                .filter(assignment -> !assignment.getAssignedDate().isBefore(today.minusDays(6)))
                .filter(assignment -> !assignment.getAssignedDate().isAfter(today))
                .toList();
        return clientInsightGateway.generateWeeklySummary(cr, recentCheckIns, recentAssignments);
    }

    public List<WeeklyClientSummaryView> getRecentWeeklySummaries(User ptUser, int limit) {
        return clientRecordRepository.findByPtUser(ptUser).stream()
                .filter(record -> record.getStatus() == ClientStatus.ACTIVE)
                .sorted(Comparator.comparing((ClientRecord record) -> latestActivity(record, ptUser))
                        .reversed())
                .limit(limit)
                .map(record -> WeeklyClientSummaryView.builder()
                        .clientRecordId(record.getId())
                        .clientName(record.getClientUser().getFullName())
                        .summary(getWeeklySummary(record))
                        .build())
                .toList();
    }

    private LocalDateTime latestActivity(ClientRecord record, User ptUser) {
        Optional<LocalDateTime> latestCheckIn = checkInRepository.findTopByClientRecordOrderBySubmittedAtDesc(record)
                .map(CheckIn::getSubmittedAt);

        Optional<LocalDateTime> latestWorkout = workoutAssignmentRepository.findByClientRecordOrderByAssignedDateDesc(record).stream()
                .flatMap(assignment -> Stream.of(
                        assignment.getCompletedAt(),
                        assignment.getStartedAt(),
                        assignment.getAssignedDate() != null ? assignment.getAssignedDate().atStartOfDay() : null
                ))
                .filter(value -> value != null)
                .max(Comparator.naturalOrder());

        Optional<LocalDateTime> latestMessage = conversationRepository.findByPtUserAndClientUser(ptUser, record.getClientUser())
                .map(Conversation::getLastMessageAt);

        return Stream.of(
                        latestCheckIn.orElse(null),
                        latestWorkout.orElse(null),
                        latestMessage.orElse(null),
                        record.getStartDate() != null ? record.getStartDate().atStartOfDay() : null
                )
                .filter(value -> value != null)
                .max(Comparator.naturalOrder())
                .orElse(LocalDateTime.MIN);
    }
}

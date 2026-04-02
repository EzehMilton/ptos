package com.ptos.service;

import com.ptos.domain.*;
import com.ptos.dto.BusinessView;
import com.ptos.dto.BusinessView.ClientBusinessRow;
import com.ptos.repository.CheckInRepository;
import com.ptos.repository.ClientProfileRepository;
import com.ptos.repository.ClientRecordRepository;
import com.ptos.repository.WorkoutAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BusinessService {

    private final ClientRecordRepository clientRecordRepository;
    private final ClientProfileRepository clientProfileRepository;
    private final CheckInRepository checkInRepository;
    private final WorkoutAssignmentRepository workoutAssignmentRepository;

    public BusinessView getBusinessData(User ptUser) {
        List<ClientRecord> records = clientRecordRepository.findByPtUser(ptUser);
        LocalDateTime recentThreshold = LocalDateTime.now().minusDays(7);

        List<ClientBusinessRow> rows = records.stream()
                .map(record -> toRow(record, recentThreshold))
                .sorted(Comparator.comparing(ClientBusinessRow::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());

        int activeCount = (int) rows.stream()
                .filter(r -> r.getStatus() == ClientStatus.ACTIVE)
                .count();

        BigDecimal revenue = rows.stream()
                .filter(r -> r.getStatus() == ClientStatus.ACTIVE && r.getPackagePrice() != null)
                .map(ClientBusinessRow::getPackagePrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgRevenue = activeCount > 0
                ? revenue.divide(BigDecimal.valueOf(activeCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        int needingAttention = (int) rows.stream()
                .filter(r -> "Needs Attention".equals(r.getHealthLabel()))
                .count();

        int completeCount = (int) rows.stream()
                .filter(r -> r.getProfileCompletion() >= 4)
                .count();

        int totalCount = rows.size();
        int completionRate = totalCount > 0 ? (completeCount * 100) / totalCount : 0;
        int recentCheckInCount = (int) rows.stream()
                .filter(r -> r.getStatus() == ClientStatus.ACTIVE)
                .filter(r -> r.getLastCheckInDate() != null && !r.getLastCheckInDate().isBefore(recentThreshold))
                .count();
        int checkInComplianceRate = activeCount > 0 ? (recentCheckInCount * 100) / activeCount : 0;

        long totalAssignments = workoutAssignmentRepository.countByClientRecord_PtUser(ptUser);
        long completedAssignments = workoutAssignmentRepository.countByClientRecord_PtUserAndStatus(
                ptUser, AssignmentStatus.COMPLETED);
        int workoutCompletionRate = totalAssignments > 0
                ? (int) ((completedAssignments * 100) / totalAssignments)
                : 0;

        List<ClientBusinessRow> incomplete = rows.stream()
                .filter(r -> r.getProfileCompletion() < 4)
                .collect(Collectors.toList());

        return BusinessView.builder()
                .estimatedMonthlyRevenue(revenue)
                .averageRevenuePerClient(avgRevenue)
                .activeClientCount(activeCount)
                .clientsNeedingAttention(needingAttention)
                .profileCompletionRate(completionRate)
                .checkInComplianceRate(checkInComplianceRate)
                .workoutCompletionRate(workoutCompletionRate)
                .clientBreakdown(rows)
                .completeProfileCount(completeCount)
                .totalClientCount(totalCount)
                .incompleteProfileClients(incomplete)
                .build();
    }

    private ClientBusinessRow toRow(ClientRecord record, LocalDateTime recentThreshold) {
        User client = record.getClientUser();
        Optional<ClientProfile> profileOpt = clientProfileRepository.findByUserId(client.getId());
        Optional<CheckIn> latestCheckIn = checkInRepository.findTopByClientRecordOrderBySubmittedAtDesc(record);
        long workoutsAssigned = workoutAssignmentRepository.findByClientRecordOrderByAssignedDateDesc(record).size();
        long workoutsCompleted = workoutAssignmentRepository.countByClientRecordAndStatus(
                record, AssignmentStatus.COMPLETED);

        int completion = 0;
        if (profileOpt.isPresent()) {
            ClientProfile p = profileOpt.get();
            if (p.getAge() != null) completion++;
            if (p.getHeightCm() != null) completion++;
            if (p.getCurrentWeightKg() != null) completion++;
            if (p.getGoalType() != null) completion++;
            if (p.getTargetWeightKg() != null) completion++;
            if (p.getTrainingExperience() != null) completion++;
        }

        boolean hasRecentCheckIn = latestCheckIn
                .map(checkIn -> !checkIn.getSubmittedAt().isBefore(recentThreshold))
                .orElse(false);
        boolean hasCompletedWorkout = workoutsCompleted > 0;

        String health;
        if (record.getStatus() == ClientStatus.INACTIVE || record.getStatus() == ClientStatus.ARCHIVED) {
            health = "Inactive";
        } else if (record.getStatus() == ClientStatus.ACTIVE && hasRecentCheckIn && hasCompletedWorkout) {
            health = "Good";
        } else {
            health = "Needs Attention";
        }

        return ClientBusinessRow.builder()
                .clientRecordId(record.getId())
                .name(client.getFullName())
                .status(record.getStatus())
                .packagePrice(record.getMonthlyPackagePrice())
                .profileCompletion(completion)
                .lastCheckInDate(latestCheckIn.map(CheckIn::getSubmittedAt).orElse(null))
                .workoutsAssigned(workoutsAssigned)
                .workoutsCompleted(workoutsCompleted)
                .healthLabel(health)
                .build();
    }
}

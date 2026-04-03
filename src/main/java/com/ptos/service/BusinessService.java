package com.ptos.service;

import com.ptos.domain.*;
import com.ptos.dto.BusinessView;
import com.ptos.dto.BusinessView.ClientBusinessRow;
import com.ptos.dto.ClientHealthScoreResult;
import com.ptos.repository.CheckInRepository;
import com.ptos.repository.ClientProfileRepository;
import com.ptos.repository.ClientRecordRepository;
import com.ptos.repository.MealPlanRepository;
import com.ptos.repository.WorkoutAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
    private final MealPlanRepository mealPlanRepository;
    private final HealthScoreService healthScoreService;

    public BusinessView getBusinessData(User ptUser) {
        List<ClientRecord> records = clientRecordRepository.findByPtUser(ptUser);
        LocalDateTime recentThreshold = LocalDateTime.now().minusDays(7);
        LocalDateTime twoWeeksAgo = LocalDateTime.now().minusDays(14);

        List<ClientBusinessRow> rows = records.stream()
                .map(this::toRow)
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
        int mealPlanCoverage = activeCount > 0
                ? (int) ((records.stream()
                .filter(record -> record.getStatus() == ClientStatus.ACTIVE)
                .filter(record -> mealPlanRepository.findByClientRecordAndActiveTrue(record).isPresent())
                .count() * 100) / activeCount)
                : 0;

        List<ClientRecord> activeRecords = records.stream()
                .filter(record -> record.getStatus() == ClientStatus.ACTIVE)
                .toList();
        List<ClientHealthScoreResult> currentScores = activeRecords.stream()
                .map(healthScoreService::calculateHealthScore)
                .toList();

        double averageHealthScore = currentScores.stream()
                .mapToInt(ClientHealthScoreResult::getOverallScore)
                .average()
                .orElse(0);
        int churnRiskCount = (int) currentScores.stream()
                .filter(score -> score.getRiskLevel() == RiskLevel.AT_RISK || score.getRiskLevel() == RiskLevel.CHURNING)
                .count();
        int clientsImproving = 0;
        int clientsSlipping = 0;
        for (ClientRecord activeRecord : activeRecords) {
            int current = healthScoreService.calculateHealthScore(activeRecord).getOverallScore();
            int previous = healthScoreService.calculateHealthScore(activeRecord, twoWeeksAgo).getOverallScore();
            if (current > previous) {
                clientsImproving++;
            } else if (current < previous) {
                clientsSlipping++;
            }
        }
        int clientsStable = activeRecords.size() - clientsImproving - clientsSlipping;

        List<ClientHealthScoreResult> reEngagementCandidates = currentScores.stream()
                .filter(score -> score.getRiskLevel() == RiskLevel.AT_RISK || score.getRiskLevel() == RiskLevel.CHURNING)
                .filter(score -> score.getDaysSinceLastCheckIn() != null)
                .filter(score -> score.getDaysSinceLastCheckIn() > 14 && score.getDaysSinceLastCheckIn() < 60)
                .sorted(Comparator.comparing(ClientHealthScoreResult::getOverallScore))
                .toList();

        double averageTenureMonths = activeRecords.stream()
                .mapToDouble(record -> Math.max(1, ChronoUnit.DAYS.between(record.getStartDate(), LocalDate.now())) / 30.0)
                .average()
                .orElse(0);
        BigDecimal estimatedClientLifetimeValue = avgRevenue.multiply(BigDecimal.valueOf(averageTenureMonths))
                .setScale(2, RoundingMode.HALF_UP);

        List<ClientBusinessRow> incomplete = rows.stream()
                .filter(r -> r.getProfileCompletion() < 4)
                .collect(Collectors.toList());

        return BusinessView.builder()
                .estimatedMonthlyRevenue(revenue)
                .averageRevenuePerClient(avgRevenue)
                .estimatedClientLifetimeValue(estimatedClientLifetimeValue)
                .activeClientCount(activeCount)
                .churnRiskCount(churnRiskCount)
                .averageHealthScore(averageHealthScore)
                .clientsImproving(clientsImproving)
                .clientsStable(clientsStable)
                .clientsSlipping(clientsSlipping)
                .profileCompletionRate(completionRate)
                .checkInComplianceRate(checkInComplianceRate)
                .workoutCompletionRate(workoutCompletionRate)
                .mealPlanCoverage(mealPlanCoverage)
                .clientBreakdown(rows)
                .completeProfileCount(completeCount)
                .totalClientCount(totalCount)
                .incompleteProfileClients(incomplete)
                .reEngagementCandidates(reEngagementCandidates)
                .build();
    }

    private ClientBusinessRow toRow(ClientRecord record) {
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

        ClientHealthScoreResult healthScore = healthScoreService.calculateHealthScore(record);

        return ClientBusinessRow.builder()
                .clientRecordId(record.getId())
                .name(client.getFullName())
                .status(record.getStatus())
                .packagePrice(record.getMonthlyPackagePrice())
                .profileCompletion(completion)
                .lastCheckInDate(latestCheckIn.map(CheckIn::getSubmittedAt).orElse(null))
                .workoutsAssigned(workoutsAssigned)
                .workoutsCompleted(workoutsCompleted)
                .healthScore(healthScore.getOverallScore())
                .riskLevel(healthScore.getRiskLevel())
                .build();
    }
}

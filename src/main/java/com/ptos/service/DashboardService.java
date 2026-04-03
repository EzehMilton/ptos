package com.ptos.service;

import com.ptos.domain.CheckIn;
import com.ptos.domain.AssignmentStatus;
import com.ptos.domain.ClientStatus;
import com.ptos.domain.RiskLevel;
import com.ptos.domain.ClientRecord;
import com.ptos.dto.ClientHealthScoreResult;
import com.ptos.domain.User;
import com.ptos.dto.ClientListView;
import com.ptos.dto.DashboardView;
import com.ptos.repository.WorkoutAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ClientRecordService clientRecordService;
    private final CheckInService checkInService;
    private final WorkoutAssignmentRepository workoutAssignmentRepository;
    private final HealthScoreService healthScoreService;

    public DashboardView getDashboardData(User ptUser) {
        Map<ClientStatus, Long> counts = clientRecordService.getStatusCounts(ptUser);

        long active = counts.getOrDefault(ClientStatus.ACTIVE, 0L);
        long inactive = counts.getOrDefault(ClientStatus.INACTIVE, 0L);
        long archived = counts.getOrDefault(ClientStatus.ARCHIVED, 0L);
        long atRiskStatus = counts.getOrDefault(ClientStatus.AT_RISK, 0L);
        long total = active + atRiskStatus + inactive + archived;

        List<ClientListView> allClients = clientRecordService.getClientListForPT(ptUser);
        List<ClientRecord> activeRecords = clientRecordService.getClientsForPT(ptUser).stream()
                .filter(record -> record.getStatus() == ClientStatus.ACTIVE)
                .toList();
        List<ClientHealthScoreResult> healthScores = activeRecords.stream()
                .map(healthScoreService::calculateHealthScore)
                .toList();
        long healthyClients = healthScores.stream().filter(score -> score.getRiskLevel() == RiskLevel.HEALTHY).count();
        long watchClients = healthScores.stream().filter(score -> score.getRiskLevel() == RiskLevel.WATCH).count();
        long atRiskClients = healthScores.stream().filter(score -> score.getRiskLevel() == RiskLevel.AT_RISK).count();
        long churningClients = healthScores.stream().filter(score -> score.getRiskLevel() == RiskLevel.CHURNING).count();
        long churnRiskCount = atRiskClients + churningClients;

        List<ClientListView> recentlyUpdated = allClients.stream()
                .filter(c -> c.getProfileLastUpdated() != null)
                .sorted(Comparator.comparing(ClientListView::getProfileLastUpdated).reversed())
                .limit(5)
                .collect(Collectors.toList());

        BigDecimal revenue = allClients.stream()
                .filter(c -> c.getStatus() == ClientStatus.ACTIVE && c.getMonthlyPackagePrice() != null)
                .map(ClientListView::getMonthlyPackagePrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long noPrice = allClients.stream()
                .filter(c -> c.getStatus() == ClientStatus.ACTIVE && c.getMonthlyPackagePrice() == null)
                .count();

        List<CheckIn> pendingCheckIns = checkInService.getPendingCheckIns(ptUser);
        long totalWorkoutsAssigned = workoutAssignmentRepository.countByClientRecord_PtUser(ptUser);
        long workoutsCompletedThisWeek = workoutAssignmentRepository.countByClientRecord_PtUserAndStatusAndCompletedAtAfter(
                ptUser,
                AssignmentStatus.COMPLETED,
                LocalDateTime.now().minusDays(7)
        );

        return DashboardView.builder()
                .totalClients(total)
                .activeClients(active)
                .churnRiskCount(churnRiskCount)
                .inactiveOrArchived(inactive + archived)
                .healthyClients(healthyClients)
                .watchClients(watchClients)
                .atRiskClients(atRiskClients)
                .churningClients(churningClients)
                .recentlyUpdatedClients(recentlyUpdated)
                .estimatedMonthlyRevenue(revenue)
                .clientsWithoutPackagePrice(noPrice)
                .pendingCheckInCount(pendingCheckIns.size())
                .recentPendingCheckIns(pendingCheckIns.stream().limit(3).toList())
                .totalWorkoutsAssigned(totalWorkoutsAssigned)
                .workoutsCompletedThisWeek(workoutsCompletedThisWeek)
                .build();
    }
}

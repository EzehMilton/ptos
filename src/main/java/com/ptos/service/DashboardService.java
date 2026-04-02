package com.ptos.service;

import com.ptos.domain.ClientStatus;
import com.ptos.domain.User;
import com.ptos.dto.ClientListView;
import com.ptos.dto.DashboardView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ClientRecordService clientRecordService;

    public DashboardView getDashboardData(User ptUser) {
        Map<ClientStatus, Long> counts = clientRecordService.getStatusCounts(ptUser);

        long active = counts.getOrDefault(ClientStatus.ACTIVE, 0L);
        long atRisk = counts.getOrDefault(ClientStatus.AT_RISK, 0L);
        long inactive = counts.getOrDefault(ClientStatus.INACTIVE, 0L);
        long archived = counts.getOrDefault(ClientStatus.ARCHIVED, 0L);
        long total = active + atRisk + inactive + archived;

        List<ClientListView> allClients = clientRecordService.getClientListForPT(ptUser);

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

        return DashboardView.builder()
                .totalClients(total)
                .activeClients(active)
                .atRiskClients(atRisk)
                .inactiveOrArchived(inactive + archived)
                .recentlyUpdatedClients(recentlyUpdated)
                .estimatedMonthlyRevenue(revenue)
                .clientsWithoutPackagePrice(noPrice)
                .build();
    }
}

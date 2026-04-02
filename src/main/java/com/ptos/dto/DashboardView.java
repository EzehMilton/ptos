package com.ptos.dto;

import com.ptos.domain.CheckIn;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter @AllArgsConstructor @Builder
public class DashboardView {

    private long totalClients;
    private long activeClients;
    private long atRiskClients;
    private long inactiveOrArchived;
    private List<ClientListView> recentlyUpdatedClients;
    private BigDecimal estimatedMonthlyRevenue;
    private long clientsWithoutPackagePrice;
    private long pendingCheckInCount;
    private List<CheckIn> recentPendingCheckIns;
    private long totalWorkoutsAssigned;
    private long workoutsCompletedThisWeek;
}

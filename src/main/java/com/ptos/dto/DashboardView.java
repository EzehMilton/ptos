package com.ptos.dto;

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
}

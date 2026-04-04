package com.ptos.dto;

import com.ptos.domain.ClientStatus;
import com.ptos.domain.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter @AllArgsConstructor @Builder
public class BusinessView {

    private BigDecimal estimatedMonthlyRevenue;
    private BigDecimal averageRevenuePerClient;
    private BigDecimal estimatedClientLifetimeValue;
    private int activeClientCount;
    private int churnRiskCount;
    private double averageHealthScore;
    private int clientsImproving;
    private int clientsStable;
    private int clientsSlipping;
    private int retentionRate;
    private int profileCompletionRate;
    private int checkInComplianceRate;
    private int workoutCompletionRate;
    private int mealPlanCoverage;
    private List<ClientBusinessRow> clientBreakdown;
    private int completeProfileCount;
    private int totalClientCount;
    private List<ClientBusinessRow> incompleteProfileClients;
    private List<ClientHealthScoreResult> reEngagementCandidates;
    private int healthyCount;
    private int watchCount;
    private int atRiskCount;
    private int churningCount;
    private List<ClientTrendRow> trendClients;
    private List<ClientBusinessRow> topRevenueClients;
    private List<MonthlyRevenue> revenueTrend;

    @Getter @AllArgsConstructor @Builder
    public static class ClientBusinessRow {
        private Long clientRecordId;
        private String name;
        private ClientStatus status;
        private BigDecimal packagePrice;
        private int profileCompletion;
        private LocalDateTime lastCheckInDate;
        private long workoutsAssigned;
        private long workoutsCompleted;
        private int healthScore;
        private RiskLevel riskLevel;
    }

    @Getter @AllArgsConstructor @Builder
    public static class ClientTrendRow {
        private Long clientRecordId;
        private String name;
        private int currentScore;
        private int scoreDelta;
    }

    @Getter @AllArgsConstructor @Builder
    public static class MonthlyRevenue {
        private String label;
        private BigDecimal amount;
        private int heightPercent;
    }
}
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
    private int profileCompletionRate;
    private int checkInComplianceRate;
    private int workoutCompletionRate;
    private int mealPlanCoverage;
    private List<ClientBusinessRow> clientBreakdown;
    private int completeProfileCount;
    private int totalClientCount;
    private List<ClientBusinessRow> incompleteProfileClients;
    private List<ClientHealthScoreResult> reEngagementCandidates;

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
}

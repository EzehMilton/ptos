package com.ptos.dto;

import com.ptos.domain.ClientStatus;
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
    private int activeClientCount;
    private int clientsNeedingAttention;
    private int profileCompletionRate;
    private int checkInComplianceRate;
    private int workoutCompletionRate;
    private List<ClientBusinessRow> clientBreakdown;
    private int completeProfileCount;
    private int totalClientCount;
    private List<ClientBusinessRow> incompleteProfileClients;

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
        private String healthLabel;
    }
}

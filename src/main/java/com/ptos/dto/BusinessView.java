package com.ptos.dto;

import com.ptos.domain.ClientStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter @AllArgsConstructor @Builder
public class BusinessView {

    private BigDecimal estimatedMonthlyRevenue;
    private BigDecimal averageRevenuePerClient;
    private int activeClientCount;
    private int clientsNeedingAttention;
    private int profileCompletionRate;
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
        private String healthLabel;
    }
}

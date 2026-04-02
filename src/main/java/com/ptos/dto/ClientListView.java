package com.ptos.dto;

import com.ptos.domain.ClientStatus;
import com.ptos.domain.GoalType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @AllArgsConstructor @Builder
public class ClientListView {

    private Long clientRecordId;
    private String clientName;
    private String clientEmail;
    private GoalType goalType;
    private Double currentWeightKg;
    private Double targetWeightKg;
    private ClientStatus status;
    private LocalDate startDate;
    private LocalDateTime profileLastUpdated;
    private BigDecimal monthlyPackagePrice;
}

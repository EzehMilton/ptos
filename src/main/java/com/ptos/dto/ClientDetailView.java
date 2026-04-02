package com.ptos.dto;

import com.ptos.domain.ClientStatus;
import com.ptos.domain.GoalType;
import com.ptos.domain.TrainingExperience;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @AllArgsConstructor @Builder
public class ClientDetailView {

    private Long clientRecordId;
    private String clientName;
    private String clientEmail;
    private ClientStatus status;
    private LocalDate startDate;
    private BigDecimal monthlyPackagePrice;
    private String ptNotes;
    private Integer age;
    private Double heightCm;
    private Double currentWeightKg;
    private GoalType goalType;
    private Double targetWeightKg;
    private String injuriesOrConditions;
    private String dietaryPreferences;
    private TrainingExperience trainingExperience;
    private String clientNotes;
    private boolean profileComplete;
    private int profileCompletionCount;
    private long daysSinceStart;
}

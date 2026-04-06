package com.ptos.dto.api;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record MealComplianceRequest(
        @NotNull LocalDate date,
        @NotNull String compliance,
        String notes
) {}

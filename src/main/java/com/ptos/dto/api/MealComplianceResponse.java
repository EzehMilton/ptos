package com.ptos.dto.api;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MealComplianceResponse(
        LocalDate date,
        String compliance,
        String notes,
        LocalDateTime loggedAt
) {}

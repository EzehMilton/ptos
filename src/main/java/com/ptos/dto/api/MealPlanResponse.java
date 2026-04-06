package com.ptos.dto.api;

import java.time.LocalDateTime;
import java.util.List;

public record MealPlanResponse(
        Long id,
        String title,
        String overview,
        String dailyGuidance,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<MealComplianceResponse> recentCompliance
) {}

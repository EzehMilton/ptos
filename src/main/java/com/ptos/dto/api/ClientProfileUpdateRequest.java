package com.ptos.dto.api;

public record ClientProfileUpdateRequest(
        Integer age,
        Double heightCm,
        Double currentWeightKg,
        String goalType,
        Double targetWeightKg,
        String trainingExperience,
        String injuriesOrConditions,
        String dietaryPreferences,
        String notes
) {}

package com.ptos.dto.api;

public record ClientProfileResponse(
        String fullName,
        String email,
        Integer age,
        Double heightCm,
        Double currentWeightKg,
        String goalType,
        Double targetWeightKg,
        String injuriesOrConditions,
        String dietaryPreferences,
        String trainingExperience,
        String notes
) {}

package com.ptos.dto.api;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record ClientProfileUpdateRequest(
        @Min(16) @Max(100) Integer age,
        @Min(100) @Max(250) Double heightCm,
        @Min(30) @Max(300) Double currentWeightKg,
        String goalType,
        @Min(30) @Max(300) Double targetWeightKg,
        String trainingExperience,
        @Size(max = 500) String injuriesOrConditions,
        @Size(max = 500) String dietaryPreferences,
        @JsonAlias("notes") @Size(max = 1000) String additionalNotes,
        Boolean onboardingCompleted
) {}

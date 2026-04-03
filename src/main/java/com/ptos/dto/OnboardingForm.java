package com.ptos.dto;

import com.ptos.domain.GoalType;
import com.ptos.domain.TrainingExperience;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OnboardingForm {

    @NotNull(message = "Age is required")
    @Min(value = 16, message = "Age must be at least 16")
    @Max(value = 100, message = "Age must be at most 100")
    private Integer age;

    @NotNull(message = "Height is required")
    @Min(value = 100, message = "Height must be at least 100 cm")
    @Max(value = 250, message = "Height must be at most 250 cm")
    private Double heightCm;

    @NotNull(message = "Current weight is required")
    @Min(value = 30, message = "Weight must be at least 30 kg")
    @Max(value = 300, message = "Weight must be at most 300 kg")
    private Double currentWeightKg;

    @NotNull(message = "Goal is required")
    private GoalType goalType;

    @Min(value = 30, message = "Target weight must be at least 30 kg")
    @Max(value = 300, message = "Target weight must be at most 300 kg")
    private Double targetWeightKg;

    @NotNull(message = "Training experience is required")
    private TrainingExperience trainingExperience;

    @Size(max = 500, message = "Must be at most 500 characters")
    private String injuriesOrConditions;

    @Size(max = 500, message = "Must be at most 500 characters")
    private String dietaryPreferences;

    @Size(max = 500, message = "Must be at most 500 characters")
    private String goalNotes;
}

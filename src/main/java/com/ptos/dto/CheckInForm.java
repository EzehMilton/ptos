package com.ptos.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class CheckInForm {

    @NotNull(message = "Current weight is required")
    @Min(value = 30, message = "Weight must be at least 30 kg")
    @Max(value = 300, message = "Weight must be at most 300 kg")
    private Double currentWeightKg;

    @Min(value = 1, message = "Mood score must be between 1 and 5")
    @Max(value = 5, message = "Mood score must be between 1 and 5")
    private Integer moodScore;

    @Min(value = 1, message = "Energy score must be between 1 and 5")
    @Max(value = 5, message = "Energy score must be between 1 and 5")
    private Integer energyScore;

    @Min(value = 1, message = "Sleep score must be between 1 and 5")
    @Max(value = 5, message = "Sleep score must be between 1 and 5")
    private Integer sleepScore;

    @Size(max = 1000, message = "Notes must be at most 1000 characters")
    private String notes;
}

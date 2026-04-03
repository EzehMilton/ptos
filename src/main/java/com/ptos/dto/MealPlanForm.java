package com.ptos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MealPlanForm {

    @NotBlank(message = "Title is required")
    @Size(max = 100, message = "Title must be at most 100 characters")
    private String title;

    @Size(max = 1000, message = "Overview must be at most 1000 characters")
    private String overview;

    @NotBlank(message = "Daily guidance is required")
    @Size(max = 5000, message = "Daily guidance must be at most 5000 characters")
    private String dailyGuidance;
}

package com.ptos.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class WorkoutExerciseForm {

    @NotBlank(message = "Exercise name is required")
    @Size(max = 100, message = "Name must be at most 100 characters")
    private String exerciseName;

    @NotNull(message = "Sets is required")
    @Min(value = 1, message = "At least 1 set")
    @Max(value = 20, message = "At most 20 sets")
    private Integer setsCount;

    @NotBlank(message = "Reps is required")
    @Size(max = 50, message = "Reps must be at most 50 characters")
    private String repsText;

    @Size(max = 300, message = "Notes must be at most 300 characters")
    private String notes;
}

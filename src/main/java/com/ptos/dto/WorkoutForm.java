package com.ptos.dto;

import com.ptos.domain.WorkoutCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor
public class WorkoutForm {

    @NotBlank(message = "Workout name is required")
    @Size(max = 100, message = "Name must be at most 100 characters")
    private String name;

    @Size(max = 500, message = "Description must be at most 500 characters")
    private String description;

    @NotNull(message = "Category is required")
    private WorkoutCategory category;

    private boolean template;

    @Valid
    private List<WorkoutExerciseForm> exercises = new ArrayList<>();
}

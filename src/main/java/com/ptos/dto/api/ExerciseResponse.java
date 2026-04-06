package com.ptos.dto.api;

public record ExerciseResponse(
        String exerciseName,
        Integer setsCount,
        String repsText,
        String notes,
        Integer sortOrder
) {}

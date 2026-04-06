package com.ptos.dto.api;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record WorkoutAssignmentResponse(
        Long assignmentId,
        String workoutName,
        String description,
        String category,
        LocalDate assignedDate,
        String status,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        String completionNotes,
        List<ExerciseResponse> exercises
) {}

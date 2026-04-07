package com.ptos.controller.api;

import com.ptos.domain.User;
import com.ptos.domain.WorkoutAssignment;
import com.ptos.domain.WorkoutExercise;
import com.ptos.dto.api.ExerciseResponse;
import com.ptos.dto.api.WorkoutAssignmentResponse;
import com.ptos.dto.api.WorkoutStatusUpdateRequest;
import com.ptos.security.PtosUserDetails;
import com.ptos.service.WorkoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/client/workouts")
@RequiredArgsConstructor
public class ClientWorkoutApiController {

    private final WorkoutService workoutService;

    @GetMapping
    public ResponseEntity<List<WorkoutAssignmentResponse>> getWorkouts(
            @AuthenticationPrincipal PtosUserDetails userDetails) {
        User user = userDetails.getUser();
        List<WorkoutAssignmentResponse> assignments = workoutService.getAssignmentsForClient(user).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(assignments);
    }

    @PutMapping("/{assignmentId}/status")
    public ResponseEntity<WorkoutAssignmentResponse> updateStatus(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal PtosUserDetails userDetails,
            @Valid @RequestBody WorkoutStatusUpdateRequest request) {
        User user = userDetails.getUser();

        WorkoutAssignment assignment = workoutService.getAssignment(assignmentId, user)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found or does not belong to this client"));

        return switch (request.resolveUpdateCommand()) {
            case "START", "IN_PROGRESS" -> {
                workoutService.startWorkout(assignmentId, user);
                yield ResponseEntity.ok(toResponse(workoutService.getAssignment(assignmentId, user).orElseThrow()));
            }
            case "COMPLETE", "COMPLETED" -> {
                workoutService.completeWorkout(assignmentId, user, request.notes());
                yield ResponseEntity.ok(toResponse(workoutService.getAssignment(assignmentId, user).orElseThrow()));
            }
            default -> throw new IllegalArgumentException(
                    "Status update must be one of: START, IN_PROGRESS, COMPLETE, COMPLETED");
        };
    }

    private WorkoutAssignmentResponse toResponse(WorkoutAssignment a) {
        List<ExerciseResponse> exercises = a.getWorkout().getExercises().stream()
                .sorted(java.util.Comparator.comparingInt(WorkoutExercise::getSortOrder))
                .map(e -> new ExerciseResponse(
                        e.getExerciseName(), e.getSetsCount(), e.getRepsText(),
                        e.getNotes(), e.getSortOrder()))
                .toList();
        return new WorkoutAssignmentResponse(
                a.getId(), a.getWorkout().getName(), a.getWorkout().getDescription(),
                a.getWorkout().getCategory() != null ? a.getWorkout().getCategory().name() : null,
                a.getAssignedDate(), a.getStatus().name(),
                a.getStartedAt(), a.getCompletedAt(), a.getCompletionNotes(),
                exercises
        );
    }
}

package com.ptos.service;

import com.ptos.domain.*;
import com.ptos.dto.WorkoutExerciseForm;
import com.ptos.dto.WorkoutForm;
import com.ptos.repository.ClientRecordRepository;
import com.ptos.repository.WorkoutAssignmentRepository;
import com.ptos.repository.WorkoutRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WorkoutService {

    private final WorkoutRepository workoutRepository;
    private final WorkoutAssignmentRepository workoutAssignmentRepository;
    private final ClientRecordRepository clientRecordRepository;

    public List<Workout> getWorkoutsForPT(User ptUser) {
        return workoutRepository.findByPtUserOrderByCreatedAtDesc(ptUser);
    }

    public Optional<Workout> getWorkout(Long workoutId, User ptUser) {
        return workoutRepository.findByIdAndPtUser(workoutId, ptUser);
    }

    @Transactional
    public Workout createWorkout(User ptUser, WorkoutForm form) {
        Workout workout = Workout.builder()
                .ptUser(ptUser)
                .name(form.getName())
                .description(form.getDescription())
                .category(form.getCategory())
                .isTemplate(form.isTemplate())
                .build();

        if (form.getExercises() != null) {
            for (int i = 0; i < form.getExercises().size(); i++) {
                WorkoutExerciseForm row = form.getExercises().get(i);
                if (row.getExerciseName() == null || row.getExerciseName().isBlank()) {
                    continue;
                }
                WorkoutExercise exercise = WorkoutExercise.builder()
                        .workout(workout)
                        .exerciseName(row.getExerciseName())
                        .setsCount(row.getSetsCount())
                        .repsText(row.getRepsText())
                        .notes(row.getNotes())
                        .sortOrder(i)
                        .build();
                workout.getExercises().add(exercise);
            }
        }

        return workoutRepository.save(workout);
    }

    @Transactional
    public void assignWorkout(Long workoutId, Long clientRecordId, LocalDate date, User ptUser) {
        Workout workout = workoutRepository.findByIdAndPtUser(workoutId, ptUser)
                .orElseThrow(() -> new IllegalArgumentException("Workout not found"));
        ClientRecord clientRecord = clientRecordRepository.findByIdAndPtUser(clientRecordId, ptUser)
                .orElseThrow(() -> new IllegalArgumentException("Client record not found"));

        WorkoutAssignment assignment = WorkoutAssignment.builder()
                .workout(workout)
                .clientRecord(clientRecord)
                .assignedDate(date)
                .build();
        workoutAssignmentRepository.save(assignment);
    }

    public List<WorkoutAssignment> getAssignmentsForClient(User clientUser) {
        return workoutAssignmentRepository.findByClientRecord_ClientUserOrderByAssignedDateDesc(clientUser);
    }

    public Optional<WorkoutAssignment> getAssignment(Long assignmentId, User clientUser) {
        return workoutAssignmentRepository.findByIdAndClientRecord_ClientUser(assignmentId, clientUser);
    }

    @Transactional
    public void startWorkout(Long assignmentId, User clientUser) {
        WorkoutAssignment assignment = workoutAssignmentRepository.findByIdAndClientRecord_ClientUser(assignmentId, clientUser)
                .orElseThrow(() -> new IllegalArgumentException("Workout assignment not found"));

        assignment.setStatus(AssignmentStatus.IN_PROGRESS);
        if (assignment.getStartedAt() == null) {
            assignment.setStartedAt(LocalDateTime.now());
        }
        workoutAssignmentRepository.save(assignment);
    }

    @Transactional
    public void completeWorkout(Long assignmentId, User clientUser, String notes) {
        WorkoutAssignment assignment = workoutAssignmentRepository.findByIdAndClientRecord_ClientUser(assignmentId, clientUser)
                .orElseThrow(() -> new IllegalArgumentException("Workout assignment not found"));

        LocalDateTime now = LocalDateTime.now();
        assignment.setStatus(AssignmentStatus.COMPLETED);
        if (assignment.getStartedAt() == null) {
            assignment.setStartedAt(now);
        }
        assignment.setCompletedAt(now);
        assignment.setCompletionNotes(notes != null && !notes.isBlank() ? notes.trim() : null);
        workoutAssignmentRepository.save(assignment);
    }
}

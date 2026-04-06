package com.ptos.controller;

import com.ptos.domain.ClientStatus;
import com.ptos.domain.User;
import com.ptos.domain.Workout;
import com.ptos.dto.WorkoutExerciseForm;
import com.ptos.dto.WorkoutForm;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import com.ptos.security.SecurityHelper;
import com.ptos.service.ClientRecordService;
import com.ptos.service.WorkoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.FieldError;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

@Controller
@RequestMapping("/pt/workouts")
@RequiredArgsConstructor
@Slf4j
public class PtWorkoutController {

    private final WorkoutService workoutService;
    private final ClientRecordService clientRecordService;
    private final SecurityHelper securityHelper;
    private final Validator validator;

    @GetMapping
    public String listWorkouts(Model model) {
        log.info("Showing PT workouts");
        User ptUser = securityHelper.getCurrentUserDetails().getUser();
        model.addAttribute("workouts", workoutService.getWorkoutsForPT(ptUser));
        return "pt/workouts/list";
    }

    @GetMapping("/new")
    public String newWorkoutForm(Model model) {
        log.info("Showing new PT workout form");
        WorkoutForm form = new WorkoutForm();
        IntStream.range(0, 1).forEach(i -> form.getExercises().add(new WorkoutExerciseForm()));
        model.addAttribute("workoutForm", form);
        return "pt/workouts/form";
    }

    @PostMapping
    public String createWorkout(@Valid @ModelAttribute("workoutForm") WorkoutForm form,
                                BindingResult result,
                                RedirectAttributes redirectAttributes) {
        log.info("Creating PT workout");
        List<WorkoutExerciseForm> populatedExercises = normalizeExercises(form.getExercises());
        form.setExercises(populatedExercises);

        if (populatedExercises.isEmpty()) {
            result.reject("exercises.empty", "At least one exercise is required");
        }

        validateExercises(form, result);

        if (result.hasErrors()) {
            ensureExerciseRow(form);
            return "pt/workouts/form";
        }

        User ptUser = securityHelper.getCurrentUserDetails().getUser();
        workoutService.createWorkout(ptUser, form);
        redirectAttributes.addFlashAttribute("success", "Workout created");
        return "redirect:/pt/workouts";
    }

    @GetMapping("/{id}")
    public String workoutDetail(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        log.info("Showing PT workout detail for ID: {}", id);
        User ptUser = securityHelper.getCurrentUserDetails().getUser();
        Optional<Workout> workoutOpt = workoutService.getWorkout(id, ptUser);

        if (workoutOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Workout not found.");
            return "redirect:/pt/workouts";
        }

        model.addAttribute("workout", workoutOpt.get());
        model.addAttribute("activeClients",
                clientRecordService.getClientsForPT(ptUser).stream()
                        .filter(cr -> cr.getStatus() == ClientStatus.ACTIVE)
                        .toList());
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("totalSets", workoutOpt.get().getExercises().stream()
                .mapToInt(exercise -> exercise.getSetsCount() != null ? exercise.getSetsCount() : 0)
                .sum());
        return "pt/workouts/detail";
    }

    @PostMapping("/{id}/assign")
    public String assignWorkout(@PathVariable Long id,
                                @RequestParam(name = "clientRecordIds", required = false) List<Long> clientRecordIds,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate assignedDate,
                                RedirectAttributes redirectAttributes) {
        log.info("Assigning PT workout to clients");
        User ptUser = securityHelper.getCurrentUserDetails().getUser();
        try {
            workoutService.assignWorkout(id, clientRecordIds, assignedDate, ptUser);
            redirectAttributes.addFlashAttribute("success",
                    clientRecordIds.size() == 1 ? "Workout assigned" : "Workout assigned to " + clientRecordIds.size() + " clients");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/pt/workouts/" + id;
    }

    @PostMapping("/{id}/delete")
    public String deleteWorkout(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.info("Deleting PT workout with ID: {}", id);
        User ptUser = securityHelper.getCurrentUserDetails().getUser();

        try {
            workoutService.deleteWorkout(id, ptUser);
            redirectAttributes.addFlashAttribute("success", "Workout deleted");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }

        return "redirect:/pt/workouts";
    }

    @PostMapping("/assignments/{assignmentId}/delete")
    public String unassignWorkout(@PathVariable Long assignmentId, RedirectAttributes redirectAttributes) {
        log.info("Unassigning PT workout with ID: {}", assignmentId);
        User ptUser = securityHelper.getCurrentUserDetails().getUser();

        try {
            Long clientRecordId = workoutService.unassignWorkout(assignmentId, ptUser).getClientRecord().getId();
            redirectAttributes.addFlashAttribute("success", "Workout unassigned");
            return "redirect:/pt/clients/" + clientRecordId;
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/pt/clients";
        }
    }

    private List<WorkoutExerciseForm> normalizeExercises(List<WorkoutExerciseForm> exercises) {
        if (exercises == null) {
            return new ArrayList<>();
        }

        return exercises.stream()
                .filter(this::hasExerciseContent)
                .toList();
    }

    private boolean hasExerciseContent(WorkoutExerciseForm exercise) {
        if (exercise == null) {
            return false;
        }

        return (exercise.getExerciseName() != null && !exercise.getExerciseName().isBlank())
                || exercise.getSetsCount() != null
                || (exercise.getRepsText() != null && !exercise.getRepsText().isBlank())
                || (exercise.getNotes() != null && !exercise.getNotes().isBlank());
    }

    private void validateExercises(WorkoutForm form, BindingResult result) {
        for (int i = 0; i < form.getExercises().size(); i++) {
            WorkoutExerciseForm exercise = form.getExercises().get(i);
            for (ConstraintViolation<WorkoutExerciseForm> violation : validator.validate(exercise)) {
                String field = "exercises[" + i + "]." + violation.getPropertyPath();
                if (result.getFieldError(field) == null) {
                    result.addError(new FieldError("workoutForm", field, violation.getMessage()));
                }
            }
        }
    }

    private void ensureExerciseRow(WorkoutForm form) {
        if (form.getExercises() == null || form.getExercises().isEmpty()) {
            form.setExercises(new ArrayList<>(List.of(new WorkoutExerciseForm())));
        }
    }
}

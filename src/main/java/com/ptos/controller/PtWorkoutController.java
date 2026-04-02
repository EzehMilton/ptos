package com.ptos.controller;

import com.ptos.domain.ClientStatus;
import com.ptos.domain.User;
import com.ptos.domain.Workout;
import com.ptos.dto.WorkoutExerciseForm;
import com.ptos.dto.WorkoutForm;
import com.ptos.security.SecurityHelper;
import com.ptos.service.ClientRecordService;
import com.ptos.service.WorkoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.IntStream;

@Controller
@RequestMapping("/pt/workouts")
@RequiredArgsConstructor
public class PtWorkoutController {

    private final WorkoutService workoutService;
    private final ClientRecordService clientRecordService;
    private final SecurityHelper securityHelper;

    @GetMapping
    public String listWorkouts(Model model) {
        User ptUser = securityHelper.getCurrentUserDetails().getUser();
        model.addAttribute("workouts", workoutService.getWorkoutsForPT(ptUser));
        return "pt/workouts/list";
    }

    @GetMapping("/new")
    public String newWorkoutForm(Model model) {
        WorkoutForm form = new WorkoutForm();
        IntStream.range(0, 3).forEach(i -> form.getExercises().add(new WorkoutExerciseForm()));
        model.addAttribute("workoutForm", form);
        return "pt/workouts/form";
    }

    @PostMapping
    public String createWorkout(@Valid @ModelAttribute("workoutForm") WorkoutForm form,
                                BindingResult result,
                                RedirectAttributes redirectAttributes) {
        if (form.getExercises() == null || form.getExercises().stream()
                .noneMatch(e -> e.getExerciseName() != null && !e.getExerciseName().isBlank())) {
            result.reject("exercises.empty", "At least one exercise is required");
        }

        if (result.hasErrors()) {
            return "pt/workouts/form";
        }

        User ptUser = securityHelper.getCurrentUserDetails().getUser();
        workoutService.createWorkout(ptUser, form);
        redirectAttributes.addFlashAttribute("success", "Workout created");
        return "redirect:/pt/workouts";
    }

    @GetMapping("/{id}")
    public String workoutDetail(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
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
        return "pt/workouts/detail";
    }

    @PostMapping("/{id}/assign")
    public String assignWorkout(@PathVariable Long id,
                                @RequestParam Long clientRecordId,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate assignedDate,
                                RedirectAttributes redirectAttributes) {
        User ptUser = securityHelper.getCurrentUserDetails().getUser();
        workoutService.assignWorkout(id, clientRecordId, assignedDate, ptUser);
        redirectAttributes.addFlashAttribute("success", "Workout assigned");
        return "redirect:/pt/clients/" + clientRecordId;
    }
}

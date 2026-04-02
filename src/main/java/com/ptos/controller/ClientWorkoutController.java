package com.ptos.controller;

import com.ptos.domain.AssignmentStatus;
import com.ptos.domain.User;
import com.ptos.domain.WorkoutAssignment;
import com.ptos.security.SecurityHelper;
import com.ptos.service.WorkoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Duration;
import java.util.List;

@Controller
@RequestMapping("/client/workouts")
@RequiredArgsConstructor
public class ClientWorkoutController {

    private final WorkoutService workoutService;
    private final SecurityHelper securityHelper;

    @GetMapping
    public String listWorkouts(Model model) {
        User clientUser = securityHelper.getCurrentUserDetails().getUser();
        List<WorkoutAssignment> assignments = workoutService.getAssignmentsForClient(clientUser);

        model.addAttribute("upcomingAssignments", filterAssignments(assignments, AssignmentStatus.ASSIGNED));
        model.addAttribute("inProgressAssignments", filterAssignments(assignments, AssignmentStatus.IN_PROGRESS));
        model.addAttribute("completedAssignments", filterAssignments(assignments, AssignmentStatus.COMPLETED));
        model.addAttribute("hasAssignments", !assignments.isEmpty());
        return "client/workouts/list";
    }

    @GetMapping("/{assignmentId}")
    public String workoutDetail(@PathVariable Long assignmentId,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        User clientUser = securityHelper.getCurrentUserDetails().getUser();
        return workoutService.getAssignment(assignmentId, clientUser)
                .map(assignment -> {
                    model.addAttribute("assignment", assignment);
                    model.addAttribute("workout", assignment.getWorkout());
                    model.addAttribute("durationText", formatDuration(assignment));
                    return "client/workouts/detail";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Workout assignment not found.");
                    return "redirect:/client/workouts";
                });
    }

    @PostMapping("/{assignmentId}/start")
    public String startWorkout(@PathVariable Long assignmentId, RedirectAttributes redirectAttributes) {
        User clientUser = securityHelper.getCurrentUserDetails().getUser();
        try {
            workoutService.startWorkout(assignmentId, clientUser);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Workout assignment not found.");
            return "redirect:/client/workouts";
        }
        return "redirect:/client/workouts/" + assignmentId;
    }

    @PostMapping("/{assignmentId}/complete")
    public String completeWorkout(@PathVariable Long assignmentId,
                                  @RequestParam(required = false) String completionNotes,
                                  RedirectAttributes redirectAttributes) {
        User clientUser = securityHelper.getCurrentUserDetails().getUser();
        try {
            workoutService.completeWorkout(assignmentId, clientUser, completionNotes);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Workout assignment not found.");
            return "redirect:/client/workouts";
        }
        redirectAttributes.addFlashAttribute("success", "Workout completed! Great work.");
        return "redirect:/client/workouts/" + assignmentId;
    }

    private List<WorkoutAssignment> filterAssignments(List<WorkoutAssignment> assignments, AssignmentStatus status) {
        return assignments.stream()
                .filter(assignment -> assignment.getStatus() == status)
                .toList();
    }

    private String formatDuration(WorkoutAssignment assignment) {
        if (assignment.getStartedAt() == null || assignment.getCompletedAt() == null) {
            return null;
        }

        Duration duration = Duration.between(assignment.getStartedAt(), assignment.getCompletedAt());
        long totalMinutes = duration.toMinutes();
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;

        if (hours == 0) {
            return totalMinutes + " min";
        }
        if (minutes == 0) {
            return hours + "h";
        }
        return hours + "h " + minutes + "m";
    }
}

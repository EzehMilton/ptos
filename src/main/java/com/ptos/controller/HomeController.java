package com.ptos.controller;

import com.ptos.domain.AssignmentStatus;
import com.ptos.domain.CheckIn;
import com.ptos.domain.ClientProfile;
import com.ptos.domain.ClientRecord;
import com.ptos.domain.MealComplianceLog;
import com.ptos.domain.MealPlan;
import com.ptos.domain.User;
import com.ptos.domain.WorkoutAssignment;
import com.ptos.dto.Milestone;
import com.ptos.security.SecurityHelper;
import com.ptos.service.CheckInService;
import com.ptos.service.ClientProfileService;
import com.ptos.service.ClientRecordService;
import com.ptos.service.MilestoneService;
import com.ptos.service.NutritionService;
import com.ptos.service.WorkoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final SecurityHelper securityHelper;
    private final ClientProfileService clientProfileService;
    private final WorkoutService workoutService;
    private final CheckInService checkInService;
    private final ClientRecordService clientRecordService;
    private final MilestoneService milestoneService;
    private final NutritionService nutritionService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/client/home")
    public String clientHome(Model model) {
        Long userId = securityHelper.getCurrentUserId();
        User clientUser = securityHelper.getCurrentUserDetails().getUser();
        Optional<ClientProfile> profileOpt = clientProfileService.getProfileForUser(userId);
        if (profileOpt.isEmpty() || !profileOpt.get().isOnboardingComplete()) {
            return "redirect:/client/onboarding";
        }

        String fullName = clientUser.getFullName();
        model.addAttribute("fullName", fullName);

        List<WorkoutAssignment> todaysWorkouts = workoutService.getAssignmentsForClient(clientUser).stream()
                .filter(assignment -> assignment.getStatus() == AssignmentStatus.ASSIGNED
                        || assignment.getStatus() == AssignmentStatus.IN_PROGRESS)
                .toList();
        model.addAttribute("todaysWorkouts", todaysWorkouts);

        Optional<CheckIn> latestCheckIn = checkInService.getLatestCheckInForClient(clientUser);
        boolean hasRecentCheckIn = latestCheckIn
                .map(checkIn -> !checkIn.getSubmittedAt().isBefore(LocalDateTime.now().minusDays(7)))
                .orElse(false);
        model.addAttribute("hasRecentCheckIn", hasRecentCheckIn);
        model.addAttribute("latestCheckIn", latestCheckIn.orElse(null));

        Optional<ClientRecord> clientRecordOpt = clientRecordService.getClientRecord(clientUser);
        clientRecordOpt.ifPresent(record -> {
            List<Milestone> milestones = milestoneService.getMilestones(record, clientUser);
            List<Milestone> achievedMilestones = milestones.stream()
                    .filter(milestone -> milestone.getAchievedDate() != null)
                    .toList();
            model.addAttribute("achievedMilestoneCount", achievedMilestones.size());
            model.addAttribute("recentAchievedMilestones", achievedMilestones.stream().limit(3).toList());
        });

        MealPlan activeMealPlan = clientRecordOpt.flatMap(nutritionService::getActiveMealPlan).orElse(null);
        MealComplianceLog todayMealLog = clientRecordOpt
                .flatMap(record -> nutritionService.getComplianceLog(record, java.time.LocalDate.now()))
                .orElse(null);
        model.addAttribute("activeMealPlan", activeMealPlan);
        model.addAttribute("todayMealLog", todayMealLog);

        ClientProfile profile = profileOpt.get();
        model.addAttribute("hasProfile", true);
        model.addAttribute("completion", clientProfileService.computeCompletion(profile));

        if (!model.containsAttribute("achievedMilestoneCount")) {
            model.addAttribute("achievedMilestoneCount", 0);
            model.addAttribute("recentAchievedMilestones", List.of());
        }

        return "client/home";
    }
}

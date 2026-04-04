package com.ptos.controller;

import com.ptos.domain.AssignmentStatus;
import com.ptos.domain.CheckIn;
import com.ptos.domain.CheckInPhoto;
import com.ptos.domain.CheckInStatus;
import com.ptos.domain.GoalType;
import com.ptos.domain.PhotoType;
import com.ptos.domain.User;
import com.ptos.domain.WorkoutAssignment;
import com.ptos.dto.CheckInFeedbackForm;
import com.ptos.dto.ClientDetailView;
import com.ptos.integration.FileStorageGateway;
import com.ptos.repository.WorkoutAssignmentRepository;
import com.ptos.security.SecurityHelper;
import com.ptos.service.CheckInService;
import com.ptos.service.ClientRecordService;
import com.ptos.service.InsightService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/pt/checkins")
@RequiredArgsConstructor
public class PtCheckInController {

    private final CheckInService checkInService;
    private final ClientRecordService clientRecordService;
    private final InsightService insightService;
    private final FileStorageGateway fileStorageGateway;
    private final WorkoutAssignmentRepository workoutAssignmentRepository;
    private final SecurityHelper securityHelper;

    @GetMapping
    public String listCheckIns(@RequestParam(name = "filter", required = false, defaultValue = "pending") String filter,
                               Model model) {
        User ptUser = securityHelper.getCurrentUserDetails().getUser();
        String normalizedFilter = normalizeFilter(filter);
        List<CheckIn> pendingCheckIns = checkInService.getPendingCheckIns(ptUser);
        List<CheckIn> reviewedCheckIns = checkInService.getReviewedCheckIns(ptUser);
        List<CheckIn> filteredCheckIns = switch (normalizedFilter) {
            case "reviewed" -> reviewedCheckIns;
            case "all" -> checkInService.getAllCheckInsForPT(ptUser);
            default -> pendingCheckIns;
        };

        model.addAttribute("selectedFilter", normalizedFilter);
        model.addAttribute("checkIns", filteredCheckIns);
        model.addAttribute("pendingCount", pendingCheckIns.size());
        model.addAttribute("reviewedCount", reviewedCheckIns.size());
        model.addAttribute("allCount", pendingCheckIns.size() + reviewedCheckIns.size());
        return "pt/checkins/list";
    }

    @GetMapping("/{id}")
    public String reviewCheckIn(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        User ptUser = securityHelper.getCurrentUserDetails().getUser();
        Optional<CheckIn> checkInOpt = checkInService.getCheckInForPT(id, ptUser);
        if (checkInOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Check-in not found.");
            return "redirect:/pt/checkins";
        }

        CheckIn checkIn = checkInOpt.get();
        ClientDetailView clientDetail = clientRecordService.getClientDetail(checkIn.getClientRecord());
        Optional<CheckIn> previousCheckIn = checkInService.getPreviousCheckIn(checkIn);

        model.addAttribute("checkIn", checkIn);
        model.addAttribute("clientDetail", clientDetail);
        model.addAttribute("previousCheckIn", previousCheckIn.orElse(null));
        model.addAttribute("weightComparison", buildWeightComparison(checkIn, previousCheckIn.orElse(null), clientDetail));
        model.addAttribute("checkInInsight", insightService.getCheckInInsight(checkIn.getClientRecord(), checkIn));
        model.addAttribute("submittedDateLabel", checkIn.getSubmittedAt().toLocalDate());
        model.addAttribute("goalLabel", formatGoalLabel(clientDetail.getGoalType()));
        model.addAttribute("weightDeltaText", buildWeightDeltaText(checkIn, previousCheckIn.orElse(null)));
        model.addAttribute("moodDeltaText", buildScoreDeltaText(checkIn.getMoodScore(),
                previousCheckIn.map(CheckIn::getMoodScore).orElse(null), "vs last", "Same as last"));
        model.addAttribute("energyDeltaText", buildScoreDeltaText(checkIn.getEnergyScore(),
                previousCheckIn.map(CheckIn::getEnergyScore).orElse(null), "vs last", "Same as last"));
        model.addAttribute("sleepDeltaText", buildScoreDeltaText(checkIn.getSleepScore(),
                previousCheckIn.map(CheckIn::getSleepScore).orElse(null), "vs last", "Same as last"));
        model.addAttribute("workoutSummary", buildWorkoutSummary(checkIn, previousCheckIn.orElse(null)));
        addPhotoAttributes(model, checkIn, previousCheckIn.orElse(null));

        if (checkIn.getStatus() == CheckInStatus.PENDING_REVIEW) {
            model.addAttribute("feedbackForm", new CheckInFeedbackForm());
        }

        return "pt/checkins/review";
    }

    @PostMapping("/{id}/feedback")
    public String submitFeedback(@PathVariable Long id,
                                 @Valid @ModelAttribute("feedbackForm") CheckInFeedbackForm form,
                                 BindingResult result,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        User ptUser = securityHelper.getCurrentUserDetails().getUser();
        Optional<CheckIn> checkInOpt = checkInService.getCheckInForPT(id, ptUser);
        if (checkInOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Check-in not found.");
            return "redirect:/pt/checkins";
        }

        CheckIn checkIn = checkInOpt.get();
        if (checkIn.getStatus() != CheckInStatus.PENDING_REVIEW) {
            redirectAttributes.addFlashAttribute("error", "This check-in has already been reviewed.");
            return "redirect:/pt/checkins/" + id;
        }
        if (result.hasErrors()) {
            ClientDetailView clientDetail = clientRecordService.getClientDetail(checkIn.getClientRecord());
            Optional<CheckIn> previousCheckIn = checkInService.getPreviousCheckIn(checkIn);
            model.addAttribute("checkIn", checkIn);
            model.addAttribute("clientDetail", clientDetail);
            model.addAttribute("previousCheckIn", previousCheckIn.orElse(null));
            model.addAttribute("weightComparison", buildWeightComparison(checkIn, previousCheckIn.orElse(null), clientDetail));
            model.addAttribute("checkInInsight", insightService.getCheckInInsight(checkIn.getClientRecord(), checkIn));
            model.addAttribute("submittedDateLabel", checkIn.getSubmittedAt().toLocalDate());
            model.addAttribute("goalLabel", formatGoalLabel(clientDetail.getGoalType()));
            model.addAttribute("weightDeltaText", buildWeightDeltaText(checkIn, previousCheckIn.orElse(null)));
            model.addAttribute("moodDeltaText", buildScoreDeltaText(checkIn.getMoodScore(),
                    previousCheckIn.map(CheckIn::getMoodScore).orElse(null), "vs last", "Same as last"));
            model.addAttribute("energyDeltaText", buildScoreDeltaText(checkIn.getEnergyScore(),
                    previousCheckIn.map(CheckIn::getEnergyScore).orElse(null), "vs last", "Same as last"));
            model.addAttribute("sleepDeltaText", buildScoreDeltaText(checkIn.getSleepScore(),
                    previousCheckIn.map(CheckIn::getSleepScore).orElse(null), "vs last", "Same as last"));
            model.addAttribute("workoutSummary", buildWorkoutSummary(checkIn, previousCheckIn.orElse(null)));
            addPhotoAttributes(model, checkIn, previousCheckIn.orElse(null));
            return "pt/checkins/review";
        }

        try {
            checkInService.submitFeedback(id, ptUser, form.getFeedbackText());
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", "This check-in has already been reviewed.");
            return "redirect:/pt/checkins/" + id;
        }
        redirectAttributes.addFlashAttribute(
                "success",
                "Feedback sent to " + checkIn.getClientRecord().getClientUser().getFullName()
        );
        return "redirect:/pt/checkins";
    }

    private String normalizeFilter(String filter) {
        if ("reviewed".equalsIgnoreCase(filter)) {
            return "reviewed";
        }
        if ("all".equalsIgnoreCase(filter)) {
            return "all";
        }
        return "pending";
    }

    private WeightComparisonView buildWeightComparison(CheckIn currentCheckIn,
                                                       CheckIn previousCheckIn,
                                                       ClientDetailView clientDetail) {
        if (previousCheckIn == null) {
            return null;
        }

        double delta = currentCheckIn.getCurrentWeightKg() - previousCheckIn.getCurrentWeightKg();
        if (delta == 0) {
            return new WeightComparisonView("No change", "comparison-neutral");
        }

        String direction = delta < 0 ? "↓" : "↑";
        String changeText = direction + " " + String.format("%.1f", Math.abs(delta)) + " kg "
                + (delta < 0 ? "lost" : "gained");

        if (clientDetail.getGoalType() == GoalType.WEIGHT_LOSS) {
            return new WeightComparisonView(changeText, delta < 0 ? "comparison-good" : "comparison-bad");
        }
        if (clientDetail.getGoalType() == GoalType.MUSCLE_GAIN) {
            return new WeightComparisonView(changeText, delta > 0 ? "comparison-good" : "comparison-bad");
        }
        return new WeightComparisonView(changeText, "comparison-neutral");
    }

    public record WeightComparisonView(String text, String cssClass) {
    }

    public record WorkoutReviewSummary(long completedCount, long assignedCount, String detailText, String cssClass) {
    }

    private void addPhotoAttributes(Model model, CheckIn checkIn, CheckIn previousCheckIn) {
        List<CheckInPhoto> previousPhotos = previousCheckIn == null
                ? List.of()
                : checkInService.getPreviousCheckInPhotos(checkIn.getClientRecord(), checkIn.getId());
        model.addAttribute("photoTypes", PhotoType.values());
        model.addAttribute("currentPhotoUrls", buildPhotoUrls(checkInService.getPhotosForCheckIn(checkIn.getId())));
        model.addAttribute("previousPhotoUrls", previousPhotos.isEmpty() ? Map.of() : buildPhotoUrls(previousPhotos));
    }

    private Map<PhotoType, String> buildPhotoUrls(List<CheckInPhoto> photos) {
        Map<PhotoType, String> urls = new EnumMap<>(PhotoType.class);
        for (CheckInPhoto photo : photos) {
            urls.put(photo.getPhotoType(), fileStorageGateway.getUrl(photo.getStorageKey()));
        }
        return urls;
    }

    private String formatGoalLabel(GoalType goalType) {
        if (goalType == null) {
            return "No goal set";
        }
        return switch (goalType) {
            case WEIGHT_LOSS -> "Weight loss";
            case MUSCLE_GAIN -> "Muscle gain";
            case STRENGTH -> "Strength";
            case ENDURANCE -> "Endurance";
            case GENERAL_FITNESS -> "General fitness";
        };
    }

    private String buildWeightDeltaText(CheckIn currentCheckIn, CheckIn previousCheckIn) {
        if (previousCheckIn == null) {
            return "First logged weight";
        }
        double delta = currentCheckIn.getCurrentWeightKg() - previousCheckIn.getCurrentWeightKg();
        if (delta == 0) {
            return "No change";
        }
        return (delta > 0 ? "+" : "") + String.format("%.1f", delta) + " kg vs last";
    }

    private String buildScoreDeltaText(Integer current, Integer previous, String comparisonLabel, String neutralLabel) {
        if (current == null) {
            return "Not logged";
        }
        if (previous == null) {
            return "First check-in";
        }
        int delta = current - previous;
        if (delta == 0) {
            return neutralLabel;
        }
        return (delta > 0 ? "+" : "") + delta + " " + comparisonLabel;
    }

    private WorkoutReviewSummary buildWorkoutSummary(CheckIn currentCheckIn, CheckIn previousCheckIn) {
        LocalDate windowEnd = currentCheckIn.getSubmittedAt().toLocalDate();
        LocalDate windowStart = previousCheckIn != null
                ? previousCheckIn.getSubmittedAt().toLocalDate().plusDays(1)
                : windowEnd.minusDays(6);

        List<WorkoutAssignment> assignments = workoutAssignmentRepository
                .findByClientRecordOrderByAssignedDateDesc(currentCheckIn.getClientRecord()).stream()
                .filter(assignment -> !assignment.getAssignedDate().isBefore(windowStart))
                .filter(assignment -> !assignment.getAssignedDate().isAfter(windowEnd))
                .toList();

        long completedCount = assignments.stream()
                .filter(assignment -> assignment.getStatus() == AssignmentStatus.COMPLETED)
                .count();
        long assignedCount = assignments.size();

        if (assignedCount == 0) {
            return new WorkoutReviewSummary(0, 0, "No workouts assigned", "checkin-review-stat-note-neutral");
        }
        if (completedCount == assignedCount) {
            return new WorkoutReviewSummary(completedCount, assignedCount, "All completed", "checkin-review-stat-note-good");
        }
        if (completedCount == 0) {
            return new WorkoutReviewSummary(0, assignedCount, "Missed", "checkin-review-stat-note-bad");
        }
        return new WorkoutReviewSummary(completedCount, assignedCount,
                completedCount + " completed", "checkin-review-stat-note-neutral");
    }
}

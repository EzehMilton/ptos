package com.ptos.controller;

import com.ptos.domain.CheckIn;
import com.ptos.domain.CheckInStatus;
import com.ptos.domain.GoalType;
import com.ptos.domain.User;
import com.ptos.dto.CheckInFeedbackForm;
import com.ptos.dto.ClientDetailView;
import com.ptos.security.SecurityHelper;
import com.ptos.service.CheckInService;
import com.ptos.service.ClientRecordService;
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

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/pt/checkins")
@RequiredArgsConstructor
public class PtCheckInController {

    private final CheckInService checkInService;
    private final ClientRecordService clientRecordService;
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
}

package com.ptos.controller;

import com.ptos.domain.ClientRecord;
import com.ptos.domain.MealComplianceLog;
import com.ptos.domain.MealPlan;
import com.ptos.domain.User;
import com.ptos.dto.ClientDetailView;
import com.ptos.dto.MealPlanForm;
import com.ptos.security.SecurityHelper;
import com.ptos.service.ClientRecordService;
import com.ptos.service.NutritionService;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/pt/clients/{clientRecordId}/nutrition")
@RequiredArgsConstructor
public class PtNutritionController {

    private final NutritionService nutritionService;
    private final ClientRecordService clientRecordService;
    private final SecurityHelper securityHelper;

    @GetMapping
    public String viewNutrition(@PathVariable Long clientRecordId, Model model, RedirectAttributes redirectAttributes) {
        User ptUser = securityHelper.getCurrentUserDetails().getUser();
        Optional<ClientRecord> clientRecordOpt = clientRecordService.getClientRecord(clientRecordId, ptUser);
        if (clientRecordOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Client not found.");
            return "redirect:/pt/clients";
        }

        ClientRecord clientRecord = clientRecordOpt.get();
        ClientDetailView detail = clientRecordService.getClientDetail(clientRecord);
        List<MealComplianceLog> complianceLogs = nutritionService.getComplianceLogs(clientRecord, 14);
        MealPlan mealPlan = nutritionService.getActiveMealPlan(clientRecord).orElse(null);

        model.addAttribute("detail", detail);
        model.addAttribute("mealPlan", mealPlan);
        model.addAttribute("complianceLogs", complianceLogs);
        model.addAttribute("complianceRate", nutritionService.getComplianceRate(clientRecord, 14));
        model.addAttribute("mealBlocks", buildMealBlocks(mealPlan));
        model.addAttribute("nutritionSummary", buildNutritionSummary(complianceLogs, mealPlan));
        return "pt/nutrition/view";
    }

    @GetMapping("/new")
    public String newMealPlan(@PathVariable Long clientRecordId, Model model, RedirectAttributes redirectAttributes) {
        User ptUser = securityHelper.getCurrentUserDetails().getUser();
        Optional<ClientRecord> clientRecordOpt = clientRecordService.getClientRecord(clientRecordId, ptUser);
        if (clientRecordOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Client not found.");
            return "redirect:/pt/clients";
        }

        if (!model.containsAttribute("mealPlanForm")) {
            model.addAttribute("mealPlanForm", new MealPlanForm());
        }
        model.addAttribute("detail", clientRecordService.getClientDetail(clientRecordOpt.get()));
        model.addAttribute("isEdit", false);
        return "pt/nutrition/form";
    }

    @PostMapping
    public String createMealPlan(@PathVariable Long clientRecordId,
                                 @Valid @ModelAttribute("mealPlanForm") MealPlanForm form,
                                 BindingResult result,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        User ptUser = securityHelper.getCurrentUserDetails().getUser();
        Optional<ClientRecord> clientRecordOpt = clientRecordService.getClientRecord(clientRecordId, ptUser);
        if (clientRecordOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Client not found.");
            return "redirect:/pt/clients";
        }

        if (result.hasErrors()) {
            model.addAttribute("detail", clientRecordService.getClientDetail(clientRecordOpt.get()));
            model.addAttribute("isEdit", false);
            return "pt/nutrition/form";
        }

        nutritionService.createMealPlan(ptUser, clientRecordId, form);
        redirectAttributes.addFlashAttribute("success", "Meal plan created");
        return "redirect:/pt/clients/" + clientRecordId + "/nutrition";
    }

    @GetMapping("/edit")
    public String editMealPlan(@PathVariable Long clientRecordId, Model model, RedirectAttributes redirectAttributes) {
        User ptUser = securityHelper.getCurrentUserDetails().getUser();
        Optional<ClientRecord> clientRecordOpt = clientRecordService.getClientRecord(clientRecordId, ptUser);
        if (clientRecordOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Client not found.");
            return "redirect:/pt/clients";
        }

        ClientRecord clientRecord = clientRecordOpt.get();
        Optional<MealPlan> mealPlanOpt = nutritionService.getActiveMealPlan(clientRecord);
        if (mealPlanOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "No active meal plan to edit.");
            return "redirect:/pt/clients/" + clientRecordId + "/nutrition";
        }

        if (!model.containsAttribute("mealPlanForm")) {
            MealPlan mealPlan = mealPlanOpt.get();
            MealPlanForm form = new MealPlanForm();
            form.setTitle(mealPlan.getTitle());
            form.setOverview(mealPlan.getOverview());
            form.setDailyGuidance(mealPlan.getDailyGuidance());
            model.addAttribute("mealPlanForm", form);
        }
        model.addAttribute("detail", clientRecordService.getClientDetail(clientRecord));
        model.addAttribute("isEdit", true);
        return "pt/nutrition/form";
    }

    @PostMapping("/update")
    public String updateMealPlan(@PathVariable Long clientRecordId,
                                 @Valid @ModelAttribute("mealPlanForm") MealPlanForm form,
                                 BindingResult result,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        User ptUser = securityHelper.getCurrentUserDetails().getUser();
        Optional<ClientRecord> clientRecordOpt = clientRecordService.getClientRecord(clientRecordId, ptUser);
        if (clientRecordOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Client not found.");
            return "redirect:/pt/clients";
        }

        ClientRecord clientRecord = clientRecordOpt.get();
        Optional<MealPlan> mealPlanOpt = nutritionService.getActiveMealPlan(clientRecord);
        if (mealPlanOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "No active meal plan to edit.");
            return "redirect:/pt/clients/" + clientRecordId + "/nutrition";
        }

        if (result.hasErrors()) {
            model.addAttribute("detail", clientRecordService.getClientDetail(clientRecord));
            model.addAttribute("isEdit", true);
            return "pt/nutrition/form";
        }

        nutritionService.updateMealPlan(ptUser, mealPlanOpt.get().getId(), form);
        redirectAttributes.addFlashAttribute("success", "Meal plan updated");
        return "redirect:/pt/clients/" + clientRecordId + "/nutrition";
    }

    @PostMapping("/delete")
    public String deleteMealPlan(@PathVariable Long clientRecordId, RedirectAttributes redirectAttributes) {
        User ptUser = securityHelper.getCurrentUserDetails().getUser();
        Optional<ClientRecord> clientRecordOpt = clientRecordService.getClientRecord(clientRecordId, ptUser);
        if (clientRecordOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Client not found.");
            return "redirect:/pt/clients";
        }

        Optional<MealPlan> mealPlanOpt = nutritionService.getActiveMealPlan(clientRecordOpt.get());
        if (mealPlanOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "No active meal plan to delete.");
            return "redirect:/pt/clients/" + clientRecordId + "/nutrition";
        }

        nutritionService.deleteMealPlan(ptUser, mealPlanOpt.get().getId());
        redirectAttributes.addFlashAttribute("success", "Meal plan deleted");
        return "redirect:/pt/clients/" + clientRecordId + "/nutrition";
    }

    private List<MealBlockView> buildMealBlocks(MealPlan mealPlan) {
        List<MealBlockView> blocks = new ArrayList<>();
        if (mealPlan == null || mealPlan.getDailyGuidance() == null || mealPlan.getDailyGuidance().isBlank()) {
            return blocks;
        }

        if (!mealPlan.getDailyGuidance().contains("\n\n") && mealPlan.getDailyGuidance().contains("\n")) {
            return buildLineByLineBlocks(mealPlan.getDailyGuidance());
        }

        String[] rawBlocks = mealPlan.getDailyGuidance().trim().split("(\\r?\\n){2,}");
        for (String rawBlock : rawBlocks) {
            String block = rawBlock.trim();
            if (block.isBlank()) {
                continue;
            }

            String[] lines = block.split("\\r?\\n");
            String label = "GUIDANCE";
            String headline = lines[0].trim();
            String supporting = lines.length > 1
                    ? String.join(" ", java.util.Arrays.copyOfRange(lines, 1, lines.length)).trim()
                    : "";

            if (headline.contains(":")) {
                String[] parts = headline.split(":", 2);
                label = parts[0].trim().isBlank() ? "GUIDANCE" : parts[0].trim().toUpperCase();
                headline = parts[1].trim().isBlank() ? parts[0].trim() : parts[1].trim();
            }

            blocks.add(new MealBlockView(label, headline, supporting));
        }
        return blocks;
    }

    private List<MealBlockView> buildLineByLineBlocks(String guidance) {
        List<MealBlockView> blocks = new ArrayList<>();
        int guidanceIndex = 1;
        for (String inlineLine : guidance.split("\\r?\\n")) {
            String entry = inlineLine.trim();
            if (entry.isBlank()) {
                continue;
            }
            String inlineLabel = "GUIDANCE " + guidanceIndex++;
            String inlineHeadline = entry;
            if (entry.contains(":")) {
                String[] parts = entry.split(":", 2);
                inlineLabel = parts[0].trim().isBlank() ? inlineLabel : parts[0].trim().toUpperCase();
                inlineHeadline = parts[1].trim().isBlank() ? parts[0].trim() : parts[1].trim();
            }
            blocks.add(new MealBlockView(inlineLabel, inlineHeadline, ""));
        }
        return blocks;
    }

    private NutritionSummaryView buildNutritionSummary(List<MealComplianceLog> complianceLogs, MealPlan mealPlan) {
        int followedCount = 0;
        int partialCount = 0;
        int notFollowedCount = 0;
        for (MealComplianceLog log : complianceLogs) {
            switch (log.getCompliance()) {
                case FOLLOWED -> followedCount++;
                case PARTIALLY_FOLLOWED -> partialCount++;
                case NOT_FOLLOWED -> notFollowedCount++;
            }
        }

        String trendLabel = "Holding steady";
        String trendCopy = complianceLogs.isEmpty()
                ? "No compliance entries yet. Once the client starts logging, progress trends will appear here."
                : "Compliance has been mixed recently. Keep the current plan under review.";

        if (complianceLogs.size() >= 2) {
            int recentScore = complianceScore(complianceLogs.get(0).getCompliance());
            int previousScore = complianceScore(complianceLogs.get(Math.min(1, complianceLogs.size() - 1)).getCompliance());
            if (recentScore > previousScore) {
                trendLabel = "Trending upward";
                trendCopy = "Recent compliance is improving compared with the previous entry.";
            } else if (recentScore < previousScore) {
                trendLabel = "Needs attention";
                trendCopy = "Recent compliance dropped versus the previous entry. Consider tightening the plan or checking in.";
            } else if (recentScore == 2) {
                trendLabel = "Trending upward";
                trendCopy = "The client has stayed consistent across the most recent compliance entries.";
            }
        } else if (followedCount > 0 && partialCount == 0 && notFollowedCount == 0) {
            trendLabel = "Trending upward";
            trendCopy = "Early compliance is strong. The client is following the plan so far.";
        }

        String updatedLabel = mealPlan == null || mealPlan.getUpdatedAt() == null
                ? null
                : "Updated " + mealPlan.getUpdatedAt().format(DateTimeFormatter.ofPattern("d MMM yyyy"));

        return new NutritionSummaryView(
                complianceLogs.size(),
                followedCount,
                partialCount,
                notFollowedCount,
                trendLabel,
                trendCopy,
                updatedLabel
        );
    }

    private int complianceScore(com.ptos.domain.ComplianceLevel level) {
        return switch (level) {
            case FOLLOWED -> 2;
            case PARTIALLY_FOLLOWED -> 1;
            case NOT_FOLLOWED -> 0;
        };
    }

    private record MealBlockView(String label, String headline, String supportingCopy) {
    }

    private record NutritionSummaryView(
            int totalLogs,
            int followedCount,
            int partialCount,
            int notFollowedCount,
            String trendLabel,
            String trendCopy,
            String updatedLabel
    ) {
    }
}

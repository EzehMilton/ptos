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

        model.addAttribute("detail", detail);
        model.addAttribute("mealPlan", nutritionService.getActiveMealPlan(clientRecord).orElse(null));
        model.addAttribute("complianceLogs", complianceLogs);
        model.addAttribute("complianceRate", nutritionService.getComplianceRate(clientRecord, 14));
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
}

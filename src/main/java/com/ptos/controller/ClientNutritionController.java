package com.ptos.controller;

import com.ptos.domain.ClientRecord;
import com.ptos.domain.ComplianceLevel;
import com.ptos.domain.MealComplianceLog;
import com.ptos.domain.MealPlan;
import com.ptos.domain.User;
import com.ptos.security.SecurityHelper;
import com.ptos.service.ClientRecordService;
import com.ptos.service.NutritionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/client/nutrition")
@RequiredArgsConstructor
public class ClientNutritionController {

    private final NutritionService nutritionService;
    private final ClientRecordService clientRecordService;
    private final SecurityHelper securityHelper;

    @GetMapping
    public String viewNutrition(Model model) {
        User clientUser = securityHelper.getCurrentUserDetails().getUser();
        Optional<ClientRecord> clientRecordOpt = clientRecordService.getClientRecord(clientUser);

        MealPlan mealPlan = null;
        MealComplianceLog todayLog = null;
        List<MealComplianceLog> recentLogs = List.of();

        if (clientRecordOpt.isPresent()) {
            ClientRecord clientRecord = clientRecordOpt.get();
            mealPlan = nutritionService.getActiveMealPlan(clientRecord).orElse(null);
            todayLog = nutritionService.getComplianceLog(clientRecord, LocalDate.now()).orElse(null);
            recentLogs = nutritionService.getComplianceLogs(clientRecord, 14);
        }

        model.addAttribute("mealPlan", mealPlan);
        model.addAttribute("todayLog", todayLog);
        model.addAttribute("recentLogs", recentLogs);
        return "client/nutrition/view";
    }

    @PostMapping("/log")
    public String logCompliance(@RequestParam("compliance") ComplianceLevel compliance,
                                @RequestParam(name = "notes", required = false) String notes,
                                RedirectAttributes redirectAttributes) {
        User clientUser = securityHelper.getCurrentUserDetails().getUser();
        try {
            nutritionService.logCompliance(clientUser, LocalDate.now(), compliance, notes);
            redirectAttributes.addFlashAttribute("success", "Logged for today");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/client/nutrition";
    }
}

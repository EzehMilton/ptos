package com.ptos.controller;

import com.ptos.domain.ClientProfile;
import com.ptos.domain.ClientRecord;
import com.ptos.dto.OnboardingForm;
import com.ptos.security.SecurityHelper;
import com.ptos.service.ClientProfileService;
import com.ptos.service.ClientRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/client/onboarding")
@RequiredArgsConstructor
public class ClientOnboardingController {

    private final ClientProfileService clientProfileService;
    private final ClientRecordService clientRecordService;
    private final SecurityHelper securityHelper;

    @GetMapping
    public String showOnboarding(Model model) {
        Long userId = securityHelper.getCurrentUserId();
        Optional<ClientProfile> profileOpt = clientProfileService.getProfileForUser(userId);
        if (profileOpt.map(ClientProfile::isOnboardingComplete).orElse(false)) {
            return "redirect:/client/home";
        }

        OnboardingForm form = new OnboardingForm();
        profileOpt.ifPresent(profile -> {
            form.setAge(profile.getAge());
            form.setHeightCm(profile.getHeightCm());
            form.setCurrentWeightKg(profile.getCurrentWeightKg());
            form.setGoalType(profile.getGoalType());
            form.setTargetWeightKg(profile.getTargetWeightKg());
            form.setTrainingExperience(profile.getTrainingExperience());
            form.setInjuriesOrConditions(profile.getInjuriesOrConditions());
            form.setDietaryPreferences(profile.getDietaryPreferences());
            form.setGoalNotes(profile.getNotes());
        });

        model.addAttribute("onboardingForm", form);
        addPageContext(model);
        return "client/onboarding";
    }

    @PostMapping
    public String completeOnboarding(@Valid @ModelAttribute("onboardingForm") OnboardingForm form,
                                     BindingResult result,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            addPageContext(model);
            return "client/onboarding";
        }

        clientProfileService.createOrUpdateOnboardingProfile(securityHelper.getCurrentUserId(), form);
        redirectAttributes.addFlashAttribute("success", "Welcome to PTOS! You're all set.");
        return "redirect:/client/home";
    }

    private void addPageContext(Model model) {
        clientRecordService.getClientRecord(securityHelper.getCurrentUserDetails().getUser())
                .map(ClientRecord::getPtUser)
                .ifPresent(ptUser -> model.addAttribute("ptName", ptUser.getFullName()));
    }
}

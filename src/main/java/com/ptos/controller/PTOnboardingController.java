package com.ptos.controller;

import com.ptos.domain.PTProfile;
import com.ptos.dto.PTProfileForm;
import com.ptos.security.SecurityHelper;
import com.ptos.service.PTProfileService;
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
@RequestMapping("/pt/onboarding")
@RequiredArgsConstructor
public class PTOnboardingController {

    private final PTProfileService ptProfileService;
    private final SecurityHelper securityHelper;

    @GetMapping
    public String showOnboarding(Model model) {
        Long userId = securityHelper.getCurrentUserId();
        Optional<PTProfile> profileOpt = ptProfileService.getProfileForUser(userId);
        if (profileOpt.map(PTProfile::isOnboardingComplete).orElse(false)) {
            return "redirect:/pt/dashboard";
        }

        PTProfileForm form = new PTProfileForm();
        profileOpt.ifPresent(profile -> {
            form.setBusinessName(profile.getBusinessName());
            form.setSpecialisation(profile.getSpecialisation());
            form.setLocation(profile.getLocation());
            form.setBio(profile.getBio());
            form.setLogoUrl(profile.getLogoUrl());
        });

        model.addAttribute("ptProfileForm", form);
        return "pt/onboarding";
    }

    @PostMapping
    public String saveOnboarding(@Valid @ModelAttribute("ptProfileForm") PTProfileForm form,
                                 BindingResult result,
                                 RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "pt/onboarding";
        }

        ptProfileService.createOrUpdateProfile(securityHelper.getCurrentUserId(), form);
        redirectAttributes.addFlashAttribute("success", "Welcome to PTOS! Your profile is set up.");
        return "redirect:/pt/dashboard";
    }

    @GetMapping("/skip")
    public String skipOnboarding(RedirectAttributes redirectAttributes) {
        ptProfileService.markOnboardingComplete(securityHelper.getCurrentUserId());
        redirectAttributes.addFlashAttribute("success", "Welcome to PTOS! Your profile is set up.");
        return "redirect:/pt/dashboard";
    }
}

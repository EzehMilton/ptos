package com.ptos.controller;

import com.ptos.domain.ClientProfile;
import com.ptos.dto.ProfileForm;
import com.ptos.security.SecurityHelper;
import com.ptos.service.ClientProfileService;
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
@RequestMapping("/client/profile")
@RequiredArgsConstructor
public class ClientProfileController {

    private final ClientProfileService clientProfileService;
    private final SecurityHelper securityHelper;

    @GetMapping
    public String showProfile(Model model) {
        Long userId = securityHelper.getCurrentUserId();
        Optional<ClientProfile> profileOpt = clientProfileService.getProfileForUser(userId);

        ProfileForm form = new ProfileForm();
        if (profileOpt.isPresent()) {
            ClientProfile profile = profileOpt.get();
            form.setAge(profile.getAge());
            form.setHeightCm(profile.getHeightCm());
            form.setCurrentWeightKg(profile.getCurrentWeightKg());
            form.setGoalType(profile.getGoalType());
            form.setTargetWeightKg(profile.getTargetWeightKg());
            form.setInjuriesOrConditions(profile.getInjuriesOrConditions());
            form.setDietaryPreferences(profile.getDietaryPreferences());
            form.setTrainingExperience(profile.getTrainingExperience());
            form.setNotes(profile.getNotes());
            model.addAttribute("profile", profile);
            model.addAttribute("completion", clientProfileService.computeCompletion(profile));
            model.addAttribute("onboardingComplete", profile.isOnboardingComplete());
        }

        model.addAttribute("profileForm", form);
        model.addAttribute("fullName", securityHelper.getCurrentUserDetails().getUser().getFullName());
        return "client/profile";
    }

    @PostMapping
    public String saveProfile(@Valid @ModelAttribute("profileForm") ProfileForm form,
                              BindingResult result,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            Long userId = securityHelper.getCurrentUserId();
            clientProfileService.getProfileForUser(userId).ifPresent(profile -> {
                model.addAttribute("profile", profile);
                model.addAttribute("completion", clientProfileService.computeCompletion(profile));
                model.addAttribute("onboardingComplete", profile.isOnboardingComplete());
            });
            model.addAttribute("fullName", securityHelper.getCurrentUserDetails().getUser().getFullName());
            return "client/profile";
        }

        Long userId = securityHelper.getCurrentUserId();
        clientProfileService.createOrUpdateProfile(userId, form);
        redirectAttributes.addFlashAttribute("success", "Profile updated");
        return "redirect:/client/profile";
    }
}

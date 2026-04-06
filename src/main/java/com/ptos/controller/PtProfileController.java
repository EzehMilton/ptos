package com.ptos.controller;

import com.ptos.domain.PTProfile;
import com.ptos.dto.PTProfileForm;
import com.ptos.security.SecurityHelper;
import com.ptos.service.PTProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequestMapping("/pt/profile")
@RequiredArgsConstructor
@Slf4j
public class PtProfileController {

    private final PTProfileService ptProfileService;
    private final SecurityHelper securityHelper;

    @GetMapping
    public String showProfile(Model model) {
        log.info("Showing PT profile");
        Long userId = securityHelper.getCurrentUserId();
        Optional<PTProfile> profileOpt = ptProfileService.getProfileForUser(userId);

        PTProfileForm form = new PTProfileForm();
        profileOpt.ifPresent(profile -> {
            form.setBusinessName(profile.getBusinessName());
            form.setSpecialisation(profile.getSpecialisation());
            form.setLocation(profile.getLocation());
            form.setBio(profile.getBio());
            form.setLogoUrl(profile.getLogoUrl());
        });

        model.addAttribute("ptProfileForm", form);
        return "pt/profile";
    }

    @PostMapping
    public String updateProfile(@Valid @ModelAttribute("ptProfileForm") PTProfileForm form,
                                BindingResult result,
                                RedirectAttributes redirectAttributes) {
        log.info("Updating PT profile");
        if (result.hasErrors()) {
            return "pt/profile";
        }

        ptProfileService.createOrUpdateProfile(securityHelper.getCurrentUserId(), form);
        redirectAttributes.addFlashAttribute("success", "Profile updated.");
        return "redirect:/pt/profile";
    }
}

package com.ptos.controller;

import com.ptos.domain.ClientProfile;
import com.ptos.security.SecurityHelper;
import com.ptos.service.ClientProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final SecurityHelper securityHelper;
    private final ClientProfileService clientProfileService;

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
        String fullName = securityHelper.getCurrentUserDetails().getUser().getFullName();
        model.addAttribute("fullName", fullName);

        Optional<ClientProfile> profileOpt = clientProfileService.getProfileForUser(userId);
        if (profileOpt.isPresent()) {
            ClientProfile profile = profileOpt.get();
            model.addAttribute("hasProfile", true);
            int completion = 0;
            if (profile.getAge() != null) completion++;
            if (profile.getHeightCm() != null) completion++;
            if (profile.getCurrentWeightKg() != null) completion++;
            if (profile.getGoalType() != null) completion++;
            if (profile.getTargetWeightKg() != null) completion++;
            if (profile.getTrainingExperience() != null) completion++;
            model.addAttribute("completion", completion);
        } else {
            model.addAttribute("hasProfile", false);
        }

        return "client/home";
    }
}

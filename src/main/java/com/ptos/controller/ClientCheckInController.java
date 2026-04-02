package com.ptos.controller;

import com.ptos.domain.CheckIn;
import com.ptos.domain.User;
import com.ptos.dto.CheckInForm;
import com.ptos.security.SecurityHelper;
import com.ptos.service.CheckInService;
import com.ptos.service.ClientProfileService;
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

@Controller
@RequestMapping("/client/checkins")
@RequiredArgsConstructor
public class ClientCheckInController {

    private final CheckInService checkInService;
    private final ClientProfileService clientProfileService;
    private final SecurityHelper securityHelper;

    @GetMapping
    public String listCheckIns(Model model) {
        User clientUser = securityHelper.getCurrentUserDetails().getUser();
        List<CheckIn> checkIns = checkInService.getCheckInsForClient(clientUser);
        model.addAttribute("checkIns", checkIns);
        return "client/checkins/list";
    }

    @GetMapping("/new")
    public String newCheckIn(Model model) {
        CheckInForm form = new CheckInForm();
        clientProfileService.getProfileForUser(securityHelper.getCurrentUserId())
                .ifPresent(profile -> form.setCurrentWeightKg(profile.getCurrentWeightKg()));
        model.addAttribute("checkInForm", form);
        return "client/checkins/form";
    }

    @PostMapping
    public String submitCheckIn(@Valid @ModelAttribute("checkInForm") CheckInForm form,
                                BindingResult result,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "client/checkins/form";
        }

        User clientUser = securityHelper.getCurrentUserDetails().getUser();
        checkInService.submitCheckIn(clientUser, form);
        redirectAttributes.addFlashAttribute("success", "Check-in submitted! Your PT will review it.");
        return "redirect:/client/checkins";
    }

    @GetMapping("/{id}")
    public String checkInDetail(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        User clientUser = securityHelper.getCurrentUserDetails().getUser();
        return checkInService.getCheckInForClient(id, clientUser)
                .map(checkIn -> {
                    model.addAttribute("checkIn", checkIn);
                    return "client/checkins/detail";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Check-in not found.");
                    return "redirect:/client/checkins";
                });
    }
}

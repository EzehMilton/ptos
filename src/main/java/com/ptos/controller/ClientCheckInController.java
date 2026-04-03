package com.ptos.controller;

import com.ptos.domain.CheckIn;
import com.ptos.domain.CheckInPhoto;
import com.ptos.domain.PhotoType;
import com.ptos.domain.User;
import com.ptos.dto.CheckInForm;
import com.ptos.integration.FileStorageGateway;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/client/checkins")
@RequiredArgsConstructor
public class ClientCheckInController {

    private final CheckInService checkInService;
    private final ClientProfileService clientProfileService;
    private final FileStorageGateway fileStorageGateway;
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
                                @RequestParam(name = "frontPhoto", required = false) MultipartFile frontPhoto,
                                @RequestParam(name = "sidePhoto", required = false) MultipartFile sidePhoto,
                                @RequestParam(name = "backPhoto", required = false) MultipartFile backPhoto,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "client/checkins/form";
        }

        User clientUser = securityHelper.getCurrentUserDetails().getUser();
        try {
            checkInService.submitCheckIn(clientUser, form, frontPhoto, sidePhoto, backPhoto);
        } catch (IllegalArgumentException ex) {
            result.reject("photo.invalid", ex.getMessage());
            return "client/checkins/form";
        }
        redirectAttributes.addFlashAttribute("success", "Check-in submitted! Your PT will review it.");
        return "redirect:/client/checkins";
    }

    @GetMapping("/{id}")
    public String checkInDetail(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        User clientUser = securityHelper.getCurrentUserDetails().getUser();
        return checkInService.getCheckInForClient(id, clientUser)
                .map(checkIn -> {
                    model.addAttribute("checkIn", checkIn);
                    model.addAttribute("photoTypes", PhotoType.values());
                    model.addAttribute("photoUrls", buildPhotoUrls(checkInService.getPhotosForCheckIn(checkIn.getId())));
                    return "client/checkins/detail";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Check-in not found.");
                    return "redirect:/client/checkins";
                });
    }

    private Map<PhotoType, String> buildPhotoUrls(List<CheckInPhoto> photos) {
        Map<PhotoType, String> urls = new EnumMap<>(PhotoType.class);
        for (CheckInPhoto photo : photos) {
            urls.put(photo.getPhotoType(), fileStorageGateway.getUrl(photo.getStorageKey()));
        }
        return urls;
    }
}

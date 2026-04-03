package com.ptos.controller;

import com.ptos.domain.ClientInvitation;
import com.ptos.domain.InvitationStatus;
import com.ptos.dto.InviteAcceptForm;
import com.ptos.service.ClientInvitationService;
import com.ptos.service.PTProfileService;
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

import java.util.Optional;

@Controller
@RequestMapping("/invite")
@RequiredArgsConstructor
public class InviteController {

    private final ClientInvitationService clientInvitationService;
    private final PTProfileService ptProfileService;

    @GetMapping("/{token}")
    public String showInvite(@PathVariable String token, Model model) {
        Optional<ClientInvitation> invitationOpt = clientInvitationService.getInvitationByToken(token);
        populateInviteModel(model, invitationOpt.orElse(null));

        if (!model.containsAttribute("inviteAcceptForm")) {
            model.addAttribute("inviteAcceptForm", new InviteAcceptForm());
        }

        return "auth/accept-invite";
    }

    @PostMapping("/{token}")
    public String acceptInvite(@PathVariable String token,
                               @Valid @ModelAttribute("inviteAcceptForm") InviteAcceptForm form,
                               BindingResult result,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        Optional<ClientInvitation> invitationOpt = clientInvitationService.getInvitationByToken(token);
        if (invitationOpt.isEmpty()) {
            model.addAttribute("inviteError", "This invitation link is invalid.");
            model.addAttribute("inviteAcceptForm", form);
            return "auth/accept-invite";
        }

        ClientInvitation invitation = invitationOpt.get();
        populateInviteModel(model, invitation);

        if (invitation.getStatus() == InvitationStatus.EXPIRED || invitation.getStatus() == InvitationStatus.ACCEPTED) {
            return "auth/accept-invite";
        }

        if (!form.getPassword().equals(form.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "password.mismatch", "Passwords do not match");
        }

        if (result.hasErrors()) {
            return "auth/accept-invite";
        }

        try {
            clientInvitationService.acceptInvitation(token, form.getPassword());
            redirectAttributes.addFlashAttribute("invitedName", invitation.getFullName());
            redirectAttributes.addFlashAttribute("ptDisplayName", getPtDisplayName(invitation));
            return "redirect:/invite/" + token + "/accepted";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("inviteError", ex.getMessage());
            return "auth/accept-invite";
        }
    }

    @GetMapping("/{token}/accepted")
    public String inviteAccepted(@PathVariable String token, Model model, RedirectAttributes redirectAttributes) {
        Optional<ClientInvitation> invitationOpt = clientInvitationService.getInvitationByToken(token);
        if (invitationOpt.isEmpty() || invitationOpt.get().getStatus() != InvitationStatus.ACCEPTED) {
            redirectAttributes.addFlashAttribute("inviteError", "This invitation is not available.");
            return "redirect:/invite/" + token;
        }

        ClientInvitation invitation = invitationOpt.get();
        model.addAttribute("invitedName", model.getAttribute("invitedName") != null
                ? model.getAttribute("invitedName")
                : invitation.getFullName());
        model.addAttribute("ptDisplayName", model.getAttribute("ptDisplayName") != null
                ? model.getAttribute("ptDisplayName")
                : getPtDisplayName(invitation));
        return "auth/invite-complete";
    }

    private void populateInviteModel(Model model, ClientInvitation invitation) {
        if (invitation == null) {
            model.addAttribute("inviteError", "This invitation link is invalid.");
            return;
        }

        model.addAttribute("invitation", invitation);
        model.addAttribute("ptDisplayName", getPtDisplayName(invitation));
        model.addAttribute("ptName", invitation.getPtUser().getFullName());
        model.addAttribute("invitedName", invitation.getFullName());
    }

    private String getPtDisplayName(ClientInvitation invitation) {
        return ptProfileService.getProfileForUser(invitation.getPtUser().getId())
                .map(profile -> profile.getBusinessName())
                .filter(name -> name != null && !name.isBlank())
                .orElse(invitation.getPtUser().getFullName());
    }
}

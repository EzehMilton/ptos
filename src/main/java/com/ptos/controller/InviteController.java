package com.ptos.controller;

import com.ptos.domain.ClientInvitation;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class InviteController {

    private final ClientInvitationService clientInvitationService;
    private final PTProfileService ptProfileService;

    @GetMapping("/invite/{token}")
    public String showInvite(@PathVariable String token, Model model) {
        Optional<ClientInvitation> invitationOpt = clientInvitationService.getInvitationByToken(token);
        if (invitationOpt.isEmpty()) {
            model.addAttribute("inviteMessage", "This invitation link is invalid.");
            model.addAttribute("pageTitle", "Join PTOS");
            return "auth/accept-invite";
        }

        addInvitationAttributes(model, invitationOpt.get());
        model.addAttribute("inviteAcceptForm", new InviteAcceptForm());
        return "auth/accept-invite";
    }

    @PostMapping("/invite/{token}")
    public String acceptInvite(@PathVariable String token,
                               @Valid @ModelAttribute("inviteAcceptForm") InviteAcceptForm form,
                               BindingResult result,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        Optional<ClientInvitation> invitationOpt = clientInvitationService.getInvitationByToken(token);
        if (invitationOpt.isEmpty()) {
            model.addAttribute("inviteMessage", "This invitation link is invalid.");
            model.addAttribute("pageTitle", "Join PTOS");
            return "auth/accept-invite";
        }

        ClientInvitation invitation = invitationOpt.get();
        addInvitationAttributes(model, invitation);

        if (!form.getPassword().equals(form.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "password.mismatch", "Passwords do not match");
        }

        if (result.hasErrors()) {
            return "auth/accept-invite";
        }

        try {
            clientInvitationService.acceptInvitation(token, form);
            redirectAttributes.addFlashAttribute("success", "Invitation accepted. Please log in.");
            return "redirect:/login";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("inviteMessage", ex.getMessage());
            return "auth/accept-invite";
        }
    }

    private void addInvitationAttributes(Model model, ClientInvitation invitation) {
        String ptName = invitation.getPtUser().getFullName();
        String ptDisplayName = ptProfileService.getProfileForUser(invitation.getPtUser().getId())
                .map(profile -> profile.getBusinessName())
                .filter(name -> name != null && !name.isBlank())
                .orElse(ptName);

        model.addAttribute("invitation", invitation);
        model.addAttribute("ptName", ptName);
        model.addAttribute("ptDisplayName", ptDisplayName);
        model.addAttribute("pageTitle", "Join " + ptDisplayName + " on PTOS");

        if (invitation.getStatus().name().equals("EXPIRED")) {
            model.addAttribute("inviteMessage", "This invitation has expired. Please ask your PT for a new one.");
        } else if (invitation.getStatus().name().equals("ACCEPTED")) {
            model.addAttribute("inviteMessage", "This invitation has already been used.");
        }
    }
}

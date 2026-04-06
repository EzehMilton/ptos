package com.ptos.controller.api;

import com.ptos.domain.ClientInvitation;
import com.ptos.dto.api.PublicInviteValidationResponse;
import com.ptos.service.ClientInvitationService;
import com.ptos.service.PTProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/invites")
@RequiredArgsConstructor
public class PublicInviteValidationController {

    private static final String INVALID_MESSAGE = "This invitation link is invalid or has expired.";

    private final ClientInvitationService clientInvitationService;
    private final PTProfileService ptProfileService;

    @GetMapping("/{token}")
    public ResponseEntity<PublicInviteValidationResponse> validateInvite(@PathVariable String token) {
        return clientInvitationService.getValidInvitationByToken(token)
                .map(this::toValidResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok(PublicInviteValidationResponse.invalid(INVALID_MESSAGE)));
    }

    private PublicInviteValidationResponse toValidResponse(ClientInvitation invitation) {
        String ptName = invitation.getPtUser().getFullName();
        String ptBusinessName = ptProfileService.getProfileForUser(invitation.getPtUser().getId())
                .map(profile -> profile.getBusinessName())
                .filter(name -> name != null && !name.isBlank())
                .orElse(null);

        return PublicInviteValidationResponse.valid(
                invitation.getToken(),
                ptName,
                ptBusinessName,
                invitation.getEmail(),
                invitation.getFullName(),
                invitation.getExpiresAt()
        );
    }
}

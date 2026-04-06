package com.ptos.controller.api;

import com.ptos.domain.ClientRecord;
import com.ptos.dto.InvitationAcceptResult;
import com.ptos.dto.api.PublicInviteSignupRequest;
import com.ptos.dto.api.PublicInviteSignupResponse;
import com.ptos.repository.ClientRecordRepository;
import com.ptos.security.JwtUtil;
import com.ptos.service.ClientInvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/auth")
@RequiredArgsConstructor
public class PublicInviteAuthController {

    private final ClientInvitationService clientInvitationService;
    private final ClientRecordRepository clientRecordRepository;
    private final JwtUtil jwtUtil;

    @PostMapping("/signup/invite")
    public ResponseEntity<PublicInviteSignupResponse> signupFromInvite(
            @Valid @RequestBody PublicInviteSignupRequest request) {
        InvitationAcceptResult result = clientInvitationService.acceptInvitation(
                request.token(),
                request.password()
        );

        ClientRecord clientRecord = clientRecordRepository
                .findByPtUserAndClientUser(result.ptUser(), result.clientUser())
                .orElseThrow(() -> new IllegalStateException("Client record was not created correctly."));

        String jwt = jwtUtil.generateToken(result.clientUser());

        return ResponseEntity.ok(new PublicInviteSignupResponse(
                jwt,
                result.clientUser().getId(),
                clientRecord.getId(),
                result.clientUser().getFullName(),
                result.ptUser().getFullName()
        ));
    }
}

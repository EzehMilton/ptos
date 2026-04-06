package com.ptos.controller.api;

import com.ptos.domain.Role;
import com.ptos.domain.User;
import com.ptos.dto.api.ApiError;
import com.ptos.dto.api.LoginRequest;
import com.ptos.dto.api.LoginResponse;
import com.ptos.dto.api.RegisterRequest;
import com.ptos.service.ClientInvitationService;
import com.ptos.repository.UserRepository;
import com.ptos.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class ClientAuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ClientInvitationService clientInvitationService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        User user = userRepository.findByEmail(request.email()).orElse(null);

        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError(401, "Invalid email or password", LocalDateTime.now()));
        }

        if (user.getRole() != Role.CLIENT) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiError(403, "Only client accounts can use this API", LocalDateTime.now()));
        }

        if (!user.isEnabled()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError(401, "Account is disabled", LocalDateTime.now()));
        }

        String token = jwtUtil.generateToken(user);
        return ResponseEntity.ok(new LoginResponse(token, user.getEmail(), user.getFullName()));
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        clientInvitationService.getInvitationByToken(request.inviteToken())
                .map(invitation -> {
                    if (!invitation.getEmail().equalsIgnoreCase(request.email().trim())) {
                        throw new IllegalArgumentException("Email does not match the invitation.");
                    }
                    if (!invitation.getFullName().equalsIgnoreCase(request.fullName().trim())) {
                        throw new IllegalArgumentException("Full name does not match the invitation.");
                    }
                    return invitation;
                })
                .orElseThrow(() -> new IllegalArgumentException("This invitation link is invalid."));

        User clientUser = clientInvitationService.acceptInvitation(
                request.inviteToken(),
                request.password()
        ).clientUser();

        String token = jwtUtil.generateToken(clientUser);
        return ResponseEntity.ok(new LoginResponse(token, clientUser.getEmail(), clientUser.getFullName()));
    }
}

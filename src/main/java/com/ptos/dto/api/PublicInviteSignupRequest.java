package com.ptos.dto.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PublicInviteSignupRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 8, max = 100) String password
) {
}

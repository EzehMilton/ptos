package com.ptos.dto.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PublicInviteValidationResponse(
        boolean valid,
        String token,
        String ptName,
        String ptBusinessName,
        String clientEmail,
        String clientFullName,
        LocalDateTime expiresAt,
        String message
) {

    public static PublicInviteValidationResponse valid(String token,
                                                       String ptName,
                                                       String ptBusinessName,
                                                       String clientEmail,
                                                       String clientFullName,
                                                       LocalDateTime expiresAt) {
        return new PublicInviteValidationResponse(
                true,
                token,
                ptName,
                ptBusinessName,
                clientEmail,
                clientFullName,
                expiresAt,
                null
        );
    }

    public static PublicInviteValidationResponse invalid(String message) {
        return new PublicInviteValidationResponse(
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                message
        );
    }
}

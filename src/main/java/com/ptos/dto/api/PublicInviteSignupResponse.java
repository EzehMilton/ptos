package com.ptos.dto.api;

public record PublicInviteSignupResponse(
        String jwt,
        Long clientUserId,
        Long clientRecordId,
        String fullName,
        String ptName
) {
}

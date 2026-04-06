package com.ptos.dto.api;

public record ClientProfilePhotoResponse(
        String photoType,
        String storageKey,
        String url,
        String originalFilename,
        Long fileSize
) {
}

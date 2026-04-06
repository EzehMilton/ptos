package com.ptos.dto.api;

import java.util.List;

public record ClientProfilePhotoUploadResponse(
        Long clientUserId,
        Long clientProfileId,
        int savedCount,
        List<ClientProfilePhotoResponse> savedPhotos
) {
}

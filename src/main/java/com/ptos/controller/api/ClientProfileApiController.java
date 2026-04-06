package com.ptos.controller.api;

import com.ptos.domain.ClientProfile;
import com.ptos.domain.ClientProfilePhoto;
import com.ptos.domain.User;
import com.ptos.dto.api.ClientProfilePhotoResponse;
import com.ptos.dto.api.ClientProfilePhotoUploadResponse;
import com.ptos.dto.api.ClientProfileResponse;
import com.ptos.dto.api.ClientProfileUpdateRequest;
import com.ptos.integration.FileStorageGateway;
import com.ptos.security.PtosUserDetails;
import com.ptos.service.ClientProfilePhotoService;
import com.ptos.service.ClientProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/client/profile")
@RequiredArgsConstructor
public class ClientProfileApiController {

    private final ClientProfileService clientProfileService;
    private final ClientProfilePhotoService clientProfilePhotoService;
    private final FileStorageGateway fileStorageGateway;

    @GetMapping
    public ResponseEntity<ClientProfileResponse> getProfile(@AuthenticationPrincipal PtosUserDetails userDetails) {
        User user = userDetails.getUser();
        ClientProfile profile = clientProfileService.getProfileForUser(user.getId()).orElse(null);
        return ResponseEntity.ok(toResponse(user, profile));
    }

    @PutMapping
    public ResponseEntity<ClientProfileResponse> updateProfile(
            @AuthenticationPrincipal PtosUserDetails userDetails,
            @Valid @RequestBody ClientProfileUpdateRequest request) {
        User user = userDetails.getUser();
        ClientProfile profile = clientProfileService.createOrUpdateProfile(user.getId(), request);
        return ResponseEntity.ok(toResponse(user, profile));
    }

    @PostMapping(value = "/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClientProfilePhotoUploadResponse> uploadProfilePhotos(
            @AuthenticationPrincipal PtosUserDetails userDetails,
            @RequestPart(required = false) MultipartFile frontPhoto,
            @RequestPart(required = false) MultipartFile sidePhoto,
            @RequestPart(required = false) MultipartFile backPhoto) {
        User user = userDetails.getUser();
        ClientProfile profile = clientProfileService.getOrCreateProfileForUser(user.getId());
        List<ClientProfilePhoto> savedPhotos = clientProfilePhotoService.uploadOnboardingPhotos(
                user, frontPhoto, sidePhoto, backPhoto);

        return ResponseEntity.ok(new ClientProfilePhotoUploadResponse(
                user.getId(),
                profile.getId(),
                savedPhotos.size(),
                savedPhotos.stream().map(this::toPhotoResponse).toList()
        ));
    }

    private ClientProfileResponse toResponse(User user, ClientProfile profile) {
        if (profile == null) {
            return new ClientProfileResponse(user.getFullName(), user.getEmail(),
                    null, null, null, null, null, null, null, null, null, false);
        }
        return new ClientProfileResponse(
                user.getFullName(), user.getEmail(),
                profile.getAge(), profile.getHeightCm(), profile.getCurrentWeightKg(),
                profile.getGoalType() != null ? profile.getGoalType().name() : null,
                profile.getTargetWeightKg(),
                profile.getInjuriesOrConditions(), profile.getDietaryPreferences(),
                profile.getTrainingExperience() != null ? profile.getTrainingExperience().name() : null,
                profile.getNotes(),
                profile.isOnboardingComplete()
        );
    }

    private ClientProfilePhotoResponse toPhotoResponse(ClientProfilePhoto photo) {
        return new ClientProfilePhotoResponse(
                photo.getPhotoType().name(),
                photo.getStorageKey(),
                fileStorageGateway.getUrl(photo.getStorageKey()),
                photo.getOriginalFilename(),
                photo.getFileSize()
        );
    }
}

package com.ptos.service;

import com.ptos.domain.ClientProfile;
import com.ptos.domain.ClientProfilePhoto;
import com.ptos.domain.PhotoType;
import com.ptos.domain.User;
import com.ptos.integration.FileStorageGateway;
import com.ptos.repository.ClientProfilePhotoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ClientProfilePhotoService {

    private static final long MAX_PHOTO_SIZE_BYTES = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
    );

    private final ClientProfileService clientProfileService;
    private final ClientProfilePhotoRepository clientProfilePhotoRepository;
    private final FileStorageGateway fileStorageGateway;

    @Transactional
    public List<ClientProfilePhoto> uploadOnboardingPhotos(User clientUser,
                                                           MultipartFile frontPhoto,
                                                           MultipartFile sidePhoto,
                                                           MultipartFile backPhoto) {
        ClientProfile clientProfile = clientProfileService.getOrCreateProfileForUser(clientUser.getId());

        List<ClientProfilePhoto> savedPhotos = new ArrayList<>();
        replacePhoto(clientProfile, PhotoType.FRONT, frontPhoto, "Front photo", savedPhotos);
        replacePhoto(clientProfile, PhotoType.SIDE, sidePhoto, "Side photo", savedPhotos);
        replacePhoto(clientProfile, PhotoType.BACK, backPhoto, "Back photo", savedPhotos);

        return savedPhotos.stream()
                .sorted(Comparator.comparing(ClientProfilePhoto::getPhotoType))
                .toList();
    }

    private void replacePhoto(ClientProfile clientProfile,
                              PhotoType photoType,
                              MultipartFile file,
                              String label,
                              List<ClientProfilePhoto> savedPhotos) {
        if (file == null || file.isEmpty()) {
            return;
        }

        validatePhoto(file, label);

        ClientProfilePhoto existingPhoto = clientProfilePhotoRepository
                .findByClientProfileAndPhotoType(clientProfile, photoType)
                .orElse(null);

        String oldStorageKey = existingPhoto != null ? existingPhoto.getStorageKey() : null;
        String newStorageKey = fileStorageGateway.store(file, "photos/onboarding");

        try {
            ClientProfilePhoto photo = existingPhoto != null ? existingPhoto : new ClientProfilePhoto();
            photo.setClientProfile(clientProfile);
            photo.setPhotoType(photoType);
            photo.setStorageKey(newStorageKey);
            photo.setOriginalFilename(trimFilename(file.getOriginalFilename()));
            photo.setFileSize(file.getSize());
            photo = clientProfilePhotoRepository.save(photo);
            savedPhotos.add(photo);

            if (oldStorageKey != null) {
                fileStorageGateway.delete(oldStorageKey);
            }
        } catch (RuntimeException ex) {
            fileStorageGateway.delete(newStorageKey);
            throw ex;
        }
    }

    private void validatePhoto(MultipartFile file, String label) {
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(label + " must be a JPEG, PNG, or WEBP file.");
        }
        if (file.getSize() > MAX_PHOTO_SIZE_BYTES) {
            throw new IllegalArgumentException(label + " must be 10MB or smaller.");
        }
    }

    private String trimFilename(String originalFilename) {
        if (originalFilename == null) {
            return null;
        }
        String trimmed = originalFilename.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() > 255 ? trimmed.substring(0, 255) : trimmed;
    }
}

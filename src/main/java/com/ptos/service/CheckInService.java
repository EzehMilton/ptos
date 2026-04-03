package com.ptos.service;

import com.ptos.domain.*;
import com.ptos.dto.CheckInForm;
import com.ptos.integration.FileStorageGateway;
import com.ptos.repository.CheckInFeedbackRepository;
import com.ptos.repository.CheckInPhotoRepository;
import com.ptos.repository.CheckInRepository;
import com.ptos.repository.ClientProfileRepository;
import com.ptos.repository.ClientRecordRepository;
import com.ptos.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CheckInService {

    private static final long MAX_PHOTO_SIZE_BYTES = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
    );

    private final CheckInRepository checkInRepository;
    private final CheckInFeedbackRepository checkInFeedbackRepository;
    private final CheckInPhotoRepository checkInPhotoRepository;
    private final ClientRecordRepository clientRecordRepository;
    private final ClientProfileRepository clientProfileRepository;
    private final UserRepository userRepository;
    private final FileStorageGateway fileStorageGateway;

    @Transactional
    public CheckIn submitCheckIn(User clientUser,
                                 CheckInForm form,
                                 MultipartFile frontPhoto,
                                 MultipartFile sidePhoto,
                                 MultipartFile backPhoto) {
        ClientRecord clientRecord = clientRecordRepository.findByClientUser(clientUser)
                .orElseThrow(() -> new IllegalArgumentException("Client record not found"));

        validatePhoto(frontPhoto, "Front photo");
        validatePhoto(sidePhoto, "Side photo");
        validatePhoto(backPhoto, "Back photo");

        CheckIn checkIn = CheckIn.builder()
                .clientRecord(clientRecord)
                .submittedAt(LocalDateTime.now())
                .currentWeightKg(form.getCurrentWeightKg())
                .moodScore(form.getMoodScore())
                .energyScore(form.getEnergyScore())
                .sleepScore(form.getSleepScore())
                .notes(trimToNull(form.getNotes()))
                .status(CheckInStatus.PENDING_REVIEW)
                .build();
        checkIn = checkInRepository.save(checkIn);

        List<String> storedKeys = new ArrayList<>();
        try {
            storePhoto(checkIn, PhotoType.FRONT, frontPhoto, storedKeys);
            storePhoto(checkIn, PhotoType.SIDE, sidePhoto, storedKeys);
            storePhoto(checkIn, PhotoType.BACK, backPhoto, storedKeys);
        } catch (RuntimeException ex) {
            storedKeys.forEach(fileStorageGateway::delete);
            throw ex;
        }

        ClientProfile profile = clientProfileRepository.findByUserId(clientUser.getId())
                .orElseGet(() -> ClientProfile.builder()
                        .user(userRepository.getReferenceById(clientUser.getId()))
                        .build());
        profile.setCurrentWeightKg(form.getCurrentWeightKg());
        clientProfileRepository.save(profile);

        return checkIn;
    }

    public List<CheckIn> getCheckInsForClient(User clientUser) {
        return checkInRepository.findByClientRecord_ClientUserOrderBySubmittedAtDesc(clientUser);
    }

    public List<CheckIn> getPendingCheckIns(User ptUser) {
        return checkInRepository.findByClientRecord_PtUserAndStatusOrderBySubmittedAtDesc(
                ptUser, CheckInStatus.PENDING_REVIEW);
    }

    public List<CheckIn> getAllCheckInsForPT(User ptUser) {
        return checkInRepository.findByClientRecord_PtUserOrderBySubmittedAtDesc(ptUser);
    }

    public List<CheckIn> getReviewedCheckIns(User ptUser) {
        return checkInRepository.findByClientRecord_PtUserAndStatusOrderBySubmittedAtDesc(
                ptUser, CheckInStatus.REVIEWED);
    }

    public Optional<CheckIn> getCheckIn(Long checkInId) {
        return checkInRepository.findById(checkInId);
    }

    public Optional<CheckIn> getCheckInForClient(Long checkInId, User clientUser) {
        return checkInRepository.findByIdAndClientRecord_ClientUser(checkInId, clientUser);
    }

    public Optional<CheckIn> getLatestCheckInForClient(User clientUser) {
        return getCheckInsForClient(clientUser).stream().findFirst();
    }

    public Optional<CheckIn> getCheckInForPT(Long checkInId, User ptUser) {
        return checkInRepository.findByIdAndClientRecord_PtUser(checkInId, ptUser);
    }

    public Optional<CheckIn> getPreviousCheckIn(CheckIn checkIn) {
        return checkInRepository.findTopByClientRecordAndSubmittedAtBeforeOrderBySubmittedAtDesc(
                checkIn.getClientRecord(), checkIn.getSubmittedAt());
    }

    public List<CheckIn> getCheckInsForClientRecord(ClientRecord clientRecord) {
        return checkInRepository.findByClientRecordOrderBySubmittedAtDesc(clientRecord);
    }

    public List<CheckInPhoto> getPhotosForCheckIn(Long checkInId) {
        return checkInPhotoRepository.findByCheckInId(checkInId).stream()
                .sorted(Comparator.comparing(CheckInPhoto::getPhotoType))
                .toList();
    }

    public List<CheckInPhoto> getPreviousCheckInPhotos(ClientRecord clientRecord, Long currentCheckInId) {
        return checkInRepository.findById(currentCheckInId)
                .filter(checkIn -> checkIn.getClientRecord().getId().equals(clientRecord.getId()))
                .flatMap(this::getPreviousCheckIn)
                .map(previousCheckIn -> getPhotosForCheckIn(previousCheckIn.getId()))
                .orElse(List.of());
    }

    public long countPendingCheckIns(User ptUser) {
        return checkInRepository.countByClientRecord_PtUserAndStatus(ptUser, CheckInStatus.PENDING_REVIEW);
    }

    @Transactional
    public CheckIn submitFeedback(Long checkInId, User ptUser, String feedbackText) {
        CheckIn checkIn = checkInRepository.findById(checkInId)
                .orElseThrow(() -> new IllegalArgumentException("Check-in not found"));

        if (!checkIn.getClientRecord().getPtUser().getId().equals(ptUser.getId())) {
            throw new IllegalArgumentException("Check-in does not belong to this PT");
        }
        if (checkIn.getStatus() != CheckInStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Check-in has already been reviewed");
        }

        CheckInFeedback feedback = checkIn.getFeedback();
        if (feedback == null) {
            feedback = new CheckInFeedback();
            feedback.setCheckIn(checkIn);
        }
        feedback.setPtUser(ptUser);
        feedback.setFeedbackText(requireFeedbackText(feedbackText));
        feedback.setSentAt(LocalDateTime.now());
        checkInFeedbackRepository.save(feedback);

        checkIn.setFeedback(feedback);
        checkIn.setStatus(CheckInStatus.REVIEWED);
        return checkInRepository.save(checkIn);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String requireFeedbackText(String feedbackText) {
        String trimmed = trimToNull(feedbackText);
        if (trimmed == null) {
            throw new IllegalArgumentException("Feedback text is required");
        }
        return trimmed;
    }

    private void validatePhoto(MultipartFile file, String label) {
        if (file == null || file.isEmpty()) {
            return;
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(label + " must be a JPEG, PNG, or WEBP file.");
        }
        if (file.getSize() > MAX_PHOTO_SIZE_BYTES) {
            throw new IllegalArgumentException(label + " must be 10MB or smaller.");
        }
    }

    private void storePhoto(CheckIn checkIn,
                            PhotoType photoType,
                            MultipartFile file,
                            List<String> storedKeys) {
        if (file == null || file.isEmpty()) {
            return;
        }

        String storageKey = fileStorageGateway.store(file, "photos/checkins");
        storedKeys.add(storageKey);

        CheckInPhoto photo = CheckInPhoto.builder()
                .checkIn(checkIn)
                .photoType(photoType)
                .storageKey(storageKey)
                .originalFilename(trimFilename(file.getOriginalFilename()))
                .fileSize(file.getSize())
                .build();
        checkIn.getPhotos().add(photo);
        checkInPhotoRepository.save(photo);
    }

    private String trimFilename(String originalFilename) {
        String value = trimToNull(originalFilename);
        if (value == null) {
            return null;
        }
        return value.length() > 255 ? value.substring(0, 255) : value;
    }
}

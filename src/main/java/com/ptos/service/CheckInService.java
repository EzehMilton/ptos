package com.ptos.service;

import com.ptos.domain.*;
import com.ptos.dto.CheckInForm;
import com.ptos.repository.CheckInFeedbackRepository;
import com.ptos.repository.CheckInRepository;
import com.ptos.repository.ClientProfileRepository;
import com.ptos.repository.ClientRecordRepository;
import com.ptos.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CheckInService {

    private final CheckInRepository checkInRepository;
    private final CheckInFeedbackRepository checkInFeedbackRepository;
    private final ClientRecordRepository clientRecordRepository;
    private final ClientProfileRepository clientProfileRepository;
    private final UserRepository userRepository;

    @Transactional
    public CheckIn submitCheckIn(User clientUser, CheckInForm form) {
        ClientRecord clientRecord = clientRecordRepository.findByClientUser(clientUser)
                .orElseThrow(() -> new IllegalArgumentException("Client record not found"));

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

        ClientProfile profile = clientProfileRepository.findByUserId(clientUser.getId())
                .orElseGet(() -> ClientProfile.builder()
                        .user(userRepository.getReferenceById(clientUser.getId()))
                        .build());
        profile.setCurrentWeightKg(form.getCurrentWeightKg());
        clientProfileRepository.save(profile);

        return checkInRepository.save(checkIn);
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
}

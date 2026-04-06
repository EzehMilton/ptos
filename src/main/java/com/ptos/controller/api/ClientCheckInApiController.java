package com.ptos.controller.api;

import com.ptos.domain.CheckIn;
import com.ptos.domain.CheckInPhoto;
import com.ptos.domain.User;
import com.ptos.dto.api.CheckInResponse;
import com.ptos.dto.api.PhotoResponse;
import com.ptos.security.PtosUserDetails;
import com.ptos.service.CheckInService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/client/checkins")
@RequiredArgsConstructor
public class ClientCheckInApiController {

    private final CheckInService checkInService;

    @GetMapping
    public ResponseEntity<List<CheckInResponse>> getCheckIns(
            @AuthenticationPrincipal PtosUserDetails userDetails) {
        User user = userDetails.getUser();
        List<CheckInResponse> checkIns = checkInService.getCheckInsForClient(user).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(checkIns);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CheckInResponse> submitCheckIn(
            @AuthenticationPrincipal PtosUserDetails userDetails,
            @RequestParam Double currentWeightKg,
            @RequestParam(required = false) Integer moodScore,
            @RequestParam(required = false) Integer energyScore,
            @RequestParam(required = false) Integer sleepScore,
            @RequestParam(required = false) String notes,
            @RequestPart(required = false) MultipartFile frontPhoto,
            @RequestPart(required = false) MultipartFile sidePhoto,
            @RequestPart(required = false) MultipartFile backPhoto) {
        User user = userDetails.getUser();
        CheckIn checkIn = checkInService.submitCheckIn(
                user, currentWeightKg, moodScore, energyScore, sleepScore,
                notes, frontPhoto, sidePhoto, backPhoto);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(checkIn));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CheckInResponse> getCheckIn(
            @PathVariable Long id,
            @AuthenticationPrincipal PtosUserDetails userDetails) {
        User user = userDetails.getUser();
        CheckIn checkIn = checkInService.getCheckInForClient(id, user)
                .orElseThrow(() -> new IllegalArgumentException("Check-in not found"));
        return ResponseEntity.ok(toResponse(checkIn));
    }

    private CheckInResponse toResponse(CheckIn checkIn) {
        List<PhotoResponse> photos = checkIn.getPhotos() != null
                ? checkIn.getPhotos().stream()
                    .map(p -> new PhotoResponse(p.getPhotoType().name(), p.getStorageKey()))
                    .toList()
                : List.of();
        return new CheckInResponse(
                checkIn.getId(), checkIn.getSubmittedAt(),
                checkIn.getCurrentWeightKg(), checkIn.getMoodScore(),
                checkIn.getEnergyScore(), checkIn.getSleepScore(),
                checkIn.getNotes(), checkIn.getStatus().name(),
                checkIn.getFeedback() != null ? checkIn.getFeedback().getFeedbackText() : null,
                checkIn.getFeedback() != null ? checkIn.getFeedback().getSentAt() : null,
                photos
        );
    }
}

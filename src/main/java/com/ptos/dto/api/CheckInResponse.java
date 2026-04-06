package com.ptos.dto.api;

import java.time.LocalDateTime;
import java.util.List;

public record CheckInResponse(
        Long id,
        LocalDateTime submittedAt,
        Double currentWeightKg,
        Integer moodScore,
        Integer energyScore,
        Integer sleepScore,
        String notes,
        String status,
        String feedbackText,
        LocalDateTime feedbackSentAt,
        List<PhotoResponse> photos
) {}

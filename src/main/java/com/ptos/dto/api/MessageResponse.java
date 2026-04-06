package com.ptos.dto.api;

import java.time.LocalDateTime;

public record MessageResponse(
        Long id,
        String senderRole,
        String content,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {}

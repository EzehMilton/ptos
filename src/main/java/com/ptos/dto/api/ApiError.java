package com.ptos.dto.api;

import java.time.LocalDateTime;

public record ApiError(int status, String message, LocalDateTime timestamp) {}

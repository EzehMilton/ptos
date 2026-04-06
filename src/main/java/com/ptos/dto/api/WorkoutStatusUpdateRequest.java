package com.ptos.dto.api;

import jakarta.validation.constraints.NotBlank;

public record WorkoutStatusUpdateRequest(@NotBlank String action, String notes) {}

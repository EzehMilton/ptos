package com.ptos.dto.api;

import jakarta.validation.constraints.NotBlank;

public record SendMessageRequest(@NotBlank String content) {}

package com.ptos.dto.api;

import java.util.Locale;

public record WorkoutStatusUpdateRequest(String action, String status, String notes) {

    public String resolveUpdateCommand() {
        String value = hasText(status) ? status : action;
        if (!hasText(value)) {
            throw new IllegalArgumentException("Provide either 'action' or 'status'");
        }
        return value.trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

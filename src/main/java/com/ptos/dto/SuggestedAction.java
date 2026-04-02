package com.ptos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class SuggestedAction {

    private Priority priority;
    private String icon;
    private String message;
    private String actionUrl;
    private String actionLabel;

    public enum Priority {
        HIGH,
        MEDIUM,
        LOW
    }
}

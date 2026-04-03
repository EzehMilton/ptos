package com.ptos.dto;

import com.ptos.domain.ClientStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class ClientDirectoryItem {

    private Long clientRecordId;
    private String clientName;
    private String clientEmail;
    private String initials;
    private String goalLabel;
    private String weightSummary;
    private String packageSummary;
    private String activitySummary;
    private ClientStatus status;
    private int healthScore;
    private String healthScoreClass;
    private String primaryActionLabel;
    private String primaryActionUrl;
    private String secondaryActionLabel;
    private String secondaryActionUrl;
}

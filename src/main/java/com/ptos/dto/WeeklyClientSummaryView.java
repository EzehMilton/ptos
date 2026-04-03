package com.ptos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class WeeklyClientSummaryView {

    private Long clientRecordId;
    private String clientName;
    private String summary;
}

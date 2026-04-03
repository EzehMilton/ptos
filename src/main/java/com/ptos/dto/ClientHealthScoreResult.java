package com.ptos.dto;

import com.ptos.domain.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Comparator;
import java.util.stream.Stream;

@Getter
@AllArgsConstructor
@Builder
public class ClientHealthScoreResult {

    private Long clientRecordId;
    private String clientName;
    private int overallScore;
    private int profileCompletionScore;
    private int checkInActivityScore;
    private int workoutActivityScore;
    private int goalProgressScore;
    private int communicationScore;
    private RiskLevel riskLevel;
    private Integer daysSinceLastCheckIn;
    private Integer daysSinceLastWorkout;
    private Integer daysSinceLastMessage;

    public Integer getDaysSinceLastActivity() {
        return Stream.of(daysSinceLastCheckIn, daysSinceLastWorkout, daysSinceLastMessage)
                .filter(value -> value != null)
                .min(Comparator.naturalOrder())
                .orElse(null);
    }
}

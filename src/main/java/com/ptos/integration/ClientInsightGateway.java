package com.ptos.integration;

import com.ptos.domain.CheckIn;
import com.ptos.domain.ClientRecord;
import com.ptos.domain.WorkoutAssignment;
import com.ptos.dto.ClientHealthScoreResult;

import java.util.List;

public interface ClientInsightGateway {

    String generateCheckInSummary(ClientRecord cr, CheckIn current, CheckIn previous);

    String generateAtRiskInsight(ClientRecord cr, ClientHealthScoreResult healthScore);

    String generateWeeklySummary(ClientRecord cr, List<CheckIn> recentCheckIns, List<WorkoutAssignment> recentAssignments);
}

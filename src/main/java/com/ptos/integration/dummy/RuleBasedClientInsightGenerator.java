package com.ptos.integration.dummy;

import com.ptos.domain.AssignmentStatus;
import com.ptos.domain.CheckIn;
import com.ptos.domain.ClientProfile;
import com.ptos.domain.ClientRecord;
import com.ptos.domain.GoalType;
import com.ptos.domain.WorkoutAssignment;
import com.ptos.dto.ClientHealthScoreResult;
import com.ptos.integration.ClientInsightGateway;
import com.ptos.repository.ClientProfileRepository;
import com.ptos.repository.WorkoutAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

// Replace with AiClientInsightGenerator backed by OpenAI/Claude API for production-quality insights.
// This implementation uses simple deterministic rules.
@Service
@RequiredArgsConstructor
public class RuleBasedClientInsightGenerator implements ClientInsightGateway {

    private final ClientProfileRepository clientProfileRepository;
    private final WorkoutAssignmentRepository workoutAssignmentRepository;

    @Override
    public String generateCheckInSummary(ClientRecord cr, CheckIn current, CheckIn previous) {
        List<String> sentences = new ArrayList<>();
        sentences.add(buildWeightSentence(cr, current, previous));

        List<String> trendParts = new ArrayList<>();
        addTrendPart(trendParts, "Mood", previous == null ? null : previous.getMoodScore(), current.getMoodScore());
        addTrendPart(trendParts, "Energy", previous == null ? null : previous.getEnergyScore(), current.getEnergyScore());
        addTrendPart(trendParts, "Sleep quality", previous == null ? null : previous.getSleepScore(), current.getSleepScore());

        String workoutSentence = buildWorkoutSentence(cr, current.getSubmittedAt().toLocalDate());
        if (!trendParts.isEmpty()) {
            sentences.add(joinWithCommas(trendParts) + ". " + workoutSentence);
        } else {
            sentences.add(workoutSentence);
        }

        return String.join(" ", sentences);
    }

    @Override
    public String generateAtRiskInsight(ClientRecord cr, ClientHealthScoreResult healthScore) {
        List<String> reasons = new ArrayList<>();

        if (healthScore.getDaysSinceLastCheckIn() != null) {
            reasons.add("No check-in in " + healthScore.getDaysSinceLastCheckIn() + " days");
        } else {
            reasons.add("No check-ins submitted yet");
        }

        WorkoutWindowStats workoutStats = getRecentWorkoutStats(cr, LocalDate.now());
        if (workoutStats.assigned() > 0) {
            reasons.add("only " + workoutStats.completed() + " of " + workoutStats.assigned() + " workouts completed recently");
        } else {
            reasons.add("no workouts completed recently");
        }

        Integer daysSinceLastMessage = healthScore.getDaysSinceLastMessage();
        if (daysSinceLastMessage != null) {
            reasons.add("no messages exchanged in " + daysSinceLastMessage + " days");
        } else {
            reasons.add("no messages exchanged yet");
        }

        return capitalize(joinWithCommas(reasons)) + ". Consider sending a motivational message or adjusting their programme.";
    }

    @Override
    public String generateWeeklySummary(ClientRecord cr, List<CheckIn> recentCheckIns, List<WorkoutAssignment> recentAssignments) {
        long completedWorkouts = recentAssignments.stream()
                .filter(assignment -> assignment.getStatus() == AssignmentStatus.COMPLETED)
                .filter(assignment -> assignment.getCompletedAt() != null || assignment.getAssignedDate() != null)
                .count();
        boolean submittedCheckIn = !recentCheckIns.isEmpty();

        String firstSentence = cr.getClientUser().getFullName() + " completed " + completedWorkouts + " workout"
                + (completedWorkouts == 1 ? "" : "s")
                + (submittedCheckIn ? " and submitted a check-in." : " and has not submitted a check-in yet.");

        if (recentCheckIns.isEmpty()) {
            return firstSentence + " Weight trend is unclear this week.";
        }

        CheckIn latest = recentCheckIns.stream()
                .max(Comparator.comparing(CheckIn::getSubmittedAt))
                .orElseThrow();
        String direction = resolveWeightTrend(recentCheckIns);
        return firstSentence + " Weight is trending " + direction + " at "
                + formatNumber(latest.getCurrentWeightKg()) + " kg.";
    }

    private String buildWeightSentence(ClientRecord cr, CheckIn current, CheckIn previous) {
        String base;
        if (previous == null) {
            base = "Current weight is " + formatNumber(current.getCurrentWeightKg()) + " kg.";
        } else {
            double delta = current.getCurrentWeightKg() - previous.getCurrentWeightKg();
            if (Math.abs(delta) < 0.1) {
                base = "Weight stayed stable this week.";
            } else if (delta < 0) {
                base = "Weight decreased by " + formatNumber(Math.abs(delta)) + " kg this week.";
            } else {
                base = "Weight increased by " + formatNumber(Math.abs(delta)) + " kg this week.";
            }
        }

        Optional<ClientProfile> profileOpt = clientProfileRepository.findByUserId(cr.getClientUser().getId());
        if (profileOpt.isEmpty() || profileOpt.get().getTargetWeightKg() == null) {
            return base;
        }

        ClientProfile profile = profileOpt.get();
        double targetWeight = profile.getTargetWeightKg();
        String targetDirection = "toward";
        if (previous != null) {
            double previousDistance = Math.abs(previous.getCurrentWeightKg() - targetWeight);
            double currentDistance = Math.abs(current.getCurrentWeightKg() - targetWeight);
            if (currentDistance > previousDistance + 0.1) {
                targetDirection = "away from";
            }
        } else if (profile.getGoalType() == GoalType.MUSCLE_GAIN && current.getCurrentWeightKg() < targetWeight) {
            targetDirection = "toward";
        } else if (profile.getGoalType() == GoalType.WEIGHT_LOSS && current.getCurrentWeightKg() > targetWeight) {
            targetDirection = "toward";
        }

        return base + " This moves " + targetDirection + " the target of " + formatNumber(targetWeight) + " kg.";
    }

    private String buildWorkoutSentence(ClientRecord cr, LocalDate weekEnding) {
        WorkoutWindowStats stats = getRecentWorkoutStats(cr, weekEnding);
        if (stats.assigned() == 0) {
            return "No workouts were assigned this week.";
        }
        return "Completed " + stats.completed() + " of " + stats.assigned() + " assigned workouts this week.";
    }

    private WorkoutWindowStats getRecentWorkoutStats(ClientRecord cr, LocalDate anchorDate) {
        LocalDate from = anchorDate.minusDays(6);
        List<WorkoutAssignment> assignments = workoutAssignmentRepository.findByClientRecordOrderByAssignedDateDesc(cr).stream()
                .filter(assignment -> assignment.getAssignedDate() != null)
                .filter(assignment -> !assignment.getAssignedDate().isBefore(from) && !assignment.getAssignedDate().isAfter(anchorDate))
                .toList();

        long completed = assignments.stream()
                .filter(assignment -> assignment.getStatus() == AssignmentStatus.COMPLETED)
                .count();

        return new WorkoutWindowStats(assignments.size(), completed);
    }

    private void addTrendPart(List<String> parts, String label, Integer previous, Integer current) {
        if (current == null) {
            return;
        }
        if (previous == null) {
            parts.add(label + " is " + current + "/5");
            return;
        }
        if (current > previous) {
            parts.add(label + " improved from " + previous + "/5 to " + current + "/5");
        } else if (current < previous) {
            parts.add(label + " dropped from " + previous + "/5 to " + current + "/5");
        } else {
            parts.add(label + " is consistent at " + current + "/5");
        }
    }

    private String resolveWeightTrend(List<CheckIn> recentCheckIns) {
        if (recentCheckIns.size() < 2) {
            return "stable";
        }
        CheckIn latest = recentCheckIns.stream()
                .max(Comparator.comparing(CheckIn::getSubmittedAt))
                .orElseThrow();
        CheckIn earliest = recentCheckIns.stream()
                .min(Comparator.comparing(CheckIn::getSubmittedAt))
                .orElseThrow();
        double delta = latest.getCurrentWeightKg() - earliest.getCurrentWeightKg();
        if (Math.abs(delta) < 0.1) {
            return "stable";
        }
        return delta < 0 ? "down" : "up";
    }

    private String joinWithCommas(List<String> parts) {
        if (parts.isEmpty()) {
            return "";
        }
        if (parts.size() == 1) {
            return parts.get(0);
        }
        if (parts.size() == 2) {
            return parts.get(0) + " and " + parts.get(1);
        }
        return String.join(", ", parts.subList(0, parts.size() - 1))
                + ", and " + parts.get(parts.size() - 1);
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return value.substring(0, 1).toUpperCase(Locale.ENGLISH) + value.substring(1);
    }

    private String formatNumber(double value) {
        return String.format(Locale.ENGLISH, "%.1f", value);
    }

    private record WorkoutWindowStats(long assigned, long completed) {
    }
}

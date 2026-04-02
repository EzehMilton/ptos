package com.ptos.service;

import com.ptos.domain.AssignmentStatus;
import com.ptos.domain.CheckIn;
import com.ptos.domain.ClientProfile;
import com.ptos.domain.ClientRecord;
import com.ptos.domain.User;
import com.ptos.domain.WorkoutAssignment;
import com.ptos.dto.Milestone;
import com.ptos.repository.CheckInRepository;
import com.ptos.repository.ClientProfileRepository;
import com.ptos.repository.ClientRecordRepository;
import com.ptos.repository.WorkoutAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MilestoneService {

    private final WorkoutAssignmentRepository workoutAssignmentRepository;
    private final CheckInRepository checkInRepository;
    private final ClientProfileRepository clientProfileRepository;
    private final ClientRecordRepository clientRecordRepository;

    public List<Milestone> getMilestones(ClientRecord clientRecord, User clientUser) {
        List<WorkoutAssignment> completedAssignments = workoutAssignmentRepository.findByClientRecordOrderByAssignedDateDesc(clientRecord)
                .stream()
                .filter(assignment -> assignment.getStatus() == AssignmentStatus.COMPLETED && assignment.getCompletedAt() != null)
                .sorted(Comparator.comparing(WorkoutAssignment::getCompletedAt))
                .toList();
        List<CheckIn> checkIns = checkInRepository.findByClientRecordOrderBySubmittedAtDesc(clientRecord)
                .stream()
                .sorted(Comparator.comparing(CheckIn::getSubmittedAt))
                .toList();
        Optional<ClientProfile> profileOpt = clientProfileRepository.findByUserId(clientUser.getId());

        List<Milestone> milestones = new ArrayList<>();
        milestones.add(buildWorkoutMilestone("💪", "First Workout Completed",
                "Completed the first assigned workout.", completedAssignments, 1));
        milestones.add(buildWorkoutMilestone("🏋️", "3 Workouts Completed",
                "Completed three workouts.", completedAssignments, 3));
        milestones.add(buildWorkoutMilestone("🔥", "5 Workouts Completed",
                "Completed five workouts.", completedAssignments, 5));
        milestones.add(buildCheckInMilestone("📝", "First Check-in Submitted",
                "Submitted the first progress update.", checkIns, 1));
        milestones.add(buildCheckInMilestone("📋", "Consistent Check-ins",
                "Submitted at least two check-ins.", checkIns, 2));
        milestones.add(buildWeightMilestone("⚖️", "Weight Milestone",
                "Moved closer to the target weight.", profileOpt.orElse(null), checkIns));
        milestones.add(buildProfileMilestone("✅", "Profile Complete",
                "Filled out the full client profile.", profileOpt.orElse(null)));

        List<Milestone> achieved = milestones.stream()
                .filter(milestone -> milestone.getAchievedDate() != null)
                .sorted(Comparator.comparing(Milestone::getAchievedDate).reversed())
                .toList();
        List<Milestone> unachieved = milestones.stream()
                .filter(milestone -> milestone.getAchievedDate() == null)
                .toList();

        List<Milestone> ordered = new ArrayList<>(achieved);
        ordered.addAll(unachieved);
        return ordered;
    }

    public List<String> getRecentMilestoneFeed(User ptUser) {
        LocalDate threshold = LocalDate.now().minusDays(7);
        return clientRecordRepository.findByPtUser(ptUser).stream()
                .flatMap(record -> getMilestones(record, record.getClientUser()).stream()
                        .filter(milestone -> milestone.getAchievedDate() != null)
                        .filter(milestone -> !milestone.getAchievedDate().isBefore(threshold))
                        .map(milestone -> new RecentMilestone(record.getClientUser().getFullName(), milestone.getTitle(), milestone.getAchievedDate())))
                .sorted(Comparator.comparing(RecentMilestone::achievedDate).reversed())
                .map(item -> "🏆 " + item.clientName() + " achieved: " + item.title())
                .limit(5)
                .toList();
    }

    private Milestone buildWorkoutMilestone(String icon,
                                            String title,
                                            String description,
                                            List<WorkoutAssignment> completedAssignments,
                                            int targetCount) {
        LocalDate achievedDate = completedAssignments.size() >= targetCount
                ? completedAssignments.get(targetCount - 1).getCompletedAt().toLocalDate()
                : null;
        return Milestone.builder()
                .icon(icon)
                .title(title)
                .description(description)
                .achievedDate(achievedDate)
                .build();
    }

    private Milestone buildCheckInMilestone(String icon,
                                            String title,
                                            String description,
                                            List<CheckIn> checkIns,
                                            int targetCount) {
        LocalDate achievedDate = checkIns.size() >= targetCount
                ? checkIns.get(targetCount - 1).getSubmittedAt().toLocalDate()
                : null;
        return Milestone.builder()
                .icon(icon)
                .title(title)
                .description(description)
                .achievedDate(achievedDate)
                .build();
    }

    private Milestone buildWeightMilestone(String icon,
                                           String title,
                                           String description,
                                           ClientProfile profile,
                                           List<CheckIn> checkIns) {
        LocalDate achievedDate = null;
        if (profile != null && profile.getTargetWeightKg() != null && checkIns.size() >= 2) {
            double targetWeight = profile.getTargetWeightKg();
            double startingDistance = Math.abs(checkIns.get(0).getCurrentWeightKg() - targetWeight);
            for (CheckIn checkIn : checkIns) {
                double distance = Math.abs(checkIn.getCurrentWeightKg() - targetWeight);
                if (distance < startingDistance) {
                    achievedDate = checkIn.getSubmittedAt().toLocalDate();
                    break;
                }
            }
        }

        return Milestone.builder()
                .icon(icon)
                .title(title)
                .description(description)
                .achievedDate(achievedDate)
                .build();
    }

    private Milestone buildProfileMilestone(String icon,
                                            String title,
                                            String description,
                                            ClientProfile profile) {
        LocalDate achievedDate = null;
        if (profile != null
                && profile.getAge() != null
                && profile.getHeightCm() != null
                && profile.getCurrentWeightKg() != null
                && profile.getGoalType() != null
                && profile.getTargetWeightKg() != null
                && profile.getTrainingExperience() != null
                && profile.getUpdatedAt() != null) {
            achievedDate = profile.getUpdatedAt().toLocalDate();
        }

        return Milestone.builder()
                .icon(icon)
                .title(title)
                .description(description)
                .achievedDate(achievedDate)
                .build();
    }

    private record RecentMilestone(String clientName, String title, LocalDate achievedDate) {
    }
}

package com.ptos.config;

import com.ptos.domain.*;
import com.ptos.repository.CheckInFeedbackRepository;
import com.ptos.repository.CheckInRepository;
import com.ptos.repository.ClientProfileRepository;
import com.ptos.repository.ClientRecordRepository;
import com.ptos.repository.UserRepository;
import com.ptos.repository.WorkoutAssignmentRepository;
import com.ptos.repository.WorkoutRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(name = "ptos.seed-data", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ClientProfileRepository clientProfileRepository;
    private final ClientRecordRepository clientRecordRepository;
    private final WorkoutRepository workoutRepository;
    private final WorkoutAssignmentRepository workoutAssignmentRepository;
    private final CheckInRepository checkInRepository;
    private final CheckInFeedbackRepository checkInFeedbackRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 1) {
            return;
        }

        String encoded = passwordEncoder.encode("password123");

        User pt = userRepository.save(User.builder()
                .email("pt@ptos.local")
                .password(encoded)
                .fullName("Michael Uchenna Ezeh")
                .role(Role.PT)
                .enabled(true)
                .build());
        log.info("Seeded PT user: pt@ptos.local");

        LocalDate today = LocalDate.now();
        List<ClientRecord> records = new ArrayList<>();

        records.add(seedClient(pt, encoded, "Alex Morgan", "alex@ptos.local",
                ClientProfile.builder()
                        .age(28).heightCm(180.0).currentWeightKg(85.0)
                        .goalType(GoalType.WEIGHT_LOSS).targetWeightKg(72.0)
                        .trainingExperience(TrainingExperience.INTERMEDIATE)
                        .dietaryPreferences("No dairy"),
                ClientStatus.ACTIVE, new BigDecimal("120.00"), today.minusDays(75)));

        records.add(seedClient(pt, encoded, "Jamie Chen", "jamie@ptos.local",
                ClientProfile.builder()
                        .age(24).heightCm(175.0).currentWeightKg(70.0)
                        .goalType(GoalType.MUSCLE_GAIN).targetWeightKg(78.0)
                        .trainingExperience(TrainingExperience.BEGINNER)
                        .dietaryPreferences("High protein"),
                ClientStatus.ACTIVE, new BigDecimal("150.00"), today.minusDays(60)));

        records.add(seedClient(pt, encoded, "Sam Williams", "sam@ptos.local",
                ClientProfile.builder()
                        .age(31).heightCm(188.0).currentWeightKg(95.0)
                        .goalType(GoalType.STRENGTH)
                        .trainingExperience(TrainingExperience.ADVANCED)
                        .injuriesOrConditions("Previous knee injury — avoid heavy squats"),
                ClientStatus.AT_RISK, new BigDecimal("100.00"), today.minusDays(90)));

        records.add(seedClient(pt, encoded, "Nnuola Ezeh", "nnuola@ptos.local",
                ClientProfile.builder()
                        .age(26).heightCm(165.0).currentWeightKg(78.0)
                        .goalType(GoalType.WEIGHT_LOSS).targetWeightKg(65.0)
                        .trainingExperience(TrainingExperience.BEGINNER)
                        .dietaryPreferences("Vegetarian"),
                ClientStatus.ACTIVE, new BigDecimal("120.00"), today.minusDays(45)));

        records.add(seedClient(pt, encoded, "Ben Patel", "ben@ptos.local",
                ClientProfile.builder()
                        .age(35).heightCm(178.0).currentWeightKg(82.0)
                        .goalType(GoalType.GENERAL_FITNESS).targetWeightKg(80.0)
                        .trainingExperience(TrainingExperience.INTERMEDIATE)
                        .notes("Prefers morning sessions"),
                ClientStatus.ACTIVE, new BigDecimal("90.00"), today.minusDays(30)));

        records.add(seedClient(pt, encoded, "Chloe Smith", "chloe@ptos.local",
                ClientProfile.builder()
                        .age(29).heightCm(170.0).currentWeightKg(60.0),
                ClientStatus.ACTIVE, new BigDecimal("100.00"), today.minusDays(20)));

        records.add(seedClient(pt, encoded, "Chikere Ezeh", "chikere@ptos.local",
                ClientProfile.builder()
                        .currentWeightKg(105.0),
                ClientStatus.INACTIVE, null, today.minusDays(85)));

        records.add(seedClient(pt, encoded, "Mia Johnson", "mia@ptos.local",
                ClientProfile.builder()
                        .age(22).heightCm(162.0).currentWeightKg(90.0)
                        .goalType(GoalType.WEIGHT_LOSS).targetWeightKg(70.0)
                        .trainingExperience(TrainingExperience.BEGINNER)
                        .dietaryPreferences("Gluten-free"),
                ClientStatus.ACTIVE, new BigDecimal("130.00"), today.minusDays(10)));
        records.add(seedClient(pt, encoded, "Emeka Okafor", "emeka@ptos.local",
                ClientProfile.builder()
                        .age(33).heightCm(182.0).currentWeightKg(92.0)
                        .goalType(GoalType.WEIGHT_LOSS).targetWeightKg(80.0)
                        .trainingExperience(TrainingExperience.INTERMEDIATE)
                        .notes("Travels often — prefers flexible schedule"),
                ClientStatus.ACTIVE, new BigDecimal("110.00"), today.minusDays(50)));

        records.add(seedClient(pt, encoded, "Ngozi Nwankwo", "ngozi@ptos.local",
                ClientProfile.builder()
                        .age(41).heightCm(168.0).currentWeightKg(88.0)
                        .goalType(GoalType.GENERAL_FITNESS).targetWeightKg(75.0)
                        .trainingExperience(TrainingExperience.BEGINNER)
                        .dietaryPreferences("Low carb"),
                ClientStatus.ACTIVE, new BigDecimal("95.00"), today.minusDays(40)));

        records.add(seedClient(pt, encoded, "Tunde Adeyemi", "tunde@ptos.local",
                ClientProfile.builder()
                        .age(37).heightCm(176.0).currentWeightKg(85.0)
                        .goalType(GoalType.STRENGTH)
                        .trainingExperience(TrainingExperience.INTERMEDIATE)
                        .notes("Focus on upper body strength"),
                ClientStatus.AT_RISK, new BigDecimal("105.00"), today.minusDays(70)));

        records.add(seedClient(pt, encoded, "Zainab Bello", "zainab@ptos.local",
                ClientProfile.builder()
                        .age(27).heightCm(160.0).currentWeightKg(72.0)
                        .goalType(GoalType.WEIGHT_LOSS).targetWeightKg(60.0)
                        .trainingExperience(TrainingExperience.BEGINNER)
                        .dietaryPreferences("Halal diet"),
                ClientStatus.ACTIVE, new BigDecimal("120.00"), today.minusDays(25)));

        records.add(seedClient(pt, encoded, "Ifeanyi Obi", "ifeanyi@ptos.local",
                ClientProfile.builder()
                        .age(30).heightCm(185.0).currentWeightKg(98.0)
                        .goalType(GoalType.MUSCLE_GAIN).targetWeightKg(105.0)
                        .trainingExperience(TrainingExperience.INTERMEDIATE)
                        .notes("Wants structured hypertrophy program"),
                ClientStatus.ACTIVE, new BigDecimal("140.00"), today.minusDays(15)));

        records.add(seedClient(pt, encoded, "Funmi Ogunleye", "funmi@ptos.local",
                ClientProfile.builder()
                        .age(34).heightCm(167.0).currentWeightKg(76.0)
                        .goalType(GoalType.GENERAL_FITNESS)
                        .trainingExperience(TrainingExperience.BEGINNER)
                        .injuriesOrConditions("Lower back pain — avoid deadlifts"),
                ClientStatus.INACTIVE, new BigDecimal("85.00"), today.minusDays(100)));
        records.add(seedClient(pt, encoded, "Carlos Ramirez", "carlos@ptos.local",
                ClientProfile.builder()
                        .age(36).heightCm(174.0).currentWeightKg(88.0)
                        .goalType(GoalType.WEIGHT_LOSS).targetWeightKg(78.0)
                        .trainingExperience(TrainingExperience.INTERMEDIATE)
                        .dietaryPreferences("Mediterranean diet"),
                ClientStatus.ACTIVE, new BigDecimal("115.00"), today.minusDays(55)));

        records.add(seedClient(pt, encoded, "Lucia Fernandez", "lucia@ptos.local",
                ClientProfile.builder()
                        .age(25).heightCm(168.0).currentWeightKg(62.0)
                        .goalType(GoalType.MUSCLE_GAIN).targetWeightKg(68.0)
                        .trainingExperience(TrainingExperience.BEGINNER)
                        .notes("Prefers evening workouts"),
                ClientStatus.ACTIVE, new BigDecimal("125.00"), today.minusDays(18)));

        records.add(seedClient(pt, encoded, "Javier Morales", "javier@ptos.local",
                ClientProfile.builder()
                        .age(42).heightCm(180.0).currentWeightKg(94.0)
                        .goalType(GoalType.GENERAL_FITNESS).targetWeightKg(85.0)
                        .trainingExperience(TrainingExperience.INTERMEDIATE)
                        .injuriesOrConditions("Shoulder strain — avoid overhead press"),
                ClientStatus.AT_RISK, new BigDecimal("100.00"), today.minusDays(80)));

        seedHistoricalActivity(pt, records, today);
        log.info("Seeded {} clients with profiles, workouts, assignments, and check-ins", records.size());
    }

    private ClientRecord seedClient(User pt, String encodedPassword, String fullName, String email,
                                    ClientProfile.ClientProfileBuilder profileBuilder,
                                    ClientStatus status, BigDecimal packagePrice, LocalDate startDate) {
        User user = userRepository.save(User.builder()
                .email(email)
                .password(encodedPassword)
                .fullName(fullName)
                .role(Role.CLIENT)
                .enabled(true)
                .build());

        clientProfileRepository.save(profileBuilder.user(user).build());

        return clientRecordRepository.save(ClientRecord.builder()
                .ptUser(pt)
                .clientUser(user)
                .status(status)
                .monthlyPackagePrice(packagePrice)
                .startDate(startDate)
                .build());
    }

    private void seedHistoricalActivity(User pt, List<ClientRecord> records, LocalDate today) {
        Workout fatLoss = createWorkout(pt,
                "Fat Loss Foundation",
                "Full body circuit focused on consistency and calorie burn.",
                WorkoutCategory.FULL_BODY,
                new String[][]{
                        {"Goblet Squat", "3", "12", "Steady tempo"},
                        {"Incline Push-Up", "3", "10", "Keep core tight"},
                        {"Bike Intervals", "6", "30 sec", "Hard effort"}
                });

        Workout strength = createWorkout(pt,
                "Strength Builder",
                "Compound lifts with accessory work.",
                WorkoutCategory.STRENGTH,
                new String[][]{
                        {"Deadlift", "4", "5", "Reset each rep"},
                        {"Bench Press", "4", "6", "Controlled lowering"},
                        {"Split Squat", "3", "8/side", "Full range"}
                });

        Workout mobility = createWorkout(pt,
                "Mobility Reset",
                "Light session for recovery and range of motion.",
                WorkoutCategory.FLEXIBILITY,
                new String[][]{
                        {"Hip Flow", "2", "60 sec", "Smooth reps"},
                        {"Thoracic Rotations", "2", "12/side", "Pause at end range"},
                        {"Breathing Drill", "3", "5 breaths", "Slow nasal breathing"}
                });

        seedAssignments(records.get(0),
                new AssignmentSeed(fatLoss, today.minusDays(10), AssignmentStatus.COMPLETED,
                        today.minusDays(10).atTime(7, 30), today.minusDays(9).atTime(8, 20), "Felt strong."),
                new AssignmentSeed(strength, today.minusDays(2), AssignmentStatus.IN_PROGRESS,
                        today.minusDays(2).atTime(7, 45), null, null));
        seedCheckIns(records.get(0), pt,
                new CheckInSeed(today.minusDays(12).atTime(9, 15), 86.5, 3, 3, 3,
                        "Busy week but stayed consistent.", true, "Solid effort. Keep protein high."),
                new CheckInSeed(today.minusDays(3).atTime(8, 40), 84.8, 4, 4, 4,
                        "Energy is improving and steps are up.", false, null));

        seedAssignments(records.get(1),
                new AssignmentSeed(strength, today.minusDays(14), AssignmentStatus.COMPLETED,
                        today.minusDays(14).atTime(18, 0), today.minusDays(13).atTime(19, 5), "Added 2.5kg to bench."),
                new AssignmentSeed(mobility, today.minusDays(5), AssignmentStatus.ASSIGNED,
                        null, null, null));
        seedCheckIns(records.get(1), pt,
                new CheckInSeed(today.minusDays(8).atTime(19, 10), 69.8, 4, 3, 4,
                        "Lifts are moving well.", true, "Great momentum. Keep pushing calories slightly up."),
                new CheckInSeed(today.minusDays(1).atTime(20, 5), 71.0, 5, 4, 4,
                        "Feeling stronger and recovering better.", false, null));

        seedAssignments(records.get(3),
                new AssignmentSeed(fatLoss, today.minusDays(18), AssignmentStatus.COMPLETED,
                        today.minusDays(18).atTime(6, 45), today.minusDays(17).atTime(7, 40), "Tough but manageable."),
                new AssignmentSeed(mobility, today.minusDays(1), AssignmentStatus.ASSIGNED,
                        null, null, null));
        seedCheckIns(records.get(3), pt,
                new CheckInSeed(today.minusDays(2).atTime(7, 15), 79.2, 4, 3, 5,
                        "Sleep improved a lot this week.", true, "Excellent recovery. We can progress volume next week."));

        seedAssignments(records.get(4),
                new AssignmentSeed(strength, today.minusDays(20), AssignmentStatus.ASSIGNED,
                        null, null, null));
        seedCheckIns(records.get(4), pt,
                new CheckInSeed(today.minusDays(15).atTime(6, 55), 82.0, 2, 2, 2,
                        "Missed sessions due to travel.", true, "Let’s reset with two shorter sessions next week."));

        seedAssignments(records.get(7),
                new AssignmentSeed(fatLoss, today.minusDays(9), AssignmentStatus.COMPLETED,
                        today.minusDays(9).atTime(12, 0), today.minusDays(8).atTime(12, 50), "Managed all intervals."),
                new AssignmentSeed(strength, today.minusDays(4), AssignmentStatus.COMPLETED,
                        today.minusDays(4).atTime(12, 15), today.minusDays(3).atTime(13, 5), "Felt more confident with form."));
        seedCheckIns(records.get(7), pt,
                new CheckInSeed(today.minusDays(9).atTime(18, 45), 91.2, 3, 3, 3,
                        "Week started slow.", true, "Good reset. Keep your step target consistent."),
                new CheckInSeed(today.minusDays(4).atTime(18, 50), 89.9, 4, 4, 4,
                        "Much better routine this week.", true, "Great turnaround. Let’s build on this."));
    }

    private Workout createWorkout(User pt, String name, String description, WorkoutCategory category, String[][] exercises) {
        Workout workout = Workout.builder()
                .ptUser(pt)
                .name(name)
                .description(description)
                .category(category)
                .build();

        for (int i = 0; i < exercises.length; i++) {
            String[] row = exercises[i];
            workout.getExercises().add(WorkoutExercise.builder()
                    .workout(workout)
                    .exerciseName(row[0])
                    .setsCount(Integer.parseInt(row[1]))
                    .repsText(row[2])
                    .notes(row[3])
                    .sortOrder(i)
                    .build());
        }

        return workoutRepository.save(workout);
    }

    private void seedAssignments(ClientRecord record, AssignmentSeed... assignments) {
        for (AssignmentSeed seed : assignments) {
            workoutAssignmentRepository.save(WorkoutAssignment.builder()
                    .workout(seed.workout())
                    .clientRecord(record)
                    .assignedDate(seed.assignedDate())
                    .status(seed.status())
                    .startedAt(seed.startedAt())
                    .completedAt(seed.completedAt())
                    .completionNotes(seed.completionNotes())
                    .build());
        }
    }

    private void seedCheckIns(ClientRecord record, User pt, CheckInSeed... seeds) {
        LocalDateTime latestSubmittedAt = null;
        Double latestWeight = null;

        for (CheckInSeed seed : seeds) {
            CheckIn checkIn = checkInRepository.save(CheckIn.builder()
                    .clientRecord(record)
                    .submittedAt(seed.submittedAt())
                    .currentWeightKg(seed.currentWeightKg())
                    .moodScore(seed.moodScore())
                    .energyScore(seed.energyScore())
                    .sleepScore(seed.sleepScore())
                    .notes(seed.notes())
                    .status(seed.reviewed() ? CheckInStatus.REVIEWED : CheckInStatus.PENDING_REVIEW)
                    .build());

            if (seed.reviewed() && seed.feedbackText() != null) {
                CheckInFeedback feedback = checkInFeedbackRepository.save(CheckInFeedback.builder()
                        .checkIn(checkIn)
                        .ptUser(pt)
                        .feedbackText(seed.feedbackText())
                        .sentAt(seed.submittedAt().plusDays(1))
                        .build());
                checkIn.setFeedback(feedback);
                checkInRepository.save(checkIn);
            }

            if (latestSubmittedAt == null || seed.submittedAt().isAfter(latestSubmittedAt)) {
                latestSubmittedAt = seed.submittedAt();
                latestWeight = seed.currentWeightKg();
            }
        }

        if (latestWeight != null) {
            Double finalLatestWeight = latestWeight;
            clientProfileRepository.findByUserId(record.getClientUser().getId()).ifPresent(profile -> {
                profile.setCurrentWeightKg(finalLatestWeight);
                clientProfileRepository.save(profile);
            });
        }
    }

    private record AssignmentSeed(Workout workout,
                                  LocalDate assignedDate,
                                  AssignmentStatus status,
                                  LocalDateTime startedAt,
                                  LocalDateTime completedAt,
                                  String completionNotes) {
    }

    private record CheckInSeed(LocalDateTime submittedAt,
                               double currentWeightKg,
                               Integer moodScore,
                               Integer energyScore,
                               Integer sleepScore,
                               String notes,
                               boolean reviewed,
                               String feedbackText) {
    }
}

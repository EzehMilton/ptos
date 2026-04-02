package com.ptos.config;

import com.ptos.domain.*;
import com.ptos.repository.ClientProfileRepository;
import com.ptos.repository.ClientRecordRepository;
import com.ptos.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@ConditionalOnProperty(name = "ptos.seed-data", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ClientProfileRepository clientProfileRepository;
    private final ClientRecordRepository clientRecordRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 1) {
            return;
        }

        String encoded = passwordEncoder.encode("password123");

        // Seed PT
        User pt;
        if (!userRepository.existsByEmail("pt@ptos.local")) {
            pt = userRepository.save(User.builder()
                    .email("pt@ptos.local")
                    .password(encoded)
                    .fullName("Michael Uchenna Ezeh")
                    .role(Role.PT)
                    .enabled(true)
                    .build());
            log.info("Seeded PT user: pt@ptos.local");
        } else {
            pt = userRepository.findByEmail("pt@ptos.local").orElseThrow();
        }

        LocalDate today = LocalDate.now();
        int count = 0;

        // 1. Alex Morgan — full profile, ACTIVE
        count += seedClient(pt, encoded, "Alex Morgan", "alex@ptos.local",
                ClientProfile.builder()
                        .age(28).heightCm(180.0).currentWeightKg(85.0)
                        .goalType(GoalType.WEIGHT_LOSS).targetWeightKg(72.0)
                        .trainingExperience(TrainingExperience.INTERMEDIATE)
                        .dietaryPreferences("No dairy"),
                ClientStatus.ACTIVE, new BigDecimal("120.00"), today.minusDays(75));

        // 2. Jamie Chen — full profile, ACTIVE
        count += seedClient(pt, encoded, "Jamie Chen", "jamie@ptos.local",
                ClientProfile.builder()
                        .age(24).heightCm(175.0).currentWeightKg(70.0)
                        .goalType(GoalType.MUSCLE_GAIN).targetWeightKg(78.0)
                        .trainingExperience(TrainingExperience.BEGINNER)
                        .dietaryPreferences("High protein"),
                ClientStatus.ACTIVE, new BigDecimal("150.00"), today.minusDays(60));

        // 3. Sam Williams — full profile, AT_RISK
        count += seedClient(pt, encoded, "Sam Williams", "sam@ptos.local",
                ClientProfile.builder()
                        .age(31).heightCm(188.0).currentWeightKg(95.0)
                        .goalType(GoalType.STRENGTH)
                        .trainingExperience(TrainingExperience.ADVANCED)
                        .injuriesOrConditions("Previous knee injury — avoid heavy squats"),
                ClientStatus.AT_RISK, new BigDecimal("100.00"), today.minusDays(90));

        // 4. Nnuola Ezeh — full profile, ACTIVE
        count += seedClient(pt, encoded, "Nnuola Ezeh", "nnuola@ptos.local",
                ClientProfile.builder()
                        .age(26).heightCm(165.0).currentWeightKg(78.0)
                        .goalType(GoalType.WEIGHT_LOSS).targetWeightKg(65.0)
                        .trainingExperience(TrainingExperience.BEGINNER)
                        .dietaryPreferences("Vegetarian"),
                ClientStatus.ACTIVE, new BigDecimal("120.00"), today.minusDays(45));

        // 5. Ben Patel — full profile, ACTIVE
        count += seedClient(pt, encoded, "Ben Patel", "ben@ptos.local",
                ClientProfile.builder()
                        .age(35).heightCm(178.0).currentWeightKg(82.0)
                        .goalType(GoalType.GENERAL_FITNESS).targetWeightKg(80.0)
                        .trainingExperience(TrainingExperience.INTERMEDIATE)
                        .notes("Prefers morning sessions"),
                ClientStatus.ACTIVE, new BigDecimal("90.00"), today.minusDays(30));

        // 6. Chloe Smith — partial profile (3/6), ACTIVE
        count += seedClient(pt, encoded, "Chloe Smith", "chloe@ptos.local",
                ClientProfile.builder()
                        .age(29).heightCm(170.0).currentWeightKg(60.0),
                ClientStatus.ACTIVE, new BigDecimal("100.00"), today.minusDays(20));

        // 7. Chikere Ezeh — minimal profile (1/6), INACTIVE
        count += seedClient(pt, encoded, "Chikere Ezeh", "chikere@ptos.local",
                ClientProfile.builder()
                        .currentWeightKg(75.0),
                ClientStatus.INACTIVE, null, today.minusDays(85));

        // 8. Mia Johnson — full profile, ACTIVE
        count += seedClient(pt, encoded, "Mia Johnson", "mia@ptos.local",
                ClientProfile.builder()
                        .age(22).heightCm(162.0).currentWeightKg(90.0)
                        .goalType(GoalType.WEIGHT_LOSS).targetWeightKg(70.0)
                        .trainingExperience(TrainingExperience.BEGINNER)
                        .dietaryPreferences("Gluten-free"),
                ClientStatus.ACTIVE, new BigDecimal("130.00"), today.minusDays(10));

        log.info("Seeded {} clients with profiles and records", count);
    }

    private int seedClient(User pt, String encodedPassword, String fullName, String email,
                           ClientProfile.ClientProfileBuilder profileBuilder,
                           ClientStatus status, BigDecimal packagePrice, LocalDate startDate) {
        if (userRepository.existsByEmail(email)) {
            return 0;
        }
        User user = userRepository.save(User.builder()
                .email(email)
                .password(encodedPassword)
                .fullName(fullName)
                .role(Role.CLIENT)
                .enabled(true)
                .build());
        clientProfileRepository.save(profileBuilder.user(user).build());
        clientRecordRepository.save(ClientRecord.builder()
                .ptUser(pt)
                .clientUser(user)
                .status(status)
                .monthlyPackagePrice(packagePrice)
                .startDate(startDate)
                .build());
        return 1;
    }
}

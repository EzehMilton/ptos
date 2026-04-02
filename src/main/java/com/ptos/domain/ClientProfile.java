package com.ptos.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "client_profiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ClientProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private Integer age;

    private Double heightCm;

    private Double currentWeightKg;

    @Enumerated(EnumType.STRING)
    private GoalType goalType;

    private Double targetWeightKg;

    @Column(length = 500)
    private String injuriesOrConditions;

    @Column(length = 500)
    private String dietaryPreferences;

    @Enumerated(EnumType.STRING)
    private TrainingExperience trainingExperience;

    @Column(length = 1000)
    private String notes;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

package com.ptos.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "workout_exercises")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkoutExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "workout_id", nullable = false)
    private Workout workout;

    @Column(nullable = false, length = 100)
    private String exerciseName;

    @Column(nullable = false)
    private Integer setsCount;

    @Column(length = 50)
    private String repsText;

    @Column(length = 300)
    private String notes;

    @Column(nullable = false)
    private Integer sortOrder;
}

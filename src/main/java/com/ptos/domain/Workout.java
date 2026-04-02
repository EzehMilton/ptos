package com.ptos.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "workouts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Workout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "pt_user_id", nullable = false)
    private User ptUser;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkoutCategory category;

    @Builder.Default
    private boolean isTemplate = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "workout", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder")
    @Builder.Default
    private List<WorkoutExercise> exercises = new ArrayList<>();
}

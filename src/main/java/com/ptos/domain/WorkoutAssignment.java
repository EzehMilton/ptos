package com.ptos.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "workout_assignments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkoutAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "workout_id", nullable = false)
    private Workout workout;

    @ManyToOne
    @JoinColumn(name = "client_record_id", nullable = false)
    private ClientRecord clientRecord;

    @Column(nullable = false)
    private LocalDate assignedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AssignmentStatus status = AssignmentStatus.ASSIGNED;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    @Column(length = 500)
    private String completionNotes;

    @CreationTimestamp
    private LocalDateTime createdAt;
}

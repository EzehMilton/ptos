package com.ptos.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

@Entity
@Table(name = "check_ins")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CheckIn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "client_record_id", nullable = false)
    private ClientRecord clientRecord;

    @Column(nullable = false)
    private LocalDateTime submittedAt;

    @Column(nullable = false)
    private Double currentWeightKg;

    private Integer moodScore;

    private Integer energyScore;

    private Integer sleepScore;

    @Column(length = 1000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CheckInStatus status = CheckInStatus.PENDING_REVIEW;

    @OneToOne(mappedBy = "checkIn", cascade = CascadeType.ALL)
    private CheckInFeedback feedback;

    @OneToMany(mappedBy = "checkIn", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CheckInPhoto> photos = new ArrayList<>();
}

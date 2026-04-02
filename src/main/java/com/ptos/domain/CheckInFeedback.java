package com.ptos.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "check_in_feedback")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CheckInFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "check_in_id", nullable = false)
    private CheckIn checkIn;

    @ManyToOne
    @JoinColumn(name = "pt_user_id", nullable = false)
    private User ptUser;

    @Column(length = 2000, nullable = false)
    private String feedbackText;

    @Column(nullable = false)
    private LocalDateTime sentAt;
}

package com.ptos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "conversations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"pt_user_id", "client_user_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "pt_user_id", nullable = false)
    private User ptUser;

    @ManyToOne
    @JoinColumn(name = "client_user_id", nullable = false)
    private User clientUser;

    private LocalDateTime lastMessageAt;

    @Builder.Default
    @Column(nullable = false)
    private int unreadCountPt = 0;

    @Builder.Default
    @Column(nullable = false)
    private int unreadCountClient = 0;

    @CreationTimestamp
    private LocalDateTime createdAt;
}

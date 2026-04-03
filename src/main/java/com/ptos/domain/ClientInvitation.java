package com.ptos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "client_invitations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "pt_user_id", nullable = false)
    private User ptUser;

    @NotBlank
    @Email
    @Size(max = 255)
    @Column(nullable = false, length = 255)
    private String email;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, unique = true, length = 36)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private InvitationStatus status = InvitationStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime acceptedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;
}

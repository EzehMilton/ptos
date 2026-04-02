package com.ptos.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "client_records",
       uniqueConstraints = @UniqueConstraint(columnNames = {"pt_user_id", "client_user_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ClientRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "pt_user_id", nullable = false)
    private User ptUser;

    @ManyToOne
    @JoinColumn(name = "client_user_id", nullable = false)
    private User clientUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ClientStatus status = ClientStatus.ACTIVE;

    @Column(nullable = false)
    @Builder.Default
    private LocalDate startDate = LocalDate.now();

    private BigDecimal monthlyPackagePrice;

    @Column(length = 2000)
    private String ptNotes;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

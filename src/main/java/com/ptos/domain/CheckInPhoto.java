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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "check_in_photos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckInPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "check_in_id", nullable = false)
    private CheckIn checkIn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PhotoType photoType;

    @Column(nullable = false)
    private String storageKey;

    @Column(length = 255)
    private String originalFilename;

    private Long fileSize;

    @CreationTimestamp
    private LocalDateTime uploadedAt;
}

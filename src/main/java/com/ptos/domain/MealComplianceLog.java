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
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "meal_compliance_logs",
        uniqueConstraints = @UniqueConstraint(columnNames = {"client_record_id", "log_date"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MealComplianceLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "client_record_id", nullable = false)
    private ClientRecord clientRecord;

    @ManyToOne
    @JoinColumn(name = "meal_plan_id", nullable = false)
    private MealPlan mealPlan;

    @NotNull
    @Column(name = "log_date", nullable = false)
    private LocalDate date;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplianceLevel compliance;

    @Size(max = 500)
    @Column(length = 500)
    private String notes;

    @CreationTimestamp
    private LocalDateTime loggedAt;
}

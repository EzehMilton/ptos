package com.ptos.repository;

import com.ptos.domain.ClientRecord;
import com.ptos.domain.ComplianceLevel;
import com.ptos.domain.MealComplianceLog;
import com.ptos.domain.MealPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MealComplianceLogRepository extends JpaRepository<MealComplianceLog, Long> {

    List<MealComplianceLog> findByClientRecordOrderByDateDesc(ClientRecord clientRecord);

    List<MealComplianceLog> findByClientRecordAndDateBetween(ClientRecord clientRecord, LocalDate from, LocalDate to);

    List<MealComplianceLog> findByMealPlan(MealPlan mealPlan);

    Optional<MealComplianceLog> findByClientRecordAndDate(ClientRecord clientRecord, LocalDate date);

    long countByClientRecordAndComplianceAndDateAfter(ClientRecord clientRecord,
                                                      ComplianceLevel compliance,
                                                      LocalDate after);
}

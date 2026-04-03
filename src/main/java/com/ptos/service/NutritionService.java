package com.ptos.service;

import com.ptos.domain.ClientRecord;
import com.ptos.domain.ClientStatus;
import com.ptos.domain.ComplianceLevel;
import com.ptos.domain.MealComplianceLog;
import com.ptos.domain.MealPlan;
import com.ptos.domain.User;
import com.ptos.dto.MealPlanForm;
import com.ptos.repository.ClientRecordRepository;
import com.ptos.repository.MealComplianceLogRepository;
import com.ptos.repository.MealPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NutritionService {

    private final MealPlanRepository mealPlanRepository;
    private final MealComplianceLogRepository mealComplianceLogRepository;
    private final ClientRecordRepository clientRecordRepository;

    public Optional<MealPlan> getActiveMealPlan(ClientRecord clientRecord) {
        return mealPlanRepository.findByClientRecordAndActiveTrue(clientRecord);
    }

    @Transactional
    public MealPlan createMealPlan(User ptUser, Long clientRecordId, MealPlanForm form) {
        ClientRecord clientRecord = clientRecordRepository.findByIdAndPtUser(clientRecordId, ptUser)
                .orElseThrow(() -> new IllegalArgumentException("Client record not found"));

        mealPlanRepository.findByClientRecordAndActiveTrue(clientRecord).ifPresent(existingPlan -> {
            existingPlan.setActive(false);
            mealPlanRepository.save(existingPlan);
        });

        MealPlan mealPlan = MealPlan.builder()
                .ptUser(ptUser)
                .clientRecord(clientRecord)
                .title(requireText(form.getTitle()))
                .overview(trimToNull(form.getOverview()))
                .dailyGuidance(requireText(form.getDailyGuidance()))
                .active(true)
                .build();

        return mealPlanRepository.save(mealPlan);
    }

    @Transactional
    public MealPlan updateMealPlan(User ptUser, Long mealPlanId, MealPlanForm form) {
        MealPlan mealPlan = mealPlanRepository.findByIdAndPtUser(mealPlanId, ptUser)
                .orElseThrow(() -> new IllegalArgumentException("Meal plan not found"));

        mealPlan.setTitle(requireText(form.getTitle()));
        mealPlan.setOverview(trimToNull(form.getOverview()));
        mealPlan.setDailyGuidance(requireText(form.getDailyGuidance()));
        return mealPlanRepository.save(mealPlan);
    }

    @Transactional
    public void logCompliance(User clientUser, LocalDate date, ComplianceLevel level, String notes) {
        ClientRecord clientRecord = clientRecordRepository.findByClientUser(clientUser)
                .orElseThrow(() -> new IllegalArgumentException("Client record not found"));
        MealPlan mealPlan = mealPlanRepository.findByClientRecordAndActiveTrue(clientRecord)
                .orElseThrow(() -> new IllegalArgumentException("No active meal plan assigned"));

        MealComplianceLog log = mealComplianceLogRepository.findByClientRecordAndDate(clientRecord, date)
                .orElseGet(() -> MealComplianceLog.builder()
                        .clientRecord(clientRecord)
                        .date(date)
                        .build());

        log.setMealPlan(mealPlan);
        log.setCompliance(level);
        log.setNotes(trimToMaxLength(notes, 500, "Notes must be at most 500 characters"));
        mealComplianceLogRepository.save(log);
    }

    public List<MealComplianceLog> getComplianceLogs(ClientRecord clientRecord, int lastNDays) {
        if (lastNDays <= 0) {
            return List.of();
        }

        LocalDate from = LocalDate.now().minusDays(lastNDays - 1L);
        LocalDate to = LocalDate.now();
        return mealComplianceLogRepository.findByClientRecordAndDateBetween(clientRecord, from, to).stream()
                .sorted(Comparator.comparing(MealComplianceLog::getDate).reversed())
                .toList();
    }

    public double getComplianceRate(ClientRecord clientRecord, int lastNDays) {
        List<MealComplianceLog> logs = getComplianceLogs(clientRecord, lastNDays);
        if (logs.isEmpty()) {
            return 0;
        }

        long followedCount = logs.stream()
                .filter(log -> log.getCompliance() == ComplianceLevel.FOLLOWED)
                .count();
        return (followedCount * 100.0) / logs.size();
    }

    public Optional<MealComplianceLog> getComplianceLog(ClientRecord clientRecord, LocalDate date) {
        return mealComplianceLogRepository.findByClientRecordAndDate(clientRecord, date);
    }

    public int getMealPlanCoverage(User ptUser) {
        List<ClientRecord> activeClients = clientRecordRepository.findByPtUserAndStatus(ptUser, ClientStatus.ACTIVE);
        if (activeClients.isEmpty()) {
            return 0;
        }

        long coveredClients = activeClients.stream()
                .filter(clientRecord -> mealPlanRepository.findByClientRecordAndActiveTrue(clientRecord).isPresent())
                .count();
        return (int) ((coveredClients * 100) / activeClients.size());
    }

    private String requireText(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new IllegalArgumentException("Required text is missing");
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String trimToMaxLength(String value, int maxLength, String message) {
        String trimmed = trimToNull(value);
        if (trimmed != null && trimmed.length() > maxLength) {
            throw new IllegalArgumentException(message);
        }
        return trimmed;
    }
}

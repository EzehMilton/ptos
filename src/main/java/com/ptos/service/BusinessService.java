package com.ptos.service;

import com.ptos.domain.*;
import com.ptos.dto.BusinessView;
import com.ptos.dto.BusinessView.ClientBusinessRow;
import com.ptos.repository.ClientProfileRepository;
import com.ptos.repository.ClientRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BusinessService {

    private final ClientRecordRepository clientRecordRepository;
    private final ClientProfileRepository clientProfileRepository;

    public BusinessView getBusinessData(User ptUser) {
        List<ClientRecord> records = clientRecordRepository.findByPtUser(ptUser);

        List<ClientBusinessRow> rows = records.stream()
                .map(this::toRow)
                .sorted(Comparator.comparing(ClientBusinessRow::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());

        int activeCount = (int) rows.stream()
                .filter(r -> r.getStatus() == ClientStatus.ACTIVE)
                .count();

        BigDecimal revenue = rows.stream()
                .filter(r -> r.getStatus() == ClientStatus.ACTIVE && r.getPackagePrice() != null)
                .map(ClientBusinessRow::getPackagePrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgRevenue = activeCount > 0
                ? revenue.divide(BigDecimal.valueOf(activeCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        int needingAttention = (int) rows.stream()
                .filter(r -> r.getStatus() == ClientStatus.AT_RISK || r.getProfileCompletion() < 3)
                .count();

        int completeCount = (int) rows.stream()
                .filter(r -> r.getProfileCompletion() >= 4)
                .count();

        int totalCount = rows.size();
        int completionRate = totalCount > 0 ? (completeCount * 100) / totalCount : 0;

        List<ClientBusinessRow> incomplete = rows.stream()
                .filter(r -> r.getProfileCompletion() < 4)
                .collect(Collectors.toList());

        return BusinessView.builder()
                .estimatedMonthlyRevenue(revenue)
                .averageRevenuePerClient(avgRevenue)
                .activeClientCount(activeCount)
                .clientsNeedingAttention(needingAttention)
                .profileCompletionRate(completionRate)
                .clientBreakdown(rows)
                .completeProfileCount(completeCount)
                .totalClientCount(totalCount)
                .incompleteProfileClients(incomplete)
                .build();
    }

    private ClientBusinessRow toRow(ClientRecord record) {
        User client = record.getClientUser();
        Optional<ClientProfile> profileOpt = clientProfileRepository.findByUserId(client.getId());

        int completion = 0;
        if (profileOpt.isPresent()) {
            ClientProfile p = profileOpt.get();
            if (p.getAge() != null) completion++;
            if (p.getHeightCm() != null) completion++;
            if (p.getCurrentWeightKg() != null) completion++;
            if (p.getGoalType() != null) completion++;
            if (p.getTargetWeightKg() != null) completion++;
            if (p.getTrainingExperience() != null) completion++;
        }

        String health;
        if (record.getStatus() == ClientStatus.INACTIVE || record.getStatus() == ClientStatus.ARCHIVED) {
            health = "Inactive";
        } else if (record.getStatus() == ClientStatus.AT_RISK || completion < 3) {
            health = "Needs Attention";
        } else {
            health = "Good";
        }

        return ClientBusinessRow.builder()
                .clientRecordId(record.getId())
                .name(client.getFullName())
                .status(record.getStatus())
                .packagePrice(record.getMonthlyPackagePrice())
                .profileCompletion(completion)
                .healthLabel(health)
                .build();
    }
}

package com.ptos.service;

import com.ptos.domain.ClientProfile;
import com.ptos.domain.ClientRecord;
import com.ptos.domain.ClientStatus;
import com.ptos.domain.User;
import com.ptos.dto.ClientDetailView;
import com.ptos.dto.ClientListView;
import com.ptos.dto.ClientRecordUpdateForm;
import com.ptos.repository.ClientProfileRepository;
import com.ptos.repository.ClientRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientRecordService {

    private final ClientRecordRepository clientRecordRepository;
    private final ClientProfileRepository clientProfileRepository;

    public List<ClientRecord> getClientsForPT(User ptUser) {
        return clientRecordRepository.findByPtUser(ptUser);
    }

    public Optional<ClientRecord> getClientRecord(Long id, User ptUser) {
        return clientRecordRepository.findByIdAndPtUser(id, ptUser);
    }

    public Optional<ClientRecord> getClientRecord(User clientUser) {
        return clientRecordRepository.findByClientUser(clientUser);
    }

    public Optional<ClientRecord> getClientRecord(User ptUser, User clientUser) {
        return clientRecordRepository.findByPtUserAndClientUser(ptUser, clientUser);
    }

    public ClientRecord createRecord(User ptUser, User clientUser) {
        ClientRecord record = ClientRecord.builder()
                .ptUser(ptUser)
                .clientUser(clientUser)
                .build();
        return clientRecordRepository.save(record);
    }

    public ClientRecord updateRecord(Long id, User ptUser, ClientRecordUpdateForm form) {
        ClientRecord record = clientRecordRepository.findByIdAndPtUser(id, ptUser)
                .orElseThrow(() -> new IllegalArgumentException("Client record not found"));
        record.setStatus(form.getStatus());
        record.setPtNotes(form.getPtNotes());
        record.setMonthlyPackagePrice(form.getMonthlyPackagePrice());
        return clientRecordRepository.save(record);
    }

    public Map<ClientStatus, Long> getStatusCounts(User ptUser) {
        Map<ClientStatus, Long> counts = new LinkedHashMap<>();
        for (ClientStatus status : ClientStatus.values()) {
            counts.put(status, clientRecordRepository.countByPtUserAndStatus(ptUser, status));
        }
        return counts;
    }

    public Optional<ClientDetailView> getClientDetail(Long id, User ptUser) {
        return clientRecordRepository.findByIdAndPtUser(id, ptUser)
                .map(this::toClientDetailView);
    }

    public ClientDetailView getClientDetail(ClientRecord record) {
        return toClientDetailView(record);
    }

    public List<ClientListView> getClientListForPT(User ptUser) {
        return clientRecordRepository.findByPtUser(ptUser).stream()
                .map(this::toClientListView)
                .sorted(Comparator.comparing(ClientListView::getClientName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    public List<ClientListView> searchClients(User ptUser, String query) {
        String lowerQuery = query.toLowerCase();
        return getClientListForPT(ptUser).stream()
                .filter(v -> v.getClientName().toLowerCase().contains(lowerQuery)
                        || v.getClientEmail().toLowerCase().contains(lowerQuery))
                .collect(Collectors.toList());
    }

    private ClientDetailView toClientDetailView(ClientRecord record) {
        User client = record.getClientUser();
        Optional<ClientProfile> profileOpt = clientProfileRepository.findByUserId(client.getId());

        ClientDetailView.ClientDetailViewBuilder builder = ClientDetailView.builder()
                .clientRecordId(record.getId())
                .clientName(client.getFullName())
                .clientEmail(client.getEmail())
                .status(record.getStatus())
                .startDate(record.getStartDate())
                .monthlyPackagePrice(record.getMonthlyPackagePrice())
                .ptNotes(record.getPtNotes())
                .daysSinceStart(ChronoUnit.DAYS.between(record.getStartDate(), LocalDate.now()));

        if (profileOpt.isPresent()) {
            ClientProfile p = profileOpt.get();
            builder.age(p.getAge())
                    .heightCm(p.getHeightCm())
                    .currentWeightKg(p.getCurrentWeightKg())
                    .goalType(p.getGoalType())
                    .targetWeightKg(p.getTargetWeightKg())
                    .injuriesOrConditions(p.getInjuriesOrConditions())
                    .dietaryPreferences(p.getDietaryPreferences())
                    .trainingExperience(p.getTrainingExperience())
                    .clientNotes(p.getNotes());

            int count = 0;
            if (p.getAge() != null) count++;
            if (p.getHeightCm() != null) count++;
            if (p.getCurrentWeightKg() != null) count++;
            if (p.getGoalType() != null) count++;
            if (p.getTargetWeightKg() != null) count++;
            if (p.getTrainingExperience() != null) count++;
            builder.profileCompletionCount(count);
            builder.profileComplete(count == 6);
        }

        return builder.build();
    }

    private ClientListView toClientListView(ClientRecord record) {
        User client = record.getClientUser();
        Optional<ClientProfile> profileOpt = clientProfileRepository.findByUserId(client.getId());

        ClientListView.ClientListViewBuilder builder = ClientListView.builder()
                .clientRecordId(record.getId())
                .clientName(client.getFullName())
                .clientEmail(client.getEmail())
                .status(record.getStatus())
                .startDate(record.getStartDate())
                .monthlyPackagePrice(record.getMonthlyPackagePrice());

        profileOpt.ifPresent(profile -> {
            builder.goalType(profile.getGoalType());
            builder.currentWeightKg(profile.getCurrentWeightKg());
            builder.targetWeightKg(profile.getTargetWeightKg());
            builder.profileLastUpdated(profile.getUpdatedAt());
        });

        return builder.build();
    }
}

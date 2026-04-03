package com.ptos.service;

import com.ptos.domain.ClientProfile;
import com.ptos.domain.ClientInvitation;
import com.ptos.domain.ClientRecord;
import com.ptos.domain.ClientRecordNote;
import com.ptos.domain.ClientStatus;
import com.ptos.domain.Conversation;
import com.ptos.domain.MealPlan;
import com.ptos.domain.Message;
import com.ptos.domain.User;
import com.ptos.dto.ClientDetailView;
import com.ptos.dto.ClientListView;
import com.ptos.dto.ClientRecordUpdateForm;
import com.ptos.repository.CheckInRepository;
import com.ptos.repository.ClientProfileRepository;
import com.ptos.repository.ClientInvitationRepository;
import com.ptos.repository.ClientRecordNoteRepository;
import com.ptos.repository.ClientRecordRepository;
import com.ptos.repository.ConversationRepository;
import com.ptos.repository.MealComplianceLogRepository;
import com.ptos.repository.MealPlanRepository;
import com.ptos.repository.MessageRepository;
import com.ptos.repository.UserRepository;
import com.ptos.repository.WorkoutAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientRecordService {

    private final ClientRecordRepository clientRecordRepository;
    private final ClientProfileRepository clientProfileRepository;
    private final ClientRecordNoteRepository clientRecordNoteRepository;
    private final CheckInRepository checkInRepository;
    private final WorkoutAssignmentRepository workoutAssignmentRepository;
    private final MealPlanRepository mealPlanRepository;
    private final MealComplianceLogRepository mealComplianceLogRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ClientInvitationRepository clientInvitationRepository;
    private final UserRepository userRepository;

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

    @Transactional
    public ClientRecord updateRecord(Long id, User ptUser, ClientRecordUpdateForm form) {
        ClientRecord record = clientRecordRepository.findByIdAndPtUser(id, ptUser)
                .orElseThrow(() -> new IllegalArgumentException("Client record not found"));
        record.setStatus(form.getStatus());
        record.setMonthlyPackagePrice(form.getMonthlyPackagePrice());

        String trimmedNote = form.getPtNotes() != null ? form.getPtNotes().trim() : "";
        if (!trimmedNote.isBlank()) {
            clientRecordNoteRepository.save(ClientRecordNote.builder()
                    .clientRecord(record)
                    .ptUser(ptUser)
                    .noteText(trimmedNote)
                    .build());
            record.setPtNotes(trimmedNote);
        }

        return clientRecordRepository.save(record);
    }

    public List<ClientRecordNote> getNotesForClientRecord(ClientRecord record) {
        return clientRecordNoteRepository.findByClientRecordOrderByCreatedAtDesc(record);
    }

    @Transactional
    public void deleteClientRecord(Long id, User ptUser) {
        ClientRecord record = clientRecordRepository.findByIdAndPtUser(id, ptUser)
                .orElseThrow(() -> new IllegalArgumentException("Client record not found"));
        User clientUser = record.getClientUser();

        conversationRepository.findByPtUserAndClientUser(ptUser, clientUser)
                .ifPresent(conversation -> {
                    List<Message> messages = messageRepository.findByConversationOrderByCreatedAtAsc(conversation);
                    messageRepository.deleteAll(messages);
                    conversationRepository.delete(conversation);
                });

        clientInvitationRepository.deleteAll(
                clientInvitationRepository.findByPtUserAndEmailOrderByCreatedAtDesc(ptUser, clientUser.getEmail())
        );

        mealComplianceLogRepository.deleteAll(
                mealComplianceLogRepository.findByClientRecordOrderByDateDesc(record)
        );

        List<MealPlan> mealPlans = mealPlanRepository.findByClientRecordOrderByCreatedAtDesc(record);
        mealPlanRepository.deleteAll(mealPlans);

        workoutAssignmentRepository.deleteAll(
                workoutAssignmentRepository.findByClientRecordOrderByAssignedDateDesc(record)
        );

        clientRecordNoteRepository.deleteAll(
                clientRecordNoteRepository.findByClientRecordOrderByCreatedAtDesc(record)
        );

        checkInRepository.deleteAll(
                checkInRepository.findByClientRecordOrderBySubmittedAtDesc(record)
        );

        clientRecordRepository.delete(record);

        if (!clientRecordRepository.existsByClientUser(clientUser)) {
            clientProfileRepository.findByUserId(clientUser.getId())
                    .ifPresent(clientProfileRepository::delete);
            userRepository.delete(clientUser);
        }
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

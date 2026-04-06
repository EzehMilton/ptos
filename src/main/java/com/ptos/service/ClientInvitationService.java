package com.ptos.service;

import com.ptos.domain.ClientInvitation;
import com.ptos.domain.ClientRecord;
import com.ptos.domain.ClientStatus;
import com.ptos.domain.InvitationStatus;
import com.ptos.domain.Role;
import com.ptos.domain.User;
import com.ptos.dto.DirectCreateClientForm;
import com.ptos.dto.InvitationAcceptResult;
import com.ptos.dto.InviteClientForm;
import com.ptos.repository.ClientInvitationRepository;
import com.ptos.repository.ClientRecordRepository;
import com.ptos.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClientInvitationService {

    private final ClientInvitationRepository clientInvitationRepository;
    private final ClientRecordRepository clientRecordRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public ClientInvitation createInvitation(User ptUser, InviteClientForm form) {
        String email = form.getEmail().trim();
        validateCanInvite(ptUser, email);

        ClientInvitation invitation = ClientInvitation.builder()
                .ptUser(ptUser)
                .email(email)
                .fullName(form.getFullName().trim())
                .token(UUID.randomUUID().toString())
                .status(InvitationStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        return clientInvitationRepository.save(invitation);
    }

    @Transactional
    public ClientRecord createDirectClient(User ptUser, DirectCreateClientForm form) {
        String email = form.getEmail().trim();
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("An account with this email already exists. Use an invitation instead.");
        }

        User clientUser = userRepository.save(User.builder()
                .fullName(form.getFullName().trim())
                .email(email)
                .password(passwordEncoder.encode(form.getTemporaryPassword()))
                .role(Role.CLIENT)
                .enabled(true)
                .build());

        expirePendingInvitations(ptUser, email);

        ClientRecord record = ClientRecord.builder()
                .ptUser(ptUser)
                .clientUser(clientUser)
                .status(ClientStatus.ACTIVE)
                .startDate(LocalDate.now())
                .monthlyPackagePrice(form.getMonthlyPackagePrice())
                .build();
        return clientRecordRepository.save(record);
    }

    @Transactional
    public InvitationAcceptResult acceptInvitation(String token, String rawPassword) {
        ClientInvitation invitation = getRequiredInvitation(token);
        User ptUser = invitation.getPtUser();

        if (invitation.getStatus() == InvitationStatus.EXPIRED) {
            throw new IllegalArgumentException("This invitation has expired. Please ask your PT for a new one.");
        }
        if (invitation.getStatus() == InvitationStatus.ACCEPTED) {
            throw new IllegalArgumentException("This invitation has already been used.");
        }

        User clientUser = userRepository.findByEmail(invitation.getEmail())
                .map(existingUser -> {
                    if (existingUser.getRole() != Role.CLIENT) {
                        throw new IllegalArgumentException("This email belongs to a non-client account.");
                    }
                    validateClientCanBeLinked(ptUser, existingUser);
                    return existingUser;
                })
                .orElseGet(() -> userRepository.save(User.builder()
                        .fullName(invitation.getFullName())
                        .email(invitation.getEmail())
                        .password(passwordEncoder.encode(rawPassword))
                        .role(Role.CLIENT)
                        .enabled(true)
                        .build()));

        clientRecordRepository.save(ClientRecord.builder()
                .ptUser(ptUser)
                .clientUser(clientUser)
                .status(ClientStatus.ACTIVE)
                .startDate(LocalDate.now())
                .build());

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(LocalDateTime.now());
        clientInvitationRepository.save(invitation);

        expirePendingInvitations(ptUser, invitation.getEmail());

        return new InvitationAcceptResult(clientUser, ptUser);
    }

    public List<ClientInvitation> getPendingInvitations(User ptUser) {
        return getAllInvitations(ptUser).stream()
                .filter(invitation -> invitation.getStatus() == InvitationStatus.PENDING)
                .toList();
    }

    public List<ClientInvitation> getAllInvitations(User ptUser) {
        return clientInvitationRepository.findByPtUserOrderByCreatedAtDesc(ptUser).stream()
                .map(this::refreshStatus)
                .toList();
    }

    public Optional<ClientInvitation> getInvitationByToken(String token) {
        return clientInvitationRepository.findByToken(token)
                .map(this::refreshStatus);
    }

    public Optional<ClientInvitation> getValidInvitationByToken(String token) {
        return getInvitationByToken(token)
                .filter(this::isInvitationValidForAcceptance);
    }

    private void validateCanInvite(User ptUser, String email) {
        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            if (existingUser.get().getRole() != Role.CLIENT) {
                throw new IllegalArgumentException("This email belongs to a non-client account.");
            }
            validateClientCanBeLinked(ptUser, existingUser.get());
        }

        boolean hasPendingInvitation = getAllInvitations(ptUser).stream()
                .anyMatch(invitation -> invitation.getStatus() == InvitationStatus.PENDING
                        && invitation.getEmail().equalsIgnoreCase(email));

        if (hasPendingInvitation) {
            throw new IllegalArgumentException("A pending invitation already exists for this email.");
        }
    }

    private void validateClientCanBeLinked(User ptUser, User clientUser) {
        Optional<ClientRecord> existingRecord = clientRecordRepository.findByClientUser(clientUser);
        if (existingRecord.isEmpty()) {
            return;
        }

        if (existingRecord.get().getPtUser().getId().equals(ptUser.getId())) {
            throw new IllegalArgumentException("This person is already your client.");
        }

        throw new IllegalArgumentException("This email is already linked to another PT.");
    }

    private void expirePendingInvitations(User ptUser, String email) {
        List<ClientInvitation> invitations = clientInvitationRepository.findByPtUserOrderByCreatedAtDesc(ptUser);
        for (ClientInvitation invitation : invitations) {
            refreshStatus(invitation);
            if (invitation.getStatus() == InvitationStatus.PENDING
                    && invitation.getEmail().equalsIgnoreCase(email)) {
                invitation.setStatus(InvitationStatus.EXPIRED);
                clientInvitationRepository.save(invitation);
            }
        }
    }

    private ClientInvitation getRequiredInvitation(String token) {
        return getInvitationByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("This invitation link is invalid."));
    }

    private ClientInvitation refreshStatus(ClientInvitation invitation) {
        if (invitation.getStatus() == InvitationStatus.PENDING
                && invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            return clientInvitationRepository.save(invitation);
        }

        return invitation;
    }

    private boolean isInvitationValidForAcceptance(ClientInvitation invitation) {
        return invitation.getStatus() == InvitationStatus.PENDING
                && !invitation.getExpiresAt().isBefore(LocalDateTime.now());
    }
}

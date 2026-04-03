package com.ptos.repository;

import com.ptos.domain.ClientInvitation;
import com.ptos.domain.InvitationStatus;
import com.ptos.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClientInvitationRepository extends JpaRepository<ClientInvitation, Long> {

    Optional<ClientInvitation> findByToken(String token);

    List<ClientInvitation> findByPtUserAndStatus(User ptUser, InvitationStatus status);

    List<ClientInvitation> findByPtUserOrderByCreatedAtDesc(User ptUser);

    boolean existsByPtUserAndEmailAndStatus(User ptUser, String email, InvitationStatus status);
}

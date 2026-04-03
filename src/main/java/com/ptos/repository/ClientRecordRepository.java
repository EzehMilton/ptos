package com.ptos.repository;

import com.ptos.domain.ClientRecord;
import com.ptos.domain.ClientStatus;
import com.ptos.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClientRecordRepository extends JpaRepository<ClientRecord, Long> {

    List<ClientRecord> findByPtUser(User ptUser);

    List<ClientRecord> findByPtUserAndStatus(User ptUser, ClientStatus status);

    Optional<ClientRecord> findByPtUserAndClientUser(User ptUser, User clientUser);

    Optional<ClientRecord> findByIdAndPtUser(Long id, User ptUser);

    boolean existsByPtUserAndClientUser(User ptUser, User clientUser);

    boolean existsByClientUser(User clientUser);

    long countByPtUser(User ptUser);

    long countByPtUserAndStatus(User ptUser, ClientStatus status);

    Optional<ClientRecord> findByClientUser(User clientUser);
}

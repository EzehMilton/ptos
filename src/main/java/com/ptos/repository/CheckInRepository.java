package com.ptos.repository;

import com.ptos.domain.CheckIn;
import com.ptos.domain.CheckInStatus;
import com.ptos.domain.ClientRecord;
import com.ptos.domain.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CheckInRepository extends JpaRepository<CheckIn, Long> {

    @EntityGraph(attributePaths = {"feedback", "clientRecord", "clientRecord.clientUser", "clientRecord.ptUser"})
    List<CheckIn> findByClientRecordOrderBySubmittedAtDesc(ClientRecord cr);

    @EntityGraph(attributePaths = {"feedback", "clientRecord", "clientRecord.clientUser", "clientRecord.ptUser"})
    List<CheckIn> findByClientRecord_ClientUserOrderBySubmittedAtDesc(User clientUser);

    @EntityGraph(attributePaths = {"feedback", "clientRecord", "clientRecord.clientUser", "clientRecord.ptUser"})
    List<CheckIn> findByClientRecord_PtUserAndStatusOrderBySubmittedAtDesc(User ptUser, CheckInStatus status);

    @EntityGraph(attributePaths = {"feedback", "clientRecord", "clientRecord.clientUser", "clientRecord.ptUser"})
    List<CheckIn> findByClientRecord_PtUserOrderBySubmittedAtDesc(User ptUser);

    long countByClientRecord_PtUserAndStatus(User ptUser, CheckInStatus status);

    @EntityGraph(attributePaths = {"feedback", "clientRecord", "clientRecord.clientUser", "clientRecord.ptUser"})
    Optional<CheckIn> findTopByClientRecordOrderBySubmittedAtDesc(ClientRecord cr);

    @EntityGraph(attributePaths = {"feedback", "clientRecord", "clientRecord.clientUser", "clientRecord.ptUser"})
    Optional<CheckIn> findByIdAndClientRecord_ClientUser(Long id, User clientUser);

    @EntityGraph(attributePaths = {"feedback", "clientRecord", "clientRecord.clientUser", "clientRecord.ptUser"})
    Optional<CheckIn> findByIdAndClientRecord_PtUser(Long id, User ptUser);

    @EntityGraph(attributePaths = {"feedback", "clientRecord", "clientRecord.clientUser", "clientRecord.ptUser"})
    Optional<CheckIn> findTopByClientRecordAndSubmittedAtBeforeOrderBySubmittedAtDesc(ClientRecord cr, java.time.LocalDateTime submittedAt);

    @Override
    @EntityGraph(attributePaths = {"feedback", "clientRecord", "clientRecord.clientUser", "clientRecord.ptUser"})
    Optional<CheckIn> findById(Long id);
}

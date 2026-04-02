package com.ptos.repository;

import com.ptos.domain.AssignmentStatus;
import com.ptos.domain.ClientRecord;
import com.ptos.domain.User;
import com.ptos.domain.WorkoutAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkoutAssignmentRepository extends JpaRepository<WorkoutAssignment, Long> {

    List<WorkoutAssignment> findByClientRecordOrderByAssignedDateDesc(ClientRecord cr);

    List<WorkoutAssignment> findByClientRecord_ClientUserOrderByAssignedDateDesc(User clientUser);

    Optional<WorkoutAssignment> findByIdAndClientRecord_ClientUser(Long id, User clientUser);

    long countByClientRecord_PtUser(User ptUser);

    long countByClientRecordAndStatus(ClientRecord cr, AssignmentStatus status);
}

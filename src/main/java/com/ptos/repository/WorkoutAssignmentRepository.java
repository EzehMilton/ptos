package com.ptos.repository;

import com.ptos.domain.AssignmentStatus;
import com.ptos.domain.ClientRecord;
import com.ptos.domain.User;
import com.ptos.domain.WorkoutAssignment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WorkoutAssignmentRepository extends JpaRepository<WorkoutAssignment, Long> {

    @EntityGraph(attributePaths = {"workout", "workout.exercises"})
    List<WorkoutAssignment> findByClientRecordOrderByAssignedDateDesc(ClientRecord cr);

    @EntityGraph(attributePaths = {"workout", "workout.exercises"})
    List<WorkoutAssignment> findByClientRecord_ClientUserOrderByAssignedDateDesc(User clientUser);

    @EntityGraph(attributePaths = {"workout", "workout.exercises"})
    Optional<WorkoutAssignment> findByIdAndClientRecord_ClientUser(Long id, User clientUser);

    long countByClientRecord_PtUser(User ptUser);

    long countByClientRecordAndStatus(ClientRecord cr, AssignmentStatus status);

    long countByClientRecord_PtUserAndStatus(User ptUser, AssignmentStatus status);

    long countByClientRecord_PtUserAndStatusAndCompletedAtAfter(User ptUser,
                                                                AssignmentStatus status,
                                                                LocalDateTime completedAt);
}

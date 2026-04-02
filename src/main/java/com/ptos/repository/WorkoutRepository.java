package com.ptos.repository;

import com.ptos.domain.User;
import com.ptos.domain.Workout;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkoutRepository extends JpaRepository<Workout, Long> {

    List<Workout> findByPtUserOrderByCreatedAtDesc(User ptUser);

    Optional<Workout> findByIdAndPtUser(Long id, User ptUser);
}

package com.ptos.repository;

import com.ptos.domain.ClientRecord;
import com.ptos.domain.MealPlan;
import com.ptos.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MealPlanRepository extends JpaRepository<MealPlan, Long> {

    Optional<MealPlan> findByClientRecordAndActiveTrue(ClientRecord clientRecord);

    List<MealPlan> findByClientRecordOrderByCreatedAtDesc(ClientRecord clientRecord);

    List<MealPlan> findByPtUserOrderByCreatedAtDesc(User ptUser);

    Optional<MealPlan> findByIdAndPtUser(Long id, User ptUser);
}

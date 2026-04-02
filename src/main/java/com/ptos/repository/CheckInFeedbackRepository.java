package com.ptos.repository;

import com.ptos.domain.CheckInFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckInFeedbackRepository extends JpaRepository<CheckInFeedback, Long> {
}

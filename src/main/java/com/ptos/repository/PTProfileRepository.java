package com.ptos.repository;

import com.ptos.domain.PTProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PTProfileRepository extends JpaRepository<PTProfile, Long> {

    Optional<PTProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}

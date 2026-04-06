package com.ptos.repository;

import com.ptos.domain.ClientProfile;
import com.ptos.domain.ClientProfilePhoto;
import com.ptos.domain.PhotoType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClientProfilePhotoRepository extends JpaRepository<ClientProfilePhoto, Long> {

    List<ClientProfilePhoto> findByClientProfileOrderByPhotoTypeAsc(ClientProfile clientProfile);

    Optional<ClientProfilePhoto> findByClientProfileAndPhotoType(ClientProfile clientProfile, PhotoType photoType);
}

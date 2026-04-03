package com.ptos.service;

import com.ptos.domain.PTProfile;
import com.ptos.domain.User;
import com.ptos.dto.PTProfileForm;
import com.ptos.repository.PTProfileRepository;
import com.ptos.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PTProfileService {

    private final PTProfileRepository ptProfileRepository;
    private final UserRepository userRepository;

    public Optional<PTProfile> getProfileForUser(Long userId) {
        return ptProfileRepository.findByUserId(userId);
    }

    public PTProfile createOrUpdateProfile(Long userId, PTProfileForm form) {
        PTProfile profile = ptProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.getReferenceById(userId);
                    return PTProfile.builder()
                            .user(user)
                            .build();
                });

        profile.setBusinessName(form.getBusinessName());
        profile.setSpecialisation(form.getSpecialisation());
        profile.setLocation(form.getLocation());
        profile.setBio(form.getBio());
        profile.setLogoUrl(form.getLogoUrl());
        profile.setOnboardingComplete(true);

        return ptProfileRepository.save(profile);
    }

    public PTProfile markOnboardingComplete(Long userId) {
        PTProfile profile = ptProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.getReferenceById(userId);
                    return PTProfile.builder()
                            .user(user)
                            .build();
                });

        profile.setOnboardingComplete(true);
        return ptProfileRepository.save(profile);
    }
}

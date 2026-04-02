package com.ptos.service;

import com.ptos.domain.ClientProfile;
import com.ptos.domain.User;
import com.ptos.dto.ProfileForm;
import com.ptos.repository.ClientProfileRepository;
import com.ptos.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClientProfileService {

    private final ClientProfileRepository clientProfileRepository;
    private final UserRepository userRepository;

    public Optional<ClientProfile> getProfileForUser(Long userId) {
        return clientProfileRepository.findByUserId(userId);
    }

    public ClientProfile createOrUpdateProfile(Long userId, ProfileForm form) {
        ClientProfile profile = clientProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.getReferenceById(userId);
                    return ClientProfile.builder().user(user).build();
                });

        profile.setAge(form.getAge());
        profile.setHeightCm(form.getHeightCm());
        profile.setCurrentWeightKg(form.getCurrentWeightKg());
        profile.setGoalType(form.getGoalType());
        profile.setTargetWeightKg(form.getTargetWeightKg());
        profile.setInjuriesOrConditions(form.getInjuriesOrConditions());
        profile.setDietaryPreferences(form.getDietaryPreferences());
        profile.setTrainingExperience(form.getTrainingExperience());
        profile.setNotes(form.getNotes());

        return clientProfileRepository.save(profile);
    }
}

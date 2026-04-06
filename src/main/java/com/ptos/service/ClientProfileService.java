package com.ptos.service;

import com.ptos.domain.ClientProfile;
import com.ptos.domain.GoalType;
import com.ptos.domain.TrainingExperience;
import com.ptos.domain.User;
import com.ptos.dto.ProfileForm;
import com.ptos.dto.api.ClientProfileUpdateRequest;
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

    public ClientProfile getOrCreateProfileForUser(Long userId) {
        ClientProfile profile = getOrCreateProfile(userId);
        return profile.getId() == null ? clientProfileRepository.save(profile) : profile;
    }

    public ClientProfile createOrUpdateProfile(Long userId, ProfileForm form) {
        ClientProfile profile = getOrCreateProfile(userId);

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

    public ClientProfile createOrUpdateProfile(Long userId, ClientProfileUpdateRequest request) {
        ClientProfile profile = getOrCreateProfile(userId);

        if (request.age() != null) {
            profile.setAge(request.age());
        }
        if (request.heightCm() != null) {
            profile.setHeightCm(request.heightCm());
        }
        if (request.currentWeightKg() != null) {
            profile.setCurrentWeightKg(request.currentWeightKg());
        }
        if (request.goalType() != null) {
            profile.setGoalType(parseGoalType(request.goalType()));
        }
        if (request.targetWeightKg() != null) {
            profile.setTargetWeightKg(request.targetWeightKg());
        }
        if (request.trainingExperience() != null) {
            profile.setTrainingExperience(parseTrainingExperience(request.trainingExperience()));
        }
        if (request.injuriesOrConditions() != null) {
            profile.setInjuriesOrConditions(normalizeText(request.injuriesOrConditions()));
        }
        if (request.dietaryPreferences() != null) {
            profile.setDietaryPreferences(normalizeText(request.dietaryPreferences()));
        }
        if (request.additionalNotes() != null) {
            profile.setNotes(normalizeText(request.additionalNotes()));
        }

        if (Boolean.TRUE.equals(request.onboardingCompleted())) {
            validateOnboardingCompletion(profile);
            profile.setOnboardingComplete(true);
        } else if (request.onboardingCompleted() != null) {
            profile.setOnboardingComplete(false);
        }

        return clientProfileRepository.save(profile);
    }

    public ClientProfile markOnboardingComplete(Long userId) {
        ClientProfile profile = getOrCreateProfileForUser(userId);
        validateOnboardingCompletion(profile);
        profile.setOnboardingComplete(true);
        return clientProfileRepository.save(profile);
    }

    public int computeCompletion(ClientProfile profile) {
        int count = 0;
        if (profile.getAge() != null) count++;
        if (profile.getHeightCm() != null) count++;
        if (profile.getCurrentWeightKg() != null) count++;
        if (profile.getGoalType() != null) count++;
        if (profile.getTargetWeightKg() != null) count++;
        if (profile.getTrainingExperience() != null) count++;
        return count;
    }

    private ClientProfile getOrCreateProfile(Long userId) {
        return clientProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.getReferenceById(userId);
                    return ClientProfile.builder().user(user).build();
                });
    }

    private GoalType parseGoalType(String goalType) {
        return goalType == null || goalType.isBlank() ? null : GoalType.valueOf(goalType.trim().toUpperCase());
    }

    private TrainingExperience parseTrainingExperience(String trainingExperience) {
        return trainingExperience == null || trainingExperience.isBlank()
                ? null
                : TrainingExperience.valueOf(trainingExperience.trim().toUpperCase());
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void validateOnboardingCompletion(ClientProfile profile) {
        StringBuilder missingFields = new StringBuilder();
        appendMissing(missingFields, profile.getAge() == null, "age");
        appendMissing(missingFields, profile.getHeightCm() == null, "heightCm");
        appendMissing(missingFields, profile.getCurrentWeightKg() == null, "currentWeightKg");
        appendMissing(missingFields, profile.getGoalType() == null, "goalType");
        appendMissing(missingFields, profile.getTrainingExperience() == null, "trainingExperience");

        if (!missingFields.isEmpty()) {
            throw new IllegalArgumentException("Cannot complete onboarding. Missing required fields: " + missingFields);
        }
    }

    private void appendMissing(StringBuilder missingFields, boolean missing, String fieldName) {
        if (!missing) {
            return;
        }
        if (!missingFields.isEmpty()) {
            missingFields.append(", ");
        }
        missingFields.append(fieldName);
    }
}

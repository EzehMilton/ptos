package com.ptos.controller.api;

import com.ptos.domain.ClientProfile;
import com.ptos.domain.GoalType;
import com.ptos.domain.TrainingExperience;
import com.ptos.domain.User;
import com.ptos.dto.api.ClientProfileResponse;
import com.ptos.dto.api.ClientProfileUpdateRequest;
import com.ptos.repository.ClientProfileRepository;
import com.ptos.security.PtosUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/client/profile")
@RequiredArgsConstructor
public class ClientProfileApiController {

    private final ClientProfileRepository clientProfileRepository;

    @GetMapping
    public ResponseEntity<ClientProfileResponse> getProfile(@AuthenticationPrincipal PtosUserDetails userDetails) {
        User user = userDetails.getUser();
        ClientProfile profile = clientProfileRepository.findByUserId(user.getId()).orElse(null);
        return ResponseEntity.ok(toResponse(user, profile));
    }

    @PutMapping
    public ResponseEntity<ClientProfileResponse> updateProfile(
            @AuthenticationPrincipal PtosUserDetails userDetails,
            @RequestBody ClientProfileUpdateRequest request) {
        User user = userDetails.getUser();
        ClientProfile profile = clientProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> ClientProfile.builder().user(user).build());

        if (request.age() != null) profile.setAge(request.age());
        if (request.heightCm() != null) profile.setHeightCm(request.heightCm());
        if (request.currentWeightKg() != null) profile.setCurrentWeightKg(request.currentWeightKg());
        if (request.targetWeightKg() != null) profile.setTargetWeightKg(request.targetWeightKg());
        if (request.goalType() != null) profile.setGoalType(GoalType.valueOf(request.goalType()));
        if (request.trainingExperience() != null) profile.setTrainingExperience(TrainingExperience.valueOf(request.trainingExperience()));
        if (request.injuriesOrConditions() != null) profile.setInjuriesOrConditions(request.injuriesOrConditions());
        if (request.dietaryPreferences() != null) profile.setDietaryPreferences(request.dietaryPreferences());
        if (request.notes() != null) profile.setNotes(request.notes());

        profile = clientProfileRepository.save(profile);
        return ResponseEntity.ok(toResponse(user, profile));
    }

    private ClientProfileResponse toResponse(User user, ClientProfile profile) {
        if (profile == null) {
            return new ClientProfileResponse(user.getFullName(), user.getEmail(),
                    null, null, null, null, null, null, null, null, null);
        }
        return new ClientProfileResponse(
                user.getFullName(), user.getEmail(),
                profile.getAge(), profile.getHeightCm(), profile.getCurrentWeightKg(),
                profile.getGoalType() != null ? profile.getGoalType().name() : null,
                profile.getTargetWeightKg(),
                profile.getInjuriesOrConditions(), profile.getDietaryPreferences(),
                profile.getTrainingExperience() != null ? profile.getTrainingExperience().name() : null,
                profile.getNotes()
        );
    }
}

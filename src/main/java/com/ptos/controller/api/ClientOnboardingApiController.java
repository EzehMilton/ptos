package com.ptos.controller.api;

import com.ptos.domain.ClientProfile;
import com.ptos.domain.User;
import com.ptos.dto.api.OnboardingCompletionResponse;
import com.ptos.security.PtosUserDetails;
import com.ptos.service.ClientProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/client/onboarding")
@RequiredArgsConstructor
public class ClientOnboardingApiController {

    private final ClientProfileService clientProfileService;

    @PutMapping("/complete")
    public ResponseEntity<OnboardingCompletionResponse> completeOnboarding(
            @AuthenticationPrincipal PtosUserDetails userDetails) {
        User user = userDetails.getUser();
        ClientProfile profile = clientProfileService.markOnboardingComplete(user.getId());
        return ResponseEntity.ok(new OnboardingCompletionResponse(profile.isOnboardingComplete()));
    }
}

package com.ptos.controller.api;

import com.ptos.domain.ClientRecord;
import com.ptos.domain.ComplianceLevel;
import com.ptos.domain.MealComplianceLog;
import com.ptos.domain.MealPlan;
import com.ptos.domain.User;
import com.ptos.dto.api.ApiError;
import com.ptos.dto.api.MealComplianceRequest;
import com.ptos.dto.api.MealComplianceResponse;
import com.ptos.dto.api.MealPlanResponse;
import com.ptos.repository.ClientRecordRepository;
import com.ptos.security.PtosUserDetails;
import com.ptos.service.NutritionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/client/meal-plan")
@RequiredArgsConstructor
public class ClientMealPlanApiController {

    private final NutritionService nutritionService;
    private final ClientRecordRepository clientRecordRepository;

    @GetMapping
    public ResponseEntity<?> getMealPlan(@AuthenticationPrincipal PtosUserDetails userDetails) {
        User user = userDetails.getUser();
        ClientRecord clientRecord = clientRecordRepository.findByClientUser(user)
                .orElseThrow(() -> new IllegalArgumentException("Client record not found"));

        MealPlan mealPlan = nutritionService.getActiveMealPlan(clientRecord).orElse(null);
        if (mealPlan == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiError(404, "No active meal plan", LocalDateTime.now()));
        }

        List<MealComplianceResponse> compliance = nutritionService.getComplianceLogs(clientRecord, 14).stream()
                .map(log -> new MealComplianceResponse(
                        log.getDate(), log.getCompliance().name(), log.getNotes(), log.getLoggedAt()))
                .toList();

        return ResponseEntity.ok(new MealPlanResponse(
                mealPlan.getId(), mealPlan.getTitle(), mealPlan.getOverview(),
                mealPlan.getDailyGuidance(), mealPlan.getCreatedAt(), mealPlan.getUpdatedAt(),
                compliance
        ));
    }

    @PostMapping("/compliance")
    public ResponseEntity<MealComplianceResponse> logCompliance(
            @AuthenticationPrincipal PtosUserDetails userDetails,
            @Valid @RequestBody MealComplianceRequest request) {
        User user = userDetails.getUser();
        ComplianceLevel level = ComplianceLevel.valueOf(request.compliance());
        nutritionService.logCompliance(user, request.date(), level, request.notes());

        ClientRecord clientRecord = clientRecordRepository.findByClientUser(user)
                .orElseThrow(() -> new IllegalArgumentException("Client record not found"));
        MealComplianceLog log = nutritionService.getComplianceLog(clientRecord, request.date())
                .orElseThrow();
        return ResponseEntity.ok(new MealComplianceResponse(
                log.getDate(), log.getCompliance().name(), log.getNotes(), log.getLoggedAt()));
    }
}

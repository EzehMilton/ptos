package com.ptos;

import com.ptos.repository.UserRepository;
import com.ptos.security.PtosUserDetails;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "ptos.storage.local-dir=target/test-uploads")
@AutoConfigureMockMvc
class ApiCorsIntegrationTest {

    private static final String FLUTTER_WEB_ORIGIN = "http://localhost:54321";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    void apiLoginPreflightIsAllowedForFlutterWebOrigin() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, FLUTTER_WEB_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FLUTTER_WEB_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, Matchers.containsString("POST")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, Matchers.containsString("Authorization")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, Matchers.containsString("Content-Type")));
    }

    @Test
    void protectedApiPreflightIsNotBlockedByJwtFilter() throws Exception {
        mockMvc.perform(options("/api/client/profile")
                        .header(HttpHeaders.ORIGIN, FLUTTER_WEB_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FLUTTER_WEB_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, Matchers.containsString("GET")));
    }

    @Test
    void apiLoginReturnsCorsHeadersForCrossOriginJsonRequest() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, FLUTTER_WEB_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "alex@ptos.local",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FLUTTER_WEB_ORIGIN))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value("alex@ptos.local"));
    }

    @Test
    void apiRegisterAcceptsInvitedClientAndReturnsLoginStyleTokenResponse() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .header(HttpHeaders.ORIGIN, FLUTTER_WEB_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "inviteToken": "17c49f6b-1c3e-4e80-a5ab-3489c1d0ab10",
                                  "fullName": "Layla Green",
                                  "email": "layla.green@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FLUTTER_WEB_ORIGIN))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value("layla.green@example.com"))
                .andExpect(jsonPath("$.fullName").value("Layla Green"));
    }

    @Test
    void publicInviteValidationReturnsInviteDetailsForValidToken() throws Exception {
        mockMvc.perform(get("/api/public/invites/{token}", "17c49f6b-1c3e-4e80-a5ab-3489c1d0ab10")
                        .header(HttpHeaders.ORIGIN, FLUTTER_WEB_ORIGIN))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FLUTTER_WEB_ORIGIN))
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.token").value("17c49f6b-1c3e-4e80-a5ab-3489c1d0ab10"))
                .andExpect(jsonPath("$.ptName").value("Michael Uchenna Ezeh"))
                .andExpect(jsonPath("$.ptBusinessName").value("Big Mike Fitness"))
                .andExpect(jsonPath("$.clientEmail").value("layla.green@example.com"))
                .andExpect(jsonPath("$.clientFullName").value("Layla Green"))
                .andExpect(jsonPath("$.expiresAt").isNotEmpty())
                .andExpect(jsonPath("$.message").doesNotExist());
    }

    @Test
    void publicInviteValidationReturnsCleanInvalidResponseForUnknownToken() throws Exception {
        mockMvc.perform(get("/api/public/invites/{token}", "missing-token")
                        .header(HttpHeaders.ORIGIN, FLUTTER_WEB_ORIGIN))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FLUTTER_WEB_ORIGIN))
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.message").value("This invitation link is invalid or has expired."))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.ptName").doesNotExist())
                .andExpect(jsonPath("$.ptBusinessName").doesNotExist())
                .andExpect(jsonPath("$.clientEmail").doesNotExist())
                .andExpect(jsonPath("$.clientFullName").doesNotExist())
                .andExpect(jsonPath("$.expiresAt").doesNotExist());
    }

    @Test
    void publicInviteSignupAcceptsPendingInviteAndReturnsJwtContext() throws Exception {
        mockMvc.perform(post("/api/public/auth/signup/invite")
                        .header(HttpHeaders.ORIGIN, FLUTTER_WEB_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "2f5b0fb4-4f83-4c60-b7d4-9f79b5c1df58",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FLUTTER_WEB_ORIGIN))
                .andExpect(jsonPath("$.jwt").isNotEmpty())
                .andExpect(jsonPath("$.clientUserId").isNumber())
                .andExpect(jsonPath("$.clientRecordId").isNumber())
                .andExpect(jsonPath("$.fullName").value("Daniel Foster"))
                .andExpect(jsonPath("$.ptName").value("Michael Uchenna Ezeh"));
    }

    @Test
    void publicInviteSignupReturnsCleanJsonErrorForInvalidToken() throws Exception {
        mockMvc.perform(post("/api/public/auth/signup/invite")
                        .header(HttpHeaders.ORIGIN, FLUTTER_WEB_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "missing-token",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FLUTTER_WEB_ORIGIN))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("This invitation link is invalid."))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void clientProfileGetAndPutSupportMobileOnboardingFields() throws Exception {
        PtosUserDetails alex = new PtosUserDetails(userRepository.findByEmail("alex@ptos.local").orElseThrow());

        mockMvc.perform(get("/api/client/profile")
                        .header(HttpHeaders.ORIGIN, FLUTTER_WEB_ORIGIN)
                        .with(user(alex)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FLUTTER_WEB_ORIGIN))
                .andExpect(jsonPath("$.fullName").value("Alex Morgan"))
                .andExpect(jsonPath("$.email").value("alex@ptos.local"))
                .andExpect(jsonPath("$.age").value(28))
                .andExpect(jsonPath("$.heightCm").value(180.0))
                .andExpect(jsonPath("$.currentWeightKg").isNumber())
                .andExpect(jsonPath("$.goalType").value("WEIGHT_LOSS"))
                .andExpect(jsonPath("$.targetWeightKg").value(72.0))
                .andExpect(jsonPath("$.trainingExperience").value("INTERMEDIATE"))
                .andExpect(jsonPath("$.dietaryPreferences").value("No dairy"))
                .andExpect(jsonPath("$.additionalNotes").isEmpty())
                .andExpect(jsonPath("$.onboardingCompleted").value(true));

        mockMvc.perform(put("/api/client/profile")
                        .header(HttpHeaders.ORIGIN, FLUTTER_WEB_ORIGIN)
                        .with(user(alex))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "age": 29,
                                  "heightCm": 181.5,
                                  "currentWeightKg": 83.2,
                                  "goalType": "STRENGTH",
                                  "targetWeightKg": 82.0,
                                  "trainingExperience": "ADVANCED",
                                  "injuriesOrConditions": "Minor shoulder tightness",
                                  "dietaryPreferences": "High protein, low dairy",
                                  "additionalNotes": "Prefers early morning sessions",
                                  "onboardingCompleted": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FLUTTER_WEB_ORIGIN))
                .andExpect(jsonPath("$.age").value(29))
                .andExpect(jsonPath("$.heightCm").value(181.5))
                .andExpect(jsonPath("$.currentWeightKg").value(83.2))
                .andExpect(jsonPath("$.goalType").value("STRENGTH"))
                .andExpect(jsonPath("$.targetWeightKg").value(82.0))
                .andExpect(jsonPath("$.trainingExperience").value("ADVANCED"))
                .andExpect(jsonPath("$.injuriesOrConditions").value("Minor shoulder tightness"))
                .andExpect(jsonPath("$.dietaryPreferences").value("High protein, low dairy"))
                .andExpect(jsonPath("$.additionalNotes").value("Prefers early morning sessions"))
                .andExpect(jsonPath("$.onboardingCompleted").value(true));
    }

    @Test
    void clientProfileCompletionUpdateDoesNotClearPreviouslySavedOnboardingFields() throws Exception {
        PtosUserDetails layla = new PtosUserDetails(userRepository.findByEmail("layla.green@example.com").orElseThrow());

        mockMvc.perform(put("/api/client/profile")
                        .header(HttpHeaders.ORIGIN, FLUTTER_WEB_ORIGIN)
                        .with(user(layla))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "age": 32,
                                  "heightCm": 168.5,
                                  "currentWeightKg": 74.2,
                                  "goalType": "WEIGHT_LOSS",
                                  "targetWeightKg": 68.0,
                                  "trainingExperience": "BEGINNER",
                                  "dietaryPreferences": "Vegetarian"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.age").value(32))
                .andExpect(jsonPath("$.heightCm").value(168.5))
                .andExpect(jsonPath("$.currentWeightKg").value(74.2));

        mockMvc.perform(put("/api/client/profile")
                        .header(HttpHeaders.ORIGIN, FLUTTER_WEB_ORIGIN)
                        .with(user(layla))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "onboardingCompleted": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.age").value(32))
                .andExpect(jsonPath("$.heightCm").value(168.5))
                .andExpect(jsonPath("$.currentWeightKg").value(74.2))
                .andExpect(jsonPath("$.goalType").value("WEIGHT_LOSS"))
                .andExpect(jsonPath("$.targetWeightKg").value(68.0))
                .andExpect(jsonPath("$.trainingExperience").value("BEGINNER"))
                .andExpect(jsonPath("$.dietaryPreferences").value("Vegetarian"))
                .andExpect(jsonPath("$.onboardingCompleted").value(true));
    }

    @Test
    void clientProfilePhotoUploadSupportsPartialMultipartOnboardingPhotos() throws Exception {
        PtosUserDetails alex = new PtosUserDetails(userRepository.findByEmail("alex@ptos.local").orElseThrow());
        MockMultipartFile frontPhoto = new MockMultipartFile(
                "frontPhoto",
                "front.jpg",
                "image/jpeg",
                "front-photo".getBytes()
        );
        MockMultipartFile backPhoto = new MockMultipartFile(
                "backPhoto",
                "back.png",
                "image/png",
                "back-photo".getBytes()
        );

        mockMvc.perform(multipart("/api/client/profile/photos")
                        .file(frontPhoto)
                        .file(backPhoto)
                        .header(HttpHeaders.ORIGIN, FLUTTER_WEB_ORIGIN)
                        .with(user(alex)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FLUTTER_WEB_ORIGIN))
                .andExpect(jsonPath("$.clientUserId").isNumber())
                .andExpect(jsonPath("$.clientProfileId").isNumber())
                .andExpect(jsonPath("$.savedCount").value(2))
                .andExpect(jsonPath("$.savedPhotos.length()").value(2))
                .andExpect(jsonPath("$.savedPhotos[0].photoType").value("FRONT"))
                .andExpect(jsonPath("$.savedPhotos[0].storageKey").value(Matchers.startsWith("photos/onboarding/")))
                .andExpect(jsonPath("$.savedPhotos[0].url").value(Matchers.startsWith("/uploads/photos/onboarding/")))
                .andExpect(jsonPath("$.savedPhotos[1].photoType").value("BACK"));
    }

    @Test
    void clientCanCompleteOnboardingWithDedicatedEndpoint() throws Exception {
        PtosUserDetails alex = new PtosUserDetails(userRepository.findByEmail("alex@ptos.local").orElseThrow());

        mockMvc.perform(put("/api/client/onboarding/complete")
                        .header(HttpHeaders.ORIGIN, FLUTTER_WEB_ORIGIN)
                        .with(user(alex)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FLUTTER_WEB_ORIGIN))
                .andExpect(jsonPath("$.onboardingCompleted").value(true));
    }
}

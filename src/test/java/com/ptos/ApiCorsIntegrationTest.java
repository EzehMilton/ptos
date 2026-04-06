package com.ptos;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiCorsIntegrationTest {

    private static final String FLUTTER_WEB_ORIGIN = "http://localhost:54321";

    @Autowired
    private MockMvc mockMvc;

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
}

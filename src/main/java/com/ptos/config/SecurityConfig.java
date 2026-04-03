package com.ptos.config;

import com.ptos.domain.ClientProfile;
import com.ptos.domain.PTProfile;
import com.ptos.security.PtosUserDetails;
import com.ptos.service.ClientProfileService;
import com.ptos.service.PTProfileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AuthenticationSuccessHandler roleBasedSuccessHandler) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/signup", "/pt/signup", "/invite/**", "/uploads/**", "/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/pt/onboarding", "/pt/onboarding/**").hasRole("PT")
                .requestMatchers("/pt/**").hasRole("PT")
                .requestMatchers("/client/**").hasRole("CLIENT")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(roleBasedSuccessHandler)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .permitAll()
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/h2-console/**")
            )
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
            );
        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler roleBasedSuccessHandler(PTProfileService ptProfileService,
                                                                ClientProfileService clientProfileService) {
        return new AuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request,
                                                HttpServletResponse response,
                                                Authentication authentication) throws IOException {
                PtosUserDetails userDetails = (PtosUserDetails) authentication.getPrincipal();
                String targetUrl = switch (userDetails.getRole()) {
                    case PT -> ptProfileService.getProfileForUser(userDetails.getUserId())
                            .filter(PTProfile::isOnboardingComplete)
                            .map(profile -> "/pt/dashboard")
                            .orElse("/pt/onboarding");
                    case CLIENT -> clientProfileService.getProfileForUser(userDetails.getUserId())
                            .filter(ClientProfile::isOnboardingComplete)
                            .map(profile -> "/client/home")
                            .orElse("/client/onboarding");
                };
                response.sendRedirect(targetUrl);
            }
        };
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}

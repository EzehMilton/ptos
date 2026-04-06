package com.ptos.controller;

import com.ptos.domain.User;
import com.ptos.dto.PTSignupForm;
import com.ptos.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;

    @GetMapping("/pt/signup")
    public String showPtSignupForm(Model model) {
        log.info("Showing PT signup form");
        model.addAttribute("ptSignupForm", new PTSignupForm());
        return "auth/pt-signup";
    }

    @PostMapping("/pt/signup")
    public String processPtSignup(@Valid @ModelAttribute("ptSignupForm") PTSignupForm form,
                                  BindingResult result,
                                  HttpServletRequest request,
                                  HttpServletResponse response) {
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            log.info("Passwords do not match");
            result.rejectValue("confirmPassword", "password.mismatch", "Passwords do not match");
        }

        if (userService.emailExists(form.getEmail())) {
            log.info("Email already exists");
            result.rejectValue("email", "email.taken", "An account with this email already exists");
        }

        if (result.hasErrors()) {
            log.info("Validation errors: {}", result.getAllErrors());
            return "auth/pt-signup";
        }

        User user = userService.registerPT(form);
        log.info("PT registered: {}", user);
        authenticateNewUser(user.getEmail(), form.getPassword(), request, response);
        log.info("PT authenticated");
        return "redirect:/pt/onboarding";
    }

    private void authenticateNewUser(String email,
                                     String rawPassword,
                                     HttpServletRequest request,
                                     HttpServletResponse response) {
        log.info("Authenticating new user: {}", email);
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, rawPassword)
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        new HttpSessionSecurityContextRepository().saveContext(context, request, response);
    }
}

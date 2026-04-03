package com.ptos.controller;

import com.ptos.domain.User;
import com.ptos.dto.PTSignupForm;
import com.ptos.dto.SignupForm;
import com.ptos.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;

    @GetMapping("/signup")
    public String showSignupForm(Model model) {
        model.addAttribute("signupForm", new SignupForm());
        return "auth/signup";
    }

    @PostMapping("/signup")
    public String processSignup(@Valid @ModelAttribute("signupForm") SignupForm form,
                                BindingResult result,
                                RedirectAttributes redirectAttributes) {
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "password.mismatch", "Passwords do not match");
        }

        if (userService.emailExists(form.getEmail())) {
            result.rejectValue("email", "email.taken", "An account with this email already exists");
        }

        if (result.hasErrors()) {
            return "auth/signup";
        }

        userService.registerClient(form);
        redirectAttributes.addFlashAttribute("success", "Account created. Please log in.");
        return "redirect:/login";
    }

    @GetMapping("/pt/signup")
    public String showPtSignupForm(Model model) {
        model.addAttribute("ptSignupForm", new PTSignupForm());
        return "auth/pt-signup";
    }

    @PostMapping("/pt/signup")
    public String processPtSignup(@Valid @ModelAttribute("ptSignupForm") PTSignupForm form,
                                  BindingResult result,
                                  HttpServletRequest request,
                                  HttpServletResponse response) {
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "password.mismatch", "Passwords do not match");
        }

        if (userService.emailExists(form.getEmail())) {
            result.rejectValue("email", "email.taken", "An account with this email already exists");
        }

        if (result.hasErrors()) {
            return "auth/pt-signup";
        }

        User user = userService.registerPT(form);
        authenticateNewUser(user.getEmail(), form.getPassword(), request, response);
        return "redirect:/pt/onboarding";
    }

    private void authenticateNewUser(String email,
                                     String rawPassword,
                                     HttpServletRequest request,
                                     HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, rawPassword)
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        new HttpSessionSecurityContextRepository().saveContext(context, request, response);
    }
}

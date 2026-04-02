package com.ptos.controller;

import com.ptos.dto.SignupForm;
import com.ptos.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

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
}

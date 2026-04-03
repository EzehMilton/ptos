package com.ptos.service;

import com.ptos.domain.Role;
import com.ptos.domain.User;
import com.ptos.dto.PTSignupForm;
import com.ptos.dto.SignupForm;
import com.ptos.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    public User registerClient(SignupForm form) {
        User user = User.builder()
                .fullName(form.getFullName())
                .email(form.getEmail())
                .password(passwordEncoder.encode(form.getPassword()))
                .role(Role.CLIENT)
                .enabled(true)
                .build();
        return userRepository.save(user);
    }

    public User registerPT(PTSignupForm form) {
        User user = User.builder()
                .fullName(form.getFullName())
                .email(form.getEmail())
                .password(passwordEncoder.encode(form.getPassword()))
                .role(Role.PT)
                .enabled(true)
                .build();
        return userRepository.save(user);
    }
}

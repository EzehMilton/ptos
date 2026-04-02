package com.ptos.security;

import com.ptos.domain.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityHelper {

    public PtosUserDetails getCurrentUserDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (PtosUserDetails) auth.getPrincipal();
    }

    public Long getCurrentUserId() {
        return getCurrentUserDetails().getUserId();
    }

    public String getCurrentUserEmail() {
        return getCurrentUserDetails().getUsername();
    }

    public Role getCurrentUserRole() {
        return getCurrentUserDetails().getRole();
    }

    public boolean isPT() {
        return getCurrentUserRole() == Role.PT;
    }

    public boolean isClient() {
        return getCurrentUserRole() == Role.CLIENT;
    }
}

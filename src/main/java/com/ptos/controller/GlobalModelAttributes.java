package com.ptos.controller;

import com.ptos.domain.Role;
import com.ptos.security.PtosUserDetails;
import com.ptos.service.CheckInService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributes {

    private final CheckInService checkInService;

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String requestUri = request.getRequestURI();

        if (contextPath != null && !contextPath.isEmpty() && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }

        return requestUri;
    }

    @ModelAttribute("ptPendingCheckInCount")
    public long ptPendingCheckInCount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof PtosUserDetails userDetails)) {
            return 0;
        }
        if (userDetails.getRole() != Role.PT) {
            return 0;
        }
        return checkInService.countPendingCheckIns(userDetails.getUser());
    }
}

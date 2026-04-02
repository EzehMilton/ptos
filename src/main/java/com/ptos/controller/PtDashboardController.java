package com.ptos.controller;

import com.ptos.domain.User;
import com.ptos.dto.BusinessView;
import com.ptos.dto.DashboardView;
import com.ptos.security.SecurityHelper;
import com.ptos.service.BusinessService;
import com.ptos.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class PtDashboardController {

    private final DashboardService dashboardService;
    private final BusinessService businessService;
    private final SecurityHelper securityHelper;

    @GetMapping("/pt/dashboard")
    public String dashboard(Model model) {
        User ptUser = securityHelper.getCurrentUserDetails().getUser();
        DashboardView data = dashboardService.getDashboardData(ptUser);
        model.addAttribute("d", data);
        model.addAttribute("ptName", ptUser.getFullName());
        return "pt/dashboard";
    }

    @GetMapping("/pt/business")
    public String business(Model model) {
        User ptUser = securityHelper.getCurrentUserDetails().getUser();
        BusinessView biz = businessService.getBusinessData(ptUser);
        model.addAttribute("b", biz);
        return "pt/business";
    }
}

package com.ptos.controller;

import com.ptos.domain.User;
import com.ptos.dto.BusinessView;
import com.ptos.dto.DashboardView;
import com.ptos.security.SecurityHelper;
import com.ptos.service.BusinessService;
import com.ptos.service.DashboardService;
import com.ptos.service.MilestoneService;
import com.ptos.service.SuggestedActionsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class PtDashboardController {

    private final DashboardService dashboardService;
    private final BusinessService businessService;
    private final MilestoneService milestoneService;
    private final SuggestedActionsService suggestedActionsService;
    private final SecurityHelper securityHelper;

    @GetMapping("/pt/dashboard")
    public String dashboard(Model model) {
        User ptUser = securityHelper.getCurrentUserDetails().getUser();
        DashboardView data = dashboardService.getDashboardData(ptUser);
        model.addAttribute("d", data);
        model.addAttribute("suggestedActions", suggestedActionsService.getActions(ptUser));
        model.addAttribute("recentMilestoneFeed", milestoneService.getRecentMilestoneFeed(ptUser));
        model.addAttribute("ptName", ptUser.getFullName());
        return "pt/dashboard";
    }

    @GetMapping("/pt/business")
    public String business(Model model) {
        User ptUser = securityHelper.getCurrentUserDetails().getUser();
        BusinessView biz = businessService.getBusinessData(ptUser);
        model.addAttribute("b", biz);
        model.addAttribute("suggestedActions", suggestedActionsService.getActions(ptUser).stream().limit(5).toList());
        return "pt/business";
    }
}

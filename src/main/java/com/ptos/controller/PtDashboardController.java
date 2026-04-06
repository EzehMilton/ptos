package com.ptos.controller;

import com.ptos.domain.ClientStatus;
import com.ptos.domain.User;
import com.ptos.dto.BusinessView;
import com.ptos.dto.DashboardView;
import com.ptos.dto.BusinessView.ClientBusinessRow;
import com.ptos.security.SecurityHelper;
import com.ptos.service.BusinessService;
import com.ptos.service.DashboardService;
import com.ptos.service.InsightService;
import com.ptos.service.MilestoneService;
import com.ptos.service.SuggestedActionsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.support.RequestContextUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class PtDashboardController {

    private final DashboardService dashboardService;
    private final BusinessService businessService;
    private final MilestoneService milestoneService;
    private final SuggestedActionsService suggestedActionsService;
    private final InsightService insightService;
    private final SecurityHelper securityHelper;

    @GetMapping("/pt/dashboard")
    public String dashboard(Model model, HttpServletRequest request) {
        log.info("Rendering PT dashboard");
        User ptUser = securityHelper.getCurrentUserDetails().getUser();
        DashboardView data = dashboardService.getDashboardData(ptUser);
        BusinessView businessSnapshot = businessService.getBusinessData(ptUser);
        boolean firstTimeWelcome = RequestContextUtils.getInputFlashMap(request) != null
                && Boolean.TRUE.equals(RequestContextUtils.getInputFlashMap(request).get("firstTimeWelcome"));
        model.addAttribute("d", data);
        model.addAttribute("businessSnapshot", businessSnapshot);
        model.addAttribute("dashboardGreeting", resolveGreeting(firstTimeWelcome, ptUser));
        model.addAttribute("dashboardDateLabel", LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy")));
        model.addAttribute("dashboardSummary", buildSummary(data));
        model.addAttribute("healthOverviewRows", selectHealthOverviewRows(businessSnapshot));
        model.addAttribute("suggestedActions", suggestedActionsService.getActions(ptUser));
        model.addAttribute("recentMilestoneFeed", milestoneService.getRecentMilestoneFeed(ptUser));
        model.addAttribute("weeklySummaries", insightService.getRecentWeeklySummaries(ptUser, 5));
        model.addAttribute("ptName", ptUser.getFullName());
        return "pt/dashboard";
    }

    @GetMapping("/pt/business")
    public String business(Model model) {
        log.info("Rendering PT business dashboard");
        User ptUser = securityHelper.getCurrentUserDetails().getUser();
        BusinessView biz = businessService.getBusinessData(ptUser);
        model.addAttribute("b", biz);
        model.addAttribute("suggestedActions", suggestedActionsService.getActions(ptUser).stream().limit(5).toList());
        return "pt/business";
    }

    private String resolveGreeting(boolean firstTimeWelcome, User ptUser) {
        if (firstTimeWelcome) {
            return "Welcome, " + firstNameOf(ptUser);
        }

        LocalTime now = LocalTime.now();
        String salutation = now.isBefore(LocalTime.NOON)
                ? "Good morning"
                : (now.isBefore(LocalTime.of(18, 0)) ? "Good afternoon" : "Good evening");
        return salutation + ", " + firstNameOf(ptUser);
    }

    private String buildSummary(DashboardView dashboardView) {
        List<String> items = new ArrayList<>();
        items.add(dashboardView.getPendingCheckInCount() == 1
                ? "1 check-in to review"
                : dashboardView.getPendingCheckInCount() + " check-ins to review");
        items.add(dashboardView.getWorkoutsCompletedThisWeek() == 1
                ? "1 session completed this week"
                : dashboardView.getWorkoutsCompletedThisWeek() + " sessions completed this week");
        return String.join(" and ", items);
    }

    private List<ClientBusinessRow> selectHealthOverviewRows(BusinessView businessSnapshot) {
        List<ClientBusinessRow> activeRows = businessSnapshot.getClientBreakdown().stream()
                .filter(row -> row.getStatus() == ClientStatus.ACTIVE || row.getStatus() == ClientStatus.AT_RISK)
                .toList();
        if (activeRows.size() <= 6) {
            return activeRows;
        }

        List<ClientBusinessRow> strongest = activeRows.stream()
                .sorted(Comparator.comparingInt(ClientBusinessRow::getHealthScore).reversed())
                .limit(3)
                .toList();
        List<ClientBusinessRow> slipping = activeRows.stream()
                .sorted(Comparator.comparingInt(ClientBusinessRow::getHealthScore))
                .limit(3)
                .toList();

        LinkedHashSet<ClientBusinessRow> selected = new LinkedHashSet<>();
        selected.addAll(strongest);
        selected.addAll(slipping);
        return selected.stream().toList();
    }

    private String firstNameOf(User ptUser) {
        String fullName = ptUser.getFullName();
        if (fullName == null || fullName.isBlank()) {
            return "there";
        }
        return fullName.trim().split("\\s+")[0];
    }
}

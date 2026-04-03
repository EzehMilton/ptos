package com.ptos.controller;

import com.ptos.domain.AssignmentStatus;
import com.ptos.domain.CheckIn;
import com.ptos.domain.ClientInvitation;
import com.ptos.domain.ClientRecord;
import com.ptos.domain.User;
import com.ptos.dto.ClientDetailView;
import com.ptos.dto.ClientListView;
import com.ptos.dto.ClientRecordUpdateForm;
import com.ptos.dto.DirectCreateClientForm;
import com.ptos.dto.InviteClientForm;
import com.ptos.dto.Milestone;
import com.ptos.domain.WorkoutAssignment;
import com.ptos.repository.WorkoutAssignmentRepository;
import com.ptos.security.SecurityHelper;
import com.ptos.service.CheckInService;
import com.ptos.service.ClientInvitationService;
import com.ptos.service.ClientRecordService;
import com.ptos.service.HealthScoreService;
import com.ptos.service.InsightService;
import com.ptos.service.MilestoneService;
import com.ptos.service.NutritionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/pt/clients")
@RequiredArgsConstructor
public class PtClientsController {

    private static final String INVITE_BASE_URL = "http://localhost:8082/invite/";

    private final ClientRecordService clientRecordService;
    private final ClientInvitationService clientInvitationService;
    private final CheckInService checkInService;
    private final MilestoneService milestoneService;
    private final NutritionService nutritionService;
    private final HealthScoreService healthScoreService;
    private final InsightService insightService;
    private final WorkoutAssignmentRepository workoutAssignmentRepository;
    private final SecurityHelper securityHelper;

    @GetMapping
    public String listClients(@RequestParam(name = "q", required = false) String query, Model model) {
        User ptUser = securityHelper.getCurrentUserDetails().getUser();

        List<ClientListView> clients;
        if (query != null && !query.isBlank()) {
            clients = clientRecordService.searchClients(ptUser, query.trim());
            model.addAttribute("query", query.trim());
        } else {
            clients = clientRecordService.getClientListForPT(ptUser);
        }

        List<ClientInvitation> invitations = clientInvitationService.getAllInvitations(ptUser);
        model.addAttribute("clients", clients);
        model.addAttribute("clientCount", clients.size());
        model.addAttribute("invitations", invitations);
        model.addAttribute("pendingInvitationCount", invitations.stream()
                .filter(invitation -> invitation.getStatus().name().equals("PENDING"))
                .count());
        return "pt/clients/list";
    }

    @GetMapping("/new")
    public String showAddClientPage(Model model) {
        addNewClientPageAttributes(model);
        return "pt/clients/new";
    }

    @PostMapping("/invite")
    public String inviteClient(@Valid @ModelAttribute("inviteClientForm") InviteClientForm form,
                               BindingResult result,
                               Model model) {
        if (result.hasErrors()) {
            addNewClientPageAttributes(model);
            return "pt/clients/new";
        }

        try {
            ClientInvitation invitation = clientInvitationService.createInvitation(
                    securityHelper.getCurrentUserDetails().getUser(), form
            );
            model.addAttribute("invitation", invitation);
            model.addAttribute("inviteLink", INVITE_BASE_URL + invitation.getToken());
            return "pt/clients/invite-sent";
        } catch (IllegalArgumentException ex) {
            result.rejectValue("email", "invite.error", ex.getMessage());
            addNewClientPageAttributes(model);
            return "pt/clients/new";
        }
    }

    @PostMapping("/create-direct")
    public String createDirectClient(@Valid @ModelAttribute("directCreateClientForm") DirectCreateClientForm form,
                                     BindingResult result,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            addNewClientPageAttributes(model);
            return "pt/clients/new";
        }

        try {
            ClientRecord record = clientInvitationService.createDirectClient(
                    securityHelper.getCurrentUserDetails().getUser(), form
            );
            redirectAttributes.addFlashAttribute(
                    "success",
                    "Client created. They can log in with the email and temporary password."
            );
            return "redirect:/pt/clients/" + record.getId();
        } catch (IllegalArgumentException ex) {
            result.rejectValue("email", "create.error", ex.getMessage());
            addNewClientPageAttributes(model);
            return "pt/clients/new";
        }
    }

    @GetMapping("/{id}")
    public String clientDetail(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        User ptUser = securityHelper.getCurrentUserDetails().getUser();
        Optional<ClientRecord> recordOpt = clientRecordService.getClientRecord(id, ptUser);
        if (recordOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Client not found.");
            return "redirect:/pt/clients";
        }

        ClientRecord record = recordOpt.get();
        ClientDetailView detail = clientRecordService.getClientDetail(record);
        model.addAttribute("detail", detail);
        model.addAttribute("activeMealPlan", nutritionService.getActiveMealPlan(record).orElse(null));
        model.addAttribute("healthScore", healthScoreService.calculateHealthScore(record));
        model.addAttribute("riskInsight", insightService.getAtRiskInsight(record));
        addAssignmentAttributes(record, model);

        ClientRecordUpdateForm form = new ClientRecordUpdateForm();
        form.setStatus(detail.getStatus());
        form.setMonthlyPackagePrice(detail.getMonthlyPackagePrice());
        form.setPtNotes(detail.getPtNotes());
        model.addAttribute("updateForm", form);

        return "pt/clients/detail";
    }

    @PostMapping("/{id}")
    public String updateClient(@PathVariable Long id,
                               @Valid @ModelAttribute("updateForm") ClientRecordUpdateForm form,
                               BindingResult result,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        User ptUser = securityHelper.getCurrentUserDetails().getUser();

        if (result.hasErrors()) {
            Optional<ClientRecord> recordOpt = clientRecordService.getClientRecord(id, ptUser);
            if (recordOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Client not found.");
                return "redirect:/pt/clients";
            }

            ClientRecord record = recordOpt.get();
            model.addAttribute("detail", clientRecordService.getClientDetail(record));
            model.addAttribute("activeMealPlan", nutritionService.getActiveMealPlan(record).orElse(null));
            model.addAttribute("healthScore", healthScoreService.calculateHealthScore(record));
            model.addAttribute("riskInsight", insightService.getAtRiskInsight(record));
            addAssignmentAttributes(record, model);
            return "pt/clients/detail";
        }

        clientRecordService.updateRecord(id, ptUser, form);
        redirectAttributes.addFlashAttribute("success", "Client updated");
        return "redirect:/pt/clients/" + id;
    }

    private void addAssignmentAttributes(ClientRecord record, Model model) {
        List<WorkoutAssignment> assignments = workoutAssignmentRepository.findByClientRecordOrderByAssignedDateDesc(record);
        long completedAssignmentCount = workoutAssignmentRepository.countByClientRecordAndStatus(
                record, AssignmentStatus.COMPLETED);
        List<CheckIn> recentCheckIns = checkInService.getCheckInsForClientRecord(record).stream()
                .limit(5)
                .toList();
        List<Milestone> milestones = milestoneService.getMilestones(record, record.getClientUser());

        model.addAttribute("assignments", assignments);
        model.addAttribute("assignmentCount", assignments.size());
        model.addAttribute("completedAssignmentCount", completedAssignmentCount);
        model.addAttribute("recentCheckIns", recentCheckIns);
        model.addAttribute("milestones", milestones);
    }

    private void addNewClientPageAttributes(Model model) {
        if (!model.containsAttribute("inviteClientForm")) {
            model.addAttribute("inviteClientForm", new InviteClientForm());
        }
        if (!model.containsAttribute("directCreateClientForm")) {
            model.addAttribute("directCreateClientForm", new DirectCreateClientForm());
        }
    }
}

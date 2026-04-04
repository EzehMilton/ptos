package com.ptos.controller;

import com.ptos.domain.AssignmentStatus;
import com.ptos.domain.CheckIn;
import com.ptos.domain.CheckInStatus;
import com.ptos.domain.ClientInvitation;
import com.ptos.domain.ClientRecord;
import com.ptos.domain.ClientStatus;
import com.ptos.domain.GoalType;
import com.ptos.domain.User;
import com.ptos.dto.ClientDetailView;
import com.ptos.dto.ClientDirectoryItem;
import com.ptos.dto.ClientHealthScoreResult;
import com.ptos.dto.ClientRecordUpdateForm;
import com.ptos.dto.DeleteClientForm;
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
import com.ptos.service.MessagingService;
import com.ptos.service.NutritionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/pt/clients")
@RequiredArgsConstructor
public class PtClientsController {

    private final ClientRecordService clientRecordService;
    private final ClientInvitationService clientInvitationService;
    private final CheckInService checkInService;
    private final MilestoneService milestoneService;
    private final NutritionService nutritionService;
    private final HealthScoreService healthScoreService;
    private final InsightService insightService;
    private final MessagingService messagingService;
    private final WorkoutAssignmentRepository workoutAssignmentRepository;
    private final SecurityHelper securityHelper;

    @GetMapping
    public String listClients(@RequestParam(name = "q", required = false) String query,
                              @RequestParam(name = "tab", defaultValue = "all") String tab,
                              @RequestParam(name = "sort", defaultValue = "health") String sort,
                              Model model) {
        User ptUser = securityHelper.getCurrentUserDetails().getUser();
        String normalizedQuery = query != null && !query.isBlank() ? query.trim() : null;
        String activeTab = normalizeTab(tab);
        String activeSort = normalizeSort(sort);

        List<ClientRecord> allClientRecords = clientRecordService.getClientsForPT(ptUser);
        List<ClientInvitation> allInvitations = clientInvitationService.getAllInvitations(ptUser);
        Map<ClientStatus, Long> statusCounts = clientRecordService.getStatusCounts(ptUser);

        List<ClientDirectoryItem> clients = allClientRecords.stream()
                .map(this::toDirectoryItem)
                .filter(item -> matchesQuery(item, normalizedQuery))
                .filter(item -> matchesTab(item, activeTab))
                .sorted(comparatorFor(activeSort))
                .toList();

        List<ClientInvitation> invitations = allInvitations.stream()
                .filter(invitation -> matchesInvitationQuery(invitation, normalizedQuery))
                .toList();

        model.addAttribute("query", normalizedQuery);
        model.addAttribute("activeTab", activeTab);
        model.addAttribute("activeSort", activeSort);
        model.addAttribute("clients", clients);
        model.addAttribute("clientCount", clients.size());
        model.addAttribute("invitations", invitations);
        model.addAttribute("invitationCount", invitations.size());
        model.addAttribute("allCount", statusCounts.values().stream().mapToLong(Long::longValue).sum());
        model.addAttribute("activeCount", statusCounts.getOrDefault(ClientStatus.ACTIVE, 0L));
        model.addAttribute("atRiskCount", statusCounts.getOrDefault(ClientStatus.AT_RISK, 0L));
        model.addAttribute("inactiveCount", statusCounts.getOrDefault(ClientStatus.INACTIVE, 0L));
        model.addAttribute("invitationTotalCount", allInvitations.size());
        return "pt/clients/list";
    }

    @GetMapping("/new")
    public String showAddClientPage(Model model) {
        addNewClientPageAttributes(model, "invite");
        return "pt/clients/new";
    }

    @PostMapping("/invite")
    public String inviteClient(@Valid @ModelAttribute("inviteClientForm") InviteClientForm form,
                               BindingResult result,
                               Model model) {
        if (result.hasErrors()) {
            addNewClientPageAttributes(model, "invite");
            return "pt/clients/new";
        }

        try {
            ClientInvitation invitation = clientInvitationService.createInvitation(
                    securityHelper.getCurrentUserDetails().getUser(), form
            );
            model.addAttribute("invitation", invitation);
            model.addAttribute("inviteLink", ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/invite/{token}")
                    .buildAndExpand(invitation.getToken())
                    .toUriString());
            return "pt/clients/invite-sent";
        } catch (IllegalArgumentException ex) {
            result.rejectValue("email", "invite.error", ex.getMessage());
            addNewClientPageAttributes(model, "invite");
            return "pt/clients/new";
        }
    }

    @PostMapping("/create-direct")
    public String createDirectClient(@Valid @ModelAttribute("directCreateClientForm") DirectCreateClientForm form,
                                     BindingResult result,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            addNewClientPageAttributes(model, "direct");
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
            addNewClientPageAttributes(model, "direct");
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
        model.addAttribute("existingConversation",
                messagingService.getConversationForPTAndClient(ptUser, record.getClientUser()).orElse(null));
        model.addAttribute("clientRecordNotes", clientRecordService.getNotesForClientRecord(record));
        addAssignmentAttributes(record, model);

        ClientRecordUpdateForm form = new ClientRecordUpdateForm();
        form.setStatus(detail.getStatus());
        form.setMonthlyPackagePrice(detail.getMonthlyPackagePrice());
        model.addAttribute("updateForm", form);

        return "pt/clients/detail";
    }

    @GetMapping("/{id}/delete")
    public String showDeleteClientPage(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        User ptUser = securityHelper.getCurrentUserDetails().getUser();
        Optional<ClientRecord> recordOpt = clientRecordService.getClientRecord(id, ptUser);
        if (recordOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Client not found.");
            return "redirect:/pt/clients";
        }

        ClientRecord record = recordOpt.get();
        model.addAttribute("detail", clientRecordService.getClientDetail(record));
        if (!model.containsAttribute("deleteClientForm")) {
            model.addAttribute("deleteClientForm", new DeleteClientForm());
        }
        return "pt/clients/delete";
    }

    @PostMapping("/{id}/delete")
    public String deleteClient(@PathVariable Long id,
                               @Valid @ModelAttribute("deleteClientForm") DeleteClientForm form,
                               BindingResult result,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        User ptUser = securityHelper.getCurrentUserDetails().getUser();
        Optional<ClientRecord> recordOpt = clientRecordService.getClientRecord(id, ptUser);
        if (recordOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Client not found.");
            return "redirect:/pt/clients";
        }

        ClientRecord record = recordOpt.get();
        ClientDetailView detail = clientRecordService.getClientDetail(record);

        if (!result.hasErrors() && !detail.getClientName().equals(form.getConfirmationName().trim())) {
            result.rejectValue("confirmationName", "confirmation.mismatch",
                    "The entered name does not match this client.");
        }

        if (result.hasErrors()) {
            model.addAttribute("detail", detail);
            return "pt/clients/delete";
        }

        clientRecordService.deleteClientRecord(id, ptUser);
        redirectAttributes.addFlashAttribute("success", "Client deleted.");
        return "redirect:/pt/clients";
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
            model.addAttribute("existingConversation",
                    messagingService.getConversationForPTAndClient(ptUser, record.getClientUser()).orElse(null));
            model.addAttribute("clientRecordNotes", clientRecordService.getNotesForClientRecord(record));
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

    private void addNewClientPageAttributes(Model model, String activeMode) {
        if (!model.containsAttribute("inviteClientForm")) {
            model.addAttribute("inviteClientForm", new InviteClientForm());
        }
        if (!model.containsAttribute("directCreateClientForm")) {
            model.addAttribute("directCreateClientForm", new DirectCreateClientForm());
        }
        model.addAttribute("clientCreationMode", activeMode);
    }

    private ClientDirectoryItem toDirectoryItem(ClientRecord record) {
        ClientDetailView detail = clientRecordService.getClientDetail(record);
        ClientHealthScoreResult healthScore = healthScoreService.calculateHealthScore(record);
        Optional<CheckIn> latestCheckIn = checkInService.getCheckInsForClientRecord(record).stream().findFirst();

        return ClientDirectoryItem.builder()
                .clientRecordId(detail.getClientRecordId())
                .clientName(detail.getClientName())
                .clientEmail(detail.getClientEmail())
                .initials(initialsOf(detail.getClientName()))
                .goalLabel(goalLabel(detail.getGoalType()))
                .weightSummary(weightSummary(detail))
                .packageSummary(detail.getMonthlyPackagePrice() != null
                        ? "£" + detail.getMonthlyPackagePrice().setScale(0, RoundingMode.HALF_UP) + "/mo"
                        : "No package set")
                .activitySummary(activitySummary(detail, healthScore, latestCheckIn.orElse(null)))
                .status(detail.getStatus())
                .healthScore(healthScore.getOverallScore())
                .healthScoreClass(healthScoreClass(healthScore.getOverallScore()))
                .primaryActionLabel("Message")
                .primaryActionUrl("/pt/messages/new/" + detail.getClientRecordId())
                .secondaryActionLabel(secondaryActionLabel(detail.getStatus()))
                .secondaryActionUrl("/pt/clients/" + detail.getClientRecordId())
                .build();
    }

    private boolean matchesQuery(ClientDirectoryItem item, String query) {
        if (query == null) {
            return true;
        }
        String lower = query.toLowerCase();
        return item.getClientName().toLowerCase().contains(lower)
                || item.getClientEmail().toLowerCase().contains(lower);
    }

    private boolean matchesInvitationQuery(ClientInvitation invitation, String query) {
        if (query == null) {
            return true;
        }
        String lower = query.toLowerCase();
        return invitation.getFullName().toLowerCase().contains(lower)
                || invitation.getEmail().toLowerCase().contains(lower);
    }

    private boolean matchesTab(ClientDirectoryItem item, String tab) {
        return switch (tab) {
            case "active" -> item.getStatus() == ClientStatus.ACTIVE;
            case "at-risk" -> item.getStatus() == ClientStatus.AT_RISK;
            case "inactive" -> item.getStatus() == ClientStatus.INACTIVE;
            default -> true;
        };
    }

    private Comparator<ClientDirectoryItem> comparatorFor(String sort) {
        return switch (sort) {
            case "name" -> Comparator.comparing(ClientDirectoryItem::getClientName, String.CASE_INSENSITIVE_ORDER);
            case "status" -> Comparator.comparing(item -> item.getStatus().name());
            default -> Comparator.comparingInt(ClientDirectoryItem::getHealthScore).reversed()
                    .thenComparing(ClientDirectoryItem::getClientName, String.CASE_INSENSITIVE_ORDER);
        };
    }

    private String normalizeTab(String tab) {
        return switch (tab) {
            case "active", "at-risk", "inactive", "invitations" -> tab;
            default -> "all";
        };
    }

    private String normalizeSort(String sort) {
        return switch (sort) {
            case "name", "status" -> sort;
            default -> "health";
        };
    }

    private String initialsOf(String name) {
        return java.util.Arrays.stream(name.trim().split("\\s+"))
                .filter(part -> !part.isBlank())
                .limit(2)
                .map(part -> part.substring(0, 1).toUpperCase())
                .collect(Collectors.joining());
    }

    private String goalLabel(GoalType goalType) {
        if (goalType == null) {
            return "No goal set";
        }
        return switch (goalType) {
            case WEIGHT_LOSS -> "Weight loss";
            case MUSCLE_GAIN -> "Muscle gain";
            case STRENGTH -> "Strength";
            case ENDURANCE -> "Endurance";
            case GENERAL_FITNESS -> "General fitness";
        };
    }

    private String weightSummary(ClientDetailView detail) {
        if (detail.getCurrentWeightKg() == null && detail.getTargetWeightKg() == null) {
            return "No weight data";
        }
        if (detail.getCurrentWeightKg() != null && detail.getTargetWeightKg() != null) {
            return String.format("%.1f -> %.1f kg", detail.getCurrentWeightKg(), detail.getTargetWeightKg());
        }
        if (detail.getCurrentWeightKg() != null) {
            return String.format("%.1f kg", detail.getCurrentWeightKg());
        }
        return String.format("Target %.1f kg", detail.getTargetWeightKg());
    }

    private String activitySummary(ClientDetailView detail, ClientHealthScoreResult healthScore, CheckIn latestCheckIn) {
        if (latestCheckIn != null && latestCheckIn.getStatus() == CheckInStatus.PENDING_REVIEW) {
            return "Check-in pending";
        }
        if (healthScore.getDaysSinceLastActivity() != null) {
            int days = healthScore.getDaysSinceLastActivity();
            if (days == 0) {
                return "Active today";
            }
            if (days == 1) {
                return "Last activity 1 day ago";
            }
            return "No activity for " + days + " days";
        }
        if (latestCheckIn != null) {
            return "Last check-in " + latestCheckIn.getSubmittedAt().format(DateTimeFormatter.ofPattern("dd MMM"));
        }
        return detail.getStatus() == ClientStatus.INACTIVE ? "Inactive client" : "No recent activity";
    }

    private String healthScoreClass(int score) {
        if (score >= 70) {
            return "clients-score-fill-healthy";
        }
        if (score >= 50) {
            return "clients-score-fill-watch";
        }
        if (score >= 30) {
            return "clients-score-fill-risk";
        }
        return "clients-score-fill-churning";
    }

    private String secondaryActionLabel(ClientStatus status) {
        return switch (status) {
            case ACTIVE -> "View";
            case AT_RISK -> "Re-engage";
            case INACTIVE -> "Reactivate";
            case ARCHIVED -> "View";
        };
    }
}

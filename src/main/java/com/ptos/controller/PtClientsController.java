package com.ptos.controller;

import com.ptos.domain.AssignmentStatus;
import com.ptos.domain.CheckIn;
import com.ptos.domain.ClientRecord;
import com.ptos.domain.User;
import com.ptos.dto.ClientDetailView;
import com.ptos.dto.ClientListView;
import com.ptos.dto.ClientRecordUpdateForm;
import com.ptos.dto.Milestone;
import com.ptos.domain.WorkoutAssignment;
import com.ptos.repository.WorkoutAssignmentRepository;
import com.ptos.security.SecurityHelper;
import com.ptos.service.CheckInService;
import com.ptos.service.ClientRecordService;
import com.ptos.service.MilestoneService;
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

    private final ClientRecordService clientRecordService;
    private final CheckInService checkInService;
    private final MilestoneService milestoneService;
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

        model.addAttribute("clients", clients);
        model.addAttribute("clientCount", clients.size());
        return "pt/clients/list";
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
}

package com.ptos.controller;

import com.ptos.domain.User;
import com.ptos.dto.ClientDetailView;
import com.ptos.dto.ClientListView;
import com.ptos.dto.ClientRecordUpdateForm;
import com.ptos.security.SecurityHelper;
import com.ptos.service.ClientRecordService;
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
        Optional<ClientDetailView> detailOpt = clientRecordService.getClientDetail(id, ptUser);

        if (detailOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Client not found.");
            return "redirect:/pt/clients";
        }

        ClientDetailView detail = detailOpt.get();
        model.addAttribute("detail", detail);

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
            Optional<ClientDetailView> detailOpt = clientRecordService.getClientDetail(id, ptUser);
            if (detailOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Client not found.");
                return "redirect:/pt/clients";
            }
            model.addAttribute("detail", detailOpt.get());
            return "pt/clients/detail";
        }

        clientRecordService.updateRecord(id, ptUser, form);
        redirectAttributes.addFlashAttribute("success", "Client updated");
        return "redirect:/pt/clients/" + id;
    }
}

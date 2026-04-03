package com.ptos.controller;

import com.ptos.domain.Conversation;
import com.ptos.domain.Message;
import com.ptos.domain.User;
import com.ptos.dto.MessageForm;
import com.ptos.security.SecurityHelper;
import com.ptos.service.ClientRecordService;
import com.ptos.service.MessagingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/pt/messages")
@RequiredArgsConstructor
public class PtMessageController {

    private final MessagingService messagingService;
    private final ClientRecordService clientRecordService;
    private final SecurityHelper securityHelper;

    @GetMapping
    public String inbox(Model model) {
        User ptUser = securityHelper.getCurrentUserDetails().getUser();
        model.addAttribute("conversations", messagingService.getConversationsForPT(ptUser));
        return "pt/messages/inbox";
    }

    @GetMapping("/{conversationId}")
    public String conversation(@PathVariable Long conversationId, Model model, RedirectAttributes redirectAttributes) {
        User ptUser = securityHelper.getCurrentUserDetails().getUser();
        Optional<Conversation> conversationOpt = messagingService.getConversationForPT(conversationId, ptUser);
        if (conversationOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Conversation not found.");
            return "redirect:/pt/messages";
        }

        Conversation conversation = conversationOpt.get();
        messagingService.markAsRead(conversationId, ptUser);
        populateConversationModel(model, conversation, messagingService.getMessages(conversationId, ptUser));
        if (!model.containsAttribute("messageForm")) {
            model.addAttribute("messageForm", new MessageForm());
        }
        return "pt/messages/conversation";
    }

    @PostMapping("/{conversationId}")
    public String sendMessage(@PathVariable Long conversationId,
                              @Valid @ModelAttribute("messageForm") MessageForm form,
                              BindingResult result,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        User ptUser = securityHelper.getCurrentUserDetails().getUser();
        Optional<Conversation> conversationOpt = messagingService.getConversationForPT(conversationId, ptUser);
        if (conversationOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Conversation not found.");
            return "redirect:/pt/messages";
        }

        Conversation conversation = conversationOpt.get();
        if (result.hasErrors()) {
            populateConversationModel(model, conversation, messagingService.getMessages(conversationId, ptUser));
            return "pt/messages/conversation";
        }

        try {
            messagingService.sendMessage(conversationId, ptUser, form.getContent());
        } catch (IllegalArgumentException ex) {
            result.rejectValue("content", "message.error", ex.getMessage());
            populateConversationModel(model, conversation, messagingService.getMessages(conversationId, ptUser));
            return "pt/messages/conversation";
        }
        return "redirect:/pt/messages/" + conversationId;
    }

    @GetMapping("/new/{clientRecordId}")
    public String startConversation(@PathVariable Long clientRecordId, RedirectAttributes redirectAttributes) {
        User ptUser = securityHelper.getCurrentUserDetails().getUser();
        return clientRecordService.getClientRecord(clientRecordId, ptUser)
                .map(record -> {
                    Conversation conversation = messagingService.getOrCreateConversation(ptUser, record.getClientUser());
                    return "redirect:/pt/messages/" + conversation.getId();
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Client not found.");
                    return "redirect:/pt/clients";
                });
    }

    private void populateConversationModel(Model model, Conversation conversation, List<Message> messages) {
        model.addAttribute("conversation", conversation);
        model.addAttribute("messages", messages);
        model.addAttribute("otherPersonName", conversation.getClientUser().getFullName());
        model.addAttribute(
                "clientRecordId",
                clientRecordService.getClientRecord(conversation.getPtUser(), conversation.getClientUser())
                        .map(record -> record.getId())
                        .orElse(null)
        );
    }
}

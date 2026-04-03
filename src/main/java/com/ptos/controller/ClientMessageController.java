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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/client/messages")
@RequiredArgsConstructor
public class ClientMessageController {

    private final MessagingService messagingService;
    private final ClientRecordService clientRecordService;
    private final SecurityHelper securityHelper;

    @GetMapping
    public String conversation(Model model) {
        User clientUser = securityHelper.getCurrentUserDetails().getUser();
        Optional<Conversation> conversationOpt = messagingService.getConversationForClient(clientUser);
        model.addAttribute("messages", List.of());
        if (conversationOpt.isPresent()) {
            Conversation conversation = conversationOpt.get();
            messagingService.markAsRead(conversation.getId(), clientUser);
            populateConversationModel(model, conversation, messagingService.getMessages(conversation.getId(), clientUser));
        } else {
            clientRecordService.getClientRecord(clientUser)
                    .ifPresent(record -> model.addAttribute("ptName", record.getPtUser().getFullName()));
        }

        if (!model.containsAttribute("messageForm")) {
            model.addAttribute("messageForm", new MessageForm());
        }
        return "client/messages/conversation";
    }

    @PostMapping
    public String sendMessage(@Valid @ModelAttribute("messageForm") MessageForm form,
                              BindingResult result,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        User clientUser = securityHelper.getCurrentUserDetails().getUser();
        Optional<Conversation> existingConversation = messagingService.getConversationForClient(clientUser);

        if (result.hasErrors()) {
            if (existingConversation.isPresent()) {
                Conversation conversation = existingConversation.get();
                populateConversationModel(model, conversation, messagingService.getMessages(conversation.getId(), clientUser));
            } else {
                model.addAttribute("messages", List.of());
                clientRecordService.getClientRecord(clientUser)
                        .ifPresent(record -> model.addAttribute("ptName", record.getPtUser().getFullName()));
            }
            return "client/messages/conversation";
        }

        return clientRecordService.getClientRecord(clientUser)
                .map(record -> {
                    Conversation conversation = messagingService.getOrCreateConversation(record.getPtUser(), clientUser);
                    try {
                        messagingService.sendMessage(conversation.getId(), clientUser, form.getContent());
                    } catch (IllegalArgumentException ex) {
                        model.addAttribute("ptName", record.getPtUser().getFullName());
                        populateConversationModel(model, conversation, messagingService.getMessages(conversation.getId(), clientUser));
                        result.rejectValue("content", "message.error", ex.getMessage());
                        return "client/messages/conversation";
                    }
                    redirectAttributes.addFlashAttribute("success", "Message sent");
                    return "redirect:/client/messages";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "You are not linked to a PT yet.");
                    return "redirect:/client/messages";
                });
    }

    private void populateConversationModel(Model model, Conversation conversation, List<Message> messages) {
        model.addAttribute("conversation", conversation);
        model.addAttribute("messages", messages);
        model.addAttribute("ptName", conversation.getPtUser().getFullName());
    }
}

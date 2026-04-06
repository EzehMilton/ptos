package com.ptos.controller.api;

import com.ptos.domain.ClientRecord;
import com.ptos.domain.Conversation;
import com.ptos.domain.Message;
import com.ptos.domain.User;
import com.ptos.dto.api.MessageResponse;
import com.ptos.dto.api.SendMessageRequest;
import com.ptos.repository.ClientRecordRepository;
import com.ptos.security.PtosUserDetails;
import com.ptos.service.MessagingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/client/messages")
@RequiredArgsConstructor
public class ClientMessagingApiController {

    private final MessagingService messagingService;
    private final ClientRecordRepository clientRecordRepository;

    @GetMapping
    public ResponseEntity<List<MessageResponse>> getMessages(
            @AuthenticationPrincipal PtosUserDetails userDetails) {
        User user = userDetails.getUser();
        Conversation conversation = messagingService.getConversationForClient(user).orElse(null);
        if (conversation == null) {
            return ResponseEntity.ok(List.of());
        }

        messagingService.markAsRead(conversation.getId(), user);
        List<MessageResponse> messages = messagingService.getMessages(conversation.getId(), user).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(messages);
    }

    @PostMapping
    public ResponseEntity<MessageResponse> sendMessage(
            @AuthenticationPrincipal PtosUserDetails userDetails,
            @Valid @RequestBody SendMessageRequest request) {
        User user = userDetails.getUser();
        ClientRecord clientRecord = clientRecordRepository.findByClientUser(user)
                .orElseThrow(() -> new IllegalArgumentException("Client record not found"));

        Conversation conversation = messagingService.getOrCreateConversation(
                clientRecord.getPtUser(), user);

        messagingService.markAsRead(conversation.getId(), user);
        Message message = messagingService.sendMessage(conversation.getId(), user, request.content());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(message));
    }

    private MessageResponse toResponse(Message message) {
        return new MessageResponse(
                message.getId(), message.getSenderRole().name(),
                message.getContent(), message.getCreatedAt(), message.getReadAt()
        );
    }
}

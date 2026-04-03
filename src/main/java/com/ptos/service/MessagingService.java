package com.ptos.service;

import com.ptos.domain.Conversation;
import com.ptos.domain.Message;
import com.ptos.domain.Role;
import com.ptos.domain.User;
import com.ptos.dto.ConversationListView;
import com.ptos.integration.MessageDeliveryGateway;
import com.ptos.repository.ClientRecordRepository;
import com.ptos.repository.ConversationRepository;
import com.ptos.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MessagingService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final MessageDeliveryGateway messageDeliveryGateway;
    private final ClientRecordRepository clientRecordRepository;

    @Transactional
    public Conversation getOrCreateConversation(User ptUser, User clientUser) {
        return conversationRepository.findByPtUserAndClientUser(ptUser, clientUser)
                .orElseGet(() -> {
                    if (!clientRecordRepository.existsByPtUserAndClientUser(ptUser, clientUser)) {
                        throw new IllegalArgumentException("This client does not belong to this PT");
                    }
                    return conversationRepository.save(Conversation.builder()
                            .ptUser(ptUser)
                            .clientUser(clientUser)
                            .build());
                });
    }

    public List<ConversationListView> getConversationsForPT(User ptUser) {
        return conversationRepository.findByPtUserOrderByLastMessageAtDesc(ptUser).stream()
                .map(this::toConversationListViewForPT)
                .sorted(Comparator.comparing(ConversationListView::getLastMessageAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public Optional<Conversation> getConversationForClient(User clientUser) {
        return conversationRepository.findByClientUser(clientUser);
    }

    public Optional<Conversation> getConversationForPTAndClient(User ptUser, User clientUser) {
        return conversationRepository.findByPtUserAndClientUser(ptUser, clientUser);
    }

    public Optional<Conversation> getConversationForPT(Long conversationId, User ptUser) {
        return conversationRepository.findByIdAndPtUser(conversationId, ptUser);
    }

    public List<Message> getMessages(Long conversationId, User currentUser) {
        Conversation conversation = getConversationForParticipant(conversationId, currentUser);
        return messageRepository.findByConversationOrderByCreatedAtAsc(conversation);
    }

    @Transactional
    public Message sendMessage(Long conversationId, User sender, String content) {
        Conversation conversation = getConversationForParticipant(conversationId, sender);
        String trimmedContent = requireContent(content);
        LocalDateTime now = LocalDateTime.now();

        Message message = messageRepository.save(Message.builder()
                .conversation(conversation)
                .senderUser(sender)
                .senderRole(sender.getRole())
                .content(trimmedContent)
                .build());

        conversation.setLastMessageAt(now);
        if (sender.getRole() == Role.PT) {
            conversation.setUnreadCountClient(conversation.getUnreadCountClient() + 1);
        } else {
            conversation.setUnreadCountPt(conversation.getUnreadCountPt() + 1);
        }
        conversationRepository.save(conversation);

        messageDeliveryGateway.onMessageSent(conversation, message);
        return message;
    }

    @Transactional
    public void markAsRead(Long conversationId, User reader) {
        Conversation conversation = getConversationForParticipant(conversationId, reader);
        LocalDateTime now = LocalDateTime.now();
        boolean changed = false;

        List<Message> messages = messageRepository.findByConversationOrderByCreatedAtAsc(conversation);
        if (reader.getRole() == Role.PT) {
            if (conversation.getUnreadCountPt() != 0) {
                conversation.setUnreadCountPt(0);
                changed = true;
            }
            for (Message message : messages) {
                if (message.getSenderRole() == Role.CLIENT && message.getReadAt() == null) {
                    message.setReadAt(now);
                    changed = true;
                }
            }
        } else {
            if (conversation.getUnreadCountClient() != 0) {
                conversation.setUnreadCountClient(0);
                changed = true;
            }
            for (Message message : messages) {
                if (message.getSenderRole() == Role.PT && message.getReadAt() == null) {
                    message.setReadAt(now);
                    changed = true;
                }
            }
        }

        if (changed) {
            conversationRepository.save(conversation);
            messageRepository.saveAll(messages);
            messageDeliveryGateway.onConversationRead(conversation, reader);
        }
    }

    public int getUnreadCountForPT(User ptUser) {
        return conversationRepository.sumUnreadCountPtByPtUser(ptUser);
    }

    public int getUnreadCountForClient(User clientUser) {
        return conversationRepository.sumUnreadCountClientByClientUser(clientUser);
    }

    private ConversationListView toConversationListViewForPT(Conversation conversation) {
        Message latestMessage = messageRepository.findByConversationOrderByCreatedAtDesc(
                conversation, PageRequest.of(0, 1)
        ).stream().findFirst().orElse(null);

        return ConversationListView.builder()
                .conversationId(conversation.getId())
                .otherPersonName(conversation.getClientUser().getFullName())
                .lastMessagePreview(latestMessage == null ? "No messages yet." : toPreview(latestMessage.getContent()))
                .lastMessageAt(conversation.getLastMessageAt())
                .unreadCount(conversation.getUnreadCountPt())
                .otherPersonRole(Role.CLIENT)
                .relativeTime(formatRelativeTime(conversation.getLastMessageAt()))
                .build();
    }

    private String toPreview(String content) {
        String normalized = content.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 80) {
            return normalized;
        }
        return normalized.substring(0, 77) + "...";
    }

    private String formatRelativeTime(LocalDateTime timestamp) {
        if (timestamp == null) {
            return "No messages yet";
        }

        LocalDateTime now = LocalDateTime.now();
        long minutes = java.time.Duration.between(timestamp, now).toMinutes();
        if (minutes < 1) {
            return "Just now";
        }
        if (minutes < 60) {
            return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
        }

        long hours = java.time.Duration.between(timestamp, now).toHours();
        if (hours < 24) {
            return hours + (hours == 1 ? " hour ago" : " hours ago");
        }

        if (timestamp.toLocalDate().equals(LocalDate.now().minusDays(1))) {
            return "Yesterday";
        }

        long days = java.time.Duration.between(timestamp, now).toDays();
        if (days < 7) {
            return days + " days ago";
        }

        return timestamp.format(DateTimeFormatter.ofPattern("dd MMM"));
    }

    private Conversation getConversationForParticipant(Long conversationId, User currentUser) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        boolean isParticipant = (currentUser.getRole() == Role.PT && conversation.getPtUser().getId().equals(currentUser.getId()))
                || (currentUser.getRole() == Role.CLIENT && conversation.getClientUser().getId().equals(currentUser.getId()));
        if (!isParticipant) {
            throw new IllegalArgumentException("Conversation not found");
        }

        return conversation;
    }

    private String requireContent(String content) {
        String trimmed = trimToNull(content);
        if (trimmed == null) {
            throw new IllegalArgumentException("Message cannot be empty");
        }
        if (trimmed.length() > 2000) {
            throw new IllegalArgumentException("Message must be at most 2000 characters");
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

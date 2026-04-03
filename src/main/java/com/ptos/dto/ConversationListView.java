package com.ptos.dto;

import com.ptos.domain.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class ConversationListView {

    private Long conversationId;
    private String otherPersonName;
    private String lastMessagePreview;
    private LocalDateTime lastMessageAt;
    private int unreadCount;
    private Role otherPersonRole;
    private String relativeTime;
}

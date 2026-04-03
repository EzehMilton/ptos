package com.ptos.repository;

import com.ptos.domain.Conversation;
import com.ptos.domain.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationOrderByCreatedAtAsc(Conversation conversation);

    List<Message> findByConversationOrderByCreatedAtDesc(Conversation conversation, Pageable pageable);
}

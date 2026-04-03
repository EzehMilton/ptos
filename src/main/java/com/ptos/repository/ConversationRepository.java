package com.ptos.repository;

import com.ptos.domain.Conversation;
import com.ptos.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    List<Conversation> findByPtUserOrderByLastMessageAtDesc(User ptUser);

    Optional<Conversation> findByPtUserAndClientUser(User ptUser, User clientUser);

    Optional<Conversation> findByIdAndPtUser(Long id, User ptUser);

    Optional<Conversation> findByClientUser(User clientUser);

    @Query("select coalesce(sum(c.unreadCountPt), 0) from Conversation c where c.ptUser = :ptUser")
    int sumUnreadCountPtByPtUser(@Param("ptUser") User ptUser);

    @Query("select coalesce(sum(c.unreadCountClient), 0) from Conversation c where c.clientUser = :clientUser")
    int sumUnreadCountClientByClientUser(@Param("clientUser") User clientUser);
}

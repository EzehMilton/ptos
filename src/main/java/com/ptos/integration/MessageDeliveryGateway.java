package com.ptos.integration;

import com.ptos.domain.Conversation;
import com.ptos.domain.Message;
import com.ptos.domain.User;

public interface MessageDeliveryGateway {

    void onMessageSent(Conversation conversation, Message message);

    void onConversationRead(Conversation conversation, User reader);
}

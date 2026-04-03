package com.ptos.integration.local;

// Replace with WebSocketMessageDeliveryGateway for real-time push notifications

import com.ptos.domain.Conversation;
import com.ptos.domain.Message;
import com.ptos.domain.User;
import com.ptos.integration.MessageDeliveryGateway;
import org.springframework.stereotype.Component;

@Component
public class NoOpMessageDeliveryGateway implements MessageDeliveryGateway {

    @Override
    public void onMessageSent(Conversation conversation, Message message) {
    }

    @Override
    public void onConversationRead(Conversation conversation, User reader) {
    }
}

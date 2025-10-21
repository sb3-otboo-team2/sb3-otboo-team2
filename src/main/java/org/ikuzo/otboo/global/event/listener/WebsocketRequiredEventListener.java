package org.ikuzo.otboo.global.event.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.global.event.message.MessageCreatedEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

//@Component
@RequiredArgsConstructor
public class WebsocketRequiredEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendMessage(MessageCreatedEvent messageCreatedEvent) {
        UUID senderId = messageCreatedEvent.getDto().sender().userId();
        UUID receiverId = messageCreatedEvent.getDto().receiver().userId();

        String dmKey = buildDmKey(senderId, receiverId);

        messagingTemplate.convertAndSend("/sub/direct-messages_" + dmKey, messageCreatedEvent.getDto());
    }

    public String buildDmKey(UUID userId1, UUID userId2) {
        List<String> sorted = Stream.of(userId1.toString(), userId2.toString())
            .sorted()
            .toList();
        return sorted.get(0) + "_" + sorted.get(1);
    }
}

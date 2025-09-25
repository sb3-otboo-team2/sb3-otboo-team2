package org.ikuzo.otboo.global.event.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.global.event.message.MessageCreatedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
@Component
public class WebsocketRequiredTopicListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "otboo.MessageCreatedEvent", groupId = "websocket")
    public void sendMessage(String kafkaEvent) {
        try {
            MessageCreatedEvent messageCreatedEvent = objectMapper.readValue(kafkaEvent, MessageCreatedEvent.class);

            UUID senderId = messageCreatedEvent.getDto().sender().userId();
            UUID receiverId = messageCreatedEvent.getDto().receiver().userId();

            String dmKey = buildDmKey(senderId, receiverId);

            messagingTemplate.convertAndSend("/sub/direct-messages_" + dmKey, messageCreatedEvent.getDto());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public String buildDmKey(UUID userId1, UUID userId2) {
        List<String> sorted = Stream.of(userId1.toString(), userId2.toString())
            .sorted()
            .toList();
        return sorted.get(0) + "_" + sorted.get(1);
    }
}

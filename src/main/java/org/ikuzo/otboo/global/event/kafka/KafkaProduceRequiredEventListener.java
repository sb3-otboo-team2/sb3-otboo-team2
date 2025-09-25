package org.ikuzo.otboo.global.event.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.global.event.message.FollowCreatedEvent;
import org.ikuzo.otboo.global.event.message.MessageCreatedEvent;
import org.ikuzo.otboo.global.event.message.NotificationCreatedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;


@Slf4j
@RequiredArgsConstructor
@Component
public class KafkaProduceRequiredEventListener {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @TransactionalEventListener
    public void on(FollowCreatedEvent event) {
        sendToKafka(event);
    }

    @TransactionalEventListener
    public void on(MessageCreatedEvent event) {
        sendToKafka(event);
    }

    @TransactionalEventListener
    public void on(NotificationCreatedEvent event) {
        sendToKafka(event);
    }

    private <T> void sendToKafka(T event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("otboo.".concat(event.getClass().getSimpleName()), payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to send event to Kafka", e);
            throw new RuntimeException(e);
        }
    }
}

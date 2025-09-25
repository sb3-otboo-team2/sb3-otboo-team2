package org.ikuzo.otboo.global.event.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.notification.dto.NotificationDto;
import org.ikuzo.otboo.global.event.message.NotificationCreatedEvent;
import org.ikuzo.otboo.global.sse.service.SseService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class SseRequiredTopicListener {

    private final SseService sseService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "otboo.NotificationCreatedEvent", groupId = "sse")
    public void onNotificationCreatedEvent(String kafkaEvent) {
        try {
            NotificationCreatedEvent event = objectMapper.readValue(kafkaEvent, NotificationCreatedEvent.class);

            List<NotificationDto> notifications = event.getDto();

            notifications.forEach(notification -> {
                UUID receiverId = notification.receiverId();
                sseService.send(Set.of(receiverId), "notifications", notification);
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}

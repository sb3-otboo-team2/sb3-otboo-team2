package org.ikuzo.otboo.global.event.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.notification.entity.Level;
import org.ikuzo.otboo.domain.notification.service.NotificationService;
import org.ikuzo.otboo.global.event.message.FollowCreatedEvent;
import org.ikuzo.otboo.global.event.message.MessageCreatedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Set;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class NotificationRequiredTopicListener {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "otboo.FollowCreatedEvent")
    public void onFollowCreatedEvent(String kafkaEvent) {

        try {
            FollowCreatedEvent event = objectMapper.readValue(kafkaEvent, FollowCreatedEvent.class);

            UUID receiverId = event.getDto().followee().userId();
            String followerName = event.getDto().follower().name();

            String title = "\"" + followerName + "\"님이 나를 팔로우했어요.";

            String content = "";

            notificationService.create(Set.of(receiverId), title, content, Level.INFO);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "otboo.MessageCreatedEvent")
    public void onMessageCreatedEvent(String kafkaEvent) {
        try {
            MessageCreatedEvent event = objectMapper.readValue(kafkaEvent, MessageCreatedEvent.class);

            UUID receiverId = event.getDto().receiver().userId();
            String senderName = event.getDto().sender().name();

            String title = "\"" + senderName + "\"님이 메세지를 보냈어요.";

            String content = event.getDto().content();

            notificationService.create(Set.of(receiverId), title, content, Level.INFO);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}

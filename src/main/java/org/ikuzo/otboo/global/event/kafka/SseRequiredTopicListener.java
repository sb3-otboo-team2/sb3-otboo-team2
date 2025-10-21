package org.ikuzo.otboo.global.event.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
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
    
    @PostConstruct
    public void init() {
        log.info("====================================================");
        log.info("SseRequiredTopicListener 초기화 완료");
        log.info("Topic: otboo.NotificationCreatedEvent, GroupId: sse");
        log.info("====================================================");
    }


    @KafkaListener(
        topics = "otboo.NotificationCreatedEvent", 
        groupId = "sse",
        autoStartup = "true",
        concurrency = "1"
    )
    public void onNotificationCreatedEvent(String kafkaEvent) {
        try {
            log.info("Kafka 메시지 수신 (SSE): {}", kafkaEvent);
            NotificationCreatedEvent event = objectMapper.readValue(kafkaEvent, NotificationCreatedEvent.class);

            List<NotificationDto> notifications = event.getDto();
            log.info("SSE 알림 전송 시도 - 알림 개수: {}", notifications.size());

            notifications.forEach(notification -> {
                try {
                    UUID receiverId = notification.receiverId();
                    log.debug("SSE 알림 전송 시도 - receiverId: {}, title: {}", 
                        receiverId, notification.title());
                    
                    sseService.send(Set.of(receiverId), "notifications", notification);
                    
                    log.debug("SSE 알림 전송 성공 - receiverId: {}", receiverId);
                } catch (Exception e) {
                    log.error("SSE 알림 전송 실패 - receiverId: {}", notification.receiverId(), e);
                    // 다른 알림 전송을 계속하기 위해 예외를 던지지 않음
                }
            });
            
            log.info("SSE 알림 전송 완료 - 처리된 알림 개수: {}", notifications.size());
        } catch (JsonProcessingException e) {
            log.error("Kafka 메시지 역직렬화 실패 (SSE): {}", kafkaEvent, e);
            // 리스너를 중단하지 않기 위해 예외를 던지지 않음
        } catch (Exception e) {
            log.error("SSE 알림 처리 실패: {}", kafkaEvent, e);
            // 리스너를 중단하지 않기 위해 예외를 던지지 않음
        }
    }
}

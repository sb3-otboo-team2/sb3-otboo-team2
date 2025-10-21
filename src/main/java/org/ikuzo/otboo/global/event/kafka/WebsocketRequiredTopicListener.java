package org.ikuzo.otboo.global.event.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
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
    
    @PostConstruct
    public void init() {
        log.info("====================================================");
        log.info("WebsocketRequiredTopicListener 초기화 완료");
        log.info("Topic: otboo.MessageCreatedEvent, GroupId: websocket-consumer");
        log.info("====================================================");
    }

    @KafkaListener(
        topics = "otboo.MessageCreatedEvent", 
        groupId = "websocket-consumer",
        autoStartup = "true",
        concurrency = "1",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void sendMessage(String kafkaEvent) {
        try {
            log.info("Kafka 메시지 수신 (웹소켓): {}", kafkaEvent);
            MessageCreatedEvent messageCreatedEvent = objectMapper.readValue(kafkaEvent, MessageCreatedEvent.class);

            UUID senderId = messageCreatedEvent.getDto().sender().userId();
            UUID receiverId = messageCreatedEvent.getDto().receiver().userId();

            String dmKey = buildDmKey(senderId, receiverId);
            String destination = "/sub/direct-messages_" + dmKey;

            log.info("웹소켓 메시지 전송 시도 - destination: {}, senderId: {}, receiverId: {}", 
                destination, senderId, receiverId);
            
            messagingTemplate.convertAndSend(destination, messageCreatedEvent.getDto());
            
            log.info("웹소켓 메시지 전송 성공 - destination: {}", destination);
        } catch (JsonProcessingException e) {
            log.error("Kafka 메시지 역직렬화 실패 (웹소켓): {}", kafkaEvent, e);
            // 리스너를 중단하지 않기 위해 예외를 던지지 않음
        } catch (Exception e) {
            log.error("웹소켓 메시지 전송 실패: {}", kafkaEvent, e);
            // 리스너를 중단하지 않기 위해 예외를 던지지 않음
        }
    }

    public String buildDmKey(UUID userId1, UUID userId2) {
        List<String> sorted = Stream.of(userId1.toString(), userId2.toString())
            .sorted()
            .toList();
        return sorted.get(0) + "_" + sorted.get(1);
    }
}

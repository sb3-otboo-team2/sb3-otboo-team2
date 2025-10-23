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
        log.info("Topic: otboo.MessageCreatedEvent, GroupId: websocket-consumer-${random.uuid}");
        log.info("messagingTemplate: {}", messagingTemplate != null ? "정상" : "null");
        log.info("objectMapper: {}", objectMapper != null ? "정상" : "null");
        log.info("====================================================");
    }

    @KafkaListener(
        topics = "otboo.MessageCreatedEvent", 
        groupId = "websocket-consumer-${random.uuid}",
        autoStartup = "true",
        concurrency = "1",
        containerFactory = "kafkaListenerContainerFactory",
        properties = {
            "spring.kafka.consumer.auto-offset-reset=earliest",
            "spring.kafka.consumer.enable-auto-commit=false"
        }
    )
    public void sendMessage(String kafkaEvent) {
        log.info("====================================================");
        log.info("웹소켓 Kafka 리스너 호출됨");
        log.info("수신된 Kafka 메시지: {}", kafkaEvent);
        log.info("====================================================");
        
        try {
            MessageCreatedEvent messageCreatedEvent = objectMapper.readValue(kafkaEvent, MessageCreatedEvent.class);
            log.info("Kafka 메시지 역직렬화 성공: {}", messageCreatedEvent);

            UUID senderId = messageCreatedEvent.getDto().sender().userId();
            UUID receiverId = messageCreatedEvent.getDto().receiver().userId();

            String dmKey = buildDmKey(senderId, receiverId);
            String destination = "/sub/direct-messages_" + dmKey;

            log.info("웹소켓 메시지 전송 시도 - destination: {}, senderId: {}, receiverId: {}, dmKey: {}", 
                destination, senderId, receiverId, dmKey);
            
            // 메시지 전송 전 상태 확인
            log.info("messagingTemplate 상태 확인: {}", messagingTemplate != null ? "정상" : "null");
            
            messagingTemplate.convertAndSend(destination, messageCreatedEvent.getDto());
            
            log.info("웹소켓 메시지 전송 성공 - destination: {}", destination);
            log.info("전송된 메시지 내용: {}", messageCreatedEvent.getDto());
            
            // 수동 커밋 (메시지 처리 완료 후)
            // 이렇게 하면 메시지 처리 실패 시 재시도 가능
        } catch (JsonProcessingException e) {
            log.error("Kafka 메시지 역직렬화 실패 (웹소켓): {}", kafkaEvent, e);
            e.printStackTrace();
        } catch (Exception e) {
            log.error("웹소켓 메시지 전송 실패: {}", kafkaEvent, e);
            e.printStackTrace();
        }
    }

    public String buildDmKey(UUID userId1, UUID userId2) {
        List<String> sorted = Stream.of(userId1.toString(), userId2.toString())
            .sorted()
            .toList();
        return sorted.get(0) + "_" + sorted.get(1);
    }
}

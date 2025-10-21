package org.ikuzo.otboo.global.event.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.global.event.message.ClothesAttributeDefCreatedEvent;
import org.ikuzo.otboo.global.event.message.FeedCreatedEvent;
import org.ikuzo.otboo.global.event.message.FeedLikeCreatedEvent;
import org.ikuzo.otboo.global.event.message.FollowCreatedEvent;
import org.ikuzo.otboo.global.event.message.MessageCreatedEvent;
import org.ikuzo.otboo.global.event.message.NotificationCreatedEvent;
import org.ikuzo.otboo.global.exception.kafka.KafkaInfrastructureException;
import org.ikuzo.otboo.global.exception.kafka.KafkaPublishingException;
import org.ikuzo.otboo.global.exception.kafka.KafkaSerializationException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;


@Slf4j
@RequiredArgsConstructor
@Component
public class KafkaProduceRequiredEventListener {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(FollowCreatedEvent event) {
        sendToKafka(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(MessageCreatedEvent event) {
        sendToKafka(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(NotificationCreatedEvent event) {
        sendToKafka(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(FeedLikeCreatedEvent event) {
        sendToKafka(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(FeedCreatedEvent event) {
        sendToKafka(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(ClothesAttributeDefCreatedEvent event) {
        sendToKafka(event);
    }

    private <T> void sendToKafka(T event) {
        final String topic = "otboo.".concat(event.getClass().getSimpleName());
        try {
            log.info("Kafka 메시지 발행 시작 - topic: {}, eventType: {}", topic, event.getClass().getSimpleName());
            String payload = objectMapper.writeValueAsString(event);
            log.debug("Kafka 메시지 페이로드: {}", payload);
            
            kafkaTemplate.send(topic, payload).get();
            
            log.info("Kafka 메시지 발행 완료 - topic: {}", topic);

        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("Kafka 메시지 직렬화 실패 - topic: {}, event: {}", topic, event, e);
            throw new KafkaSerializationException(e);

        } catch (java.util.concurrent.ExecutionException e) {
            log.error("Kafka 메시지 발행 실패 (ExecutionException) - topic: {}", topic, e);
            throw new KafkaPublishingException(e);

        } catch (org.springframework.kafka.KafkaException e) {
            log.error("Kafka 인프라 오류 - topic: {}", topic, e);
            throw new KafkaInfrastructureException(e);

        } catch (InterruptedException e) {
            log.error("Kafka 메시지 발행 중단 (InterruptedException) - topic: {}", topic, e);
            Thread.currentThread().interrupt();
            throw new KafkaPublishingException(e);
        }
    }
}

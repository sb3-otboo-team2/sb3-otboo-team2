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
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, payload).get();

        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new KafkaSerializationException(e);

        } catch (java.util.concurrent.ExecutionException e) {
            throw new KafkaPublishingException(e);

        } catch (org.springframework.kafka.KafkaException e) {
            throw new KafkaInfrastructureException(e);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaPublishingException(e);
        }
    }
}

package org.ikuzo.otboo.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Slf4j
@Configuration
public class KafkaConfig {

    /**
     * Kafka Consumer 에러 핸들러
     * 에러 발생 시 Consumer가 중단되지 않도록 처리
     */
    @Bean
    public CommonErrorHandler kafkaErrorHandler() {
        // 재시도: 3초 간격으로 최대 3번 재시도
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
            (consumerRecord, exception) -> {
                log.error("Kafka Consumer 에러 발생 - topic: {}, partition: {}, offset: {}, key: {}, value: {}",
                    consumerRecord.topic(),
                    consumerRecord.partition(),
                    consumerRecord.offset(),
                    consumerRecord.key(),
                    consumerRecord.value(),
                    exception);
                log.error("최대 재시도 횟수 초과. 해당 메시지는 건너뜁니다.");
            },
            new FixedBackOff(3000L, 3L)
        );

        // 모든 예외에 대해 재시도 시도
        errorHandler.addNotRetryableExceptions();
        
        return errorHandler;
    }
}


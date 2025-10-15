package org.ikuzo.otboo.global.exception.kafka;

import org.ikuzo.otboo.global.exception.ErrorCode;

public class KafkaInfrastructureException extends KafkaException {

    public KafkaInfrastructureException(Throwable cause) {
        super(ErrorCode.KAFKA_INFRA_ERROR, cause);
    }
}

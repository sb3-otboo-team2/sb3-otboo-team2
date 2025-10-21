package org.ikuzo.otboo.global.exception.kafka;

import org.ikuzo.otboo.global.exception.ErrorCode;

public class KafkaPublishingException extends KafkaException {

    public KafkaPublishingException(Throwable cause) {
        super(ErrorCode.KAFKA_PUBLISH_ERROR, cause);
    }
}
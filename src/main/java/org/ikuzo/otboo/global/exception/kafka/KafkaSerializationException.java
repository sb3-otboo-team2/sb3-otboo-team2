package org.ikuzo.otboo.global.exception.kafka;

import org.ikuzo.otboo.global.exception.ErrorCode;

public class KafkaSerializationException extends KafkaException {

    public KafkaSerializationException(Throwable cause) {
        super(ErrorCode.KAFKA_SERIALIZATION_ERROR, cause);
    }

}

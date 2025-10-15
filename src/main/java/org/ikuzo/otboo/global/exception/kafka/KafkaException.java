package org.ikuzo.otboo.global.exception.kafka;

import org.ikuzo.otboo.global.exception.ErrorCode;
import org.ikuzo.otboo.global.exception.OtbooException;

public class KafkaException extends OtbooException {

    public KafkaException(ErrorCode errorCode) {
        super(errorCode);
    }

    protected KafkaException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

}

package org.ikuzo.otboo.global.exception;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

@Getter
public class OtbooException extends RuntimeException {
    private final Instant timestamp;
    private final ErrorCode errorCode;
    private final Map<String, Object> details;

    public OtbooException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.timestamp = Instant.now();
        this.errorCode = errorCode;
        this.details = new HashMap<>();
    }

    public OtbooException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.timestamp = Instant.now();
        this.errorCode = errorCode;
        this.details = new HashMap<>();
    }

    public void addDetail(String key, Object value) {
        this.details.put(key, value);
    }
} 
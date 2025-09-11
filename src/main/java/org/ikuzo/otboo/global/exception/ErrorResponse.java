package org.ikuzo.otboo.global.exception;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ErrorResponse {
    private final String exceptionName;
    private final String message;
    private final Map<String, Object> details;


    public ErrorResponse(OtbooException exception) {
        this(exception.getClass().getSimpleName(), exception.getMessage(), exception.getDetails());
    }

    public ErrorResponse(Exception exception) {
        this(exception.getClass().getSimpleName(), exception.getMessage(), new HashMap<>());
    }
} 
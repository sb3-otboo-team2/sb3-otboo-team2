package org.ikuzo.otboo.global.exception;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        if (e instanceof AccessDeniedException) {
            log.debug("AccessDeniedException은 Spring Security에서 처리: {}", e.getMessage());
            throw (AccessDeniedException) e;  // 다시 던져서 Security가 처리하도록
        }

        log.error("예상치 못한 오류 발생: {}", e.getMessage(), e);
        ErrorResponse errorResponse = new ErrorResponse(e);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(errorResponse);
    }

    @ExceptionHandler(OtbooException.class)
    public ResponseEntity<ErrorResponse> handleOtbooException(OtbooException exception) {
        log.error("커스텀 예외 발생: code={}, message={}", exception.getErrorCode(),
            exception.getMessage(),
            exception);
        HttpStatus status = determineHttpStatus(exception);
        ErrorResponse response = new ErrorResponse(exception);
        return ResponseEntity
            .status(status)
            .body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
        MethodArgumentNotValidException ex) {
        log.error("요청 유효성 검사 실패: {}", ex.getMessage());

        Map<String, Object> validationErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            validationErrors.put(fieldName, errorMessage);
        });

        ErrorResponse response = new ErrorResponse(
            ex
        );

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(response);
    }


    private HttpStatus determineHttpStatus(OtbooException exception) {
        return switch (exception.getErrorCode()) {
            
            case DUPLICATE_USER -> HttpStatus.CONFLICT;

            case FOLLOW_SELF_NOT_ALLOWED, FOLLOW_ALREADY_EXISTS
            , DUPLICATED_ATTRIBUTE_NAME, REQUIRED_FIELD_MISSING
            , INVALID_ATTRIBUTE_OPTION -> HttpStatus.BAD_REQUEST;

            case ATTRIBUTE_NOT_FOUND, FOLLOW_NOT_FOUND, USER_NOT_FOUND, FEED_CLOTHES_NOT_FOUND, WEATHER_NOT_FOUND, NOTIFICATION_NOT_FOUND ->
                HttpStatus.NOT_FOUND;

            case INVALID_TOKEN, INVALID_USER_DETAILS -> HttpStatus.UNAUTHORIZED;

            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}

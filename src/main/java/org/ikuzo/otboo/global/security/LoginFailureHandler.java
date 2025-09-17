package org.ikuzo.otboo.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.global.exception.ErrorResponse;
import org.ikuzo.otboo.global.exception.OtbooException;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginFailureHandler implements AuthenticationFailureHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
        AuthenticationException exception) throws IOException, ServletException {
        log.error("Authentication failed: {}", exception.getMessage(), exception);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponse errorResponse = new ErrorResponse(exception);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    private ErrorResponse createErrorResponse(AuthenticationException exception) {
        // InternalAuthenticationServiceException인 경우 원본 예외 확인
        if (exception instanceof InternalAuthenticationServiceException) {
            Throwable cause = exception.getCause();

            // 원본 예외가 OtbooException인 경우 그대로 사용
            if (cause instanceof OtbooException otbooException) {
                log.info("원본 커스텀 예외 발견: {}", otbooException.getClass().getSimpleName());
                return new ErrorResponse(otbooException);
            }

            // 원본 예외가 일반 Exception인 경우
            if (cause instanceof Exception originalException) {
                log.info("원본 일반 예외 발견: {}", originalException.getClass().getSimpleName());
                return new ErrorResponse(originalException);
            }
        }

        // 그 외의 경우 Spring Security 예외 그대로 사용
        log.info("Spring Security 예외 사용: {}", exception.getClass().getSimpleName());
        return new ErrorResponse(exception);
    }
}

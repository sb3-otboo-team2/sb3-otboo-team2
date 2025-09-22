package org.ikuzo.otboo.global.oauth2.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.auth.dto.JwtDto;
import org.ikuzo.otboo.domain.user.entity.User;
import org.ikuzo.otboo.domain.user.mapper.UserMapper;
import org.ikuzo.otboo.domain.user.repository.UserRepository;
import org.ikuzo.otboo.global.security.JwtTokenProvider;
import org.ikuzo.otboo.global.security.OtbooUserDetails;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class Oauth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final ObjectMapper objectMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        
        log.info("[OAuth2LoginSuccessHandler] OAuth2 로그인 성공 처리 시작");
        
        try {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            log.info("[OAuth2LoginSuccessHandler] OAuth2User: {}", oauth2User.getName());

            if (authentication.getPrincipal() instanceof OtbooUserDetails userDetails) {
                log.info("[OAuth2LoginSuccessHandler] 사용자 정보: {}", userDetails.getUserDto().email());
                
                String accessToken = jwtTokenProvider.generateAccessToken(userDetails);
                String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);
                
                log.info("[OAuth2LoginSuccessHandler] JWT 토큰 생성 완료");

                // Refresh Token을 쿠키에 저장
                Cookie refreshCookie = jwtTokenProvider.generateRefreshTokenCookie(refreshToken);
                response.addCookie(refreshCookie);
                log.info("[OAuth2LoginSuccessHandler] Refresh Token 쿠키 설정 완료");

                // 메인 페이지로 리다이렉션
                log.info("[OAuth2LoginSuccessHandler] 메인 페이지로 리다이렉션 시작");
                response.sendRedirect("/");
                
            } else {
                log.error("[OAuth2LoginSuccessHandler] OtbooUserDetails 타입이 아닙니다: {}", 
                    authentication.getPrincipal().getClass().getName());
                response.sendRedirect("/?error=invalid_user_type");
            }
            
        } catch (Exception e) {
            log.error("[OAuth2LoginSuccessHandler] OAuth2 로그인 성공 처리 중 오류 발생", e);
            response.sendRedirect("/?error=oauth2_login_failed");
        }
    }
}

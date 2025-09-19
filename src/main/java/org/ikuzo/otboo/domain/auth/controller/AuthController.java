package org.ikuzo.otboo.domain.auth.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.auth.dto.JwtInformation;
import org.ikuzo.otboo.domain.auth.service.AuthService;
import org.ikuzo.otboo.global.security.JwtDto;
import org.ikuzo.otboo.global.security.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/refresh")
    public ResponseEntity<JwtDto> refresh(@CookieValue("REFRESH_TOKEN") String refreshToken,
        HttpServletResponse response) {
        log.info("토큰 리프레시 요청");
        JwtInformation refreshResult = authService.refreshToken(refreshToken);
        Cookie refreshCookie = jwtTokenProvider.genereateRefreshTokenCookie(
            refreshResult.refreshToken());
        response.addCookie(refreshCookie);

        JwtDto body = new JwtDto(
            refreshResult.userDto(),
            refreshResult.accessToken()
        );
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(body);
    }
}
package org.ikuzo.otboo.domain.auth.service;

import com.nimbusds.jose.JOSEException;
import java.security.SecureRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.auth.dto.JwtInformation;
import org.ikuzo.otboo.domain.auth.dto.ResetPasswordRequest;
import org.ikuzo.otboo.domain.user.entity.User;
import org.ikuzo.otboo.domain.user.exception.UserNotFoundException;
import org.ikuzo.otboo.domain.user.repository.UserRepository;
import org.ikuzo.otboo.global.exception.ErrorCode;
import org.ikuzo.otboo.global.exception.OtbooException;
import org.ikuzo.otboo.global.security.JwtTokenProvider;
import org.ikuzo.otboo.global.security.OtbooUserDetails;
import org.ikuzo.otboo.global.util.EmailService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class AuthServiceImpl implements AuthService {

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private static final String TEMP_PASSWORD_CHARS =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
    private static final int TEMP_PASSWORD_LENGTH = 12;

    @Override
    public JwtInformation refreshToken(String refreshToken) {
        // Validate refresh token
        if (!tokenProvider.validateRefreshToken(refreshToken)) {
            throw new OtbooException(ErrorCode.INVALID_TOKEN);
        }

        String email = tokenProvider.getEmailFromToken(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        if (!(userDetails instanceof OtbooUserDetails otbooUserDetails)) {
            throw new OtbooException(ErrorCode.INVALID_USER_DETAILS);
        }

        try {
            String newAccessToken = tokenProvider.generateAccessToken(otbooUserDetails);
            String newRefreshToken = tokenProvider.generateRefreshToken(otbooUserDetails);
            log.info("Access token refreshed for user: {}", email);
            return new JwtInformation(
                otbooUserDetails.getUserDto(),
                newAccessToken,
                newRefreshToken
            );
        } catch (JOSEException e) {
            log.error("Failed to generate new tokens for user: {}", email, e);
            throw new OtbooException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    @Override
    public void resetPassword(ResetPasswordRequest request) {
        log.info("비밀번호 초기화 시작: email={}", request.email());

        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> {
                log.warn("존재하지 않는 이메일로 비밀번호 초기화 시도: {}", request.email());
                return UserNotFoundException.withEmail(request.email());
            });

        String tempPassword = generateTemporaryPassword();
        log.debug("임시 비밀번호 생성 완료: userId={}", user.getId());

        String encodedPassword = passwordEncoder.encode(tempPassword);
        user.changePassword(encodedPassword);

        emailService.sendTemporaryPassword(user.getEmail(), user.getName(), tempPassword);

        log.info("비밀번호 초기화 및 이메일 전송 완료: email={}", request.email());
    }

    /**
     * 보안성 높은 임시 비밀번호 생성
     * - 대문자, 소문자, 숫자, 특수문자를 포함한 12자리
     * - SecureRandom 사용으로 예측 불가능한 난수 생성
     */
    private String generateTemporaryPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(TEMP_PASSWORD_LENGTH);

        for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
            int randomIndex = random.nextInt(TEMP_PASSWORD_CHARS.length());
            password.append(TEMP_PASSWORD_CHARS.charAt(randomIndex));
        }

        return password.toString();
    }

}

package org.ikuzo.otboo.domain.auth.service;

import com.nimbusds.jose.JOSEException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.auth.dto.JwtInformation;
import org.ikuzo.otboo.global.exception.ErrorCode;
import org.ikuzo.otboo.global.exception.OtbooException;
import org.ikuzo.otboo.global.security.JwtTokenProvider;
import org.ikuzo.otboo.global.security.OtbooUserDetails;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class AuthServiceImpl implements AuthService {

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;

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
}

package org.ikuzo.otboo.domain.auth.service;

import org.ikuzo.otboo.domain.auth.dto.JwtInformation;
import org.ikuzo.otboo.domain.auth.dto.ResetPasswordRequest;

public interface AuthService {
    JwtInformation refreshToken(String refreshToken);
    void resetPassword(ResetPasswordRequest request);
}

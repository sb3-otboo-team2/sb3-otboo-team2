package org.ikuzo.otboo.domain.auth.service;

import org.ikuzo.otboo.domain.auth.dto.JwtInformation;

public interface AuthService {
    JwtInformation refreshToken(String refreshToken);
}

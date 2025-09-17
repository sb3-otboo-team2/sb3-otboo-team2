package org.ikuzo.otboo.global.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.user.dto.UserDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtTokenProvider {

    private final int accessTokenExpirationMs;

    private final JWSSigner accessTokenSigner;
    private final JWSVerifier accessTokenVerifier;

    public JwtTokenProvider(
        @Value("${otboo.jwt.access-token.secret}") String accessTokenSecret,
        @Value("${otboo.jwt.access-token.expiration-ms}") int accessTokenExpirationMs)
        throws JOSEException {

        this.accessTokenExpirationMs = accessTokenExpirationMs;

        byte[] accessSecretBytes = accessTokenSecret.getBytes(StandardCharsets.UTF_8);
        this.accessTokenSigner = new MACSigner(accessSecretBytes);
        this.accessTokenVerifier = new MACVerifier(accessSecretBytes);
    }

    public String generateAccessToken(OtbooUserDetails userDetails) throws JOSEException {
        return generateToken(userDetails, accessTokenExpirationMs, accessTokenSigner, "access");
    }

    private String generateToken(OtbooUserDetails userDetails, int expirationMs, JWSSigner signer,
        String tokenType) throws JOSEException {
        String tokenId = UUID.randomUUID().toString();
        UserDto user = userDetails.getUserDto();

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
            .subject(user.email())
            .jwtID(tokenId)
            .claim("userId", user.id().toString())
            .claim("type", tokenType)
            .claim("roles", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()))
            .issueTime(now)
            .expirationTime(expiryDate)
            .build();

        SignedJWT signedJWT = new SignedJWT(
            new JWSHeader(JWSAlgorithm.HS256),
            claimsSet
        );

        signedJWT.sign(signer);
        String token = signedJWT.serialize();

        log.debug("Generated {} token for user email: {}", tokenType, user.email());
        return token;
    }

    public boolean validateAccessToken(String token) {
        return validateToken(token, accessTokenVerifier, "access");
    }

    private boolean validateToken(String token, JWSVerifier verifier, String expectedType) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);

            // Verify signature
            if (!signedJWT.verify(verifier)) {
                log.debug("JWT signature verification failed for {} token", expectedType);
                return false;
            }

            // Check token type
            String tokenType = (String) signedJWT.getJWTClaimsSet().getClaim("type");
            if (!expectedType.equals(tokenType)) {
                log.debug("JWT token type mismatch: expected {}, got {}", expectedType, tokenType);
                return false;
            }

            // Check expiration
            Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
            if (expirationTime == null || expirationTime.before(new Date())) {
                log.debug("JWT {} token expired", expectedType);
                return false;
            }

            return true;
        } catch (Exception e) {
            log.debug("JWT {} token validation failed: {}", expectedType, e.getMessage());
            return false;
        }
    }

    public String getEmailFromToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return signedJWT.getJWTClaimsSet().getSubject();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }
}
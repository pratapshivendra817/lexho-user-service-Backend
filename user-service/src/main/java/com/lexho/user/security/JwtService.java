package com.lexho.user.security;

import com.lexho.common.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Component
public final class JwtService {

    private final SecretKey signingKey;
    private final String issuer;
    private final String audience;

    public JwtService(@Value("${security.jwt.secret}") String secret,
                      @Value("${security.jwt.issuer:lexho-user-service}") String issuer,
                      @Value("${security.jwt.audience:lexho-clients}") String audience) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.audience = audience;
    }

    public AuthenticatedUser parse(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .requireIssuer(issuer)
                .build()
                .parseClaimsJws(token)
                .getBody();

        String tokenAudience = claims.get("aud", String.class);
        if (!audience.equals(tokenAudience)) {
            throw new IllegalArgumentException("Invalid token audience");
        }

        Number userId = claims.get("userId", Number.class);
        String roleClaim = claims.get("role", String.class);
        if (userId == null || roleClaim == null || claims.getSubject() == null) {
            throw new IllegalArgumentException("Invalid token");
        }

        return new AuthenticatedUser(
                userId.longValue(),
                claims.getSubject(),
                Role.valueOf(roleClaim.toUpperCase(Locale.ROOT))
        );
    }
}

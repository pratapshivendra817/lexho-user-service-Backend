package com.lexho.user_service.util;

import com.lexho.common.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    private static final String USER_ID_CLAIM = "userId";
    private static final String ROLE_CLAIM = "role";
    private static final String EMAIL_CLAIM = "email";

    @Value("${security.jwt.secret}")
    private String secret;

    @Value("${security.jwt.expiration-ms:86400000}")
    private long tokenValidityMs;

    @Value("${security.jwt.issuer:lexho-user-service}")
    private String issuer;

    @Value("${security.jwt.audience:lexho-clients}")
    private String audience;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long userId, String mobile, Role role) {
        return buildToken(userId, mobile, null, role);
    }

    public String generateAdminToken(Long userId, String email, Role role) {
        return buildToken(userId, email, email, role);
    }

    private String buildToken(Long userId, String subject, String email, Role role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(USER_ID_CLAIM, userId);
        claims.put(ROLE_CLAIM, role.name());
        if (email != null && !email.isBlank()) {
            claims.put(EMAIL_CLAIM, email);
        }

        Date now = new Date();
        Date expiry = new Date(now.getTime() + tokenValidityMs);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuer(issuer)
                .claim("aud", audience)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims validateToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        String tokenIssuer = claims.getIssuer();
        String tokenAudience = claims.get("aud", String.class);
        if (!issuer.equals(tokenIssuer) || !audience.equals(tokenAudience)) {
            throw new IllegalArgumentException("Invalid token issuer or audience");
        }
        return claims;
    }
}

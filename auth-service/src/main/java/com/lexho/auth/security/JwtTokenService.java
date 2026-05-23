package com.lexho.auth.security;

import com.lexho.common.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtTokenService {

    private final String jwtSecret;
    private final long expirationMs;
    private final String issuer;

    public JwtTokenService(@Value("${security.jwt.secret:change-this-to-a-long-random-secret-key-min-32-bytes}") String jwtSecret,
                           @Value("${security.jwt.expiration-ms:900000}") long expirationMs,
                           @Value("${security.jwt.issuer:lexho-auth-service}") String issuer) {
        this.jwtSecret = jwtSecret;
        this.expirationMs = expirationMs;
        this.issuer = issuer;
    }

    public String generateAccessToken(Long userId, Role role) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expirationMs);
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role.name());

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(String.valueOf(userId))
                .setIssuer(issuer)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiry))
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims validateAndParse(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(signingKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        if (!issuer.equals(claims.getIssuer())) {
            throw new IllegalArgumentException("Invalid issuer");
        }
        return claims;
    }

    private Key signingKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}

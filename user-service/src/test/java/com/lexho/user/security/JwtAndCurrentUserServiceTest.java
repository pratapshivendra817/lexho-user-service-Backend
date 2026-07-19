package com.lexho.user.security;

import com.lexho.common.enums.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtAndCurrentUserServiceTest {

    private static final String SECRET = "12345678901234567890123456789012";

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void parseReturnsAuthenticatedUserForValidToken() {
        JwtService jwtService = new JwtService(SECRET, "issuer", "clients");

        AuthenticatedUser user = jwtService.parse(token("issuer", "clients", 7L, "customer", "9999999999"));

        assertThat(user.userId()).isEqualTo(7L);
        assertThat(user.subject()).isEqualTo("9999999999");
        assertThat(user.role()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    void parseRejectsInvalidAudience() {
        JwtService jwtService = new JwtService(SECRET, "issuer", "clients");

        assertThatThrownBy(() -> jwtService.parse(token("issuer", "other", 7L, "customer", "9999999999")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid token audience");
    }

    @Test
    void parseRejectsMissingClaims() {
        JwtService jwtService = new JwtService(SECRET, "issuer", "clients");
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .setIssuer("issuer")
                .setSubject("9999999999")
                .claim("aud", "clients")
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        assertThatThrownBy(() -> jwtService.parse(token))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid token");
    }

    @Test
    void currentUserServiceReturnsAuthenticatedUserAndRejectsInvalidContexts() {
        CurrentUserService service = new CurrentUserService();
        AuthenticatedUser user = new AuthenticatedUser(10L, "9999999999", Role.CUSTOMER);
        UsernamePasswordAuthenticationToken validAuthentication =
                new UsernamePasswordAuthenticationToken("subject", "creds", java.util.List.of());
        validAuthentication.setDetails(user);
        SecurityContextHolder.getContext().setAuthentication(validAuthentication);

        assertThat(service.requireCurrentUser()).isEqualTo(user);

        SecurityContextHolder.clearContext();
        assertThatThrownBy(service::requireCurrentUser)
                .isInstanceOf(com.lexho.user.exception.ForbiddenException.class)
                .hasMessage("Authentication context is unavailable");

        UsernamePasswordAuthenticationToken invalidAuthentication =
                new UsernamePasswordAuthenticationToken("subject", "creds", java.util.List.of());
        invalidAuthentication.setDetails("not-a-user");
        SecurityContextHolder.getContext().setAuthentication(invalidAuthentication);

        assertThatThrownBy(service::requireCurrentUser)
                .isInstanceOf(com.lexho.user.exception.ForbiddenException.class)
                .hasMessage("Authentication context is unavailable");
    }

    private static String token(String issuer, String audience, Long userId, String role, String subject) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .setIssuer(issuer)
                .setSubject(subject)
                .claim("aud", audience)
                .claim("userId", userId)
                .claim("role", role)
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}

package com.lexho.auth.security;

import com.lexho.common.enums.Role;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtTokenServiceTest {

    @Test
    void shouldGenerateAndParseToken() {
        JwtTokenService service = new JwtTokenService(
                "this-is-a-very-long-test-secret-with-min-32-bytes",
                60_000,
                "test-issuer"
        );
        String token = service.generateAccessToken(42L, Role.ADMIN);
        Claims claims = service.validateAndParse(token);

        assertEquals("42", claims.getSubject());
        assertEquals("test-issuer", claims.getIssuer());
        assertEquals("ADMIN", claims.get("role", String.class));
    }

    @Test
    void shouldRejectTokenWhenIssuerMismatch() {
        JwtTokenService issuerA = new JwtTokenService(
                "this-is-a-very-long-test-secret-with-min-32-bytes",
                60_000,
                "issuer-a"
        );
        JwtTokenService issuerB = new JwtTokenService(
                "this-is-a-very-long-test-secret-with-min-32-bytes",
                60_000,
                "issuer-b"
        );

        String token = issuerA.generateAccessToken(1L, Role.CUSTOMER);
        assertThrows(IllegalArgumentException.class, () -> issuerB.validateAndParse(token));
    }
}

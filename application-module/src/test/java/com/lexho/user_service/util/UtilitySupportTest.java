package com.lexho.user_service.util;

import com.lexho.common.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UtilitySupportTest {

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void jwtUtilGeneratesAndValidatesCustomerAndAdminTokens() {
        JwtUtil jwtUtil = jwtUtil("01234567890123456789012345678901", 60_000L, "issuer-a", "aud-a");

        String customerToken = jwtUtil.generateToken(7L, "9876543210", Role.CUSTOMER);
        Claims customerClaims = jwtUtil.validateToken(customerToken);

        assertThat(customerClaims.getSubject()).isEqualTo("9876543210");
        assertThat(customerClaims.get("userId", Integer.class)).isEqualTo(7);
        assertThat(customerClaims.get("role", String.class)).isEqualTo("CUSTOMER");

        String adminToken = jwtUtil.generateAdminToken(9L, "admin@lexho.com", Role.ADMIN);
        Claims adminClaims = jwtUtil.validateToken(adminToken);

        assertThat(adminClaims.getSubject()).isEqualTo("admin@lexho.com");
        assertThat(adminClaims.get("email", String.class)).isEqualTo("admin@lexho.com");
        assertThat(adminClaims.get("role", String.class)).isEqualTo("ADMIN");
    }

    @Test
    void jwtUtilRejectsExpiredTokensAndIssuerAudienceMismatch() {
        JwtUtil expiredUtil = jwtUtil("01234567890123456789012345678901", -1L, "issuer-a", "aud-a");
        String expiredToken = expiredUtil.generateToken(7L, "9876543210", Role.CUSTOMER);

        assertThatThrownBy(() -> expiredUtil.validateToken(expiredToken))
                .isInstanceOf(ExpiredJwtException.class);

        JwtUtil issuingUtil = jwtUtil("01234567890123456789012345678901", 60_000L, "issuer-a", "aud-a");
        JwtUtil validatingUtil = jwtUtil("01234567890123456789012345678901", 60_000L, "issuer-b", "aud-b");

        String token = issuingUtil.generateToken(10L, "9999999999", Role.PARTNER);

        assertThatThrownBy(() -> validatingUtil.validateToken(token))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid token issuer or audience");
    }

    @Test
    void mediaUrlResolverHandlesAbsoluteRelativeBlankAndContextlessValues() {
        MediaUrlResolver resolver = new MediaUrlResolver();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("https");
        request.setServerName("lexho.test");
        request.setServerPort(443);
        request.setContextPath("/app");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertThat(resolver.toPublicUrl(" https://cdn.lexho.com/banner.png "))
                .isEqualTo("https://cdn.lexho.com/banner.png");
        assertThat(resolver.toPublicUrl("media/banner.png"))
                .isEqualTo("https://lexho.test/app/media/banner.png");
        assertThat(resolver.toPublicUrl(" ")).isNull();

        RequestContextHolder.resetRequestAttributes();

        assertThat(resolver.toPublicUrl("media/banner.png")).isEqualTo("/media/banner.png");
        assertThat(resolver.toStorablePath("/media/banner.png")).isEqualTo("/media/banner.png");
        assertThat(resolver.toStorablePath("https://cdn.lexho.com/media/banner.png?x=1"))
                .isEqualTo("/media/banner.png");
        assertThat(resolver.toStorablePath("https://")).isEqualTo("https://");
        assertThat(resolver.toStorablePath(null)).isNull();
    }

    private static JwtUtil jwtUtil(String secret, long validityMs, String issuer, String audience) {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", secret);
        ReflectionTestUtils.setField(jwtUtil, "tokenValidityMs", validityMs);
        ReflectionTestUtils.setField(jwtUtil, "issuer", issuer);
        ReflectionTestUtils.setField(jwtUtil, "audience", audience);
        return jwtUtil;
    }
}

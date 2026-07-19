package com.lexho.auth.dto;

import com.lexho.auth.support.AuthRequestSupport;
import com.lexho.common.enums.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthDtoSupportTest {

    @Test
    void issueTokenRequestShouldSupportAccessorsAndObjectMethods() {
        IssueTokenRequest first = new IssueTokenRequest();
        first.setUserId(10L);
        first.setRole("ADMIN");
        IssueTokenRequest second = new IssueTokenRequest();
        second.setUserId(10L);
        second.setRole("ADMIN");

        assertEquals(10L, first.getUserId());
        assertEquals("ADMIN", first.getRole());
        assertEquals(first, first);
        assertNotEquals(first, null);
        assertNotEquals(first, "other");
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertEquals("IssueTokenRequest{userId=10, role='ADMIN'}", first.toString());
        second.setRole("CUSTOMER");
        assertNotEquals(first, second);
    }

    @Test
    void refreshTokenRequestShouldSupportAccessorsAndObjectMethods() {
        RefreshTokenRequest first = new RefreshTokenRequest();
        first.setRefreshToken("token");
        RefreshTokenRequest second = new RefreshTokenRequest();
        second.setRefreshToken("token");

        assertEquals("token", first.getRefreshToken());
        assertEquals(first, first);
        assertNotEquals(first, null);
        assertNotEquals(first, "other");
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertEquals("RefreshTokenRequest{refreshToken='token'}", first.toString());
        second.setRefreshToken("other");
        assertNotEquals(first, second);
    }

    @Test
    void tokenPairResponseShouldSupportConstructorsAccessorsAndObjectMethods() {
        TokenPairResponse first = new TokenPairResponse();
        first.setAccessToken("access");
        first.setRefreshToken("refresh");
        TokenPairResponse second = new TokenPairResponse("access", "refresh");

        assertEquals("access", first.getAccessToken());
        assertEquals("refresh", first.getRefreshToken());
        assertEquals(first, first);
        assertNotEquals(first, null);
        assertNotEquals(first, "other");
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertEquals("TokenPairResponse{accessToken='access', refreshToken='refresh'}", first.toString());
        second.setRefreshToken("new-refresh");
        assertNotEquals(first, second);
    }

    @Test
    void authRequestSupportShouldNormalizeAndValidateInputs() {
        assertEquals(Role.ADMIN, AuthRequestSupport.parseRole(" admin "));
        assertEquals("refresh-token", AuthRequestSupport.normalizeRefreshToken(" refresh-token "));

        assertThrows(IllegalArgumentException.class, () -> AuthRequestSupport.parseRole(" "));
        assertThrows(IllegalArgumentException.class, () -> AuthRequestSupport.parseRole("bad"));
        assertThrows(IllegalArgumentException.class, () -> AuthRequestSupport.normalizeRefreshToken(" "));
        assertThrows(IllegalArgumentException.class, () -> AuthRequestSupport.normalizeRefreshToken(null));
    }
}

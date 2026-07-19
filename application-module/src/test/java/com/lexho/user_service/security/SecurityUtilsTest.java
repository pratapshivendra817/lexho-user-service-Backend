package com.lexho.user_service.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityUtilsTest {

    private final SecurityUtils securityUtils = new SecurityUtils();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsEmptyWhenAuthenticationIsMissingOrDetailsAreUnsupported() {
        assertThat(securityUtils.getCurrentRole()).isEmpty();
        assertThat(securityUtils.getCurrentUserId()).isEmpty();

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "user", "pwd", List.of(new SimpleGrantedAuthority("SCOPE_read")));
        authentication.setDetails("not-a-long");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThat(securityUtils.getCurrentRole()).isEmpty();
        assertThat(securityUtils.getCurrentUserId()).isEmpty();
    }

    @Test
    void extractsRoleAndUserIdFromSecurityContext() {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "user", "pwd",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        authentication.setDetails(99L);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThat(securityUtils.getCurrentRole()).contains("ADMIN");
        assertThat(securityUtils.getCurrentUserId()).contains(99L);
    }
}

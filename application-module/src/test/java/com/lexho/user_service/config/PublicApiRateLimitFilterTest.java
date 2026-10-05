package com.lexho.user_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.user_service.exception.TooManyRequestsException;
import com.lexho.user_service.service.ApiRateLimiterService;
import com.lexho.user_service.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicApiRateLimitFilterTest {

    @Mock
    private ApiRateLimiterService rateLimiterService;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private FilterChain filterChain;

    private PublicApiRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new PublicApiRateLimitFilter(rateLimiterService, new ObjectMapper(), jwtUtil);
    }

    static Stream<Arguments> bodyRules() {
        return Stream.of(
                Arguments.of("POST", "/otp/send", "{\"mobile\":\" 9999999999 \"}", "otp_send", "9999999999", 5, 15),
                Arguments.of("POST", "/otp/verify", "{\"mobile\":\"9999999998\"}", "otp_verify", "9999999998", 10, 15),
                Arguments.of("POST", "/api/payments/retry", "{\"bookingId\":\"ABC-1\"}", "payment_retry", "abc-1", 5, 1),
                Arguments.of("POST", "/api/v1/payments/retry", "{}", "payment_retry", "missing", 5, 1)
        );
    }

    @ParameterizedTest
    @MethodSource("bodyRules")
    void appliesBodyBasedRules(String method, String path, String body, String bucket,
                               String identity, int maxRequests, long windowMinutes) throws Exception {
        MockHttpServletRequest request = request(method, path, body);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(rateLimiterService).check(bucket, identity, maxRequests,
                Duration.ofMinutes(windowMinutes), "Too many requests");
        verify(filterChain).doFilter(any(), any());
    }

    @Test
    void usesRequestUserIdForPaymentCreation() throws Exception {
        MockHttpServletRequest request = request("POST", "/api/v1/payments/create-order", "{}");
        request.setAttribute("userId", 71L);

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        verify(rateLimiterService).check("payment_create_order", "71", 10,
                Duration.ofMinutes(1), "Too many requests");
    }

    @Test
    void usesNumericJwtUserIdForPartnerRequest() throws Exception {
        MockHttpServletRequest request = request("PATCH", "/api/partner/requests/42/accept", "");
        request.addHeader("Authorization", "Bearer valid-token");
        Claims claims = new DefaultClaims();
        claims.put("userId", 88);
        when(jwtUtil.validateToken("valid-token")).thenReturn(claims);

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        verify(rateLimiterService).check("partner_request_accept", "88", 20,
                Duration.ofMinutes(1), "Too many requests");
    }

    @Test
    void fallsBackToForwardedClientKeyWhenJwtIsInvalid() throws Exception {
        MockHttpServletRequest request = request("PATCH", "/api/v1/partner/requests/42/reject", "");
        request.addHeader("Authorization", "Bearer broken");
        request.addHeader("X-Forwarded-For", "10.1.1.5, 10.1.1.6");
        request.addHeader("User-Agent", "JUnit");
        when(jwtUtil.validateToken("broken")).thenThrow(new IllegalArgumentException("invalid"));

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        verify(rateLimiterService).check("partner_request_reject",
                "10.1.1.5:" + "JUnit".hashCode(), 20, Duration.ofMinutes(1), "Too many requests");
    }

    @Test
    void usesRemoteAddressAndUnknownAgentWhenNoIdentityIsAvailable() throws Exception {
        MockHttpServletRequest request = request("POST", "/api/payments/create-order", "");
        request.setRemoteAddr("127.0.0.9");

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        verify(rateLimiterService).check("payment_create_order", "127.0.0.9:unknown", 10,
                Duration.ofMinutes(1), "Too many requests");
    }

    @Test
    void usesInvalidIdentityForMalformedJson() throws Exception {
        MockHttpServletRequest request = request("POST", "/otp/send", "{bad-json");

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        verify(rateLimiterService).check("otp_send", "invalid", 5,
                Duration.ofMinutes(15), "Too many requests");
    }

    @Test
    void bypassesUnmatchedRequestsWithoutWrapping() throws Exception {
        MockHttpServletRequest request = request("GET", "/health", "{\"value\":1}");

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        verify(rateLimiterService, never()).check(anyString(), anyString(), anyInt(), any(), anyString());
        ArgumentCaptor<jakarta.servlet.ServletRequest> requestCaptor =
                ArgumentCaptor.forClass(jakarta.servlet.ServletRequest.class);
        verify(filterChain).doFilter(requestCaptor.capture(), any());
        assertThat(requestCaptor.getValue()).isSameAs(request);
    }

    @Test
    void leavesAdminLoginRateLimitToDedicatedAuthService() throws Exception {
        MockHttpServletRequest request = request("POST", "/api/admin/login", "{\"email\":\"admin@example.com\"}");

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        verify(rateLimiterService, never()).check(anyString(), anyString(), anyInt(), any(), anyString());
        ArgumentCaptor<jakarta.servlet.ServletRequest> requestCaptor =
                ArgumentCaptor.forClass(jakarta.servlet.ServletRequest.class);
        verify(filterChain).doFilter(requestCaptor.capture(), any());
        assertThat(requestCaptor.getValue()).isSameAs(request);
    }

    @Test
    void bypassesMultipartRequestsWithoutWrapping() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/admin/upload/image");
        request.setServletPath("/api/admin/upload/image");
        request.setContentType("multipart/form-data; boundary=----WebKitFormBoundary");
        request.setContent("multipart-body".getBytes());

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        verify(rateLimiterService, never()).check(anyString(), anyString(), anyInt(), any(), anyString());
        ArgumentCaptor<jakarta.servlet.ServletRequest> requestCaptor =
                ArgumentCaptor.forClass(jakarta.servlet.ServletRequest.class);
        verify(filterChain).doFilter(requestCaptor.capture(), any());
        assertThat(requestCaptor.getValue()).isSameAs(request);
    }

    @Test
    void returnsRateLimitErrorWithRetryAfterHeader() throws Exception {
        MockHttpServletRequest request = request("POST", "/otp/send", "{\"mobile\":\"999\"}");
        MockHttpServletResponse response = new MockHttpServletResponse();
        doThrow(new TooManyRequestsException("limited", 27L))
                .when(rateLimiterService).check(anyString(), anyString(), anyInt(), any(), anyString());

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("27");
        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(response.getContentAsString())
                .contains("\"code\":\"RATE_LIMIT_EXCEEDED\"")
                .contains("\"retryAfterSeconds\":27");
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void omitsRetryAfterHeaderWhenDelayIsZero() throws Exception {
        MockHttpServletRequest request = request("POST", "/otp/verify", "");
        MockHttpServletResponse response = new MockHttpServletResponse();
        doThrow(new TooManyRequestsException("limited", 0L))
                .when(rateLimiterService).check(anyString(), anyString(), anyInt(), any(), anyString());

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isNull();
    }

    private MockHttpServletRequest request(String method, String path, String body) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        request.setContent(body.getBytes());
        return request;
    }
}

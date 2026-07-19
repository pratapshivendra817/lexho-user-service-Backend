package com.lexho.user_service.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.user_service.exception.TooManyRequestsException;
import com.lexho.user_service.service.ApiRateLimiterService;
import com.lexho.user_service.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;

@Component
@Order(20)
public class PublicApiRateLimitFilter extends OncePerRequestFilter {

    private final ApiRateLimiterService rateLimiterService;
    private final ObjectMapper objectMapper;
    private final JwtUtil jwtUtil;

    public PublicApiRateLimitFilter(ApiRateLimiterService rateLimiterService,
                                    ObjectMapper objectMapper,
                                    JwtUtil jwtUtil) {
        this.rateLimiterService = rateLimiterService;
        this.objectMapper = objectMapper;
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (isMultipart(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        HttpServletRequest requestToUse = requiresCachedBody(request) ? new CachedBodyRequest(request) : request;
        RateLimitRule rule = ruleFor(requestToUse);
        if (rule == null) {
            filterChain.doFilter(requestToUse, response);
            return;
        }
        try {
            rateLimiterService.check(
                    rule.bucket(),
                    rule.identity(),
                    rule.maxRequests(),
                    rule.window(),
                    "Too many requests"
            );
            filterChain.doFilter(requestToUse, response);
        } catch (TooManyRequestsException exception) {
            long retryAfterSeconds = exception.getRetryAfterSeconds();
            if (retryAfterSeconds > 0) {
                response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            }
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), Map.of(
                    "code", "RATE_LIMIT_EXCEEDED",
                    "message", "Too many requests",
                    "retryAfterSeconds", retryAfterSeconds
            ));
        }
    }

    private RateLimitRule ruleFor(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getServletPath();
        if (path == null) {
            return null;
        }
        if ("POST".equalsIgnoreCase(method) && path.equals("/otp/send")) {
            return new RateLimitRule("otp_send", bodyText(request, "mobile"), 5, Duration.ofMinutes(15));
        }
        if ("POST".equalsIgnoreCase(method) && path.equals("/otp/verify")) {
            return new RateLimitRule("otp_verify", bodyText(request, "mobile"), 10, Duration.ofMinutes(15));
        }
        if ("POST".equalsIgnoreCase(method) && path.equals("/api/admin/login")) {
            return new RateLimitRule("admin_login", bodyText(request, "email"), 5, Duration.ofMinutes(15));
        }
        if ("POST".equalsIgnoreCase(method)
                && (path.equals("/api/payments/create-order") || path.equals("/api/v1/payments/create-order"))) {
            return new RateLimitRule("payment_create_order", userId(request), 10, Duration.ofMinutes(1));
        }
        if ("POST".equalsIgnoreCase(method)
                && (path.equals("/api/payments/retry") || path.equals("/api/v1/payments/retry"))) {
            return new RateLimitRule("payment_retry", bodyText(request, "bookingId"), 5, Duration.ofMinutes(1));
        }
        if ("PATCH".equalsIgnoreCase(method) && path.matches("/api(/v1)?/partner/requests/\\d+/accept")) {
            return new RateLimitRule("partner_request_accept", userId(request), 20, Duration.ofMinutes(1));
        }
        if ("PATCH".equalsIgnoreCase(method) && path.matches("/api(/v1)?/partner/requests/\\d+/reject")) {
            return new RateLimitRule("partner_request_reject", userId(request), 20, Duration.ofMinutes(1));
        }
        return null;
    }

    private boolean requiresCachedBody(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getServletPath();
        if (path == null) {
            return false;
        }
        return "POST".equalsIgnoreCase(method)
                && (path.equals("/otp/send")
                || path.equals("/otp/verify")
                || path.equals("/api/admin/login")
                || path.equals("/api/payments/retry")
                || path.equals("/api/v1/payments/retry"));
    }

    private boolean isMultipart(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("multipart/");
    }

    private String bodyText(HttpServletRequest request, String field) {
        if (!(request instanceof CachedBodyRequest cachedBodyRequest)) {
            return "missing";
        }
        try {
            if (cachedBodyRequest.body.length == 0) {
                return "missing";
            }
            JsonNode node = objectMapper.readTree(cachedBodyRequest.body);
            String value = node.path(field).asText();
            return value == null || value.isBlank() ? "missing" : value.trim().toLowerCase(Locale.ROOT);
        } catch (IOException ignored) {
            return "invalid";
        }
    }

    private String userId(HttpServletRequest request) {
        Object requestUserId = request.getAttribute("userId");
        if (requestUserId != null) {
            return String.valueOf(requestUserId);
        }
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                Claims claims = jwtUtil.validateToken(authHeader.substring(7).trim());
                Object claim = claims.get("userId");
                if (claim instanceof Number number) {
                    return String.valueOf(number.longValue());
                }
            } catch (RuntimeException ignored) {
                return clientKey(request);
            }
        }
        return clientKey(request);
    }

    private String clientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        String ip = forwardedFor == null || forwardedFor.isBlank()
                ? request.getRemoteAddr()
                : forwardedFor.split(",")[0].trim();
        String userAgent = request.getHeader("User-Agent");
        return (ip + ":" + (userAgent == null ? "unknown" : userAgent.hashCode()))
                .toLowerCase(Locale.ROOT);
    }

    private record RateLimitRule(String bucket, String identity, int maxRequests, Duration window) {
    }

    private static class CachedBodyRequest extends HttpServletRequestWrapper {
        private final byte[] body;

        private CachedBodyRequest(HttpServletRequest request) throws IOException {
            super(request);
            this.body = request.getInputStream().readAllBytes();
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return inputStream.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    // Synchronous request body replay.
                }

                @Override
                public int read() {
                    return inputStream.read();
                }
            };
        }

        @Override
        public java.io.BufferedReader getReader() {
            return new java.io.BufferedReader(new java.io.InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}

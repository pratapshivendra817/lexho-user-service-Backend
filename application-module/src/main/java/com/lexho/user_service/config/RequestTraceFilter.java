package com.lexho.user_service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RequestTraceFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestTraceFilter.class);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        if (path == null) {
            return true;
        }
        return !path.startsWith("/api/")
                && !path.equals("/health")
                && !path.equals("/health/readiness")
                && !path.equals("/health/liveness")
                && !path.equals("/ping")
                && !path.startsWith("/otp/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startedAt = System.currentTimeMillis();
        String correlationId = firstNonBlank(MDC.get(CorrelationIdFilter.MDC_KEY), "-");
        String requestId = firstNonBlank(MDC.get(CorrelationIdFilter.REQUEST_MDC_KEY), "-");
        log.info("""
                {"event":"request_start","requestId":"{}","correlationId":"{}","method":"{}","path":"{}"}
                """.strip(), requestId, correlationId, request.getMethod(), request.getServletPath());
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - startedAt;
            Object userId = request.getAttribute("userId");
            String bookingId = firstNonBlank(
                    request.getParameter("bookingId"),
                    pathBookingId(request.getServletPath())
            );
            log.info("""
                    {"event":"request_end","requestId":"{}","correlationId":"{}","bookingId":"{}","userId":"{}","api":"{} {}","status":"{}","durationMs":{}}
                    """.strip(),
                    requestId,
                    correlationId,
                    bookingId == null ? "" : bookingId,
                    userId == null ? "" : userId,
                    request.getMethod(),
                    request.getServletPath(),
                    response.getStatus(),
                    durationMs);
        }
    }

    private String pathBookingId(String path) {
        if (path == null || !path.contains("/bookings/")) {
            return null;
        }
        String[] parts = path.split("/");
        for (int i = 0; i < parts.length - 1; i++) {
            if ("bookings".equals(parts[i])) {
                return parts[i + 1].replaceAll("[^0-9]", "");
            }
        }
        return null;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second == null || second.isBlank() ? null : second;
    }
}

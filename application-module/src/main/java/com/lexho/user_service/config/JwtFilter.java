package com.lexho.user_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.admin.entity.Admin;
import com.lexho.admin.repository.AdminRepository;
import com.lexho.common.enums.Role;
import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Locale;

@Component
public class JwtFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final AdminRepository adminRepository;
    private final Environment environment;

    public JwtFilter(JwtUtil jwtUtil,
                     ObjectMapper objectMapper,
                     AdminRepository adminRepository,
                     Environment environment) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
        this.adminRepository = adminRepository;
        this.environment = environment;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String servletPath = request.getServletPath();

        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        if (isAlwaysPublicPath(servletPath)
                || servletPath == null
                || servletPath.isBlank()
                || "/".equals(servletPath)) {
            return true;
        }

        if (isExposeSwagger()
                && ("/swagger-ui.html".equals(servletPath)
                || servletPath.startsWith("/swagger-ui/")
                || "/v3/api-docs".equals(servletPath)
                || servletPath.startsWith("/v3/api-docs/"))) {
            return true;
        }

        if (servletPath.startsWith("/internal/")) {
            return true;
        }

        if (isAllowPublicCustomerRead()
                && (servletPath.startsWith("/api/categories")
                || servletPath.startsWith("/api/banners")
                || "/api/customer/banners".equals(servletPath)
                || servletPath.startsWith("/api/customer/home/banners")
                || servletPath.startsWith("/api/customer/home/sections")
                || servletPath.startsWith("/api/customer/services")
                || "/api/customer/profile-content".equals(servletPath)
                || "/api/customer/location/reverse-geocode".equals(servletPath)
                || "/api/v1/customer/location/reverse-geocode".equals(servletPath)
                || "/api/customer/location/serviceability/check".equals(servletPath)
                || "/api/v1/customer/location/serviceability/check".equals(servletPath))) {
            return true;
        }

        if (HttpMethod.GET.matches(request.getMethod())
                && ("/api/admin/profile-content".equals(servletPath)
                || "/api/v1/admin/profile-content".equals(servletPath))) {
            return true;
        }

        return isAllowPublicProviderOnboarding()
                && "/api/partner/onboarding".equals(servletPath);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (isAlwaysPublicPath(request.getServletPath())) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeUnauthorized(request, response, "Authorization token is required");
            return;
        }

        try {
            String token = authHeader.substring(7).trim();
            if (token.isBlank()) {
                writeUnauthorized(request, response, "Authorization token is required");
                return;
            }

            Claims claims = jwtUtil.validateToken(token);
            Long userId = ((Number) claims.get("userId")).longValue();
            String subject = claims.getSubject();
            String roleClaim = claims.get("role", String.class);
            Role role = Role.valueOf(roleClaim.toUpperCase(Locale.ROOT));
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role.name());
            String email = claims.get("email", String.class);

            if (role == Role.ADMIN && request.getServletPath().startsWith("/api/admin/")) {
                validateActiveAdmin(userId, email);
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            subject,
                            null,
                            Collections.singletonList(authority)
                    );
            authentication.setDetails(userId);

            SecurityContextHolder.getContext().setAuthentication(authentication);
            request.setAttribute("mobile", subject);
            request.setAttribute("email", email);
            request.setAttribute("userId", userId);
            request.setAttribute("role", role.name());

            filterChain.doFilter(request, response);
        } catch (Exception exception) {
            SecurityContextHolder.clearContext();
            writeUnauthorized(request, response, "Invalid or expired token");
        }
    }

    private void writeUnauthorized(HttpServletRequest request,
                                   HttpServletResponse response,
                                   String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        if (request.getServletPath().startsWith("/api/admin/")) {
            response.getWriter().write(objectMapper.writeValueAsString(java.util.Map.of("message", message)));
            return;
        }
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.failure(message)));
    }

    private void validateActiveAdmin(Long tokenUserId, String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid admin token");
        }
        Admin admin = adminRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found"));
        if (!admin.isActive() || admin.getId() == null || !admin.getId().equals(tokenUserId)) {
            throw new IllegalArgumentException("Invalid admin token");
        }
    }

    private boolean isExposeSwagger() {
        return environment.getProperty("app.security.exposure.expose-swagger", Boolean.class, false);
    }

    private boolean isAlwaysPublicPath(String servletPath) {
        return servletPath == null
                || servletPath.isBlank()
                || "/error".equals(servletPath)
                || "/favicon.ico".equals(servletPath)
                || servletPath.startsWith("/ws-notifications")
                || servletPath.startsWith("/ws/admin/notifications")
                || servletPath.startsWith("/ws/customer/bookings")
                || servletPath.startsWith("/ws-chat")
                || "/ping".equals(servletPath)
                || "/health".equals(servletPath)
                || "/health/readiness".equals(servletPath)
                || "/health/liveness".equals(servletPath)
                || "/api/health".equals(servletPath)
                || "/api/health/".equals(servletPath)
                || "/api/health/ping".equals(servletPath)
                || "/api/health/readiness".equals(servletPath)
                || "/api/health/liveness".equals(servletPath)
                || "/actuator/health".equals(servletPath)
                || "/actuator/info".equals(servletPath)
                || "/api/payments/webhook".equals(servletPath)
                || "/api/payments/webhook/razorpay".equals(servletPath)
                || "/api/v1/webhooks/razorpay".equals(servletPath)
                || servletPath.startsWith("/otp/")
                || servletPath.startsWith("/auth/")
                || servletPath.startsWith("/api/otp/")
                || servletPath.startsWith("/api/auth/")
                || servletPath.startsWith("/api/v1/auth/")
                || servletPath.startsWith("/uploads/")
                || servletPath.startsWith("/images/")
                || "/api/admin/login".equals(servletPath)
                || "/api/admin/verify-otp".equals(servletPath);
    }

    private boolean isAllowPublicCustomerRead() {
        return environment.getProperty("app.security.exposure.allow-public-customer-read", Boolean.class, false);
    }

    private boolean isAllowPublicProviderOnboarding() {
        return environment.getProperty("app.security.exposure.allow-public-partner-onboarding", Boolean.class, false);
    }

}

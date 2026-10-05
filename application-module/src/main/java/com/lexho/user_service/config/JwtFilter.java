package com.lexho.user_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.admin.repository.AdminRepository;
import com.lexho.common.enums.Role;
import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.service.AdminAuthService;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Component
public class JwtFilter extends OncePerRequestFilter {
    private static final String API_CUSTOMER_BANNERS = "/api/customer/banners";
    private static final String API_CUSTOMER_PROFILE_CONTENT = "/api/customer/profile-content";
    private static final String API_CUSTOMER_LOCATION_REVERSE_GEOCODE = "/api/customer/location/reverse-geocode";
    private static final String API_V1_CUSTOMER_LOCATION_REVERSE_GEOCODE =
            "/api/v1/customer/location/reverse-geocode";
    private static final String API_CUSTOMER_LOCATION_SERVICEABILITY_CHECK =
            "/api/customer/location/serviceability/check";
    private static final String API_V1_CUSTOMER_LOCATION_SERVICEABILITY_CHECK =
            "/api/v1/customer/location/serviceability/check";
    private static final String[] ALWAYS_PUBLIC_EXACT_PATHS = {
            "/error",
            "/favicon.ico",
            "/ping",
            "/health",
            "/health/readiness",
            "/health/liveness",
            "/api/health",
            "/api/health/",
            "/api/health/ping",
            "/api/health/readiness",
            "/api/health/liveness",
            "/actuator/health",
            "/actuator/info",
            "/api/payments/webhook",
            "/api/payments/webhook/razorpay",
            "/api/v1/webhooks/razorpay",
            "/api/admin/login",
            "/api/admin/verify-otp"
    };
    private static final String[] ALWAYS_PUBLIC_PREFIXES = {
            "/ws-notifications",
            "/ws/admin/notifications",
            "/ws/customer/bookings",
            "/ws-chat",
            "/otp/",
            "/auth/",
            "/customer/otp/",
            "/partner/otp/",
            "/api/otp/",
            "/api/auth/",
            "/api/v1/auth/",
            "/api/customer/otp/",
            "/api/partner/otp/",
            "/api/v1/customer/otp/",
            "/api/v1/partner/otp/",
            "/uploads/",
            "/images/"
    };
    private static final String[] CUSTOMER_READ_EXACT_PATHS = {
            API_CUSTOMER_BANNERS,
            API_CUSTOMER_PROFILE_CONTENT,
            "/api/public/social-media",
            "/api/home/dashboard",
            "/api/services/popular",
            "/api/services/recommended",
            "/api/services/quick",
            "/api/search/services",
            "/api/slots/available",
            "/api/payments/legacy-options",
            "/api/coupon/list",
            "/api/support/faqs",
            "/api/v1/faqs",
            "/api/v1/support/contact"
    };
    private static final String[] CUSTOMER_READ_PREFIXES = {
            "/api/categories",
            "/api/banners",
            "/api/customer/home/banners",
            "/api/customer/home/sections",
            "/api/customer/services",
            "/api/public/website/",
            "/api/reviews/service/"
    };
    private static final String[] PUBLIC_EXPOSURE_EXACT_PATHS = {
            API_CUSTOMER_BANNERS,
            API_CUSTOMER_PROFILE_CONTENT,
            API_CUSTOMER_LOCATION_REVERSE_GEOCODE,
            API_V1_CUSTOMER_LOCATION_REVERSE_GEOCODE,
            API_CUSTOMER_LOCATION_SERVICEABILITY_CHECK,
            API_V1_CUSTOMER_LOCATION_SERVICEABILITY_CHECK
    };
    private static final String[] PUBLIC_EXPOSURE_PREFIXES = {
            "/api/categories",
            "/api/banners",
            "/api/customer/home/banners",
            "/api/customer/home/sections",
            "/api/customer/services"
    };
    private static final String[] LEGAL_READ_PATHS = {
            "/api/v1/legal/current",
            "/api/v1/legal/terms",
            "/api/v1/legal/privacy",
            "/api/v1/legal/cancellation",
            "/api/v1/legal/partner-terms",
            "/api/v1/legal/cookie",
            "/api/v1/legal/disclaimer",
            "/api/v1/legal/grievance",
            "/api/v1/legal/home",
            "/api/v1/legal/about"
    };

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

        if (isOptions(request) || isRootPath(servletPath) || isAlwaysPublicPath(servletPath)) {
            return true;
        }

        if (isExposedSwaggerPath(servletPath) || isInternalPath(servletPath)) {
            return true;
        }

        if (isPublicRequest(request, servletPath) || isPublicExposurePath(servletPath)) {
            return true;
        }

        return isPublicAdminProfileContent(request, servletPath);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (isAlwaysPublicPath(request.getServletPath())
                || isPublicSupportPath(request.getMethod(), request.getServletPath())) {
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
            Long userId = userIdFrom(claims);
            String subject = claims.getSubject();
            String roleClaim = claims.get("role", String.class);
            Role role = roleFrom(roleClaim);
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role.name());
            String email = claims.get("email", String.class);

            if (isAdminApiPath(request.getServletPath())) {
                if (role != Role.ADMIN || !isActiveAdmin(userId, email)) {
                    writeUnauthorized(request, response, AdminAuthService.ADMIN_PORTAL_ACCESS_DENIED_MESSAGE);
                    return;
                }
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            subject,
                            null,
                            authoritiesFor(role, authority)
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
        if (isAdminApiPath(request.getServletPath())) {
            response.getWriter().write(objectMapper.writeValueAsString(java.util.Map.of("message", message)));
            return;
        }
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.failure(message)));
    }

    private boolean isAdminApiPath(String servletPath) {
        return servletPath != null
                && (servletPath.startsWith("/api/admin/")
                || servletPath.startsWith("/api/v1/admin/"));
    }

    private boolean isActiveAdmin(Long tokenUserId, String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return adminRepository.findByEmailIgnoreCase(email)
                .filter(admin -> admin.isActive()
                        && admin.getId() != null
                        && admin.getId().equals(tokenUserId))
                .isPresent();
    }

    private Long userIdFrom(Claims claims) {
        Object value = claims.get("userId");
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalArgumentException("Invalid token user id");
    }

    private Role roleFrom(String roleClaim) {
        if (roleClaim == null || roleClaim.isBlank()) {
            throw new IllegalArgumentException("Invalid token role");
        }
        return Role.valueOf(roleClaim.toUpperCase(Locale.ROOT));
    }

    private List<SimpleGrantedAuthority> authoritiesFor(Role role, SimpleGrantedAuthority roleAuthority) {
        if (role != Role.ADMIN) {
            return Collections.singletonList(roleAuthority);
        }
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(roleAuthority);
        authorities.add(new SimpleGrantedAuthority("VIEW_REVIEWS"));
        authorities.add(new SimpleGrantedAuthority("MODERATE_REVIEWS"));
        authorities.add(new SimpleGrantedAuthority("MANAGE_REVIEW_SETTINGS"));
        authorities.add(new SimpleGrantedAuthority("VIEW_REVIEW_REPORTS"));
        authorities.add(new SimpleGrantedAuthority("VIEW_REVIEW_STATS"));
        return authorities;
    }

    private boolean isExposeSwagger() {
        return environment.getProperty("app.security.exposure.expose-swagger", Boolean.class, false);
    }

    private boolean isAlwaysPublicPath(String servletPath) {
        return matchesAny(servletPath, ALWAYS_PUBLIC_EXACT_PATHS)
                || startsWithAny(servletPath, ALWAYS_PUBLIC_PREFIXES);
    }

    private boolean isAllowPublicCustomerRead() {
        return environment.getProperty("app.security.exposure.allow-public-customer-read", Boolean.class, false);
    }

    private boolean isPublicCustomerReadPath(String servletPath) {
        return matchesAny(servletPath, CUSTOMER_READ_EXACT_PATHS)
                || startsWithAny(servletPath, CUSTOMER_READ_PREFIXES)
                || isPublicLegalReadPath(servletPath);
    }

    private boolean isPublicSupportPath(String method, String servletPath) {
        return servletPath != null
                && ((HttpMethod.GET.matches(method) && matchesPath(servletPath, SecurityConfig.publicSupportPaths()[0]))
                || (HttpMethod.POST.matches(method) && matchesPath(servletPath, SecurityConfig.publicSupportPaths()[1])));
    }

    private boolean matchesPath(String servletPath, String expectedPath) {
        return expectedPath.equals(servletPath) || (expectedPath + "/").equals(servletPath);
    }

    private boolean isPublicLegalReadPath(String servletPath) {
        return matchesAny(servletPath, LEGAL_READ_PATHS);
    }

    private boolean isOptions(HttpServletRequest request) {
        return HttpMethod.OPTIONS.matches(request.getMethod());
    }

    private boolean isRootPath(String servletPath) {
        return servletPath == null || servletPath.isBlank() || "/".equals(servletPath);
    }

    private boolean isExposedSwaggerPath(String servletPath) {
        return isExposeSwagger()
                && ("/swagger-ui.html".equals(servletPath)
                || "/v3/api-docs".equals(servletPath)
                || startsWithAny(servletPath, "/swagger-ui/", "/v3/api-docs/"));
    }

    private boolean isInternalPath(String servletPath) {
        return servletPath != null && servletPath.startsWith("/internal/");
    }

    private boolean isPublicRequest(HttpServletRequest request, String servletPath) {
        return isPublicSupportPath(request.getMethod(), servletPath)
                || (HttpMethod.GET.matches(request.getMethod()) && isPublicCustomerReadPath(servletPath));
    }

    private boolean isPublicExposurePath(String servletPath) {
        return isAllowPublicCustomerRead()
                && (matchesAny(servletPath, PUBLIC_EXPOSURE_EXACT_PATHS)
                || startsWithAny(servletPath, PUBLIC_EXPOSURE_PREFIXES));
    }

    private boolean isPublicAdminProfileContent(HttpServletRequest request, String servletPath) {
        return HttpMethod.GET.matches(request.getMethod())
                && matchesAny(servletPath, "/api/admin/profile-content", "/api/v1/admin/profile-content");
    }

    private boolean matchesAny(String value, String... expectedValues) {
        return value != null && Arrays.asList(expectedValues).contains(value);
    }

    private boolean startsWithAny(String value, String... prefixes) {
        return value != null && Arrays.stream(prefixes).anyMatch(value::startsWith);
    }

}

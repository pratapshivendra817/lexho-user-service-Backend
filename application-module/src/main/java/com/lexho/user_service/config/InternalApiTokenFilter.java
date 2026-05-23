package com.lexho.user_service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class InternalApiTokenFilter extends OncePerRequestFilter {

    private static final String INTERNAL_HEADER = "X-Internal-Token";

    private final SecurityExposureProperties securityExposureProperties;
    private final Environment environment;

    public InternalApiTokenFilter(SecurityExposureProperties securityExposureProperties,
                                  Environment environment) {
        this.securityExposureProperties = securityExposureProperties;
        this.environment = environment;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        if (securityExposureProperties.isAllowInternalOpen()) {
            return true;
        }
        return !request.getServletPath().startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String configured = environment.getProperty("INTERNAL_API_TOKEN");
        String provided = request.getHeader(INTERNAL_HEADER);

        if (configured == null || configured.isBlank() || provided == null || !configured.equals(provided)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"message\":\"Unauthorized internal API access\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}

package com.lexho.user_service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ApiVersionHeaderFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getServletPath();
        if (path != null && path.startsWith("/api/v1/")) {
            response.setHeader("X-API-Version", "v1");
        } else if (path != null && path.startsWith("/api/") && !path.startsWith("/api/v1/")) {
            response.setHeader("X-API-Legacy-Route", "true");
        }
        filterChain.doFilter(request, response);
    }
}

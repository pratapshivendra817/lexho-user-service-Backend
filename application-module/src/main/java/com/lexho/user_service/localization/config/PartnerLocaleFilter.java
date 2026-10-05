package com.lexho.user_service.localization.config;

import com.lexho.user_service.localization.LocaleContext;
import com.lexho.user_service.localization.service.PartnerLocalizationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class PartnerLocaleFilter extends OncePerRequestFilter {

    private final PartnerLocalizationService localizationService;

    public PartnerLocaleFilter(PartnerLocalizationService localizationService) {
        this.localizationService = localizationService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            LocaleContext.setLocale(localizationService.localeFromHeader(request.getHeader("Accept-Language")));
            filterChain.doFilter(request, response);
        } finally {
            LocaleContext.clear();
        }
    }
}

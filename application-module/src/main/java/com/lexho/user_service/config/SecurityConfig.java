package com.lexho.user_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final Environment environment;

    public SecurityConfig(JwtFilter jwtFilter,
                          Environment environment) {
        this.jwtFilter = jwtFilter;
        this.environment = environment;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                    auth.requestMatchers("/", "/error").permitAll();
                    auth.requestMatchers("/api/health", "/api/health/ping", "/ping").permitAll();
                    auth.requestMatchers("/ws-notifications/**", "/ws/admin/notifications/**", "/ws/customer/bookings/**").permitAll();
                    auth.requestMatchers("/uploads/**", "/images/**").permitAll();
                    auth.requestMatchers("/otp/**", "/auth/**", "/api/otp/**", "/api/auth/**").permitAll();
                    auth.requestMatchers("/api/admin/login", "/api/admin/verify-otp").permitAll();

                    if (isExposeSwagger()) {
                        auth.requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**"
                        ).permitAll();
                    }

                    if (isAllowInternalOpen()) {
                        auth.requestMatchers("/internal/bookings/**").permitAll();
                        auth.requestMatchers("/internal/auth/**").permitAll();
                    }

                    if (isAllowPublicAdminProfileContent()) {
                        auth.requestMatchers("/api/admin/profile-content").permitAll();
                    }

                    if (isAllowPublicProviderOnboarding()) {
                        auth.requestMatchers("/api/partner/onboarding").permitAll();
                    }

                    if (isAllowPublicCustomerRead()) {
                        auth.requestMatchers("/api/banners", "/api/banners/**", "/api/categories", "/api/categories/**").permitAll();
                        auth.requestMatchers(HttpMethod.GET, "/api/customer/banners").permitAll();
                        auth.requestMatchers(HttpMethod.GET, "/api/customer/home/sections", "/api/customer/home/sections/**").permitAll();
                        auth.requestMatchers(HttpMethod.GET, "/api/customer/services", "/api/customer/services/**").permitAll();
                        auth.requestMatchers(HttpMethod.GET, "/api/customer/profile-content").permitAll();
                    }

                    auth.requestMatchers("/api/customer/**").hasRole("CUSTOMER");
                    auth.requestMatchers("/api/partner/**").hasRole("PARTNER");
                    auth.requestMatchers("/api/admin/**").hasRole("ADMIN");
                    auth.requestMatchers("/api/v1/customer/**").hasRole("CUSTOMER");
                    auth.requestMatchers("/api/v1/partner/**").hasRole("PARTNER");
                    auth.requestMatchers("/api/v1/admin/**").hasRole("ADMIN");
                    auth.anyRequest().authenticated();
                })
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        String rawOrigins = environment.getProperty(
                "app.security.cors.allowed-origins",
                "http://localhost:3000,http://localhost:5173,http://localhost:4200"
        );
        configuration.setAllowedOrigins(List.of(rawOrigins.split("\\s*,\\s*")));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private boolean isExposeSwagger() {
        return environment.getProperty("app.security.exposure.expose-swagger", Boolean.class, true);
    }

    private boolean isAllowInternalOpen() {
        return environment.getProperty("app.security.exposure.allow-internal-open", Boolean.class, true);
    }

    private boolean isAllowPublicCustomerRead() {
        return environment.getProperty("app.security.exposure.allow-public-customer-read", Boolean.class, true);
    }

    private boolean isAllowPublicProviderOnboarding() {
        return environment.getProperty("app.security.exposure.allow-public-partner-onboarding", Boolean.class, true);
    }

    private boolean isAllowPublicAdminProfileContent() {
        return environment.getProperty("app.security.exposure.allow-public-admin-profile-content", Boolean.class, true);
    }
}

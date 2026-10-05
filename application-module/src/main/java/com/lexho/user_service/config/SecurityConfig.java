package com.lexho.user_service.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private static final AntPathMatcher CSRF_PATH_MATCHER = new AntPathMatcher();
    private static final RequestMatcher DEFAULT_CSRF_MATCHER = CsrfFilter.DEFAULT_CSRF_MATCHER;
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_CUSTOMER = "CUSTOMER";
    private static final String ROLE_PARTNER = "PARTNER";

    private final JwtFilter jwtFilter;
    private final InternalApiTokenFilter internalApiTokenFilter;
    private final Environment environment;

    public SecurityConfig(JwtFilter jwtFilter,
                          InternalApiTokenFilter internalApiTokenFilter,
                          Environment environment) {
        this.jwtFilter = jwtFilter;
        this.internalApiTokenFilter = internalApiTokenFilter;
        this.environment = environment;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF remains enabled for unsafe requests unless the request targets a stateless
                // API namespace. Those API paths authenticate through explicit headers
                // (Authorization: Bearer or X-Internal-Token), Spring Security does not persist an
                // HttpSession, and cross-origin credentialed requests are disabled by the CORS
                // configuration below.
                .csrf(csrf -> csrf.requireCsrfProtectionMatcher(csrfProtectionRequestMatcher()))
                .cors(Customizer.withDefaults())
                .securityContext(securityContext -> securityContext
                        .securityContextRepository(new NullSecurityContextRepository()))
                .requestCache(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(Customizer.withDefaults())
                        .referrerPolicy(referrer -> referrer.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; frame-ancestors 'none'; object-src 'none'"))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .preload(true)
                                .maxAgeInSeconds(31536000))
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                    auth.requestMatchers("/", "/error").permitAll();
                    auth.requestMatchers(
                            "/health", "/health/readiness", "/health/liveness",
                            "/api/health", "/api/health/ping", "/api/health/readiness", "/api/health/liveness",
                            "/ping",
                            "/actuator/health", "/actuator/info"
                    ).permitAll();
                    auth.requestMatchers("/internal/**").hasRole("INTERNAL");
                    auth.requestMatchers("/ws-notifications/**", "/ws/admin/notifications/**", "/ws/customer/bookings/**", "/ws-chat/**").permitAll();
                    auth.requestMatchers("/uploads/**", "/images/**").permitAll();
                    auth.requestMatchers(
                            "/otp/**",
                            "/auth/**",
                            "/customer/otp/**",
                            "/partner/otp/**",
                            "/api/otp/**",
                            "/api/auth/**",
                            "/api/customer/otp/**",
                            "/api/partner/otp/**",
                            "/api/v1/auth/**",
                            "/api/v1/customer/otp/**",
                            "/api/v1/partner/otp/**"
                    ).permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/api/payments/webhook", "/api/payments/webhook/razorpay",
                            "/api/v1/webhooks/razorpay").permitAll();
                    auth.requestMatchers("/api/admin/login", "/api/admin/verify-otp").permitAll();
                    auth.requestMatchers("/api/banners", "/api/banners/**", "/api/categories", "/api/categories/**").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/customer/banners").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/customer/home/banners", "/api/customer/home/banners/**").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/customer/home/sections", "/api/customer/home/sections/**").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/customer/services", "/api/customer/services/**").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/customer/profile-content").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/public/social-media").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/public/website/**").permitAll();
                    auth.requestMatchers(HttpMethod.GET, publicSupportPaths()[0], publicSupportPaths()[0] + "/").permitAll();
                    auth.requestMatchers(HttpMethod.POST, publicSupportPaths()[1], publicSupportPaths()[1] + "/").permitAll();
                    auth.requestMatchers(HttpMethod.GET,
                            "/api/content/documents/**",
                            "/api/v1/content/documents/**",
                            "/api/v1/legal/current",
                            "/api/v1/legal/terms",
                            "/api/v1/legal/privacy",
                            "/api/v1/legal/cancellation",
                            "/api/v1/legal/partner-terms",
                            "/api/v1/legal/cookie",
                            "/api/v1/legal/disclaimer",
                            "/api/v1/legal/grievance",
                            "/api/v1/legal/home",
                            "/api/v1/legal/about",
                            "/api/v1/faqs",
                            "/api/v1/support/contact"
                    ).permitAll();
                    auth.requestMatchers(HttpMethod.GET,
                            "/api/home/dashboard",
                            "/api/services/popular",
                            "/api/services/recommended",
                            "/api/services/quick",
                            "/api/search/services",
                            "/api/slots/available",
                            "/api/payments/legacy-options",
                            "/api/coupon/list",
                            "/api/reviews/service/**",
                            "/api/support/faqs"
                    ).permitAll();

                    if (isExposeSwagger()) {
                        auth.requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**"
                        ).permitAll();
                    }

                    auth.requestMatchers(HttpMethod.GET, "/api/admin/profile-content", "/api/v1/admin/profile-content").permitAll();

                    if (isAllowPublicCustomerRead()) {
                        auth.requestMatchers(HttpMethod.POST,
                                "/api/customer/location/reverse-geocode",
                                "/api/v1/customer/location/reverse-geocode",
                                "/api/customer/location/serviceability/check",
                                "/api/v1/customer/location/serviceability/check"
                        ).permitAll();
                    }

                    auth.requestMatchers("/api/cart/**", "/api/cart").hasRole(ROLE_CUSTOMER);
                    auth.requestMatchers("/api/customer/**").hasRole(ROLE_CUSTOMER);
                    auth.requestMatchers("/api/partner/**").hasRole(ROLE_PARTNER);
                    auth.requestMatchers("/api/admin/**").hasRole(ROLE_ADMIN);
                    auth.requestMatchers("/api/v1/chat/**").hasAnyRole(ROLE_CUSTOMER, ROLE_PARTNER);
                    auth.requestMatchers("/api/v1/customer/**").hasRole(ROLE_CUSTOMER);
                    auth.requestMatchers("/api/v1/partner/**").hasRole(ROLE_PARTNER);
                    auth.requestMatchers("/api/v1/admin/**").hasRole(ROLE_ADMIN);
                    auth.anyRequest().authenticated();
                })
                .addFilterBefore(internalApiTokenFilter, UsernamePasswordAuthenticationFilter.class)
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
        configuration.setAllowedOrigins(parseAllowedOrigins(rawOrigins));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    static String[] csrfExemptPaths() {
        return new String[]{
                "/api/**",
                "/internal/**",
                "/otp/**",
                "/auth/**",
                "/customer/otp/**",
                "/partner/otp/**"
        };
    }

    static String[] csrfAlwaysExemptPaths() {
        return new String[]{
                "/api/admin/login",
                "/api/admin/verify-otp"
        };
    }

    static RequestMatcher csrfProtectionRequestMatcher() {
        return request -> DEFAULT_CSRF_MATCHER.matches(request)
                && !isAlwaysCsrfExemptPath(request)
                && !isStatelessHeaderAuthenticatedPath(request);
    }

    private static boolean isAlwaysCsrfExemptPath(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        return servletPath != null && Arrays.stream(csrfAlwaysExemptPaths())
                .anyMatch(pattern -> CSRF_PATH_MATCHER.match(pattern, servletPath));
    }

    private static boolean isStatelessHeaderAuthenticatedPath(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        return servletPath != null && Arrays.stream(csrfExemptPaths())
                .anyMatch(pattern -> CSRF_PATH_MATCHER.match(pattern, servletPath));
    }

    static String[] publicSupportPaths() {
        return new String[]{
                "/api/public/support/faqs",
                "/api/public/support/tickets"
        };
    }

    static List<String> parseAllowedOrigins(String rawOrigins) {
        return AllowedOriginsParser.parse(rawOrigins);
    }

    private boolean isExposeSwagger() {
        return environment.getProperty("app.security.exposure.expose-swagger", Boolean.class, false);
    }

    private boolean isAllowPublicCustomerRead() {
        return environment.getProperty("app.security.exposure.allow-public-customer-read", Boolean.class, false);
    }

}

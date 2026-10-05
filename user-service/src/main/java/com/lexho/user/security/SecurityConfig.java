package com.lexho.user.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.StringTokenizer;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final AntPathMatcher CSRF_PATH_MATCHER = new AntPathMatcher();

    private final Environment environment;

    public SecurityConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                // CSRF is ignored only for stateless calls API requests that present an explicit
                // Authorization: Bearer token and arrive without ambient cookies. Spring Security
                // does not persist an HttpSession, and CORS credentials are disabled for
                // cross-origin API calls.
                .csrf(csrf -> csrf.ignoringRequestMatchers(csrfExemptRequestMatcher()))
                .cors(Customizer.withDefaults())
                .securityContext(securityContext -> securityContext
                        .securityContextRepository(new NullSecurityContextRepository()))
                .requestCache(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/calls/**").hasAnyRole("CUSTOMER", "PARTNER")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
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
                "/calls/**"
        };
    }

    static RequestMatcher csrfExemptRequestMatcher() {
        return request -> isStatelessHeaderAuthenticatedPath(request)
                && hasBearerAuthorizationHeader(request)
                && hasNoAmbientCookie(request);
    }

    private static boolean isStatelessHeaderAuthenticatedPath(HttpServletRequest request) {
        String requestPath = requestPath(request);
        return requestPath != null && Arrays.stream(csrfExemptPaths())
                .anyMatch(pattern -> CSRF_PATH_MATCHER.match(pattern, requestPath));
    }

    private static String requestPath(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        if (servletPath != null && !servletPath.isBlank()) {
            return servletPath;
        }

        String requestUri = request.getRequestURI();
        if (requestUri == null || requestUri.isBlank()) {
            return null;
        }

        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }
        return requestUri;
    }

    private static boolean hasNoAmbientCookie(HttpServletRequest request) {
        String cookieHeader = request.getHeader("Cookie");
        return cookieHeader == null || cookieHeader.isBlank();
    }

    private static boolean hasBearerAuthorizationHeader(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        return authorizationHeader != null && authorizationHeader.startsWith("Bearer ");
    }

    static List<String> parseAllowedOrigins(String rawOrigins) {
        if (rawOrigins == null || rawOrigins.isBlank()) {
            return List.of();
        }

        LinkedHashSet<String> origins = new LinkedHashSet<>();
        StringTokenizer tokenizer = new StringTokenizer(rawOrigins, ",");
        while (tokenizer.hasMoreTokens()) {
            String origin = tokenizer.nextToken().trim();
            if (!origin.isEmpty()) {
                origins.add(origin);
            }
        }
        return new ArrayList<>(origins);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

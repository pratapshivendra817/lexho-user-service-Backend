package com.lexho.user_service.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SecurityConfigExecutionTest {

    @Test
    void filterChainBuildsWithEnabledPublicExposureFlags() throws Exception {
        StaticApplicationContext applicationContext = new StaticApplicationContext();
        applicationContext.getBeanFactory().registerSingleton(
                "mvcHandlerMappingIntrospector", new HandlerMappingIntrospector());

        ObjectPostProcessor<Object> objectPostProcessor = new ObjectPostProcessor<>() {
            @Override
            public <O> O postProcess(O object) {
                return object;
            }
        };
        applicationContext.getBeanFactory().registerSingleton("objectPostProcessor", objectPostProcessor);

        MockEnvironment environment = new MockEnvironment()
                .withProperty("app.security.exposure.expose-swagger", "true")
                .withProperty("app.security.exposure.allow-internal-open", "true")
                .withProperty("app.security.exposure.allow-public-customer-read", "true")
                .withProperty("app.security.exposure.allow-public-partner-onboarding", "true")
                .withProperty("app.security.exposure.allow-public-admin-profile-content", "true");

        SecurityConfig securityConfig = new SecurityConfig(
                mock(JwtFilter.class),
                mock(InternalApiTokenFilter.class),
                environment);
        AuthenticationManagerBuilder authenticationManagerBuilder =
                new AuthenticationManagerBuilder(objectPostProcessor);
        Map<Class<?>, Object> sharedObjects = new HashMap<>();
        sharedObjects.put(ApplicationContext.class, applicationContext);
        HttpSecurity httpSecurity = new HttpSecurity(objectPostProcessor, authenticationManagerBuilder, sharedObjects);

        DefaultSecurityFilterChain filterChain = (DefaultSecurityFilterChain) securityConfig.filterChain(httpSecurity);

        assertThat(filterChain).isNotNull();
        assertThat(filterChain.getFilters()).isNotEmpty();
        assertThat(filterChain.matches(new org.springframework.mock.web.MockHttpServletRequest("GET", "/api/customer/services")))
                .isTrue();
    }
}

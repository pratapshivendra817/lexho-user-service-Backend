package com.lexho.user_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    private final String uploadBaseDir;

    public StaticResourceConfig(@Value("${app.upload.base-dir:uploads}") String uploadBaseDir) {
        this.uploadBaseDir = uploadBaseDir;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(uploadBaseDir).toAbsolutePath().normalize();
        Path moduleUploadPath = Paths.get("application-module", uploadBaseDir).toAbsolutePath().normalize();
        registry.addResourceHandler("/images/**")
                .addResourceLocations(uploadPath.toUri().toString(), moduleUploadPath.toUri().toString());
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath.toUri().toString(), moduleUploadPath.toUri().toString());
    }
}

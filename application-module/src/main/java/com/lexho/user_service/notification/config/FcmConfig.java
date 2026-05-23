package com.lexho.user_service.notification.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Base64;

@Configuration
@EnableConfigurationProperties(FcmProperties.class)
public class FcmConfig {

    private static final Logger log = LoggerFactory.getLogger(FcmConfig.class);

    @Bean
    public FirebaseMessaging firebaseMessaging(FcmProperties properties) {
        if (!properties.isEnabled()) {
            log.info("FCM disabled by configuration");
            return null;
        }
        try {
            InputStream credentialsStream = resolveCredentialStream(properties);
            if (credentialsStream == null) {
                log.warn("FCM enabled but no service account credentials configured");
                return null;
            }
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credentialsStream))
                    .build();
            FirebaseApp app;
            synchronized (FcmConfig.class) {
                if (FirebaseApp.getApps().isEmpty()) {
                    app = FirebaseApp.initializeApp(options, "lexho-fcm");
                } else {
                    app = FirebaseApp.getApps().stream()
                            .filter(existing -> "lexho-fcm".equals(existing.getName()))
                            .findFirst()
                            .orElseGet(() -> FirebaseApp.initializeApp(options, "lexho-fcm"));
                }
            }
            log.info("FCM initialized successfully");
            return FirebaseMessaging.getInstance(app);
        } catch (Exception ex) {
            log.error("Failed to initialize FCM. Push notifications will be skipped.", ex);
            return null;
        }
    }

    private InputStream resolveCredentialStream(FcmProperties properties) throws Exception {
        if (properties.getServiceAccountBase64() != null && !properties.getServiceAccountBase64().isBlank()) {
            byte[] decoded = Base64.getDecoder().decode(properties.getServiceAccountBase64().trim());
            return new ByteArrayInputStream(decoded);
        }
        if (properties.getServiceAccountPath() != null && !properties.getServiceAccountPath().isBlank()) {
            return new FileInputStream(properties.getServiceAccountPath().trim());
        }
        return null;
    }
}


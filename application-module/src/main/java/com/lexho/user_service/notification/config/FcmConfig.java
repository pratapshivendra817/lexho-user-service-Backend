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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
@EnableConfigurationProperties(FcmProperties.class)
public class FcmConfig {

    private static final Logger log = LoggerFactory.getLogger(FcmConfig.class);
    private static final Pattern PROJECT_ID_PATTERN = Pattern.compile("\"project_id\"\\s*:\\s*\"([^\"]+)\"");
    private static final String SERVICE_ACCOUNT_BASE64_ENV = "APP_PUSH_FCM_SERVICE_ACCOUNT_BASE64";
    private static final String SERVICE_ACCOUNT_PATH_ENV = "APP_PUSH_FCM_SERVICE_ACCOUNT_PATH";

    @Bean
    public FirebaseMessaging firebaseMessaging(FcmProperties properties) {
        if (!properties.isEnabled()) {
            log.info("FCM disabled by configuration");
            return null;
        }
        try {
            CredentialSource credentialSource = resolveCredentialSource(properties);
            if (credentialSource == null) {
                log.warn("FCM enabled but no service account credentials configured");
                return null;
            }
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(new ByteArrayInputStream(credentialSource.content())))
                    .setProjectId(credentialSource.projectId())
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
            log.info("FCM initialized successfully. projectId={}", credentialSource.projectId());
            return FirebaseMessaging.getInstance(app);
        } catch (Exception ex) {
            log.error("Failed to initialize FCM. Push notifications will be skipped.", ex);
            return null;
        }
    }

    private CredentialSource resolveCredentialSource(FcmProperties properties) throws Exception {
        String serviceAccountBase64 = firstNonBlank(dotenvValue(SERVICE_ACCOUNT_BASE64_ENV),
                System.getenv(SERVICE_ACCOUNT_BASE64_ENV), properties.getServiceAccountBase64());
        if (serviceAccountBase64 != null) {
            byte[] decoded = Base64.getDecoder().decode(serviceAccountBase64.trim());
            return new CredentialSource(decoded, extractProjectId(decoded));
        }

        Set<String> candidatePaths = new LinkedHashSet<>();
        addIfNotBlank(candidatePaths, dotenvValue(SERVICE_ACCOUNT_PATH_ENV));
        addIfNotBlank(candidatePaths, System.getenv(SERVICE_ACCOUNT_PATH_ENV));
        addIfNotBlank(candidatePaths, properties.getServiceAccountPath());
        for (String serviceAccountPath : candidatePaths) {
            Path path = Path.of(serviceAccountPath.trim());
            if (Files.exists(path)) {
                byte[] content = Files.readAllBytes(path);
                return new CredentialSource(content, extractProjectId(content));
            }
        }

        if (!candidatePaths.isEmpty()) {
            throw new java.io.FileNotFoundException("Firebase service account file not found. Checked paths: "
                    + String.join(", ", candidatePaths));
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String dotenvValue(String key) {
        Path directory = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath();
        while (directory != null) {
            Path dotenv = directory.resolve(".env");
            if (Files.isRegularFile(dotenv)) {
                String value = readDotenvValue(dotenv, key);
                if (value != null) {
                    return value;
                }
            }
            directory = directory.getParent();
        }
        return null;
    }

    private String readDotenvValue(Path dotenv, String key) {
        try {
            List<String> lines = Files.readAllLines(dotenv, StandardCharsets.UTF_8);
            String prefix = key + "=";
            for (String line : lines) {
                String trimmed = line.trim();
                if (!trimmed.startsWith("#") && trimmed.startsWith(prefix)) {
                    return trimmed.substring(prefix.length()).trim();
                }
            }
        } catch (IOException ex) {
            log.warn("Unable to read FCM .env file at {}", dotenv);
        }
        return null;
    }

    private void addIfNotBlank(Set<String> values, String value) {
        if (value != null && !value.isBlank()) {
            values.add(value.trim());
        }
    }

    private String extractProjectId(byte[] content) {
        String json = new String(content, StandardCharsets.UTF_8);
        Matcher matcher = PROJECT_ID_PATTERN.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private record CredentialSource(byte[] content, String projectId) {
    }
}

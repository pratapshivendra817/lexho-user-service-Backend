package com.lexho.user_service.notification.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class FcmConfigTest {

    private final FcmConfig config = new FcmConfig();

    @Test
    void disabledAndInvalidCredentialsReturnNoMessagingClient() {
        FcmProperties properties = new FcmProperties();

        assertThat(config.firebaseMessaging(properties)).isNull();

        properties.setEnabled(true);
        properties.setServiceAccountBase64("not-base64");
        assertThat(config.firebaseMessaging(properties)).isNull();
    }

    @Test
    void resolvesBase64CredentialAndExtractsProjectId() {
        String json = "{\"type\":\"service_account\",\"project_id\":\"lexho-test\"}";
        FcmProperties properties = new FcmProperties();
        properties.setServiceAccountBase64(Base64.getEncoder()
                .encodeToString(json.getBytes(StandardCharsets.UTF_8)));

        Object source = ReflectionTestUtils.invokeMethod(config, "resolveCredentialSource", properties);

        assertThat((byte[]) ReflectionTestUtils.getField(source, "content"))
                .isEqualTo(json.getBytes(StandardCharsets.UTF_8));
        assertThat(ReflectionTestUtils.getField(source, "projectId")).isEqualTo("lexho-test");
    }

    @Test
    void dotenvReaderIgnoresCommentsAndReturnsConfiguredValue(@TempDir Path tempDir) throws Exception {
        Path dotenv = tempDir.resolve(".env");
        Files.writeString(dotenv, String.join(System.lineSeparator(),
                "# APP_PUSH_FCM_SERVICE_ACCOUNT_PATH=ignored.json",
                "OTHER=value",
                "APP_PUSH_FCM_SERVICE_ACCOUNT_PATH= firebase.json "));

        assertThat((String) ReflectionTestUtils.invokeMethod(
                config, "readDotenvValue", dotenv, "APP_PUSH_FCM_SERVICE_ACCOUNT_PATH"))
                .isEqualTo("firebase.json");
        assertThat((String) ReflectionTestUtils.invokeMethod(config, "readDotenvValue", dotenv, "MISSING"))
                .isNull();
    }

    @Test
    void helperMethodsCoverBlankSelectionSetDeduplicationAndMissingProjectId() {
        assertThat((String) ReflectionTestUtils.invokeMethod(
                config, "firstNonBlank", (Object) new String[]{null, " ", " value "}))
                .isEqualTo(" value ");
        assertThat((String) ReflectionTestUtils.invokeMethod(
                config, "firstNonBlank", (Object) new String[]{null, " "}))
                .isNull();

        Set<String> values = new LinkedHashSet<>();
        ReflectionTestUtils.invokeMethod(config, "addIfNotBlank", values, null);
        ReflectionTestUtils.invokeMethod(config, "addIfNotBlank", values, " ");
        ReflectionTestUtils.invokeMethod(config, "addIfNotBlank", values, " path ");
        ReflectionTestUtils.invokeMethod(config, "addIfNotBlank", values, "path");
        assertThat(values).containsExactly("path");

        assertThat((String) ReflectionTestUtils.invokeMethod(
                config, "extractProjectId", "{}".getBytes(StandardCharsets.UTF_8)))
                .isNull();
    }

    @Test
    void fcmPropertiesExposeConfiguredValues() {
        FcmProperties properties = new FcmProperties();

        properties.setEnabled(true);
        properties.setServiceAccountPath("firebase.json");
        properties.setServiceAccountBase64("encoded");

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getServiceAccountPath()).isEqualTo("firebase.json");
        assertThat(properties.getServiceAccountBase64()).isEqualTo("encoded");
    }

    @Test
    void readDotenvValueReturnsNullWhenPathCannotBeRead(@TempDir Path tempDir) throws Exception {
        Path directory = Files.createDirectory(tempDir.resolve("folder"));

        assertThat((String) ReflectionTestUtils.invokeMethod(
                config, "readDotenvValue", directory, "APP_PUSH_FCM_SERVICE_ACCOUNT_PATH"))
                .isNull();
    }
}

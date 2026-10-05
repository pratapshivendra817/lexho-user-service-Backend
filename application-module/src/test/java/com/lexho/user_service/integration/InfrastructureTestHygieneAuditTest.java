package com.lexho.user_service.integration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class InfrastructureTestHygieneAuditTest {

    @Test
    void dbTestsDoNotRequireCommittedLocalMysqlOrDockerHarnesses() throws Exception {
        List<Path> offenders;
        try (Stream<Path> stream = Files.walk(Path.of("src", "test", "java"))) {
            offenders = stream
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(this::usesCommittedDbHarnessOrFallbackPassword)
                    .toList();
        }

        assertThat(offenders).isEmpty();
    }

    private boolean usesCommittedDbHarnessOrFallbackPassword(Path path) {
        String fileName = path.getFileName().toString();
        if (fileName.equals("InfrastructureTestHygieneAuditTest.java")
                || fileName.equals("InfrastructureSupportTest.java")
                || fileName.equals("LexhoInfrastructureTest.java")
                || fileName.equals("LocalMySqlConnectivityIT.java")
                || fileName.equals("LocalRedisConnectivityIT.java")
                || fileName.equals("LexhoMySqlDataJpaTest.java")) {
            return false;
        }
        String source = read(path);
        boolean usesLocalMysql = source.contains("jdbc:mysql://localhost:3306/");
        boolean usesTestcontainers = source.contains("Testcontainers")
                || source.contains("MySQLContainer")
                || source.contains("@Container");
        boolean hasInfraTag = source.contains("@Tag(\"infra-it\")");
        boolean hasCommittedFallbackPassword = source.contains("change-me");
        return hasCommittedFallbackPassword || usesLocalMysql || usesTestcontainers || hasInfraTag;
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read test source " + path, ex);
        }
    }
}

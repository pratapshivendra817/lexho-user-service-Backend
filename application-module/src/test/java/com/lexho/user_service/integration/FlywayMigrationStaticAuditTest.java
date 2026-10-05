package com.lexho.user_service.integration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationStaticAuditTest {

    private static final Pattern VERSIONED_MIGRATION =
            Pattern.compile("^V(?<version>\\d+)__.+\\.sql$");
    private static final Pattern UNSAFE_OPERATION =
            Pattern.compile("\\b(DROP\\s+TABLE|TRUNCATE\\s+TABLE|DELETE\\s+FROM)\\b", Pattern.CASE_INSENSITIVE);
    private static final Set<String> LEGACY_UNSAFE_MIGRATIONS = Set.of(
            "V119__partner_app_localization.sql",
            "V145__remove_obsolete_provider_onboarding_tables.sql"
    );

    @Test
    void migrationVersionsAreUniqueAndSortable() throws Exception {
        List<Integer> versions = migrationFiles().stream()
                .map(path -> extractVersion(path.getFileName().toString()))
                .sorted()
                .toList();

        Set<Integer> uniqueVersions = versions.stream().collect(Collectors.toSet());
        assertThat(uniqueVersions).hasSameSizeAs(versions);
        assertThat(versions).isSorted();
    }

    @Test
    void migrationsDoNotContainUnsafeTableResetStatements() throws Exception {
        List<Path> offenders = migrationFiles().stream()
                .filter(path -> !LEGACY_UNSAFE_MIGRATIONS.contains(path.getFileName().toString()))
                .filter(this::containsUnsafeOperation)
                .toList();

        assertThat(offenders)
                .as("Unsafe destructive statements should not exist in Flyway migrations")
                .isEmpty();
    }

    private List<Path> migrationFiles() throws IOException {
        try (var stream = Files.list(Path.of("src", "main", "resources", "db", "migration", "application-module"))) {
            return stream
                    .filter(path -> VERSIONED_MIGRATION.matcher(path.getFileName().toString()).matches())
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        }
    }

    private int extractVersion(String fileName) {
        Matcher matcher = VERSIONED_MIGRATION.matcher(fileName);
        assertThat(matcher.matches()).isTrue();
        return Integer.parseInt(matcher.group("version"));
    }

    private boolean containsUnsafeOperation(Path migrationPath) {
        try {
            String sql = Files.readString(migrationPath);
            return UNSAFE_OPERATION.matcher(sql).find();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read migration " + migrationPath, ex);
        }
    }
}

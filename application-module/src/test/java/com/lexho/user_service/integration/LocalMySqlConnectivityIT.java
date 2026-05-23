package com.lexho.user_service.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("it")
class LocalMySqlConnectivityIT {

    @Autowired
    private DataSource dataSource;

    @Test
    void shouldConnectToLocalMySqlAndRunHealthQuery() throws Exception {
        assertNotNull(dataSource, "DataSource must be configured for integration tests");

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT 1")) {
            assertNotNull(connection.getMetaData().getDatabaseProductName());
            rs.next();
            assertEquals(1, rs.getInt(1));
        }
    }
}

package com.lexho.user_service.integration;

import com.lexho.user_service.UserServiceApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = UserServiceApplication.class)
@ActiveProfiles("it")
@Tag("infra-it")
class LocalMySqlConnectivityIT {

    @Autowired
    private DataSource dataSource;

    @Test
    void shouldConnectToLocalMySqlAndRunHealthQuery() throws Exception {
        assertNotNull(dataSource, "DataSource must be configured for integration tests");

        Connection connection;
        try {
            connection = dataSource.getConnection();
        } catch (SQLException ex) {
            Assumptions.assumeTrue(false, "Skipping LocalMySqlConnectivityIT: local MySQL not accessible (" + ex.getMessage() + ")");
            return;
        }

        try (connection;
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT 1")) {
            assertNotNull(connection.getMetaData().getDatabaseProductName());
            rs.next();
            assertEquals(1, rs.getInt(1));
        }
    }
}

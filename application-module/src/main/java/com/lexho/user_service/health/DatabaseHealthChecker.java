package com.lexho.user_service.health;

import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

public final class DatabaseHealthChecker {

    private DatabaseHealthChecker() {
    }

    public static String check(DataSource dataSource) {
        Connection connection = null;
        try {
            connection = DataSourceUtils.getConnection(dataSource);
            try (Statement statement = connection.createStatement()) {
                statement.execute("SELECT 1");
            }
            return "UP";
        } catch (Exception exception) {
            return "DOWN";
        } finally {
            if (connection != null) {
                DataSourceUtils.releaseConnection(connection, dataSource);
            }
        }
    }
}

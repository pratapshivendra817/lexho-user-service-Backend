package com.lexho.user.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "socket.io.enabled=false",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "security.jwt.secret=12345678901234567890123456789012"
})
@ActiveProfiles("it")
@EnabledIfSystemProperty(named = "lexho.it.mysql.user-service.local", matches = "true")
class CallControllerLocalMySqlIT {

    @Autowired
    private DataSource dataSource;

    @Test
    void contextLoadsWithLocalMySql() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getURL()).startsWith("jdbc:mysql://");
        }
    }
}

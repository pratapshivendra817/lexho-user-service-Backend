package com.lexho.user;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.mockito.Mockito.mockStatic;

class UserServiceApplicationTest {

    @Test
    void mainDelegatesToSpringApplication() {
        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            UserServiceApplication.main(new String[]{"--test"});

            springApplication.verify(() -> SpringApplication.run(UserServiceApplication.class, new String[]{"--test"}));
        }
    }
}

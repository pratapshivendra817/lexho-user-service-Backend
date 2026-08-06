package com.lexho.user_service.admin;

import com.lexho.user_service.admin.controller.AdminMonitoringController;
import com.lexho.user_service.admin.service.AdminMonitoringService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminMonitoringControllerTest {

    @Mock
    private AdminMonitoringService adminMonitoringService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new AdminMonitoringController(adminMonitoringService)).build();
    }

    @Test
    void returnsMonitoringDashboardOnVersionedEndpoint() throws Exception {
        when(adminMonitoringService.dashboard()).thenReturn(Map.of(
                "status", "UP",
                "apiHealth", "UP",
                "redisHealth", "UP",
                "databaseHealth", "UP",
                "totalUsers", 25L,
                "apiRequestCount", 100L));

        mockMvc.perform(get("/api/v1/admin/monitoring/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.apiHealth").value("UP"))
                .andExpect(jsonPath("$.data.redisHealth").value("UP"))
                .andExpect(jsonPath("$.data.databaseHealth").value("UP"))
                .andExpect(jsonPath("$.data.totalUsers").value(25))
                .andExpect(jsonPath("$.data.apiRequestCount").value(100));
    }

    @Test
    void supportsLegacyAdminEndpoint() throws Exception {
        when(adminMonitoringService.dashboard()).thenReturn(Map.of("status", "UP"));

        mockMvc.perform(get("/api/admin/monitoring/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UP"));
    }
}

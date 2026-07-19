package com.lexho.user.controller;

import com.lexho.common.enums.Role;
import com.lexho.user.dto.AdminUserResponse;
import com.lexho.user.entity.User;
import com.lexho.user.enums.UserStatus;
import com.lexho.user.exception.ForbiddenException;
import com.lexho.user.mapper.UserManagementMapper;
import com.lexho.user.service.UserManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ControllerAndMapperAdditionalTest {

    @Mock
    private UserManagementService userManagementService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminUserManagementController(userManagementService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void blockAndUnblockEndpointsAreCovered() throws Exception {
        when(userManagementService.blockUser(1L)).thenReturn(response(1L, "BLOCKED"));
        when(userManagementService.unblockUser(1L)).thenReturn(response(1L, "ACTIVE"));

        mockMvc.perform(put("/api/admin/users/{id}/block", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));

        mockMvc.perform(put("/api/admin/users/{id}/unblock", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void forbiddenExceptionIsMappedByAdvice() throws Exception {
        when(userManagementService.unblockUser(2L)).thenThrow(new ForbiddenException("Denied"));

        mockMvc.perform(put("/api/admin/users/{id}/unblock", 2L))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Denied"));
    }

    @Test
    void mapperCoversPartnerAndDefaultRoleBranches() {
        UserManagementMapper mapper = new UserManagementMapper();
        User partner = user(Role.PARTNER);
        User customer = user(Role.CUSTOMER);

        assertThat(mapper.toResponse(partner).getRole()).isEqualTo("PARTNER");
        assertThat(mapper.toResponse(customer).getRole()).isEqualTo("CUSTOMER");
        assertThat(mapper.toDetailResponse(customer, null).getFullName()).isEqualTo("User");
    }

    private static AdminUserResponse response(Long id, String status) {
        AdminUserResponse response = new AdminUserResponse();
        response.setId(id);
        response.setStatus(status);
        return response;
    }

    private static User user(Role role) {
        User user = new User();
        user.setFullName("User");
        user.setPhoneNumber("9999999999");
        user.setPassword("secret");
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setTotalBookings(0L);
        return user;
    }
}

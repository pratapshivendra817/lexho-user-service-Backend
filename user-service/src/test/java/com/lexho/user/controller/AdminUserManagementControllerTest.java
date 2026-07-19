package com.lexho.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.user.dto.AdminUserCreateRequest;
import com.lexho.user.dto.AdminUserDetailResponse;
import com.lexho.user.dto.AdminUserResponse;
import com.lexho.user.dto.AdminUserUpdateRequest;
import com.lexho.user.dto.PageResponse;
import com.lexho.user.exception.ConflictException;
import com.lexho.user.exception.ResourceNotFoundException;
import com.lexho.user.exception.TooManyRequestsException;
import com.lexho.user.service.UserManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminUserManagementControllerTest {

    @Mock
    private UserManagementService userManagementService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AdminUserManagementController(userManagementService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Test
    void getAllUsersReturnsPageResponse() throws Exception {
        AdminUserResponse user = response(1L, "Alice", "CUSTOMER");
        when(userManagementService.getAllUsers(1, 20, "fullName", "asc",
                "ali", "Alice", "9999999999", "alice@test.com", "CUSTOMER", "ACTIVE", "Pune"))
                .thenReturn(new PageResponse<>(List.of(user), 1L, 1, 20));

        mockMvc.perform(get("/api/admin/users")
                        .param("page", "1")
                        .param("size", "20")
                        .param("sortBy", "fullName")
                        .param("sortDir", "asc")
                        .param("search", "ali")
                        .param("name", "Alice")
                        .param("phone", "9999999999")
                        .param("email", "alice@test.com")
                        .param("role", "CUSTOMER")
                        .param("status", "ACTIVE")
                        .param("city", "Pune"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].fullName").value("Alice"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void addUserReturnsCreatedPayload() throws Exception {
        AdminUserCreateRequest request = validCreateRequest();
        when(userManagementService.addUser(any(AdminUserCreateRequest.class)))
                .thenReturn(response(2L, "Partner One", "PARTNER"));

        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.role").value("PARTNER"));
    }

    @Test
    void addUserReturnsBadRequestForValidationFailure() throws Exception {
        AdminUserCreateRequest request = new AdminUserCreateRequest();
        request.setFullName(" ");
        request.setPhoneNumber("12");
        request.setPassword("short");
        request.setRole("");

        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid request"));
    }

    @Test
    void updateUserDelegatesToService() throws Exception {
        AdminUserUpdateRequest request = new AdminUserUpdateRequest();
        request.setFullName("Updated User");
        request.setPhoneNumber("9999999999");
        request.setRole("CUSTOMER");
        when(userManagementService.updateUser(eq(5L), any(AdminUserUpdateRequest.class)))
                .thenReturn(response(5L, "Updated User", "CUSTOMER"));

        mockMvc.perform(put("/api/admin/users/{id}", 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.fullName").value("Updated User"));
    }

    @Test
    void controllerAdviceMapsConflictNotFoundAndRateLimitErrors() throws Exception {
        when(userManagementService.getUserDetails(9L))
                .thenThrow(new ResourceNotFoundException("User missing"));
        doThrow(new ConflictException("Already exists"))
                .when(userManagementService).softDeleteUser(10L);
        when(userManagementService.blockUser(11L))
                .thenThrow(new TooManyRequestsException("Slow down", 42));

        mockMvc.perform(get("/api/admin/users/{id}", 9L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("User missing"));

        mockMvc.perform(delete("/api/admin/users/{id}", 10L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Already exists"));

        mockMvc.perform(put("/api/admin/users/{id}/block", 11L))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "42"))
                .andExpect(jsonPath("$.message").value("Slow down"));
    }

    @Test
    void softDeleteUserReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/admin/users/{id}", 15L))
                .andExpect(status().isNoContent());

        verify(userManagementService).softDeleteUser(15L);
    }

    @Test
    void getUserDetailsReturnsPayload() throws Exception {
        AdminUserDetailResponse detail = new AdminUserDetailResponse();
        detail.setId(21L);
        detail.setFullName("Detailed User");
        when(userManagementService.getUserDetails(21L)).thenReturn(detail);

        mockMvc.perform(get("/api/admin/users/{id}", 21L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(21))
                .andExpect(jsonPath("$.fullName").value("Detailed User"));
    }

    private static AdminUserCreateRequest validCreateRequest() {
        AdminUserCreateRequest request = new AdminUserCreateRequest();
        request.setFullName("Partner One");
        request.setEmail("partner@test.com");
        request.setPhoneNumber("9999999999");
        request.setPassword("password123");
        request.setRole("PARTNER");
        return request;
    }

    private static AdminUserResponse response(Long id, String name, String role) {
        AdminUserResponse response = new AdminUserResponse();
        response.setId(id);
        response.setFullName(name);
        response.setPhoneNumber("9999999999");
        response.setRole(role);
        response.setStatus("ACTIVE");
        return response;
    }
}

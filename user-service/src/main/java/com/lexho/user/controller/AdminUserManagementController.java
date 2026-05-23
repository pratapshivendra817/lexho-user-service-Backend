package com.lexho.user.controller;

import com.lexho.user.dto.AdminUserCreateRequest;
import com.lexho.user.dto.AdminUserDetailResponse;
import com.lexho.user.dto.AdminUserResponse;
import com.lexho.user.dto.AdminUserUpdateRequest;
import com.lexho.user.dto.PageResponse;
import com.lexho.user.service.UserManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/admin/users", "/api/admin/customer-profiles"})
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin User Management", description = "Admin APIs to manage customers/partners")
public class AdminUserManagementController {

    private final UserManagementService userManagementService;

    public AdminUserManagementController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping
    @Operation(summary = "Get all users with pagination, sorting, search and filters")
    public ResponseEntity<PageResponse<AdminUserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String city) {
        return ResponseEntity.ok(userManagementService.getAllUsers(
                page, size, sortBy, sortDir, search, name, phone, email, role, status, city
        ));
    }

    @PostMapping
    @Operation(summary = "Add a new user (CUSTOMER or PARTNER)")
    public ResponseEntity<AdminUserResponse> addUser(@Valid @RequestBody AdminUserCreateRequest request) {
        return ResponseEntity.ok(userManagementService.addUser(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user details")
    public ResponseEntity<AdminUserResponse> updateUser(@PathVariable Long id,
                                                        @Valid @RequestBody AdminUserUpdateRequest request) {
        return ResponseEntity.ok(userManagementService.updateUser(id, request));
    }

    @PutMapping("/{id}/block")
    @Operation(summary = "Block a user")
    public ResponseEntity<AdminUserResponse> blockUser(@PathVariable Long id) {
        return ResponseEntity.ok(userManagementService.blockUser(id));
    }

    @PutMapping("/{id}/unblock")
    @Operation(summary = "Unblock a user")
    public ResponseEntity<AdminUserResponse> unblockUser(@PathVariable Long id) {
        return ResponseEntity.ok(userManagementService.unblockUser(id));
    }

    @GetMapping("/{id}")
    @Operation(summary = "View user details with booking summary")
    public ResponseEntity<AdminUserDetailResponse> getUserDetails(@PathVariable Long id) {
        return ResponseEntity.ok(userManagementService.getUserDetails(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete user")
    public ResponseEntity<Void> softDeleteUser(@PathVariable Long id) {
        userManagementService.softDeleteUser(id);
        return ResponseEntity.noContent().build();
    }
}

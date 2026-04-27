package com.lexho.user.controller;

import com.lexho.user.dto.*;
import com.lexho.user.service.UserManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@Tag(name = "Admin User Management", description = "Admin APIs to manage customers/providers")
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
    @Operation(summary = "Add a new user (CUSTOMER or PROVIDER)")
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

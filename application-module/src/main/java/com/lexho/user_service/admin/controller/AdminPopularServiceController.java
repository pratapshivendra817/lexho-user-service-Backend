package com.lexho.user_service.admin.controller;

import com.lexho.user_service.admin.dto.AdminMessageResponse;
import com.lexho.user_service.admin.dto.DataResponse;
import com.lexho.user_service.admin.dto.PopularServiceResponse;
import com.lexho.user_service.admin.dto.PopularServiceUpsertRequest;
import com.lexho.user_service.admin.service.PopularServiceManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/admin/popular-services", "/api/v1/admin/popular-services"})
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Popular Services", description = "Popular services management APIs")
public class AdminPopularServiceController {

    private final PopularServiceManagementService popularServiceManagementService;

    public AdminPopularServiceController(PopularServiceManagementService popularServiceManagementService) {
        this.popularServiceManagementService = popularServiceManagementService;
    }

    @PostMapping
    @Operation(summary = "Add popular service")
    public DataResponse<PopularServiceResponse> addPopularService(@Valid @RequestBody PopularServiceUpsertRequest request) {
        return popularServiceManagementService.addPopularService(request);
    }

    @GetMapping
    @Operation(summary = "Get popular services by category")
    public DataResponse<List<PopularServiceResponse>> getPopularServices(@RequestParam Long categoryId) {
        return popularServiceManagementService.getPopularServicesByCategory(categoryId);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete popular service")
    public AdminMessageResponse deletePopularService(@PathVariable Long id) {
        return popularServiceManagementService.deletePopularService(id);
    }
}

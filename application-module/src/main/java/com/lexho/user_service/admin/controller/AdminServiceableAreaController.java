package com.lexho.user_service.admin.controller;

import com.lexho.user_service.admin.serviceablearea.dto.ServiceableAreaPageResponse;
import com.lexho.user_service.admin.serviceablearea.dto.ServiceableAreaResponse;
import com.lexho.user_service.admin.serviceablearea.dto.ServiceableAreaStatusRequest;
import com.lexho.user_service.admin.serviceablearea.dto.ServiceableAreaUpsertRequest;
import com.lexho.user_service.admin.serviceablearea.service.ServiceableAreaService;
import com.lexho.user_service.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/admin/serviceable-areas", "/api/v1/admin/serviceable-areas"})
@PreAuthorize("hasRole('ADMIN')")
public class AdminServiceableAreaController {

    private final ServiceableAreaService serviceableAreaService;

    public AdminServiceableAreaController(ServiceableAreaService serviceableAreaService) {
        this.serviceableAreaService = serviceableAreaService;
    }

    @GetMapping
    public ApiResponse<ServiceableAreaPageResponse> list(@RequestParam(required = false) Integer page,
                                                          @RequestParam(required = false) Integer size,
                                                          @RequestParam(required = false) String sortBy,
                                                          @RequestParam(required = false) String sortDir) {
        return ApiResponse.success("Serviceable areas fetched",
                serviceableAreaService.list(defaultPage(page), defaultSize(size), defaultSortBy(sortBy), defaultSortDir(sortDir)));
    }

    @GetMapping("/search")
    public ApiResponse<ServiceableAreaPageResponse> search(@RequestParam(name = "q", required = false) String query,
                                                            @RequestParam(required = false) Integer page,
                                                            @RequestParam(required = false) Integer size,
                                                            @RequestParam(required = false) String sortBy,
                                                            @RequestParam(required = false) String sortDir) {
        return ApiResponse.success("Serviceable areas fetched",
                serviceableAreaService.search(
                        query,
                        defaultPage(page),
                        defaultSize(size),
                        defaultSortBy(sortBy),
                        defaultSortDir(sortDir)
                ));
    }

    @PostMapping
    public ApiResponse<ServiceableAreaResponse> create(@Valid @RequestBody ServiceableAreaUpsertRequest request) {
        return ApiResponse.success("Serviceable area created", serviceableAreaService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ServiceableAreaResponse> update(@PathVariable Long id,
                                                        @Valid @RequestBody ServiceableAreaUpsertRequest request) {
        return ApiResponse.success("Serviceable area updated", serviceableAreaService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        serviceableAreaService.delete(id);
        return ApiResponse.success("Serviceable area deleted", null);
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<ServiceableAreaResponse> status(@PathVariable Long id,
                                                        @Valid @RequestBody ServiceableAreaStatusRequest request) {
        return ApiResponse.success("Serviceable area status updated", serviceableAreaService.updateStatus(id, request));
    }

    private int defaultPage(Integer page) {
        return page == null ? 0 : page;
    }

    private int defaultSize(Integer size) {
        return size == null ? 10 : size;
    }

    private String defaultSortBy(String sortBy) {
        return (sortBy == null || sortBy.isBlank()) ? "id" : sortBy;
    }

    private String defaultSortDir(String sortDir) {
        return (sortDir == null || sortDir.isBlank()) ? "desc" : sortDir;
    }
}

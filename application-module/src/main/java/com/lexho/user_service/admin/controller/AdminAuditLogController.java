package com.lexho.user_service.admin.controller;

import com.lexho.user_service.admin.dto.DataResponse;
import com.lexho.user_service.audit.dto.AdminAuditLogListResponse;
import com.lexho.user_service.audit.dto.AdminAuditLogResponse;
import com.lexho.user_service.audit.service.AdminAuditLogQueryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/admin/audit-logs", "/api/v1/admin/audit-logs"})
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuditLogController {

    private final AdminAuditLogQueryService adminAuditLogQueryService;

    public AdminAuditLogController(AdminAuditLogQueryService adminAuditLogQueryService) {
        this.adminAuditLogQueryService = adminAuditLogQueryService;
    }

    @GetMapping
    public DataResponse<AdminAuditLogListResponse> list(@RequestParam(required = false) String startDate,
                                                        @RequestParam(required = false) String endDate,
                                                        @RequestParam(required = false) String action,
                                                        @RequestParam(required = false) String user,
                                                        @RequestParam(required = false) String role) {
        return adminAuditLogQueryService.list(startDate, endDate, action, user, role);
    }

    @GetMapping("/{id}")
    public DataResponse<AdminAuditLogResponse> detail(@PathVariable Long id) {
        return adminAuditLogQueryService.detail(id);
    }
}

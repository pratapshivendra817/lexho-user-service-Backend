package com.lexho.user_service.admin;

import com.lexho.user_service.admin.controller.AdminAuditLogController;
import com.lexho.user_service.audit.service.AdminAuditLogQueryService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AdminAuditLogControllerTest {

    private final AdminAuditLogQueryService queryService = mock(AdminAuditLogQueryService.class);
    private final AdminAuditLogController controller = new AdminAuditLogController(queryService);

    @Test
    void delegatesListAndDetail() {
        controller.list("2026-07-01", "2026-07-16", "LOGIN", "admin@lexho.com", "ADMIN");
        controller.detail(8L);

        verify(queryService).list("2026-07-01", "2026-07-16", "LOGIN", "admin@lexho.com", "ADMIN");
        verify(queryService).detail(8L);
    }
}

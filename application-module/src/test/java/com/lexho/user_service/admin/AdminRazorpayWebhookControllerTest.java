package com.lexho.user_service.admin;

import com.lexho.user_service.admin.controller.AdminRazorpayWebhookController;
import com.lexho.user_service.payment.service.AdminWebhookQueryService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AdminRazorpayWebhookControllerTest {

    private final AdminWebhookQueryService queryService = mock(AdminWebhookQueryService.class);
    private final AdminRazorpayWebhookController controller = new AdminRazorpayWebhookController(queryService);

    @Test
    void delegatesListAndDetail() {
        controller.list("pay_123", "payment.captured", "SUCCESS");
        controller.detail(15L);

        verify(queryService).list("pay_123", "payment.captured", "SUCCESS");
        verify(queryService).detail(15L);
    }
}

package com.lexho.payment.webhook;

import com.lexho.payment.dto.WebhookProcessResult;
import com.lexho.payment.exception.PaymentException;
import com.lexho.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RazorpayWebhookControllerTest {

    private final PaymentService paymentService = mock(PaymentService.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new RazorpayWebhookController(paymentService))
            .build();

    @Test
    void returnsOkForDuplicateWebhook() throws Exception {
        when(paymentService.processWebhook("evt_1", "sig", "{}"))
                .thenReturn(WebhookProcessResult.duplicate("evt_1", "payment.captured"));

        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                        .header("X-Razorpay-Event-Id", "evt_1")
                        .header("X-Razorpay-Signature", "sig")
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.duplicate").value(true));
    }

    @Test
    void returnsAcceptedForRetryScheduledWebhook() throws Exception {
        when(paymentService.processWebhook("evt_2", "sig", "{}"))
                .thenReturn(WebhookProcessResult.retryScheduled("evt_2", "payment.captured"));

        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                        .header("X-Razorpay-Event-Id", "evt_2")
                        .header("X-Razorpay-Signature", "sig")
                        .content("{}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.retryScheduled").value(true));
    }

    @Test
    void returnsUnauthorizedForInvalidSignature() throws Exception {
        when(paymentService.processWebhook("evt_3", "bad", "{}"))
                .thenThrow(new PaymentException("Invalid webhook signature"));

        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                        .header("X-Razorpay-Event-Id", "evt_3")
                        .header("X-Razorpay-Signature", "bad")
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_WEBHOOK_SIGNATURE"));
    }

    @Test
    void returnsOkForProcessedWebhookAndExposesResponseMetadata() throws Exception {
        when(paymentService.processWebhook("evt_ok", "sig", "{\"event\":\"payment.captured\"}"))
                .thenReturn(WebhookProcessResult.processed("evt_ok", "payment.captured"));

        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Event-Id", "evt_ok")
                        .header("X-Razorpay-Signature", "sig")
                        .content("{\"event\":\"payment.captured\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Webhook processed"))
                .andExpect(jsonPath("$.data.eventId").value("evt_ok"))
                .andExpect(jsonPath("$.data.eventType").value("payment.captured"))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.processed").value(true))
                .andExpect(jsonPath("$.data.retryScheduled").value(false));
    }

    @Test
    void returnsBadRequestForUnknownWebhookFailures() throws Exception {
        when(paymentService.processWebhook("evt_4", "sig", "{}"))
                .thenThrow(new PaymentException("Unsupported webhook payload"));

        mockMvc.perform(post("/api/payments/webhook/razorpay")
                        .header("X-Razorpay-Event-Id", "evt_4")
                        .header("X-Razorpay-Signature", "sig")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_WEBHOOK_REQUEST"))
                .andExpect(jsonPath("$.message").value("Unsupported webhook payload"));
    }

    @Test
    void returnsServiceUnavailableWhenWebhookSecretIsMissing() throws Exception {
        when(paymentService.processWebhook("evt_5", "sig", "{}"))
                .thenThrow(new PaymentException("Razorpay webhook secret is not configured"));

        mockMvc.perform(post("/api/payments/webhook/razorpay")
                        .header("X-Razorpay-Event-Id", "evt_5")
                        .header("X-Razorpay-Signature", "sig")
                        .content("{}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("WEBHOOK_NOT_CONFIGURED"));
    }

    @Test
    void rejectsMissingRequiredHeaders() throws Exception {
        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delegatesRawBodyAndHeadersToService() throws Exception {
        when(paymentService.processWebhook("evt_6", "sig-6", "{\"id\":1}"))
                .thenReturn(WebhookProcessResult.duplicate("evt_6", "payment.failed"));

        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Event-Id", "evt_6")
                        .header("X-Razorpay-Signature", "sig-6")
                        .content("{\"id\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.duplicate").value(true))
                .andExpect(jsonPath("$.data.eventType").value("payment.failed"));

        verify(paymentService).processWebhook("evt_6", "sig-6", "{\"id\":1}");
    }
}

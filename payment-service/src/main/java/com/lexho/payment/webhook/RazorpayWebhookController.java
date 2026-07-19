package com.lexho.payment.webhook;

import com.lexho.payment.dto.PaymentApiResponse;
import com.lexho.payment.dto.WebhookProcessResult;
import com.lexho.payment.exception.PaymentException;
import com.lexho.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "Razorpay Webhooks")
public class RazorpayWebhookController {

    private static final Logger LOG = LoggerFactory.getLogger(RazorpayWebhookController.class);

    private final PaymentService paymentService;

    public RazorpayWebhookController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping({"/api/payments/webhook/razorpay", "/api/payments/webhook", "/api/v1/webhooks/razorpay"})
    @Operation(summary = "Process Razorpay webhook events")
    public ResponseEntity<PaymentApiResponse<Map<String, Object>>> webhook(
            @RequestHeader("X-Razorpay-Event-Id") String eventId,
            @RequestHeader("X-Razorpay-Signature") String signature,
            @RequestBody String rawBody) {
        WebhookProcessResult result = paymentService.processWebhook(eventId, signature, rawBody);
        HttpStatus status = result.retryScheduled() ? HttpStatus.ACCEPTED : HttpStatus.OK;
        return ResponseEntity.status(status).body(PaymentApiResponse.success(result.message(), Map.of(
                "eventId", result.eventId(),
                "eventType", result.eventType(),
                "status", result.status(),
                "processed", result.processed(),
                "duplicate", result.duplicate(),
                "retryScheduled", result.retryScheduled()
        )));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(PaymentException.class)
    public ResponseEntity<PaymentApiResponse<Void>> handleWebhookPaymentException(PaymentException exception) {
        String message = exception.getMessage();
        LOG.warn("Razorpay webhook rejected: {}", message);
        if ("Invalid webhook signature".equals(message)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(PaymentApiResponse.failure("INVALID_WEBHOOK_SIGNATURE", message));
        }
        if (message != null && message.contains("webhook secret")) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(PaymentApiResponse.failure("WEBHOOK_NOT_CONFIGURED", message));
        }
        return ResponseEntity.badRequest()
                .body(PaymentApiResponse.failure("INVALID_WEBHOOK_REQUEST", message));
    }
}

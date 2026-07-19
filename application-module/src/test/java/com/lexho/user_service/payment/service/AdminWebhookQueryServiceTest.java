package com.lexho.user_service.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.payment.entity.PaymentWebhookEvent;
import com.lexho.payment.entity.PaymentWebhookEventStatus;
import com.lexho.payment.repository.PaymentWebhookEventRepository;
import com.lexho.user_service.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminWebhookQueryServiceTest {

    private final PaymentWebhookEventRepository repository = mock(PaymentWebhookEventRepository.class);
    private final AdminWebhookQueryService service = new AdminWebhookQueryService(repository, new ObjectMapper());

    @Test
    void listReturnsFrontendShape() {
        PaymentWebhookEvent event = webhook(3L);
        when(repository.search(eq("pay_123"), eq("payment.captured"), eq("SUCCESS"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(event)));

        var response = service.list("pay_123", "payment.captured", "SUCCESS");

        assertThat(response.getData().content()).hasSize(1);
        assertThat(response.getData().content().get(0).paymentId()).isEqualTo("pay_123");
        assertThat(response.getData().content().get(0).requestId()).isEqualTo("req-77");
    }

    @Test
    void detailReturnsPayloadAndHistory() {
        PaymentWebhookEvent event = webhook(9L);
        when(repository.findById(9L)).thenReturn(Optional.of(event));

        var response = service.detail(9L);

        assertThat(response.getData().payload()).containsEntry("entity", "event");
        assertThat(response.getData().history()).hasSize(1);
        assertThat(response.getData().retryStatus()).isEqualTo("NOT_REQUIRED");
    }

    @Test
    void detail404WhenMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.detail(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Webhook event not found");
    }

    private PaymentWebhookEvent webhook(Long id) {
        PaymentWebhookEvent event = new PaymentWebhookEvent();
        ReflectionTestUtils.setField(event, "id", id);
        event.setEventId("evt_123");
        event.setEventType("payment.captured");
        event.setRazorpayPaymentId("pay_123");
        event.setStatus(PaymentWebhookEventStatus.SUCCESS);
        event.setRetryCount(0);
        event.setRawPayload("{\"entity\":\"event\"}");
        event.setRequestId("req-77");
        event.setCorrelationId("corr-77");
        ReflectionTestUtils.setField(event, "receivedAt", LocalDateTime.of(2026, 7, 16, 12, 30));
        ReflectionTestUtils.setField(event, "processedAt", LocalDateTime.of(2026, 7, 16, 12, 31));
        ReflectionTestUtils.setField(event, "updatedAt", LocalDateTime.of(2026, 7, 16, 12, 31));
        return event;
    }
}

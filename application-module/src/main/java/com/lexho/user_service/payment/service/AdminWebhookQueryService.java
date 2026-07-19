package com.lexho.user_service.payment.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.payment.entity.PaymentWebhookEvent;
import com.lexho.payment.entity.PaymentWebhookEventStatus;
import com.lexho.payment.repository.PaymentWebhookEventRepository;
import com.lexho.user_service.admin.dto.DataResponse;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.payment.dto.AdminWebhookEventListResponse;
import com.lexho.user_service.payment.dto.AdminWebhookEventResponse;
import com.lexho.user_service.payment.dto.AdminWebhookHistoryResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class AdminWebhookQueryService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final PaymentWebhookEventRepository paymentWebhookEventRepository;
    private final ObjectMapper objectMapper;

    public AdminWebhookQueryService(PaymentWebhookEventRepository paymentWebhookEventRepository,
                                    ObjectMapper objectMapper) {
        this.paymentWebhookEventRepository = paymentWebhookEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public DataResponse<AdminWebhookEventListResponse> list(String search, String eventType, String status) {
        List<AdminWebhookEventResponse> content = paymentWebhookEventRepository.search(
                        trimToNull(search),
                        trimToNull(eventType),
                        trimToNull(status),
                        PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "updatedAt")))
                .getContent()
                .stream()
                .map(this::toListResponse)
                .toList();
        return new DataResponse<>(new AdminWebhookEventListResponse(content));
    }

    @Transactional(readOnly = true)
    public DataResponse<AdminWebhookEventResponse> detail(Long id) {
        PaymentWebhookEvent event = paymentWebhookEventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Webhook event not found"));
        return new DataResponse<>(toDetailResponse(event));
    }

    private AdminWebhookEventResponse toListResponse(PaymentWebhookEvent event) {
        return new AdminWebhookEventResponse(
                String.valueOf(event.getId()),
                event.getEventId(),
                String.valueOf(event.getId()),
                event.getEventType(),
                event.getEventType(),
                event.getRazorpayPaymentId(),
                event.getRazorpayPaymentId(),
                event.getStatus().name(),
                event.getStatus().name(),
                firstNonNull(event.getProcessedAt(), event.getUpdatedAt(), event.getReceivedAt()),
                event.getUpdatedAt(),
                event.getRetryCount(),
                event.getRetryCount(),
                event.getRequestId(),
                event.getCorrelationId(),
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private AdminWebhookEventResponse toDetailResponse(PaymentWebhookEvent event) {
        Map<String, Object> payload = parsePayload(event.getRawPayload());
        List<AdminWebhookHistoryResponse> history = List.of(new AdminWebhookHistoryResponse(
                event.getId() + "-1",
                event.getStatus().name(),
                historyMessage(event),
                event.getFailureReason(),
                firstNonNull(event.getProcessedAt(), event.getUpdatedAt(), event.getReceivedAt()),
                event.getReceivedAt()
        ));
        return new AdminWebhookEventResponse(
                String.valueOf(event.getId()),
                event.getEventId(),
                String.valueOf(event.getId()),
                event.getEventType(),
                event.getEventType(),
                event.getRazorpayPaymentId(),
                event.getRazorpayPaymentId(),
                event.getStatus().name(),
                event.getStatus().name(),
                firstNonNull(event.getProcessedAt(), event.getUpdatedAt(), event.getReceivedAt()),
                event.getUpdatedAt(),
                event.getRetryCount(),
                event.getRetryCount(),
                event.getRequestId(),
                event.getCorrelationId(),
                payload,
                payload,
                event.getFailureReason(),
                event.getFailureReason(),
                retryStatus(event),
                history,
                history
        );
    }

    private String retryStatus(PaymentWebhookEvent event) {
        if (event.getStatus() == PaymentWebhookEventStatus.SUCCESS) {
            return "NOT_REQUIRED";
        }
        if (event.getStatus() == PaymentWebhookEventStatus.DEAD_LETTER) {
            return "DEAD_LETTER";
        }
        if (event.getNextRetryAt() != null) {
            return "SCHEDULED";
        }
        return event.getRetryCount() > 0 ? "RETRYING" : "NOT_REQUIRED";
    }

    private String historyMessage(PaymentWebhookEvent event) {
        if (event.getFailureReason() != null && !event.getFailureReason().isBlank()) {
            return event.getFailureReason().trim();
        }
        if (event.getStatus() == PaymentWebhookEventStatus.SUCCESS) {
            return "Processed successfully";
        }
        if (event.getStatus() == PaymentWebhookEventStatus.DEAD_LETTER) {
            return "Moved to dead letter";
        }
        if (event.getNextRetryAt() != null) {
            return "Retry scheduled";
        }
        return "Webhook received";
    }

    private Map<String, Object> parsePayload(String rawPayload) {
        String normalized = trimToNull(rawPayload);
        if (normalized == null) {
            return null;
        }
        try {
            return objectMapper.readValue(normalized, MAP_TYPE);
        } catch (Exception ignored) {
            return Map.of("raw", normalized);
        }
    }

    private LocalDateTime firstNonNull(LocalDateTime first, LocalDateTime second, LocalDateTime third) {
        if (first != null) {
            return first;
        }
        if (second != null) {
            return second;
        }
        return third;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}

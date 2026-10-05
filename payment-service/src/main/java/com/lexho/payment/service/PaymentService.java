package com.lexho.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.booking.entity.Order;
import com.lexho.booking.repository.OrderRepository;
import com.lexho.booking.service.OrderLifecycleService;
import com.lexho.common.enums.OrderStatus;
import com.lexho.common.enums.PaymentStatus;
import com.lexho.payment.config.RazorpayProperties;
import com.lexho.payment.dto.CreatePaymentOrderResponse;
import com.lexho.payment.dto.PaymentResponse;
import com.lexho.payment.dto.VerifyPaymentRequest;
import com.lexho.payment.dto.WebhookProcessResult;
import com.lexho.payment.entity.PaymentTransaction;
import com.lexho.payment.entity.PaymentTransactionStatus;
import com.lexho.payment.entity.PaymentRefund;
import com.lexho.payment.entity.PaymentRefundStatus;
import com.lexho.payment.entity.PaymentWebhookEvent;
import com.lexho.payment.entity.PaymentWebhookEventStatus;
import com.lexho.payment.event.PaymentCapturedEvent;
import com.lexho.payment.event.PaymentFailedEvent;
import com.lexho.payment.event.PaymentRefundProcessedEvent;
import com.lexho.payment.exception.PaymentException;
import com.lexho.payment.repository.PaymentRefundRepository;
import com.lexho.payment.repository.PaymentTransactionRepository;
import com.lexho.payment.repository.PaymentWebhookEventRepository;
import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class PaymentService {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentService.class);
    private static final String BOOKING_NOT_FOUND = "Booking not found";
    private static final String PAYMENT_TRANSACTION_NOT_FOUND = "Payment transaction not found";
    private static final String WEBHOOK_ENTITY = "entity";
    private static final String WEBHOOK_EVENT = "event";
    private static final String WEBHOOK_ORDER_ID = "order_id";
    private static final String WEBHOOK_PAYMENT = "payment";
    private static final String WEBHOOK_PAYMENT_ID = "payment_id";
    private static final String WEBHOOK_PAYLOAD = "payload";
    private static final String WEBHOOK_REFUND = "refund";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_PENDING = "PENDING";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";
    private static final String REQUEST_ID_MDC_KEY = "requestId";
    private static final List<Duration> WEBHOOK_RETRY_DELAYS = List.of(
            Duration.ofMinutes(1),
            Duration.ofMinutes(5),
            Duration.ofMinutes(15),
            Duration.ofMinutes(30),
            Duration.ofMinutes(60)
    );

    private final OrderRepository orderRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final PaymentRefundRepository refundRepository;
    private final PaymentWebhookEventRepository webhookEventRepository;
    private final RazorpayGateway gateway;
    private final RazorpayProperties properties;
    private final PaymentSignatureVerifier signatureVerifier;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final PaymentMetricsService metricsService;
    private final OrderLifecycleService orderLifecycleService;
    private final PaymentService self;

    @Autowired
    public PaymentService(OrderRepository orderRepository,
                          PaymentTransactionRepository transactionRepository,
                          PaymentRefundRepository refundRepository,
                          PaymentWebhookEventRepository webhookEventRepository,
                          RazorpayGateway gateway,
                          RazorpayProperties properties,
                          PaymentSignatureVerifier signatureVerifier,
                          ObjectMapper objectMapper,
                          ApplicationEventPublisher eventPublisher,
                          PaymentMetricsService metricsService,
                          OrderLifecycleService orderLifecycleService,
                          @Lazy PaymentService self) {
        this.orderRepository = orderRepository;
        this.transactionRepository = transactionRepository;
        this.refundRepository = refundRepository;
        this.webhookEventRepository = webhookEventRepository;
        this.gateway = gateway;
        this.properties = properties;
        this.signatureVerifier = signatureVerifier;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
        this.metricsService = metricsService;
        this.orderLifecycleService = orderLifecycleService;
        this.self = self == null ? this : self;
    }

    PaymentService(OrderRepository orderRepository,
                   PaymentTransactionRepository transactionRepository,
                   PaymentRefundRepository refundRepository,
                   PaymentWebhookEventRepository webhookEventRepository,
                   RazorpayGateway gateway,
                   RazorpayProperties properties,
                   PaymentSignatureVerifier signatureVerifier,
                   ObjectMapper objectMapper,
                   ApplicationEventPublisher eventPublisher,
                   PaymentMetricsService metricsService,
                   OrderLifecycleService orderLifecycleService) {
        this(orderRepository, transactionRepository, refundRepository, webhookEventRepository, gateway, properties,
                signatureVerifier, objectMapper, eventPublisher, metricsService, orderLifecycleService, null);
    }

    @Transactional
    public CreatePaymentOrderResponse createOrder(Long customerId, Long bookingId, String idempotencyKey) {
        requireText(idempotencyKey, "Idempotency-Key header is required");
        PaymentTransaction existing = transactionRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            assertOwner(existing, customerId);
            if (!Objects.equals(existing.getBookingId(), bookingId)) {
                throw new PaymentException("Idempotency key is already used for another booking");
            }
            return toCreateOrderResponse(existing);
        }

        Order booking = findOwnedBookingForUpdate(bookingId, customerId);
        existing = transactionRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            assertOwner(existing, customerId);
            if (!Objects.equals(existing.getBookingId(), bookingId)) {
                throw new PaymentException("Idempotency key is already used for another booking");
            }
            return toCreateOrderResponse(existing);
        }
        BigDecimal amount = bookingAmount(booking);
        RazorpayGateway.RazorpayOrder razorpayOrder = gateway.createOrder(bookingId, amount);

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setBookingId(bookingId);
        transaction.setCustomerId(customerId);
        transaction.setIdempotencyKey(idempotencyKey);
        transaction.setRazorpayOrderId(razorpayOrder.orderId());
        transaction.setAmount(amount);
        transaction.setCurrency(razorpayOrder.currency());
        transaction.setPaymentStatus(PaymentTransactionStatus.CREATED);
        transactionRepository.save(transaction);
        booking.setPaymentStatus(PaymentStatus.PENDING);
        orderRepository.save(booking);
        LOG.info("Created Razorpay order {} for booking {}", razorpayOrder.orderId(), bookingId);
        return toCreateOrderResponse(transaction);
    }

    @Transactional
    public CreatePaymentOrderResponse retryOrder(Long customerId, Long bookingId, String idempotencyKey) {
        Order booking = findOwnedBookingForUpdate(bookingId, customerId);
        if (booking.getPaymentStatus() == PaymentStatus.SUCCESS) {
            throw new PaymentException("Booking is already paid");
        }
        if (booking.getPaymentStatus() == PaymentStatus.REFUNDED) {
            throw new PaymentException("Refunded booking payment cannot be retried");
        }
        booking.setPaymentStatus(PaymentStatus.PENDING);
        orderRepository.save(booking);
        String retryKey = idempotencyKey == null || idempotencyKey.isBlank()
                ? "retry-" + bookingId + "-" + System.currentTimeMillis()
                : idempotencyKey;
        return self.createOrder(customerId, bookingId, retryKey);
    }

    @Transactional
    public PaymentResponse verify(Long customerId, VerifyPaymentRequest request) {
        PaymentTransaction transaction = findByRazorpayOrderIdForUpdate(request.razorpayOrderId());
        assertOwner(transaction, customerId);
        if (transaction.getPaymentStatus() == PaymentTransactionStatus.PAID) {
            return toResponse(transaction, true);
        }

        boolean valid = signatureVerifier.verifyPayment(
                request.razorpayOrderId(),
                request.razorpayPaymentId(),
                request.razorpaySignature(),
                properties.getKeySecret()
        );
        if (!valid) {
            markFailed(transaction, "Checkout signature verification failed");
            return toResponse(transaction, false);
        }

        RazorpayGateway.RazorpayPayment payment = gateway.fetchPayment(request.razorpayPaymentId());
        validateFetchedPayment(transaction, payment);
        transaction.setRazorpayPaymentId(request.razorpayPaymentId());
        transaction.setRazorpaySignature(request.razorpaySignature());
        transactionRepository.save(transaction);
        if (payment.isCaptured()) {
            markPaid(transaction, request.razorpayPaymentId(), request.razorpaySignature());
        }
        return toResponse(transaction, true);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getLatest(Long customerId, Long bookingId) {
        Order booking = orderRepository.findById(bookingId)
                .orElseThrow(() -> new PaymentException(BOOKING_NOT_FOUND));
        if (!Objects.equals(booking.getUserId(), customerId)) {
            throw new PaymentException("You are not allowed to access this booking payment");
        }
        PaymentTransaction transaction = transactionRepository.findTopByBookingIdOrderByCreatedAtDesc(bookingId)
                .orElseThrow(() -> new PaymentException(PAYMENT_TRANSACTION_NOT_FOUND));
        return toResponse(transaction, transaction.getPaymentStatus() == PaymentTransactionStatus.PAID);
    }

    @Transactional(readOnly = true)
    public void assertPayableBooking(Long customerId, Long bookingId) {
        Order booking = orderRepository.findById(bookingId)
                .orElseThrow(() -> new PaymentException(BOOKING_NOT_FOUND));
        if (!Objects.equals(booking.getUserId(), customerId)) {
            throw new PaymentException("You are not allowed to access this booking payment");
        }
        if (booking.getTotalAmount() == null || booking.getTotalAmount() <= 0) {
            throw new PaymentException("Booking amount must be greater than zero");
        }
    }

    @Transactional
    public WebhookProcessResult processWebhook(String eventId, String signature, String rawBody) {
        requireText(eventId, "X-Razorpay-Event-Id header is required");
        requireText(properties.getWebhookSecret(), "Razorpay webhook secret is not configured");
        if (!signatureVerifier.verifyWebhook(rawBody, signature, properties.getWebhookSecret())) {
            throw new PaymentException("Invalid webhook signature");
        }

        JsonNode payload = readTree(rawBody);
        String eventType = payload.path(WEBHOOK_EVENT).asText();
        String paymentId = extractPaymentId(payload);
        String idempotencyKey = idempotencyKey(eventId, eventType, paymentId);
        PaymentWebhookEvent existing = webhookEventRepository.findByIdempotencyKeyForUpdate(idempotencyKey).orElse(null);
        if (existing != null && existing.getStatus() == PaymentWebhookEventStatus.SUCCESS) {
            LOG.info("Ignoring duplicate Razorpay webhook event {} using idempotency key {}", eventId, idempotencyKey);
            return WebhookProcessResult.duplicate(eventId, existing.getEventType());
        }
        if (existing != null && existing.getStatus() == PaymentWebhookEventStatus.PROCESSING) {
            LOG.info("Ignoring in-flight Razorpay webhook event {} using idempotency key {}", eventId, idempotencyKey);
            return WebhookProcessResult.duplicate(eventId, existing.getEventType());
        }

        PaymentWebhookEvent webhookEvent = existing == null ? new PaymentWebhookEvent() : existing;
        if (existing == null) {
            webhookEvent.setEventId(eventId);
            webhookEvent.setIdempotencyKey(idempotencyKey);
        }
        webhookEvent.setEventType(eventType);
        webhookEvent.setRazorpayPaymentId(paymentId);
        webhookEvent.setRawPayload(rawBody);
        webhookEvent.setBookingId(extractBookingId(payload));
        webhookEvent.setFailureReason(null);
        webhookEvent.setRequestId(trimToNull(MDC.get(REQUEST_ID_MDC_KEY)));
        webhookEvent.setCorrelationId(trimToNull(MDC.get(CORRELATION_ID_MDC_KEY)));
        webhookEvent.setStatus(PaymentWebhookEventStatus.PROCESSING);
        webhookEventRepository.save(webhookEvent);

        return processStoredWebhookEvent(webhookEvent, payload)
                ? WebhookProcessResult.processed(eventId, eventType)
                : WebhookProcessResult.retryScheduled(eventId, eventType);
    }

    @Transactional
    public void retryDueWebhookEvents() {
        List<PaymentWebhookEvent> candidates = webhookEventRepository.findRetryCandidates(
                List.of(PaymentWebhookEventStatus.PENDING, PaymentWebhookEventStatus.FAILED),
                LocalDateTime.now()
        );
        for (PaymentWebhookEvent event : candidates) {
            webhookEventRepository.findByEventIdForUpdate(event.getEventId()).ifPresent(locked -> {
                if (locked.getStatus() == PaymentWebhookEventStatus.SUCCESS
                        || locked.getStatus() == PaymentWebhookEventStatus.DEAD_LETTER) {
                    return;
                }
                locked.setStatus(PaymentWebhookEventStatus.PROCESSING);
                webhookEventRepository.save(locked);
                processStoredWebhookEvent(locked, readTree(locked.getRawPayload()));
            });
        }
    }

    private boolean processStoredWebhookEvent(PaymentWebhookEvent webhookEvent, JsonNode payload) {
        try {
            String eventType = webhookEvent.getEventType();
            switch (eventType) {
                case "payment.authorized" -> handleAuthorized(payload);
                case "payment.captured" -> handleCaptured(payload);
                case "payment.failed" -> handleFailed(payload);
                case "refund.created" -> handleRefundCreated(payload);
                case "refund.processed" -> handleRefunded(payload);
                default -> LOG.info("Ignoring unsupported Razorpay webhook event {}", eventType);
            }
            webhookEvent.setStatus(PaymentWebhookEventStatus.SUCCESS);
            webhookEvent.setProcessedAt(LocalDateTime.now());
            webhookEvent.setNextRetryAt(null);
            webhookEvent.setFailureReason(null);
            webhookEventRepository.save(webhookEvent);
            return true;
        } catch (RuntimeException exception) {
            markWebhookFailed(webhookEvent, exception);
            LOG.warn("Razorpay webhook event {} failed and will be retried if allowed",
                    webhookEvent.getEventId(), exception);
            return false;
        }
    }

    private void markWebhookFailed(PaymentWebhookEvent webhookEvent, RuntimeException exception) {
        int retryCount = webhookEvent.getRetryCount() + 1;
        webhookEvent.setRetryCount(retryCount);
        webhookEvent.setFailureReason(truncate(exception.getMessage(), 500));
        if (retryCount > WEBHOOK_RETRY_DELAYS.size()) {
            webhookEvent.setStatus(PaymentWebhookEventStatus.DEAD_LETTER);
            webhookEvent.setNextRetryAt(null);
        } else {
            webhookEvent.setStatus(PaymentWebhookEventStatus.FAILED);
            webhookEvent.setNextRetryAt(LocalDateTime.now().plus(WEBHOOK_RETRY_DELAYS.get(retryCount - 1)));
        }
        webhookEventRepository.save(webhookEvent);
    }

    private void handleAuthorized(JsonNode payload) {
        JsonNode payment = payload.path(WEBHOOK_PAYLOAD).path(WEBHOOK_PAYMENT).path(WEBHOOK_ENTITY);
        PaymentTransaction transaction = findByRazorpayOrderId(payment.path(WEBHOOK_ORDER_ID).asText());
        transaction.setRazorpayPaymentId(blankToNull(payment.path("id").asText()));
        if (transaction.getPaymentStatus() == null) {
            transaction.setPaymentStatus(PaymentTransactionStatus.CREATED);
        }
        transactionRepository.save(transaction);
    }

    private void handleRefundCreated(JsonNode payload) {
        JsonNode refundPayload = payload.path(WEBHOOK_PAYLOAD).path(WEBHOOK_REFUND).path(WEBHOOK_ENTITY);
        String refundId = refundPayload.path("id").asText();
        String paymentId = refundPayload.path(WEBHOOK_PAYMENT_ID).asText();
        PaymentTransaction transaction = transactionRepository.findByRazorpayPaymentId(paymentId)
                .orElseThrow(() -> new PaymentException("Payment transaction not found for refund"));
        PaymentRefund refund = refundRepository.findByRazorpayRefundId(refundId)
                .orElseGet(() -> importedRefund(transaction, refundPayload));
        refund.setRefundStatus(PaymentRefundStatus.REQUESTED);
        refundRepository.save(refund);
    }

    private void handleCaptured(JsonNode payload) {
        JsonNode payment = payload.path(WEBHOOK_PAYLOAD).path(WEBHOOK_PAYMENT).path(WEBHOOK_ENTITY);
        String orderId = payment.path(WEBHOOK_ORDER_ID).asText();
        String paymentId = payment.path("id").asText();
        PaymentTransaction transaction = findByRazorpayOrderIdForUpdate(orderId);
        if (transaction.getPaymentStatus() == PaymentTransactionStatus.PAID) {
            LOG.info("Ignoring duplicate payment.captured for booking {} payment {}", transaction.getBookingId(), paymentId);
            return;
        }
        markPaid(transaction, paymentId, transaction.getRazorpaySignature());
    }

    private void handleFailed(JsonNode payload) {
        JsonNode payment = payload.path(WEBHOOK_PAYLOAD).path(WEBHOOK_PAYMENT).path(WEBHOOK_ENTITY);
        PaymentTransaction transaction = findByRazorpayOrderIdForUpdate(payment.path(WEBHOOK_ORDER_ID).asText());
        if (transaction.getPaymentStatus() != PaymentTransactionStatus.PAID) {
            transaction.setRazorpayPaymentId(blankToNull(payment.path("id").asText()));
            markFailed(transaction, blankToNull(payment.path("error_description").asText()));
        }
    }

    private void handleRefunded(JsonNode payload) {
        JsonNode refundPayload = payload.path(WEBHOOK_PAYLOAD).path(WEBHOOK_REFUND).path(WEBHOOK_ENTITY);
        String refundId = refundPayload.path("id").asText();
        String paymentId = refundPayload.path(WEBHOOK_PAYMENT_ID).asText();
        PaymentTransaction transaction = transactionRepository.findByRazorpayPaymentIdForUpdate(paymentId)
                .orElseThrow(() -> new PaymentException("Payment transaction not found for refund"));
        PaymentRefund refund = refundRepository.findByRazorpayRefundId(refundId)
                .orElseGet(() -> importedRefund(transaction, refundPayload));
        refund.setRefundStatus(PaymentRefundStatus.PROCESSED);
        refund.setProcessedAt(LocalDateTime.now());
        refundRepository.save(refund);
        eventPublisher.publishEvent(new PaymentRefundProcessedEvent(
                transaction.getBookingId(), refund.getRazorpayRefundId(), refund.getAmount()));
        BigDecimal processedAmount = refundRepository.sumAmountByTransactionIdAndStatuses(
                transaction.getId(),
                List.of(PaymentRefundStatus.PROCESSED)
        );
        if (processedAmount.compareTo(transaction.getAmount()) < 0) {
            return;
        }
        transaction.setPaymentStatus(PaymentTransactionStatus.REFUNDED);
        transactionRepository.save(transaction);
        Order booking = orderRepository.findByIdForUpdate(transaction.getBookingId())
                .orElseThrow(() -> new PaymentException(BOOKING_NOT_FOUND));
        booking.setPaymentStatus(PaymentStatus.REFUNDED);
        orderRepository.save(booking);
    }

    private PaymentRefund importedRefund(PaymentTransaction transaction, JsonNode refundPayload) {
        PaymentRefund refund = new PaymentRefund();
        refund.setPaymentTransactionId(transaction.getId());
        refund.setBookingId(transaction.getBookingId());
        refund.setIdempotencyKey("webhook-" + refundPayload.path("id").asText());
        refund.setRazorpayRefundId(refundPayload.path("id").asText());
        refund.setAmount(BigDecimal.valueOf(refundPayload.path("amount").asLong()).movePointLeft(2));
        refund.setCurrency(transaction.getCurrency());
        refund.setReason("Imported from Razorpay webhook");
        refund.setRefundStatus(PaymentRefundStatus.PROCESSED);
        return refund;
    }

    private void markPaid(PaymentTransaction transaction, String paymentId, String signature) {
        boolean newlyPaid = transaction.getPaymentStatus() != PaymentTransactionStatus.PAID;
        transaction.setRazorpayPaymentId(paymentId);
        transaction.setRazorpaySignature(signature);
        transaction.setPaymentStatus(PaymentTransactionStatus.PAID);
        transaction.setFailureReason(null);
        transaction.setPaidAt(LocalDateTime.now());
        transactionRepository.save(transaction);

        Order booking = orderRepository.findByIdForUpdate(transaction.getBookingId())
                .orElseThrow(() -> new PaymentException(BOOKING_NOT_FOUND));
        booking.setPaymentStatus(PaymentStatus.SUCCESS);
        if (booking.getStatus() == OrderStatus.CREATED) {
            orderRepository.save(booking);
            booking = orderLifecycleService.transitionIfCurrent(
                    booking.getId(), OrderStatus.CREATED, OrderStatus.CONFIRMED);
        } else {
            orderRepository.save(booking);
        }
        if (newlyPaid) {
            eventPublisher.publishEvent(new PaymentCapturedEvent(booking.getId()));
            metricsService.incrementPaymentSuccess();
        }
        LOG.info("Marked booking {} as paid using payment {}", booking.getId(), paymentId);
    }

    private void markFailed(PaymentTransaction transaction, String reason) {
        transaction.setPaymentStatus(PaymentTransactionStatus.FAILED);
        transaction.setFailureReason(reason);
        transactionRepository.save(transaction);
        Order booking = orderRepository.findByIdForUpdate(transaction.getBookingId())
                .orElseThrow(() -> new PaymentException(BOOKING_NOT_FOUND));
        booking.setPaymentStatus(PaymentStatus.FAILED);
        orderRepository.save(booking);
        eventPublisher.publishEvent(new PaymentFailedEvent(booking.getId(), reason));
        metricsService.incrementPaymentFailed();
    }

    private void validateFetchedPayment(PaymentTransaction transaction, RazorpayGateway.RazorpayPayment payment) {
        if (!transaction.getRazorpayOrderId().equals(payment.orderId())) {
            throw new PaymentException("Razorpay payment belongs to a different order");
        }
        if (gateway.toPaise(transaction.getAmount()) != payment.amountInPaise()) {
            throw new PaymentException("Razorpay payment amount does not match booking amount");
        }
    }

    private Order findOwnedBookingForUpdate(Long bookingId, Long customerId) {
        Order booking = orderRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new PaymentException(BOOKING_NOT_FOUND));
        if (!Objects.equals(booking.getUserId(), customerId)) {
            throw new PaymentException("You are not allowed to pay for this booking");
        }
        return booking;
    }

    private BigDecimal bookingAmount(Order booking) {
        if (booking.getTotalAmount() == null || booking.getTotalAmount() <= 0) {
            throw new PaymentException("Booking amount must be greater than zero");
        }
        return BigDecimal.valueOf(booking.getTotalAmount()).setScale(2);
    }

    private PaymentTransaction findByRazorpayOrderId(String orderId) {
        requireText(orderId, "razorpayOrderId is required");
        return transactionRepository.findByRazorpayOrderId(orderId)
                .orElseThrow(() -> new PaymentException(PAYMENT_TRANSACTION_NOT_FOUND));
    }

    private PaymentTransaction findByRazorpayOrderIdForUpdate(String orderId) {
        requireText(orderId, "razorpayOrderId is required");
        return transactionRepository.findByRazorpayOrderIdForUpdate(orderId)
                .orElseThrow(() -> new PaymentException(PAYMENT_TRANSACTION_NOT_FOUND));
    }

    private void assertOwner(PaymentTransaction transaction, Long customerId) {
        if (!transaction.getCustomerId().equals(customerId)) {
            throw new PaymentException("You are not allowed to access this payment");
        }
    }

    private CreatePaymentOrderResponse toCreateOrderResponse(PaymentTransaction transaction) {
        return new CreatePaymentOrderResponse(
                transaction.getBookingId(),
                transaction.getRazorpayOrderId(),
                properties.getKeyId(),
                gateway.toPaise(transaction.getAmount()),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getPaymentStatus().name()
        );
    }

    private PaymentResponse toResponse(PaymentTransaction transaction, boolean verified) {
        String frontendStatus = paymentStatus(transaction.getPaymentStatus());
        return new PaymentResponse(
                transaction.getBookingId(),
                transaction.getRazorpayOrderId(),
                transaction.getRazorpayPaymentId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                frontendStatus,
                frontendStatus,
                subscriptionStatus(transaction, frontendStatus),
                retryPaymentAllowed(frontendStatus),
                verified,
                utc(transaction.getCreatedAt()),
                utc(transaction.getPaidAt())
        );
    }

    private String utc(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.atZone(java.time.ZoneId.systemDefault())
                .withZoneSameInstant(java.time.ZoneOffset.UTC)
                .format(java.time.format.DateTimeFormatter.ISO_INSTANT);
    }

    private boolean retryPaymentAllowed(String paymentStatus) {
        return STATUS_FAILED.equals(paymentStatus) || "EXPIRED".equals(paymentStatus);
    }

    private String paymentStatus(PaymentTransactionStatus status) {
        if (status == null || status == PaymentTransactionStatus.CREATED) {
            return STATUS_PENDING;
        }
        return switch (status) {
            case PAID -> "SUCCESS";
            case FAILED -> STATUS_FAILED;
            case REFUNDED -> "REFUNDED";
            case CREATED -> STATUS_PENDING;
        };
    }

    private String subscriptionStatus(PaymentTransaction transaction, String paymentStatus) {
        Order booking = orderRepository.findById(transaction.getBookingId()).orElse(null);
        if (booking == null || booking.getBookingType() == null || !"SUBSCRIPTION".equalsIgnoreCase(booking.getBookingType())) {
            return null;
        }
        return "SUCCESS".equals(paymentStatus) ? "ACTIVE" : STATUS_PENDING;
    }

    private JsonNode readTree(String rawBody) {
        try {
            return objectMapper.readTree(rawBody);
        } catch (Exception exception) {
            throw new PaymentException("Invalid webhook payload", exception);
        }
    }

    private Long extractBookingId(JsonNode payload) {
        String eventType = payload.path(WEBHOOK_EVENT).asText();
        try {
            if (eventType != null && eventType.startsWith("refund.")) {
                String paymentId = payload.path(WEBHOOK_PAYLOAD)
                        .path(WEBHOOK_REFUND)
                        .path(WEBHOOK_ENTITY)
                        .path(WEBHOOK_PAYMENT_ID)
                        .asText();
                return transactionRepository.findByRazorpayPaymentId(paymentId)
                        .map(PaymentTransaction::getBookingId)
                        .orElse(null);
            }
            String orderId = payload.path(WEBHOOK_PAYLOAD).path(WEBHOOK_PAYMENT).path(WEBHOOK_ENTITY).path(WEBHOOK_ORDER_ID).asText();
            return transactionRepository.findByRazorpayOrderId(orderId)
                    .map(PaymentTransaction::getBookingId)
                    .orElse(null);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private String extractPaymentId(JsonNode payload) {
        String eventType = payload.path(WEBHOOK_EVENT).asText();
        if (eventType != null && eventType.startsWith("refund.")) {
            return blankToNull(payload.path(WEBHOOK_PAYLOAD).path(WEBHOOK_REFUND).path(WEBHOOK_ENTITY).path(WEBHOOK_PAYMENT_ID).asText());
        }
        return blankToNull(payload.path(WEBHOOK_PAYLOAD).path(WEBHOOK_PAYMENT).path(WEBHOOK_ENTITY).path("id").asText());
    }

    private String idempotencyKey(String eventId, String eventType, String paymentId) {
        if (paymentId != null && ("payment.captured".equals(eventType) || "payment.failed".equals(eventType))) {
            return eventType + ":" + paymentId;
        }
        return "event:" + eventId;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new PaymentException(message);
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}

package com.lexho.payment.service;

import com.lexho.booking.entity.Order;
import com.lexho.booking.repository.OrderRepository;
import com.lexho.booking.service.OrderLifecycleService;
import com.lexho.common.enums.OrderStatus;
import com.lexho.common.enums.PaymentStatus;
import com.lexho.payment.dto.AdminPaymentDashboardResponse;
import com.lexho.payment.dto.AdminPaymentReconciliationResponse;
import com.lexho.payment.dto.AdminPaymentTransactionResponse;
import com.lexho.payment.dto.AdminPaymentTransactionPageResponse;
import com.lexho.payment.dto.AdminRefundRequest;
import com.lexho.payment.dto.AdminRefundResponse;
import com.lexho.payment.entity.PaymentRefund;
import com.lexho.payment.entity.PaymentRefundStatus;
import com.lexho.payment.entity.PaymentTransaction;
import com.lexho.payment.entity.PaymentTransactionStatus;
import com.lexho.payment.exception.PaymentException;
import com.lexho.payment.event.PaymentCapturedEvent;
import com.lexho.payment.event.PaymentRefundProcessedEvent;
import com.lexho.payment.repository.PaymentRefundRepository;
import com.lexho.payment.repository.PaymentTransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminPaymentService {

    private static final String BOOKING_NOT_FOUND = "Booking not found";
    private static final String PAYMENT_TRANSACTION_NOT_FOUND = "Payment transaction not found";

    private final OrderRepository orderRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final PaymentRefundRepository refundRepository;
    private final RazorpayGateway gateway;
    private final ApplicationEventPublisher eventPublisher;
    private final OrderLifecycleService orderLifecycleService;
    private final AdminPaymentService self;

    public AdminPaymentService(OrderRepository orderRepository,
                               PaymentTransactionRepository transactionRepository,
                               PaymentRefundRepository refundRepository,
                               RazorpayGateway gateway,
                               ApplicationEventPublisher eventPublisher,
                               OrderLifecycleService orderLifecycleService,
                               @Lazy AdminPaymentService self) {
        this.orderRepository = orderRepository;
        this.transactionRepository = transactionRepository;
        this.refundRepository = refundRepository;
        this.gateway = gateway;
        this.eventPublisher = eventPublisher;
        this.orderLifecycleService = orderLifecycleService;
        this.self = self == null ? this : self;
    }

    @Transactional(readOnly = true)
    public AdminPaymentDashboardResponse dashboard() {
        return new AdminPaymentDashboardResponse(
                transactionRepository.count(),
                transactionRepository.countByPaymentStatus(PaymentTransactionStatus.CREATED),
                transactionRepository.countByPaymentStatus(PaymentTransactionStatus.PAID),
                transactionRepository.countByPaymentStatus(PaymentTransactionStatus.FAILED),
                transactionRepository.countByPaymentStatus(PaymentTransactionStatus.REFUNDED),
                transactionRepository.sumPaidAmount(),
                refundRepository.sumProcessedAmount()
        );
    }

    @Transactional(readOnly = true)
    public AdminPaymentTransactionPageResponse transactions(Long bookingId, String status, int page, int size) {
        PaymentTransactionStatus paymentStatus = parseStatus(status);
        Page<PaymentTransaction> result = transactionRepository.search(
                bookingId,
                paymentStatus,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("createdAt").descending())
        );
        return new AdminPaymentTransactionPageResponse(
                result.getContent().stream().map(this::toTransactionResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public AdminPaymentReconciliationResponse reconciliation(Long bookingId) {
        Order booking = findBooking(bookingId);
        List<PaymentRefund> refunds = refundRepository.findByBookingIdOrderByCreatedAtDesc(bookingId);
        BigDecimal processedRefundAmount = refunds.stream()
                .filter(refund -> refund.getRefundStatus() == PaymentRefundStatus.PROCESSED)
                .map(PaymentRefund::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new AdminPaymentReconciliationResponse(
                booking.getId(),
                booking.getTotalAmount(),
                booking.getStatus() == null ? null : booking.getStatus().name(),
                booking.getPaymentStatus() == null ? null : booking.getPaymentStatus().name(),
                processedRefundAmount,
                transactionRepository.findByBookingIdOrderByCreatedAtDesc(bookingId).stream()
                        .map(this::toTransactionResponse)
                        .toList(),
                refunds.stream().map(this::toRefundResponse).toList()
        );
    }

    @Transactional
    public AdminRefundResponse createRefund(Long adminId,
                                            Long bookingId,
                                            String idempotencyKey,
                                            AdminRefundRequest request) {
        requireText(idempotencyKey, "Idempotency-Key header is required");
        PaymentRefund existing = refundRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            return toRefundResponse(existing);
        }

        PaymentTransaction latest = transactionRepository.findTopByBookingIdAndPaymentStatusInOrderByCreatedAtDesc(
                        bookingId,
                        List.of(PaymentTransactionStatus.PAID, PaymentTransactionStatus.REFUNDED)
                )
                .orElseThrow(() -> new PaymentException(PAYMENT_TRANSACTION_NOT_FOUND));
        PaymentTransaction transaction = transactionRepository.findByIdForUpdate(latest.getId())
                .orElseThrow(() -> new PaymentException(PAYMENT_TRANSACTION_NOT_FOUND));
        if (transaction.getPaymentStatus() != PaymentTransactionStatus.PAID
                && transaction.getPaymentStatus() != PaymentTransactionStatus.REFUNDED) {
            throw new PaymentException("Only captured payments can be refunded");
        }
        requireText(transaction.getRazorpayPaymentId(), "Razorpay payment ID is missing");

        BigDecimal amount = request.amount().setScale(2);
        BigDecimal reserved = refundRepository.sumAmountByTransactionIdAndStatuses(
                transaction.getId(),
                List.of(PaymentRefundStatus.REQUESTED, PaymentRefundStatus.PROCESSED)
        );
        if (reserved.add(amount).compareTo(transaction.getAmount()) > 0) {
            throw new PaymentException("Refund amount exceeds refundable balance");
        }

        PaymentRefund refund = new PaymentRefund();
        refund.setPaymentTransactionId(transaction.getId());
        refund.setBookingId(bookingId);
        refund.setRequestedByAdminId(adminId);
        refund.setIdempotencyKey(idempotencyKey);
        refund.setAmount(amount);
        refund.setCurrency(transaction.getCurrency());
        refund.setReason(request.reason().trim());
        refund.setRefundStatus(PaymentRefundStatus.REQUESTED);
        refundRepository.save(refund);

        try {
            RazorpayGateway.RazorpayRefund gatewayRefund = gateway.createRefund(
                    transaction.getRazorpayPaymentId(),
                    amount,
                    bookingId
            );
            refund.setRazorpayRefundId(gatewayRefund.refundId());
            if (gatewayRefund.isProcessed()) {
                refund.setRefundStatus(PaymentRefundStatus.PROCESSED);
                refund.setProcessedAt(LocalDateTime.now());
                updateBookingAfterProcessedRefund(transaction);
            }
        } catch (PaymentException exception) {
            refund.setRefundStatus(PaymentRefundStatus.FAILED);
            refund.setFailureReason(exception.getMessage());
        }
        PaymentRefund saved = refundRepository.save(refund);
        if (saved.getRefundStatus() == PaymentRefundStatus.PROCESSED) {
            eventPublisher.publishEvent(new PaymentRefundProcessedEvent(
                    bookingId, saved.getRazorpayRefundId(), saved.getAmount()));
        }
        return toRefundResponse(saved);
    }

    @Transactional
    public AdminPaymentReconciliationResponse reconcileLatest(Long bookingId) {
        PaymentTransaction transaction = transactionRepository.findTopByBookingIdOrderByCreatedAtDesc(bookingId)
                .orElseThrow(() -> new PaymentException(PAYMENT_TRANSACTION_NOT_FOUND));
        requireText(transaction.getRazorpayPaymentId(), "Razorpay payment ID is missing");
        RazorpayGateway.RazorpayPayment payment = gateway.fetchPayment(transaction.getRazorpayPaymentId());
        if (!transaction.getRazorpayOrderId().equals(payment.orderId())) {
            throw new PaymentException("Razorpay payment belongs to a different order");
        }
        if (gateway.toPaise(transaction.getAmount()) != payment.amountInPaise()) {
            throw new PaymentException("Razorpay payment amount does not match booking amount");
        }
        if (payment.isCaptured()) {
            boolean newlyPaid = transaction.getPaymentStatus() != PaymentTransactionStatus.PAID;
            transaction.setPaymentStatus(PaymentTransactionStatus.PAID);
            transaction.setFailureReason(null);
            if (transaction.getPaidAt() == null) {
                transaction.setPaidAt(LocalDateTime.now());
            }
            transactionRepository.save(transaction);
            Order booking = orderRepository.findByIdForUpdate(bookingId)
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
            }
        }
        return self.reconciliation(bookingId);
    }

    private void updateBookingAfterProcessedRefund(PaymentTransaction transaction) {
        BigDecimal processed = refundRepository.sumAmountByTransactionIdAndStatuses(
                transaction.getId(),
                List.of(PaymentRefundStatus.PROCESSED)
        );
        if (processed.compareTo(transaction.getAmount()) < 0) {
            return;
        }
        transaction.setPaymentStatus(PaymentTransactionStatus.REFUNDED);
        transactionRepository.save(transaction);
        Order booking = orderRepository.findByIdForUpdate(transaction.getBookingId())
                .orElseThrow(() -> new PaymentException(BOOKING_NOT_FOUND));
        booking.setPaymentStatus(PaymentStatus.REFUNDED);
        orderRepository.save(booking);
    }

    private Order findBooking(Long bookingId) {
        return orderRepository.findById(bookingId)
                .orElseThrow(() -> new PaymentException(BOOKING_NOT_FOUND));
    }

    private AdminPaymentTransactionResponse toTransactionResponse(PaymentTransaction transaction) {
        return new AdminPaymentTransactionResponse(
                transaction.getId(),
                transaction.getBookingId(),
                transaction.getCustomerId(),
                transaction.getRazorpayOrderId(),
                transaction.getRazorpayPaymentId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getPaymentStatus().name(),
                transaction.getFailureReason(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt(),
                transaction.getPaidAt()
        );
    }

    private AdminRefundResponse toRefundResponse(PaymentRefund refund) {
        return new AdminRefundResponse(
                refund.getId(),
                refund.getBookingId(),
                refund.getRazorpayRefundId(),
                refund.getAmount(),
                refund.getCurrency(),
                refund.getRefundStatus().name(),
                refund.getReason(),
                refund.getFailureReason(),
                refund.getCreatedAt(),
                refund.getProcessedAt()
        );
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new PaymentException(message);
        }
    }

    private PaymentTransactionStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return PaymentTransactionStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new PaymentException("Invalid payment status");
        }
    }
}

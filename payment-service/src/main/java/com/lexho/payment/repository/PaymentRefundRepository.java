package com.lexho.payment.repository;

import com.lexho.payment.entity.PaymentRefund;
import com.lexho.payment.entity.PaymentRefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PaymentRefundRepository extends JpaRepository<PaymentRefund, Long> {

    Optional<PaymentRefund> findByIdempotencyKey(String idempotencyKey);

    Optional<PaymentRefund> findByRazorpayRefundId(String razorpayRefundId);

    List<PaymentRefund> findByBookingIdOrderByCreatedAtDesc(Long bookingId);

    @Query("""
            SELECT COUNT(r)
            FROM PaymentRefund r
            JOIN PaymentTransaction t ON t.id = r.paymentTransactionId
            WHERE t.customerId = :customerId
              AND r.refundStatus IN :statuses
            """)
    long countByCustomerIdAndRefundStatusIn(@Param("customerId") Long customerId,
                                            @Param("statuses") List<PaymentRefundStatus> statuses);

    @Query("""
            SELECT COALESCE(SUM(r.amount), 0)
            FROM PaymentRefund r
            WHERE r.paymentTransactionId = :transactionId
              AND r.refundStatus IN :statuses
            """)
    BigDecimal sumAmountByTransactionIdAndStatuses(
            @Param("transactionId") Long transactionId,
            @Param("statuses") List<PaymentRefundStatus> statuses
    );

    @Query("""
            SELECT COALESCE(SUM(r.amount), 0)
            FROM PaymentRefund r
            WHERE r.refundStatus = com.lexho.payment.entity.PaymentRefundStatus.PROCESSED
            """)
    BigDecimal sumProcessedAmount();
}

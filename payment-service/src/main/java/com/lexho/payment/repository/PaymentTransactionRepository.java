package com.lexho.payment.repository;

import com.lexho.payment.entity.PaymentTransaction;
import com.lexho.payment.entity.PaymentTransactionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    Optional<PaymentTransaction> findByIdempotencyKey(String idempotencyKey);
    Optional<PaymentTransaction> findByRazorpayOrderId(String razorpayOrderId);
    Optional<PaymentTransaction> findByRazorpayPaymentId(String razorpayPaymentId);
    Optional<PaymentTransaction> findTopByBookingIdOrderByCreatedAtDesc(Long bookingId);
    Optional<PaymentTransaction> findTopByBookingIdAndPaymentStatusInOrderByCreatedAtDesc(
            Long bookingId,
            List<PaymentTransactionStatus> paymentStatuses
    );
    List<PaymentTransaction> findByBookingIdOrderByCreatedAtDesc(Long bookingId);
    long countByPaymentStatus(PaymentTransactionStatus paymentStatus);

    @Query("""
            SELECT t FROM PaymentTransaction t
            WHERE (:bookingId IS NULL OR t.bookingId = :bookingId)
              AND (:paymentStatus IS NULL OR t.paymentStatus = :paymentStatus)
            """)
    Page<PaymentTransaction> search(
            @Param("bookingId") Long bookingId,
            @Param("paymentStatus") PaymentTransactionStatus paymentStatus,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM PaymentTransaction t WHERE t.id = :id")
    Optional<PaymentTransaction> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM PaymentTransaction t WHERE t.razorpayOrderId = :razorpayOrderId")
    Optional<PaymentTransaction> findByRazorpayOrderIdForUpdate(@Param("razorpayOrderId") String razorpayOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM PaymentTransaction t WHERE t.razorpayPaymentId = :razorpayPaymentId")
    Optional<PaymentTransaction> findByRazorpayPaymentIdForUpdate(@Param("razorpayPaymentId") String razorpayPaymentId);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM PaymentTransaction t
            WHERE t.paymentStatus = com.lexho.payment.entity.PaymentTransactionStatus.PAID
            """)
    BigDecimal sumPaidAmount();
}

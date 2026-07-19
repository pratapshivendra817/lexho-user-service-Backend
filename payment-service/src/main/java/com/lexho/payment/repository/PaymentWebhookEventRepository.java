package com.lexho.payment.repository;

import com.lexho.payment.entity.PaymentWebhookEvent;
import com.lexho.payment.entity.PaymentWebhookEventStatus;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, Long> {
    boolean existsByEventId(String eventId);
    Optional<PaymentWebhookEvent> findByEventId(String eventId);
    Optional<PaymentWebhookEvent> findByIdempotencyKey(String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM PaymentWebhookEvent e WHERE e.eventId = :eventId")
    Optional<PaymentWebhookEvent> findByEventIdForUpdate(@Param("eventId") String eventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM PaymentWebhookEvent e WHERE e.idempotencyKey = :idempotencyKey")
    Optional<PaymentWebhookEvent> findByIdempotencyKeyForUpdate(@Param("idempotencyKey") String idempotencyKey);

    @Query("""
            SELECT e FROM PaymentWebhookEvent e
            WHERE e.status IN :statuses
              AND (e.nextRetryAt IS NULL OR e.nextRetryAt <= :now)
            ORDER BY e.receivedAt ASC
            """)
    List<PaymentWebhookEvent> findRetryCandidates(
            @Param("statuses") List<PaymentWebhookEventStatus> statuses,
            @Param("now") LocalDateTime now
    );

    @Query("""
            SELECT e FROM PaymentWebhookEvent e
            WHERE (:search IS NULL
                   OR upper(coalesce(e.eventId, '')) LIKE upper(concat('%', :search, '%'))
                   OR upper(coalesce(e.razorpayPaymentId, '')) LIKE upper(concat('%', :search, '%'))
                   OR upper(coalesce(e.idempotencyKey, '')) LIKE upper(concat('%', :search, '%')))
              AND (:eventType IS NULL OR upper(e.eventType) = upper(:eventType))
              AND (:status IS NULL OR upper(cast(e.status as string)) = upper(:status))
            ORDER BY coalesce(e.processedAt, e.updatedAt, e.receivedAt) DESC
            """)
    Page<PaymentWebhookEvent> search(@Param("search") String search,
                                     @Param("eventType") String eventType,
                                     @Param("status") String status,
                                     Pageable pageable);
}

package com.lexho.user_service.booking.idempotency;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface BookingIdempotencyRepository extends JpaRepository<BookingIdempotencyRecord, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select r from BookingIdempotencyRecord r
            where r.customerId = :customerId
              and r.idempotencyKey = :idempotencyKey
            """)
    Optional<BookingIdempotencyRecord> findByCustomerAndKeyForUpdate(@Param("customerId") Long customerId,
                                                                     @Param("idempotencyKey") String idempotencyKey);

    @Modifying
    @Query("""
            delete from BookingIdempotencyRecord r
            where r.customerId = :customerId
              and r.idempotencyKey = :idempotencyKey
              and r.status = com.lexho.user_service.booking.idempotency.BookingIdempotencyRecord.Status.IN_PROGRESS
            """)
    int deleteInProgress(@Param("customerId") Long customerId, @Param("idempotencyKey") String idempotencyKey);

    @Modifying
    @Query("delete from BookingIdempotencyRecord r where r.expiresAt < :cutoff")
    int deleteExpired(@Param("cutoff") LocalDateTime cutoff);
}

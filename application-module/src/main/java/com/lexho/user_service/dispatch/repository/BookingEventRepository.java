package com.lexho.user_service.dispatch.repository;

import com.lexho.user_service.dispatch.entity.BookingEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingEventRepository extends JpaRepository<BookingEventEntity, Long> {
    List<BookingEventEntity> findByBookingIdOrderByCreatedAtAsc(Long bookingId);
    boolean existsByBookingIdAndEventType(Long bookingId, String eventType);
    long countByBookingIdAndEventType(Long bookingId, String eventType);
}

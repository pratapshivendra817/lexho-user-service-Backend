package com.lexho.booking.repository;

import com.lexho.booking.entity.Booking;
import com.lexho.booking.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

    Optional<Booking> findByIdAndDeletedFalse(Long id);

    long countByBookingStatusAndDeletedFalse(BookingStatus status);

    List<Booking> findByCustomer_IdAndDeletedFalse(Long customerId);
}


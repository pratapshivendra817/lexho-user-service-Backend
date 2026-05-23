package com.lexho.booking.service;

import com.lexho.booking.dto.BookingStatsResponse;
import com.lexho.booking.dto.BookingUpdateRequest;
import com.lexho.booking.entity.Booking;
import com.lexho.booking.enums.BookingStatus;
import com.lexho.booking.exception.BookingOperationException;
import com.lexho.booking.mapper.BookingMapper;
import com.lexho.booking.repository.BookingRepository;
import com.lexho.catalog.repository.CategoryRepository;
import com.lexho.common.enums.Role;
import com.lexho.user.entity.User;
import com.lexho.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingManagementServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private BookingMapper bookingMapper;

    @InjectMocks
    private BookingManagementService bookingManagementService;

    private Booking booking;

    @BeforeEach
    void setup() {
        booking = new Booking();
        booking.setId(1L);
        booking.setBookingId("BK1291");
        booking.setBookingStatus(BookingStatus.PENDING);
        booking.setDeleted(false);
    }

    @Test
    void getStatsReturnsAllStatusCounts() {
        when(bookingRepository.countByBookingStatusAndDeletedFalse(BookingStatus.PENDING)).thenReturn(5L);
        when(bookingRepository.countByBookingStatusAndDeletedFalse(BookingStatus.CONFIRMED)).thenReturn(8L);
        when(bookingRepository.countByBookingStatusAndDeletedFalse(BookingStatus.COMPLETED)).thenReturn(13L);
        when(bookingRepository.countByBookingStatusAndDeletedFalse(BookingStatus.CANCELLED)).thenReturn(2L);

        BookingStatsResponse stats = bookingManagementService.getStats();

        assertEquals(5L, stats.getPending());
        assertEquals(8L, stats.getConfirmed());
        assertEquals(13L, stats.getCompleted());
        assertEquals(2L, stats.getCancelled());
    }

    @Test
    void updateBookingThrowsForCompletedBooking() {
        booking.setBookingStatus(BookingStatus.COMPLETED);
        when(bookingRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(booking));

        BookingUpdateRequest request = new BookingUpdateRequest();
        request.setBookingDate(LocalDate.now().plusDays(1));
        request.setBookingTime(LocalTime.NOON);
        request.setProviderId(100L);
        request.setAmount(BigDecimal.valueOf(499));
        request.setNotes("Updated by admin");

        assertThrows(BookingOperationException.class, () -> bookingManagementService.updateBooking(1L, request));
    }

    @Test
    void cancelBookingThrowsIfAlreadyCancelled() {
        booking.setBookingStatus(BookingStatus.CANCELLED);
        when(bookingRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(booking));

        assertThrows(BookingOperationException.class, () -> bookingManagementService.cancelBooking(1L));
    }

    @Test
    void softDeleteBookingMarksDeleted() {
        when(bookingRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(booking));

        bookingManagementService.softDeleteBooking(1L);

        assertEquals(true, booking.isDeleted());
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void updateBookingUpdatesProviderAndAmount() {
        User partner = new User();
        partner.setRole(Role.PARTNER);
        partner.setDeleted(false);
        partner.setFullName("Partner");

        when(bookingRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(booking));
        when(userRepository.findByIdAndDeletedFalse(11L)).thenReturn(Optional.of(partner));
        when(bookingRepository.save(booking)).thenReturn(booking);
        when(bookingMapper.toDetail(booking)).thenReturn(null);

        BookingUpdateRequest request = new BookingUpdateRequest();
        request.setBookingDate(LocalDate.now().plusDays(1));
        request.setBookingTime(LocalTime.of(10, 30));
        request.setProviderId(11L);
        request.setAmount(BigDecimal.valueOf(799));
        request.setNotes("Morning slot");

        bookingManagementService.updateBooking(1L, request);

        assertEquals(partner, booking.getPartner());
        assertEquals(BigDecimal.valueOf(799), booking.getAmount());
        assertEquals("Morning slot", booking.getNotes());
        verify(bookingRepository).save(booking);
    }
}

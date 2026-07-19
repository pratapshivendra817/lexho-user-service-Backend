package com.lexho.booking.mapper;

import com.lexho.booking.dto.BookingDetailResponse;
import com.lexho.booking.dto.BookingListItemResponse;
import com.lexho.booking.entity.Booking;
import com.lexho.booking.enums.BookingStatus;
import com.lexho.catalog.entity.ServiceCategory;
import com.lexho.common.enums.PaymentStatus;
import com.lexho.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BookingMapperTest {

    private final BookingMapper mapper = new BookingMapper();

    @Test
    void toListItemMapsAllFieldsWithPartner() {
        Booking booking = completeBooking(true);

        BookingListItemResponse result = mapper.toListItem(booking);

        assertEquals(1L, result.getId());
        assertEquals("BK1001", result.getBookingId());
        assertEquals("Customer", result.getCustomerName());
        assertEquals("Partner", result.getProviderName());
        assertEquals("Cleaning", result.getServiceName());
        assertEquals(BigDecimal.valueOf(850), result.getAmount());
        assertEquals(BookingStatus.CONFIRMED, result.getStatus());
    }

    @Test
    void toListItemAllowsUnassignedPartner() {
        BookingListItemResponse result = mapper.toListItem(completeBooking(false));

        assertNull(result.getProviderName());
    }

    @Test
    void toDetailMapsCompleteBooking() {
        Booking booking = completeBooking(true);

        BookingDetailResponse result = mapper.toDetail(booking);

        assertEquals(1L, result.getId());
        assertEquals(101L, result.getCustomerId());
        assertEquals("Customer", result.getCustomerName());
        assertEquals(202L, result.getProviderId());
        assertEquals("Partner", result.getProviderName());
        assertEquals(301L, result.getServiceId());
        assertEquals(LocalTime.of(9, 15), result.getBookingTime());
        assertEquals(PaymentStatus.SUCCESS, result.getPaymentStatus());
        assertEquals("Delhi", result.getCity());
        assertEquals("Call first", result.getNotes());
        assertEquals(LocalDateTime.of(2026, 6, 1, 8, 0), result.getCreatedAt());
    }

    @Test
    void toDetailAllowsUnassignedPartner() {
        BookingDetailResponse result = mapper.toDetail(completeBooking(false));

        assertNull(result.getProviderId());
        assertNull(result.getProviderName());
    }

    private Booking completeBooking(boolean withPartner) {
        User customer = new User();
        customer.setFullName("Customer");
        ReflectionTestUtils.setField(customer, "id", 101L);
        User partner = new User();
        partner.setFullName("Partner");
        ReflectionTestUtils.setField(partner, "id", 202L);
        ServiceCategory category = new ServiceCategory();
        category.setId(301L);
        category.setName("Cleaning");

        Booking booking = new Booking();
        booking.setId(1L);
        booking.setBookingId("BK1001");
        booking.setCustomer(customer);
        booking.setPartner(withPartner ? partner : null);
        booking.setService(category);
        booking.setBookingDate(LocalDate.of(2026, 7, 1));
        booking.setBookingTime(LocalTime.of(9, 15));
        booking.setAmount(BigDecimal.valueOf(850));
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        booking.setPaymentStatus(PaymentStatus.SUCCESS);
        booking.setAddress("Sector 1");
        booking.setCity("Delhi");
        booking.setNotes("Call first");
        ReflectionTestUtils.setField(booking, "createdAt", LocalDateTime.of(2026, 6, 1, 8, 0));
        ReflectionTestUtils.setField(booking, "updatedAt", LocalDateTime.of(2026, 6, 2, 8, 0));
        return booking;
    }
}

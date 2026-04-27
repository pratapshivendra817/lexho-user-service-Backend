package com.lexho.booking.mapper;

import com.lexho.booking.dto.BookingDetailResponse;
import com.lexho.booking.dto.BookingListItemResponse;
import com.lexho.booking.entity.Booking;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {

    public BookingListItemResponse toListItem(Booking booking) {
        BookingListItemResponse response = new BookingListItemResponse();
        response.setId(booking.getId());
        response.setBookingId(booking.getBookingId());
        response.setCustomerName(booking.getCustomer().getFullName());
        response.setProviderName(booking.getProvider() != null ? booking.getProvider().getFullName() : null);
        response.setServiceName(booking.getService().getName());
        response.setAmount(booking.getAmount());
        response.setBookingDate(booking.getBookingDate());
        response.setStatus(booking.getBookingStatus());
        return response;
    }

    public BookingDetailResponse toDetail(Booking booking) {
        BookingDetailResponse response = new BookingDetailResponse();
        response.setId(booking.getId());
        response.setBookingId(booking.getBookingId());
        response.setCustomerId(booking.getCustomer().getId());
        response.setCustomerName(booking.getCustomer().getFullName());
        response.setProviderId(booking.getProvider() != null ? booking.getProvider().getId() : null);
        response.setProviderName(booking.getProvider() != null ? booking.getProvider().getFullName() : null);
        response.setServiceId(booking.getService().getId());
        response.setServiceName(booking.getService().getName());
        response.setBookingDate(booking.getBookingDate());
        response.setBookingTime(booking.getBookingTime());
        response.setAmount(booking.getAmount());
        response.setBookingStatus(booking.getBookingStatus());
        response.setPaymentStatus(booking.getPaymentStatus());
        response.setAddress(booking.getAddress());
        response.setCity(booking.getCity());
        response.setNotes(booking.getNotes());
        response.setCreatedAt(booking.getCreatedAt());
        response.setUpdatedAt(booking.getUpdatedAt());
        return response;
    }
}


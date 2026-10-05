package com.lexho.user_service.booking.service;

import com.lexho.booking.entity.Order;
import com.lexho.booking.entity.OrderItem;
import com.lexho.booking.repository.OrderRepository;
import com.lexho.common.enums.OrderStatus;
import com.lexho.user.entity.User;
import com.lexho.user.repository.UserRepository;
import com.lexho.user_service.admin.entity.AdminServiceEntity;
import com.lexho.user_service.booking.dto.BookingRescheduleRequest;
import com.lexho.user_service.booking.dto.InstantBookingCreateRequest;
import com.lexho.user_service.booking.exception.BookingValidationException;
import com.lexho.user_service.customer.location.dto.ReverseGeocodeResponse;
import com.lexho.user_service.customer.location.entity.CustomerAddress;
import com.lexho.user_service.customer.location.repository.CustomerAddressRepository;
import com.lexho.user_service.customer.location.service.ReverseGeocodeService;
import com.lexho.user_service.customer.repository.CustomerCatalogRepository;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.exception.UnauthorizedException;
import com.lexho.user_service.review.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstantBookingServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock CustomerCatalogRepository catalogRepository;
    @Mock CustomerAddressRepository addressRepository;
    @Mock ReverseGeocodeService reverseGeocodeService;
    @Mock UserRepository userRepository;
    @Mock ReviewRepository reviewRepository;
    @InjectMocks InstantBookingService service;

    @Test
    void createShouldFailClosedInsteadOfBypassingPaymentDispatchFlow() {
        BookingValidationException exception = assertThrows(BookingValidationException.class,
                () -> service.create(7L, request()));

        assertEquals("LEGACY_BOOKING_FLOW_DISABLED", exception.getErrorCode());
        verifyNoInteractions(catalogRepository, reverseGeocodeService, orderRepository);
    }

    @Test
    void createShouldFailClosedBeforeLegacyScheduledValidation() {
        InstantBookingCreateRequest request = request();
        request.bookingType = "SCHEDULED";

        BookingValidationException invalidSlot = assertThrows(BookingValidationException.class,
                () -> service.create(7L, request));
        assertEquals("LEGACY_BOOKING_FLOW_DISABLED", invalidSlot.getErrorCode());
    }

    @Test
    void createShouldFailClosedBeforeLegacyServiceLookup() {
        assertThrows(BookingValidationException.class, () -> service.create(7L, request()));
        verifyNoInteractions(catalogRepository);
    }

    @Test
    void detailsShouldRejectMissingAndForeignBookings() {
        when(orderRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.details(7L, 99L));
        Order foreign = order(10L, OrderStatus.CONFIRMED);
        foreign.setUserId(8L);
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(foreign));
        assertThrows(UnauthorizedException.class, () -> service.details(7L, 10L));
    }

    @Test
    void detailsFallsBackWhenOrderItemsContainNullRow() {
        Order order = order(10L, OrderStatus.CONFIRMED);
        order.setItems(new ArrayList<>());
        order.getItems().add(null);
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));

        var response = service.details(7L, 10L);

        assertNull(response.serviceId);
        assertNull(response.serviceName);
    }

    @Test
    void detailsShouldAttachAssignedPartnerAndMapLifecycleStatuses() {
        Order order = order(10L, OrderStatus.ARRIVED);
        order.setAssignedPartnerId(20L);
        order.setServiceStartPinNonce("abc123");
        order.setServiceStartPinExpiresAt(LocalDateTime.now().plusMinutes(10));
        User partner = new User("9876543210", true);
        ReflectionTestUtils.setField(partner, "id", 20L);
        partner.setFullName("Ravi");
        partner.setProfileImage("photo.jpg");
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));
        when(catalogRepository.findById(5L)).thenReturn(Optional.of(catalogService(5L)));
        when(userRepository.findByIdAndDeletedFalse(20L)).thenReturn(Optional.of(partner));

        var response = service.details(7L, 10L);

        assertEquals("PARTNER_ARRIVED", response.assignmentStatus);
        assertNotNull(response.serviceStartPin);
        assertTrue(response.serviceStartPin.matches("\\d{4}"));
        assertNotNull(response.serviceStartPinExpiresAt);
        assertEquals("Ravi", response.partnerName);
        assertEquals("9876543210", response.partnerPhone);
        assertEquals(12, response.etaMinutes);
        assertEquals(false, response.meta.get("canReview"));
        assertEquals(false, response.meta.get("reviewSubmitted"));
    }

    @Test
    void detailsShouldMapTerminalAndProgressMessages() {
        Order order = order(10L, OrderStatus.SERVICE_IN_PROGRESS);
        order.setServiceCompletePinNonce("complete123");
        order.setServiceCompletePinExpiresAt(LocalDateTime.now().plusMinutes(10));
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));
        assertEquals("IN_PROGRESS", service.details(7L, 10L).assignmentStatus);
        assertTrue(service.details(7L, 10L).serviceCompletePin.matches("\\d{4}"));
        order.setStatus(OrderStatus.SERVICE_COMPLETED);
        var completed = service.details(7L, 10L);
        assertEquals("COMPLETED", completed.status);
        assertEquals("COMPLETED", completed.assignmentStatus);
        assertNull(completed.serviceCompletePin);
        assertEquals(true, completed.meta.get("canReview"));
        assertEquals(false, completed.meta.get("reviewSubmitted"));
        when(reviewRepository.existsByBookingIdAndDeletedFalse(10L)).thenReturn(true);
        var reviewed = service.details(7L, 10L);
        assertEquals(false, reviewed.meta.get("canReview"));
        assertEquals(true, reviewed.meta.get("reviewSubmitted"));
        order.setStatus(OrderStatus.INSTANT_FAILED);
        assertEquals("INSTANT_FAILED", service.details(7L, 10L).status);
        order.setStatus(OrderStatus.CANCELLED);
        assertEquals("CANCELLED", service.details(7L, 10L).status);
    }

    @Test
    void rescheduleShouldUpdateOwnedBookingAndRejectCancelledBooking() {
        Order order = order(10L, OrderStatus.CONFIRMED);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        BookingRescheduleRequest request = new BookingRescheduleRequest();
        request.date = OffsetDateTime.of(2026, 7, 1, 8, 0, 0, 0, ZoneOffset.UTC);

        var response = service.reschedule(7L, 10L, request);

        assertEquals("SCHEDULED", order.getBookingType());
        assertEquals("Booking rescheduled successfully.", response.message);
        assertNull(order.getSlaMinutes());
        order.setStatus(OrderStatus.CANCELLED);
        assertThrows(BookingValidationException.class, () -> service.reschedule(7L, 10L, request));
    }

    @Test
    void cancelShouldPersistCancellableBookingAndRejectCompletedBooking() {
        Order order = order(10L, OrderStatus.CONFIRMED);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        assertEquals("CANCELLED", service.cancel(7L, 10L).status);
        order.setStatus(OrderStatus.SERVICE_COMPLETED);
        assertThrows(BookingValidationException.class, () -> service.cancel(7L, 10L));
    }

    private static InstantBookingCreateRequest request() {
        InstantBookingCreateRequest request = new InstantBookingCreateRequest();
        request.serviceId = 5L;
        request.latitude = 28.0;
        request.longitude = 77.0;
        return request;
    }

    private static AdminServiceEntity catalogService(Long id) {
        AdminServiceEntity service = new AdminServiceEntity();
        ReflectionTestUtils.setField(service, "id", id);
        service.setName("Cleaning");
        service.setActive(true);
        service.setBasePrice(BigDecimal.valueOf(100));
        service.setDiscountedPrice(BigDecimal.valueOf(80));
        return service;
    }

    private static Order order(Long id, OrderStatus status) {
        Order order = new Order();
        order.setId(id);
        order.setUserId(7L);
        order.setStatus(status);
        order.setBookingType("INSTANT");
        order.setTotalAmount(100.0);
        order.setAddress("Delhi");
        order.setSlaMinutes(30);
        OrderItem item = new OrderItem();
        item.setPartnerId(5L);
        item.setPartnerName("Cleaning");
        item.setOrder(order);
        order.setItems(new ArrayList<>(java.util.List.of(item)));
        return order;
    }
}

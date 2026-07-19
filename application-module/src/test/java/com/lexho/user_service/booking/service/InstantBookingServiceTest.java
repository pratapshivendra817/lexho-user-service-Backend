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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    @InjectMocks InstantBookingService service;

    @Test
    void createInstantShouldUseCoordinateObjectAndReverseGeocodedAddress() {
        AdminServiceEntity catalogService = catalogService(5L);
        InstantBookingCreateRequest request = request();
        request.latitude = 1.0;
        request.longitude = 2.0;
        request.coordinates = new InstantBookingCreateRequest.Coordinates();
        request.coordinates.lat = 28.61;
        request.coordinates.lng = 77.20;
        ReverseGeocodeResponse reverse = new ReverseGeocodeResponse();
        reverse.formattedAddress = "New Delhi";
        when(catalogRepository.findById(5L)).thenReturn(Optional.of(catalogService));
        when(reverseGeocodeService.reverse(28.61, 77.20)).thenReturn(reverse);
        when(orderRepository.save(any())).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        var response = service.create(7L, request);

        assertEquals(100L, response.bookingId);
        assertEquals("INSTANT", response.bookingType);
        assertEquals("SEARCHING_PARTNER", response.assignmentStatus);
        assertEquals(30, response.slaMinutes);
        assertEquals("New Delhi", response.fullAddress);
        assertEquals(80.0, response.totalAmount);
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertEquals(1, captor.getValue().getItems().size());
    }

    @Test
    void createScheduledShouldUseOwnedSavedAddressAndDiscountedPrice() {
        UUID addressId = UUID.randomUUID();
        CustomerAddress address = new CustomerAddress();
        address.setFlatNo("12A");
        address.setBuildingName("Tower");
        address.setAddressLine("Main Road");
        address.setCity("Gurugram");
        InstantBookingCreateRequest request = request();
        request.bookingType = " scheduled ";
        request.date = LocalDate.of(2026, 7, 1);
        request.slot = "10:30-11:30";
        request.addressId = addressId.toString();
        when(catalogRepository.findById(5L)).thenReturn(Optional.of(catalogService(5L)));
        when(addressRepository.findByIdAndCustomerIdAndIsDeletedFalse(addressId, 7L)).thenReturn(Optional.of(address));
        when(orderRepository.save(any())).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            saved.setId(101L);
            return saved;
        });

        var response = service.create(7L, request);

        assertEquals("SCHEDULED", response.bookingType);
        assertEquals("SCHEDULED", response.assignmentStatus);
        assertEquals("10:30-11:30", response.slot);
        assertEquals("12A, Tower, Main Road, Gurugram", response.fullAddress);
        assertNull(response.slaMinutes);
        verifyNoInteractions(reverseGeocodeService);
    }

    @Test
    void createShouldRejectMissingUnavailableAndInvalidPriceServices() {
        InstantBookingCreateRequest request = request();
        when(catalogRepository.findById(5L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.create(7L, request));

        AdminServiceEntity unavailable = catalogService(5L);
        unavailable.setActive(false);
        when(catalogRepository.findById(5L)).thenReturn(Optional.of(unavailable));
        assertThrows(BookingValidationException.class, () -> service.create(7L, request));

        AdminServiceEntity noPrice = catalogService(5L);
        noPrice.setBasePrice(null);
        noPrice.setDiscountedPrice(null);
        when(catalogRepository.findById(5L)).thenReturn(Optional.of(noPrice));
        ReverseGeocodeResponse reverse = new ReverseGeocodeResponse();
        reverse.city = "Delhi";
        when(reverseGeocodeService.reverse(anyDouble(), anyDouble())).thenReturn(reverse);
        assertThrows(BookingValidationException.class, () -> service.create(7L, request));
    }

    @Test
    void scheduledCreateShouldValidateDateAndSlotFormat() {
        InstantBookingCreateRequest request = request();
        request.bookingType = "SCHEDULED";
        when(catalogRepository.findById(5L)).thenReturn(Optional.of(catalogService(5L)));

        BookingValidationException missingDate = assertThrows(BookingValidationException.class,
                () -> service.create(7L, request));
        assertEquals("DATE_REQUIRED", missingDate.getErrorCode());
        request.date = LocalDate.of(2026, 7, 1);
        BookingValidationException missingSlot = assertThrows(BookingValidationException.class,
                () -> service.create(7L, request));
        assertEquals("SLOT_REQUIRED", missingSlot.getErrorCode());
        request.slot = "morning";
        BookingValidationException invalidSlot = assertThrows(BookingValidationException.class,
                () -> service.create(7L, request));
        assertEquals("INVALID_SLOT", invalidSlot.getErrorCode());
    }

    @Test
    void reverseAddressShouldFallBackAcrossFieldsAndCoordinates() {
        InstantBookingCreateRequest request = request();
        request.addressId = "not-a-uuid";
        when(catalogRepository.findById(5L)).thenReturn(Optional.of(catalogService(5L)));
        when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ReverseGeocodeResponse area = new ReverseGeocodeResponse();
        area.area = "Sector 21";
        when(reverseGeocodeService.reverse(28.0, 77.0)).thenReturn(area);
        assertEquals("Sector 21", service.create(7L, request).fullAddress);

        ReverseGeocodeResponse empty = new ReverseGeocodeResponse();
        when(reverseGeocodeService.reverse(28.0, 77.0)).thenReturn(empty);
        assertEquals("28.0, 77.0", service.create(7L, request).fullAddress);
    }

    @Test
    void detailsShouldRejectMissingAndForeignBookings() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.details(7L, 99L));
        Order foreign = order(10L, OrderStatus.CONFIRMED);
        foreign.setUserId(8L);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(foreign));
        assertThrows(UnauthorizedException.class, () -> service.details(7L, 10L));
    }

    @Test
    void detailsShouldAttachAssignedPartnerAndMapLifecycleStatuses() {
        Order order = order(10L, OrderStatus.PARTNER_REACHED);
        order.setAssignedPartnerId(20L);
        User partner = new User("9876543210", true);
        ReflectionTestUtils.setField(partner, "id", 20L);
        partner.setFullName("Ravi");
        partner.setProfileImage("photo.jpg");
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(catalogRepository.findById(5L)).thenReturn(Optional.of(catalogService(5L)));
        when(userRepository.findByIdAndDeletedFalse(20L)).thenReturn(Optional.of(partner));

        var response = service.details(7L, 10L);

        assertEquals("PARTNER_ARRIVED", response.assignmentStatus);
        assertEquals("Ravi", response.partnerName);
        assertEquals("9876543210", response.partnerPhone);
        assertEquals(12, response.etaMinutes);
    }

    @Test
    void detailsShouldMapTerminalAndProgressMessages() {
        Order order = order(10L, OrderStatus.SERVICE_STARTED);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        assertEquals("IN_PROGRESS", service.details(7L, 10L).assignmentStatus);
        order.setStatus(OrderStatus.SERVICE_COMPLETED);
        assertEquals("COMPLETED", service.details(7L, 10L).assignmentStatus);
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

package com.lexho.booking.entity;

import com.lexho.booking.enums.BookingStatus;
import com.lexho.common.enums.OrderStatus;
import com.lexho.common.enums.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingEntityLifecycleTest {

    @Test
    void bookingPrePersistGeneratesDefaultsAndTimestamps() {
        Booking booking = new Booking();

        booking.prePersist();

        assertTrue(booking.getBookingId().matches("BK\\d{4}"));
        assertEquals(BookingStatus.PENDING, booking.getBookingStatus());
        assertEquals(PaymentStatus.PENDING, booking.getPaymentStatus());
        assertNotNull(booking.getCreatedAt());
        assertEquals(booking.getCreatedAt(), booking.getUpdatedAt());
    }

    @Test
    void bookingPrePersistPreservesExplicitValues() {
        Booking booking = new Booking();
        booking.setBookingId("CUSTOM-ID");
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        booking.setPaymentStatus(PaymentStatus.SUCCESS);

        booking.prePersist();

        assertEquals("CUSTOM-ID", booking.getBookingId());
        assertEquals(BookingStatus.CONFIRMED, booking.getBookingStatus());
        assertEquals(PaymentStatus.SUCCESS, booking.getPaymentStatus());
    }

    @Test
    void bookingPreUpdateRefreshesTimestamp() {
        Booking booking = new Booking();
        LocalDateTime old = LocalDateTime.of(2020, 1, 1, 0, 0);
        ReflectionTestUtils.setField(booking, "updatedAt", old);

        booking.preUpdate();

        assertTrue(booking.getUpdatedAt().isAfter(old));
    }

    @Test
    void orderLifecycleMaintainsAuditTimestampsAndItemsCollection() {
        Order order = new Order();
        assertNotNull(order.getItems());

        order.onCreate();
        LocalDateTime created = order.getCreatedAt();
        ReflectionTestUtils.setField(order, "updatedAt", LocalDateTime.of(2020, 1, 1, 0, 0));
        order.onUpdate();

        assertNotNull(created);
        assertTrue(order.getUpdatedAt().isAfter(created.minusSeconds(1)));
    }

    @Test
    void orderAddonLifecycleMaintainsAuditTimestamps() {
        OrderAddon addon = new OrderAddon();
        addon.onCreate();
        LocalDateTime created = addon.getCreatedAt();
        ReflectionTestUtils.setField(addon, "updatedAt", LocalDateTime.of(2020, 1, 1, 0, 0));

        addon.onUpdate();

        assertNotNull(created);
        assertTrue(addon.getUpdatedAt().isAfter(created.minusSeconds(1)));
    }

    @Test
    void cartRetainsConfiguredValues() {
        Cart cart = new Cart();
        ReflectionTestUtils.setField(cart, "id", 10L);
        cart.setUserId(1L);
        cart.setPartnerId(2L);
        cart.setPartnerName("Partner");
        cart.setPrice(299.5);
        cart.setQuantity(3);

        assertEquals(10L, cart.getId());
        assertEquals(1L, cart.getUserId());
        assertEquals(2L, cart.getPartnerId());
        assertEquals("Partner", cart.getPartnerName());
        assertEquals(299.5, cart.getPrice());
        assertEquals(3, cart.getQuantity());
    }

    @Test
    void orderNoArgsConstructorAndSettersExposeEveryPersistedField() {
        Order order = new Order();
        LocalDateTime scheduled = LocalDateTime.of(2026, 7, 1, 9, 0);
        LocalDateTime promised = scheduled.plusMinutes(15);
        LocalDateTime arrived = scheduled.plusMinutes(5);
        LocalDateTime started = scheduled.plusMinutes(10);
        LocalDateTime inProgress = scheduled.plusMinutes(12);
        LocalDateTime completed = scheduled.plusHours(1);
        LocalDateTime created = scheduled.minusDays(1);
        LocalDateTime updated = scheduled.minusHours(1);
        List<OrderItem> items = List.of(new OrderItem());

        order.setId(1L);
        order.setUserId(2L);
        order.setTotalAmount(499.0);
        order.setStatus(OrderStatus.PARTNER_ASSIGNED);
        order.setPaymentStatus(PaymentStatus.SUCCESS);
        order.setAddress("Sector 1");
        order.setScheduledAt(scheduled);
        order.setBookingType("INSTANT");
        order.setPromisedStartTime(promised);
        order.setSlaMinutes(30);
        order.setUnavailableReason("NO_PARTNER");
        order.setAssignedPartnerId(3L);
        order.setArrivedAt(arrived);
        order.setStartedAt(started);
        order.setInProgressAt(inProgress);
        order.setCompletedAt(completed);
        order.setCreatedAt(created);
        order.setUpdatedAt(updated);
        order.setItems(items);

        assertEquals(1L, order.getId());
        assertEquals(2L, order.getUserId());
        assertEquals(499.0, order.getTotalAmount());
        assertEquals(OrderStatus.PARTNER_ASSIGNED, order.getStatus());
        assertEquals(PaymentStatus.SUCCESS, order.getPaymentStatus());
        assertEquals("Sector 1", order.getAddress());
        assertEquals(scheduled, order.getScheduledAt());
        assertEquals("INSTANT", order.getBookingType());
        assertEquals(promised, order.getPromisedStartTime());
        assertEquals(30, order.getSlaMinutes());
        assertEquals("NO_PARTNER", order.getUnavailableReason());
        assertEquals(3L, order.getAssignedPartnerId());
        assertEquals(arrived, order.getArrivedAt());
        assertEquals(started, order.getStartedAt());
        assertEquals(inProgress, order.getInProgressAt());
        assertEquals(completed, order.getCompletedAt());
        assertEquals(created, order.getCreatedAt());
        assertEquals(updated, order.getUpdatedAt());
        assertEquals(items, order.getItems());
    }

    @Test
    void orderAllArgsConstructorAssignsAllFields() {
        LocalDateTime time = LocalDateTime.of(2026, 7, 1, 9, 0);
        List<OrderItem> items = List.of();

        Order order = new Order(1L, 2L, 300.0, OrderStatus.CREATED, PaymentStatus.PENDING,
                "Address", time, "SCHEDULED", time, 45, null, 4L,
                time, time, time, time, time, time, items);

        assertEquals(1L, order.getId());
        assertEquals(2L, order.getUserId());
        assertEquals(300.0, order.getTotalAmount());
        assertEquals(OrderStatus.CREATED, order.getStatus());
        assertEquals(items, order.getItems());
    }

    @Test
    void orderItemBuilderAndAccessorsCoverEveryField() {
        Order order = new Order();
        OrderItem.OrderItemBuilder builder = OrderItem.builder()
                .id(1L)
                .partnerId(2L)
                .partnerName("Chef")
                .price(125.5)
                .quantity(2)
                .order(order);
        OrderItem item = builder.build();

        assertEquals(1L, item.getId());
        assertEquals(2L, item.getPartnerId());
        assertEquals("Chef", item.getPartnerName());
        assertEquals(125.5, item.getPrice());
        assertEquals(2, item.getQuantity());
        assertEquals(order, item.getOrder());
        assertTrue(builder.toString().contains("OrderItemBuilder"));
    }

    @Test
    void orderItemNoArgsAndAllArgsConstructorsSupportMutation() {
        Order order = new Order();
        OrderItem allArgs = new OrderItem(1L, 2L, "Cook", 50.0, 3, order);
        OrderItem mutable = new OrderItem();
        mutable.setId(allArgs.getId());
        mutable.setPartnerId(allArgs.getPartnerId());
        mutable.setPartnerName(allArgs.getPartnerName());
        mutable.setPrice(allArgs.getPrice());
        mutable.setQuantity(allArgs.getQuantity());
        mutable.setOrder(allArgs.getOrder());

        assertEquals(allArgs.getId(), mutable.getId());
        assertEquals(allArgs.getPartnerId(), mutable.getPartnerId());
        assertEquals(allArgs.getPartnerName(), mutable.getPartnerName());
        assertEquals(allArgs.getPrice(), mutable.getPrice());
        assertEquals(allArgs.getQuantity(), mutable.getQuantity());
        assertEquals(allArgs.getOrder(), mutable.getOrder());
    }

    @Test
    void orderAddonAccessorsCoverEveryField() {
        OrderAddon addon = new OrderAddon();
        LocalDateTime created = LocalDateTime.of(2026, 6, 1, 8, 0);
        LocalDateTime updated = created.plusHours(1);
        addon.setId(1L);
        addon.setOrderId(2L);
        addon.setServiceId(3L);
        addon.setAddonId(4L);
        addon.setAddonNameSnapshot("Extra cleaning");
        addon.setAddonPriceSnapshot(BigDecimal.valueOf(99));
        addon.setQuantity(2);
        addon.setTotalPrice(BigDecimal.valueOf(198));
        addon.setCreatedAt(created);
        addon.setUpdatedAt(updated);

        assertEquals(1L, addon.getId());
        assertEquals(2L, addon.getOrderId());
        assertEquals(3L, addon.getServiceId());
        assertEquals(4L, addon.getAddonId());
        assertEquals("Extra cleaning", addon.getAddonNameSnapshot());
        assertEquals(BigDecimal.valueOf(99), addon.getAddonPriceSnapshot());
        assertEquals(2, addon.getQuantity());
        assertEquals(BigDecimal.valueOf(198), addon.getTotalPrice());
        assertEquals(created, addon.getCreatedAt());
        assertEquals(updated, addon.getUpdatedAt());
    }
}

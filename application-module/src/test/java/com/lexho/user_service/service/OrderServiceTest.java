package com.lexho.user_service.service;

import com.lexho.booking.entity.Cart;
import com.lexho.booking.entity.Order;
import com.lexho.booking.entity.OrderItem;
import com.lexho.booking.repository.CartRepository;
import com.lexho.booking.repository.OrderAddonRepository;
import com.lexho.booking.repository.OrderRepository;
import com.lexho.common.enums.OrderStatus;
import com.lexho.common.enums.PaymentStatus;
import com.lexho.user_service.addon.dto.AddonSelectionRequest;
import com.lexho.user_service.addon.entity.AddonEntity;
import com.lexho.user_service.addon.entity.CartItemAddonEntity;
import com.lexho.user_service.addon.repository.CartItemAddonRepository;
import com.lexho.user_service.addon.service.AddonManagementService;
import com.lexho.user_service.admin.entity.AdminServiceEntity;
import com.lexho.user_service.booking.exception.BookingValidationException;
import com.lexho.user_service.customer.entity.CustomerCartItemEntity;
import com.lexho.user_service.customer.repository.CustomerCartItemRepository;
import com.lexho.user_service.customer.repository.CustomerCatalogRepository;
import com.lexho.user_service.dto.PlaceOrderRequest;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.exception.UnauthorizedException;
import com.lexho.user_service.notification.service.NotificationManagementService;
import com.lexho.user_service.observability.BusinessMetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock OrderRepository orderRepository;
    @Mock CartRepository cartRepository;
    @Mock CustomerCartItemRepository customerCartItemRepository;
    @Mock CustomerCatalogRepository customerCatalogRepository;
    @Mock NotificationManagementService notificationManagementService;
    @Mock AddonManagementService addonManagementService;
    @Mock CartItemAddonRepository cartItemAddonRepository;
    @Mock OrderAddonRepository orderAddonRepository;
    @Mock BusinessMetricsService businessMetricsService;
    private OrderService service;

    @BeforeEach
    void setUp() {
        service = new OrderService(orderRepository, cartRepository, customerCartItemRepository,
                customerCatalogRepository, notificationManagementService, addonManagementService,
                cartItemAddonRepository, orderAddonRepository, businessMetricsService);
    }

    @Test
    void getOrderMapsOwnedOrderAndRejectsMissingOrForeignOrder() {
        Order order = order(5L, 7L, OrderStatus.CREATED);
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));
        assertEquals("CREATED", service.getOrder(5L, 7L).getStatus());
        assertThrows(UnauthorizedException.class, () -> service.getOrder(5L, 8L));

        when(orderRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getOrder(99L, 7L));
    }

    @Test
    void getUserOrdersMapsEveryOrder() {
        when(orderRepository.findByUserId(7L)).thenReturn(List.of(
                order(1L, 7L, OrderStatus.CREATED), order(2L, 7L, OrderStatus.CONFIRMED)));
        assertEquals(2, service.getUserOrders(7L).size());
    }

    @Test
    void placeOrderRejectsEmptyAndInvalidCart() {
        when(cartRepository.findByUserId(7L)).thenReturn(List.of());
        when(customerCartItemRepository.findByUserId(7L)).thenReturn(List.of());
        assertThrows(BookingValidationException.class, () -> service.placeOrder(7L, request(null)));

        Cart invalid = cart(null, 1);
        when(cartRepository.findByUserId(7L)).thenReturn(List.of(invalid));
        assertThrows(BookingValidationException.class, () -> service.placeOrder(7L, request(null)));

        invalid.setPrice(100.0); invalid.setQuantity(0);
        assertThrows(BookingValidationException.class, () -> service.placeOrder(7L, request(null)));
    }

    @Test
    void placeOrderPersistsClearsCartAndRecordsMetric() {
        Cart cart = cart(125.0, 2);
        when(cartRepository.findByUserId(7L)).thenReturn(List.of(cart));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(44L);
            return order;
        });

        var result = service.placeOrder(7L, request(BigDecimal.valueOf(250)));

        assertEquals(44L, result.getId());
        assertEquals(250.0, result.getTotalAmount());
        verify(cartRepository).deleteAll(List.of(cart));
        verify(businessMetricsService).incrementOrdersCreated();
        verify(notificationManagementService).sendBulkNotification(any());
    }

    @Test
    void placeOrderRejectsChangedPrice() {
        when(cartRepository.findByUserId(7L)).thenReturn(List.of(cart(100.0, 1)));
        assertThrows(BookingValidationException.class,
                () -> service.placeOrder(7L, request(BigDecimal.valueOf(101))));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void updateStatusValidatesTransitionAndTracksCompletion() {
        Order created = order(5L, 7L, OrderStatus.CREATED);
        when(orderRepository.findById(5L)).thenReturn(Optional.of(created));
        when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        assertEquals("CONFIRMED", service.updateStatus(5L, OrderStatus.CONFIRMED).getStatus());

        created.setStatus(OrderStatus.SERVICE_STARTED);
        assertEquals("SERVICE_COMPLETED", service.updateStatus(5L, OrderStatus.SERVICE_COMPLETED).getStatus());
        verify(businessMetricsService).incrementOrdersCompleted();

        created.setStatus(OrderStatus.DELIVERED);
        assertThrows(IllegalStateException.class, () -> service.updateStatus(5L, OrderStatus.CREATED));
    }

    @Test
    void updateStatusAlsoCompletesDeliveredOrdersAndRejectsMissingOrder() {
        Order confirmed = order(6L, 7L, OrderStatus.OUT_FOR_DELIVERY);
        when(orderRepository.findById(6L)).thenReturn(Optional.of(confirmed));
        when(orderRepository.findById(66L)).thenReturn(Optional.empty());
        when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals("DELIVERED", service.updateStatus(6L, OrderStatus.DELIVERED).getStatus());
        verify(businessMetricsService).incrementOrdersCompleted();
        assertThrows(ResourceNotFoundException.class, () -> service.updateStatus(66L, OrderStatus.CREATED));
    }

    @Test
    void cancelOrderChecksOwnershipAndLifecycle() {
        Order order = order(5L, 7L, OrderStatus.CREATED);
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        assertEquals("CANCELLED", service.cancelOrder(5L, 7L).getStatus());

        order.setStatus(OrderStatus.DELIVERED);
        assertThrows(IllegalStateException.class, () -> service.cancelOrder(5L, 7L));
        assertThrows(UnauthorizedException.class, () -> service.cancelOrder(5L, 8L));
    }

    @Test
    void placeOrderUsesCustomerCartWithAddonsAndClearsBothTables() {
        CustomerCartItemEntity cartItem = new CustomerCartItemEntity();
        cartItem.setId(9L);
        cartItem.setUserId(7L);
        cartItem.setServiceId(3L);
        cartItem.setQuantity(2);

        CartItemAddonEntity addonRow = new CartItemAddonEntity();
        addonRow.setCartItemId(9L);
        addonRow.setAddonId(12L);
        addonRow.setQuantity(3);

        AdminServiceEntity serviceEntity = new AdminServiceEntity();
        serviceEntity.setName("Cleaning");
        serviceEntity.setActive(true);
        serviceEntity.setDeleted(false);
        serviceEntity.setDiscountedPrice(BigDecimal.valueOf(90));

        AddonEntity addon = new AddonEntity();
        ReflectionTestUtils.setField(addon, "id", 12L);
        addon.setAddonName("Extra");
        addon.setPrice(BigDecimal.TEN);

        when(cartRepository.findByUserId(7L)).thenReturn(List.of());
        when(customerCartItemRepository.findByUserId(7L)).thenReturn(List.of(cartItem));
        when(cartItemAddonRepository.findByCartItemIdIn(List.of(9L))).thenReturn(List.of(addonRow));
        when(customerCatalogRepository.findById(3L)).thenReturn(Optional.of(serviceEntity));
        when(addonManagementService.validateSelections(eq(serviceEntity), any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<AddonSelectionRequest> requests = invocation.getArgument(1);
            assertThat(requests).hasSize(1);
            return new AddonManagementService.ValidatedAddonSelection(
                    List.of(new AddonManagementService.ValidatedAddon(addon, 3)));
        });
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(77L);
            return order;
        });

        var result = service.placeOrder(7L, request(BigDecimal.valueOf(210)));

        assertEquals(77L, result.getId());
        assertEquals(210.0, result.getTotalAmount());
        verify(orderAddonRepository).saveAll(any());
        verify(cartItemAddonRepository).deleteByCartItemIdIn(List.of(9L));
        verify(customerCartItemRepository).deleteAll(List.of(cartItem));
    }

    @Test
    void placeOrderCustomerCartRejectsUnavailableOrInvalidService() {
        CustomerCartItemEntity cartItem = new CustomerCartItemEntity();
        cartItem.setId(9L);
        cartItem.setUserId(7L);
        cartItem.setServiceId(3L);
        cartItem.setQuantity(1);
        when(cartRepository.findByUserId(7L)).thenReturn(List.of());
        when(customerCartItemRepository.findByUserId(7L)).thenReturn(List.of(cartItem));
        when(cartItemAddonRepository.findByCartItemIdIn(List.of(9L))).thenReturn(List.of());

        when(customerCatalogRepository.findById(3L)).thenReturn(Optional.empty());
        assertThrows(BookingValidationException.class, () -> service.placeOrder(7L, request(null)));

        AdminServiceEntity inactive = new AdminServiceEntity();
        inactive.setActive(false);
        inactive.setDeleted(false);
        when(customerCatalogRepository.findById(3L)).thenReturn(Optional.of(inactive));
        assertThrows(BookingValidationException.class, () -> service.placeOrder(7L, request(null)));

        AdminServiceEntity noPrice = new AdminServiceEntity();
        noPrice.setActive(true);
        noPrice.setDeleted(false);
        noPrice.setBasePrice(BigDecimal.ZERO);
        when(customerCatalogRepository.findById(3L)).thenReturn(Optional.of(noPrice));
        assertThrows(BookingValidationException.class, () -> service.placeOrder(7L, request(null)));
    }

    @Test
    void placeOrderIgnoresNotificationFailuresAndMapsNullItems() {
        Cart cart = cart(50.0, 1);
        when(cartRepository.findByUserId(7L)).thenReturn(List.of(cart));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(90L);
            return order;
        });
        doThrow(new RuntimeException("down")).when(notificationManagementService).sendBulkNotification(any());

        var result = service.placeOrder(7L, request(BigDecimal.valueOf(50)));

        assertEquals(90L, result.getId());

        Order raw = new Order();
        raw.setId(10L);
        raw.setUserId(7L);
        raw.setStatus(OrderStatus.CREATED);
        raw.setPaymentStatus(null);
        raw.setItems(null);
        assertThat(service.mapToDTO(raw).getItems()).isEmpty();
        assertThat(service.mapToDTO(raw).getPaymentStatus()).isEqualTo("PENDING");
    }

    @Test
    void mapToDtoCopiesItems() {
        Order order = order(15L, 7L, OrderStatus.CREATED);
        OrderItem item = new OrderItem();
        item.setPartnerId(3L);
        item.setPartnerName("Cleaner");
        item.setPrice(25.0);
        item.setQuantity(2);
        order.setItems(List.of(item));

        var dto = service.mapToDTO(order);

        assertThat(dto.getItems()).hasSize(1);
        assertThat(dto.getItems().get(0).getPartnerName()).isEqualTo("Cleaner");
    }

    private Order order(Long id, Long userId, OrderStatus status) {
        Order order = new Order();
        order.setId(id); order.setUserId(userId); order.setStatus(status);
        order.setPaymentStatus(PaymentStatus.PENDING); order.setTotalAmount(100.0);
        return order;
    }

    private Cart cart(Double price, Integer quantity) {
        Cart cart = new Cart();
        cart.setPartnerId(3L); cart.setPartnerName("Cleaner");
        cart.setPrice(price); cart.setQuantity(quantity);
        return cart;
    }

    private PlaceOrderRequest request(BigDecimal expectedTotal) {
        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setAddress("Address"); request.setBookingType("SCHEDULED"); request.setExpectedTotal(expectedTotal);
        return request;
    }
}

package com.lexho.user_service.service;

import com.lexho.booking.entity.Cart;
import com.lexho.booking.entity.Order;
import com.lexho.booking.entity.OrderAddon;
import com.lexho.booking.entity.OrderItem;
import com.lexho.booking.repository.CartRepository;
import com.lexho.booking.repository.OrderAddonRepository;
import com.lexho.booking.repository.OrderRepository;
import com.lexho.common.enums.OrderStatus;
import com.lexho.common.enums.PaymentStatus;
import com.lexho.notification.enums.ChannelType;
import com.lexho.notification.enums.NotificationPriority;
import com.lexho.notification.enums.NotificationType;
import com.lexho.notification.enums.NotificationUserType;
import com.lexho.user_service.addon.dto.AddonSelectionRequest;
import com.lexho.user_service.addon.entity.CartItemAddonEntity;
import com.lexho.user_service.addon.repository.CartItemAddonRepository;
import com.lexho.user_service.addon.service.AddonManagementService;
import com.lexho.user_service.admin.entity.AdminServiceEntity;
import com.lexho.user_service.customer.entity.CustomerCartItemEntity;
import com.lexho.user_service.customer.repository.CustomerCartItemRepository;
import com.lexho.user_service.customer.repository.CustomerCatalogRepository;
import com.lexho.user_service.coupon.service.CouponService;
import com.lexho.user_service.dto.OrderItemDTO;
import com.lexho.user_service.dto.OrderResponseDTO;
import com.lexho.user_service.dto.PlaceOrderRequest;
import com.lexho.user_service.booking.exception.BookingValidationException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.exception.UnauthorizedException;
import com.lexho.user_service.notification.dto.AdminBulkNotificationRequest;
import com.lexho.user_service.notification.service.NotificationManagementService;
import com.lexho.user_service.observability.BusinessMetricsService;
import com.lexho.user_service.lexhopartner.service.CompletionSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OrderService {
    private static final String ORDER_NOT_FOUND_WITH_ID = "Order not found with id: ";
    private static final String INVALID_CART = "INVALID_CART";
    private static final String CART_PRICE_MISSING = "Cart price missing for cart item.";

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CustomerCartItemRepository customerCartItemRepository;
    private final CustomerCatalogRepository customerCatalogRepository;
    private final NotificationManagementService notificationManagementService;
    private final AddonManagementService addonManagementService;
    private final CartItemAddonRepository cartItemAddonRepository;
    private final OrderAddonRepository orderAddonRepository;
    private final BusinessMetricsService businessMetricsService;
    private final CouponService couponService;
    private final CompletionSettingsService completionSettingsService;

    public OrderService(OrderRepository orderRepository,
                        CartRepository cartRepository,
                        CustomerCartItemRepository customerCartItemRepository,
                        CustomerCatalogRepository customerCatalogRepository,
                        NotificationManagementService notificationManagementService,
                        AddonManagementService addonManagementService,
                        CartItemAddonRepository cartItemAddonRepository,
                        OrderAddonRepository orderAddonRepository,
                        BusinessMetricsService businessMetricsService,
                        CouponService couponService) {
        this(orderRepository, cartRepository, customerCartItemRepository, customerCatalogRepository,
                notificationManagementService, addonManagementService, cartItemAddonRepository,
                orderAddonRepository, businessMetricsService, couponService, Optional.empty());
    }

    @Autowired
    public OrderService(OrderRepository orderRepository,
                        CartRepository cartRepository,
                        CustomerCartItemRepository customerCartItemRepository,
                        CustomerCatalogRepository customerCatalogRepository,
                        NotificationManagementService notificationManagementService,
                        AddonManagementService addonManagementService,
                        CartItemAddonRepository cartItemAddonRepository,
                        OrderAddonRepository orderAddonRepository,
                        BusinessMetricsService businessMetricsService,
                        CouponService couponService,
                        Optional<CompletionSettingsService> completionSettingsService) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.customerCartItemRepository = customerCartItemRepository;
        this.customerCatalogRepository = customerCatalogRepository;
        this.notificationManagementService = notificationManagementService;
        this.addonManagementService = addonManagementService;
        this.cartItemAddonRepository = cartItemAddonRepository;
        this.orderAddonRepository = orderAddonRepository;
        this.businessMetricsService = businessMetricsService;
        this.couponService = couponService;
        this.completionSettingsService = optionalValue(completionSettingsService);
    }

    private static <T> T optionalValue(Optional<T> optional) {
        return optional == null ? null : optional.orElse(null);
    }

    @Transactional(readOnly = true)
    public OrderResponseDTO getOrder(Long id, Long userId) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ORDER_NOT_FOUND_WITH_ID + id));
        validateOrderAccess(order, userId);
        return mapToDTO(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getUserOrders(Long userId) {
        List<Order> orders = orderRepository.findByUserId(userId);
        List<OrderResponseDTO> list = new ArrayList<>();
        for (Order order : orders) {
            list.add(mapToDTO(order));
        }
        return list;
    }

    @Transactional
    public OrderResponseDTO placeOrder(Long userId, PlaceOrderRequest request) {
        List<Cart> cartItems = cartRepository.findByUserId(userId);
        List<CustomerCartItemEntity> customerCartItems = java.util.Collections.emptyList();

        if (cartItems.isEmpty()) {
            customerCartItems = customerCartItemRepository.findByUserId(userId);
            if (customerCartItems.isEmpty()) {
                throw new BookingValidationException("CART_EMPTY", "Cart is empty. Add service to cart before booking.");
            }
        }

        Order order = new Order();
        order.setUserId(userId);
        // Keep initial status compatible with existing DB enum values.
        order.setStatus(OrderStatus.CREATED);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setAddress(request.getAddress());
        order.setScheduledAt(request.getScheduledAt());
        order.setBookingType(request.getBookingType());
        order.setDispatchAddressId(request.getDispatchAddressId());
        order.setDispatchServiceId(request.getDispatchServiceId());
        order.setDispatchQuantity(request.getDispatchQuantity());

        OrderContents contents;

        if (!cartItems.isEmpty()) {
            contents = fromLegacyCart(order, cartItems);
        } else {
            contents = fromCustomerCart(order, customerCartItems);
        }

        if (request.getExpectedTotal() != null
                && Math.abs(request.getExpectedTotal().doubleValue() - contents.total()) > 0.01d) {
            throw new BookingValidationException("PRICE_CHANGED",
                    "Price has changed. Please refresh cart and try again.");
        }

        order.setItems(contents.items());
        order.setTotalAmount(contents.total());
        couponService.applyToOrder(order, request.getCouponCode(), firstServiceId(contents.items()), request.getBookingType());

        Order savedOrder = orderRepository.save(order);
        for (OrderAddon addon : contents.addons()) {
            addon.setOrderId(savedOrder.getId());
        }
        if (!contents.addons().isEmpty()) {
            orderAddonRepository.saveAll(contents.addons());
        }

        try {
            sendOrderNotification(
                    userId,
                    "Order Placed",
                    "Your order has been placed successfully",
                    savedOrder.getId()
            );
        } catch (Exception ignored) {
            // Keep order placement resilient even if notification pipeline fails.
        }

        if (!cartItems.isEmpty()) {
            cartRepository.deleteAll(cartItems);
        }
        if (!customerCartItems.isEmpty()) {
            cartItemAddonRepository.deleteByCartItemIdIn(customerCartItems.stream().map(CustomerCartItemEntity::getId).toList());
            customerCartItemRepository.deleteAll(customerCartItems);
        }
        businessMetricsService.incrementOrdersCreated();
        return mapToDTO(savedOrder);
    }

    private OrderContents fromLegacyCart(Order order, List<Cart> cartItems) {
        List<OrderItem> orderItems = new ArrayList<>();
        double total = 0d;
        for (Cart cart : cartItems) {
            if (cart.getPrice() == null) {
                throw new BookingValidationException(INVALID_CART, CART_PRICE_MISSING);
            }
            validateQuantity(cart.getQuantity());

            OrderItem item = new OrderItem();
            item.setPartnerId(cart.getPartnerId());
            item.setPartnerName(cart.getPartnerName());
            item.setPrice(cart.getPrice());
            item.setQuantity(cart.getQuantity());
            item.setOrder(order);
            orderItems.add(item);
            total += cart.getPrice() * cart.getQuantity();
        }
        return new OrderContents(orderItems, List.of(), total);
    }

    private OrderContents fromCustomerCart(Order order, List<CustomerCartItemEntity> customerCartItems) {
        List<OrderItem> orderItems = new ArrayList<>();
        List<OrderAddon> orderAddons = new ArrayList<>();
        double total = 0d;
        Map<Long, List<CartItemAddonEntity>> cartAddonsByCartItemId = cartItemAddonRepository.findByCartItemIdIn(
                customerCartItems.stream().map(CustomerCartItemEntity::getId).toList()
        ).stream().collect(Collectors.groupingBy(CartItemAddonEntity::getCartItemId));
        for (CustomerCartItemEntity cart : customerCartItems) {
            AdminServiceEntity service = validatedService(cart);
            double unitPrice = resolveUnitPrice(service);
            if (unitPrice <= 0) {
                throw new BookingValidationException(INVALID_CART, CART_PRICE_MISSING);
            }
            orderItems.add(orderItem(order, cart, service, unitPrice));
            total += unitPrice * cart.getQuantity();
            total += addOrderAddons(orderAddons, cart, service, cartAddonsByCartItemId.get(cart.getId()));
        }
        return new OrderContents(orderItems, orderAddons, total);
    }

    private AdminServiceEntity validatedService(CustomerCartItemEntity cart) {
        validateQuantity(cart.getQuantity());
        AdminServiceEntity service = customerCatalogRepository.findById(cart.getServiceId()).orElse(null);
        if (service == null || !service.isActive() || service.isDeleted()) {
            throw new BookingValidationException("SERVICE_UNAVAILABLE", "Service is unavailable");
        }
        return service;
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BookingValidationException(INVALID_CART, "Invalid quantity in cart.");
        }
    }

    private OrderItem orderItem(Order order, CustomerCartItemEntity cart, AdminServiceEntity service, double unitPrice) {
        OrderItem item = new OrderItem();
        item.setPartnerId(cart.getServiceId());
        item.setPartnerName(service.getName() == null ? "Service" : service.getName());
        item.setPrice(unitPrice);
        item.setQuantity(cart.getQuantity());
        item.setOrder(order);
        return item;
    }

    private double addOrderAddons(List<OrderAddon> orderAddons,
                                  CustomerCartItemEntity cart,
                                  AdminServiceEntity service,
                                  List<CartItemAddonEntity> cartAddonRows) {
        AddonManagementService.ValidatedAddonSelection addonSelection =
                addonManagementService.validateSelections(service, toAddonSelections(cartAddonRows));
        double total = 0d;
        for (AddonManagementService.ValidatedAddon selected : addonSelection.getAddons()) {
            OrderAddon addon = new OrderAddon();
            addon.setServiceId(cart.getServiceId());
            addon.setAddonId(selected.getAddon().getId());
            addon.setAddonNameSnapshot(selected.getAddon().getAddonName());
            addon.setAddonPriceSnapshot(selected.getAddon().getPrice());
            addon.setQuantity(selected.getQuantity());
            addon.setTotalPrice(selected.totalPrice());
            orderAddons.add(addon);
            total += selected.totalPrice().doubleValue();
        }
        return total;
    }

    private record OrderContents(List<OrderItem> items, List<OrderAddon> addons, double total) {
    }

    private List<AddonSelectionRequest> toAddonSelections(List<CartItemAddonEntity> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        return rows.stream().map(row -> {
            AddonSelectionRequest request = new AddonSelectionRequest();
            request.setAddonId(row.getAddonId());
            request.setQuantity(row.getQuantity());
            return request;
        }).toList();
    }

    private double resolveUnitPrice(AdminServiceEntity service) {
        if (service == null) {
            return 0d;
        }
        if (service.getDiscountedPrice() != null && service.getDiscountedPrice().doubleValue() > 0d) {
            return service.getDiscountedPrice().doubleValue();
        }
        return service.getBasePrice() == null ? 0d : service.getBasePrice().doubleValue();
    }

    private Long firstServiceId(List<OrderItem> orderItems) {
        if (orderItems == null || orderItems.isEmpty()) {
            return null;
        }
        OrderItem firstItem = orderItems.get(0);
        return firstItem == null ? null : firstItem.getPartnerId();
    }

    public OrderResponseDTO updateStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ORDER_NOT_FOUND_WITH_ID + orderId));

        OrderStatus current = order.getStatus();
        if (current == newStatus) {
            return mapToDTO(order);
        }
        if (current == null || !current.canTransitionTo(newStatus)) {
            throw new IllegalStateException("Invalid status transition: " + current + " to " + newStatus);
        }

        order.setStatus(newStatus);
        Order updated = orderRepository.save(order);
        if (newStatus == OrderStatus.SERVICE_COMPLETED || newStatus == OrderStatus.DELIVERED) {
            businessMetricsService.incrementOrdersCompleted();
        }

        sendOrderNotification(
                order.getUserId(),
                "Order Update",
                "Your order is now " + newStatus.name(),
                order.getId()
        );

        return mapToDTO(updated);
    }

    public OrderResponseDTO cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ORDER_NOT_FOUND_WITH_ID + orderId));
        validateOrderAccess(order, userId);
        return cancelLockedOrder(order);
    }

    public OrderResponseDTO cancelOrder(Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ORDER_NOT_FOUND_WITH_ID + orderId));
        return cancelLockedOrder(order);
    }

    private OrderResponseDTO cancelLockedOrder(Order order) {
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return mapToDTO(order);
        }
        if (order.getStatus() == null || !order.getStatus().canCancel()) {
            throw new IllegalStateException("Order cannot be cancelled at this stage");
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order updated = orderRepository.save(order);

        sendOrderNotification(
                order.getUserId(),
                "Order Cancelled",
                "Your order has been cancelled",
                order.getId()
        );

        return mapToDTO(updated);
    }

    public OrderResponseDTO mapToDTO(Order order) {
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setId(order.getId());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setOriginalAmount(order.getOriginalAmount());
        dto.setDiscountAmount(order.getDiscountAmount());
        dto.setCouponCode(order.getCouponCode());
        dto.setStatus(order.getStatus().name());
        dto.setPaymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().name() : "PENDING");
        dto.setAddress(order.getAddress());
        dto.setBookingType(order.getBookingType());
        dto.setCompletedAt(order.getCompletedAt());
        dto.setReviewEligible(isReviewEligible(order));
        dto.setPromisedStartTime(order.getPromisedStartTime());
        dto.setSlaMinutes(order.getSlaMinutes());
        dto.setUnavailableReason(order.getUnavailableReason());

        List<OrderItemDTO> items = new ArrayList<>();
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                OrderItemDTO dtoItem = new OrderItemDTO();
                dtoItem.setPartnerId(item.getPartnerId());
                dtoItem.setPartnerName(item.getPartnerName());
                dtoItem.setPrice(item.getPrice());
                dtoItem.setQuantity(item.getQuantity());
                items.add(dtoItem);
            }
        }

        dto.setItems(items);
        return dto;
    }

    private void validateOrderAccess(Order order, Long userId) {
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new UnauthorizedException("You are not allowed to access this order");
        }
    }

    private boolean isReviewEligible(Order order) {
        OrderStatus status = order.getStatus();
        boolean completed = status == OrderStatus.SERVICE_COMPLETED
                || status == OrderStatus.PAYMENT_COMPLETED
                || status == OrderStatus.DELIVERED;
        return completed && (completionSettingsService == null || completionSettingsService.reviewEligibleFor(order));
    }

    private void sendOrderNotification(Long userId, String title, String message, Long orderId) {
        AdminBulkNotificationRequest request = new AdminBulkNotificationRequest();
        request.setTitle(title);
        request.setMessage(message);
        request.setNotificationType(NotificationType.BOOKING);
        request.setPriority(NotificationPriority.HIGH);
        request.setChannels(List.of(ChannelType.PUSH, ChannelType.IN_APP));
        request.setTargetUserType(NotificationUserType.CUSTOMER);
        request.setUserIds(List.of(userId));
        request.setReferenceId(orderId);
        request.setReferenceType("BOOKING");
        request.setRedirectType("TRACKING");
        request.setRedirectValue(String.valueOf(orderId));
        notificationManagementService.sendBulkNotification(request);
    }
}

package com.lexho.user_service.service;

import com.lexho.booking.entity.Cart;
import com.lexho.booking.entity.Order;
import com.lexho.booking.entity.OrderItem;
import com.lexho.booking.repository.CartRepository;
import com.lexho.booking.repository.OrderRepository;
import com.lexho.common.enums.OrderStatus;
import com.lexho.common.enums.PaymentStatus;
import com.lexho.user_service.admin.entity.AdminServiceEntity;
import com.lexho.user_service.customer.entity.CustomerCartItemEntity;
import com.lexho.user_service.customer.repository.CustomerCartItemRepository;
import com.lexho.user_service.customer.repository.CustomerCatalogRepository;
import com.lexho.user_service.dto.OrderItemDTO;
import com.lexho.user_service.dto.OrderResponseDTO;
import com.lexho.user_service.dto.PlaceOrderRequest;
import com.lexho.user_service.booking.exception.BookingValidationException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CustomerCartItemRepository customerCartItemRepository;
    private final CustomerCatalogRepository customerCatalogRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public OrderResponseDTO getOrder(Long id, Long userId) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
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

        List<OrderItem> orderItems = new ArrayList<>();
        double total = 0;

        if (!cartItems.isEmpty()) {
            for (Cart cart : cartItems) {
                if (cart.getPrice() == null) {
                    throw new BookingValidationException("INVALID_CART", "Cart price missing for cart item.");
                }
                if (cart.getQuantity() == null || cart.getQuantity() <= 0) {
                    throw new BookingValidationException("INVALID_CART", "Invalid quantity in cart.");
                }

                OrderItem item = new OrderItem();
                item.setPartnerId(cart.getPartnerId());
                item.setPartnerName(cart.getPartnerName());
                item.setPrice(cart.getPrice());
                item.setQuantity(cart.getQuantity());
                item.setOrder(order);
                orderItems.add(item);

                total += cart.getPrice() * cart.getQuantity();
            }
        } else {
            for (CustomerCartItemEntity cart : customerCartItems) {
                if (cart.getQuantity() == null || cart.getQuantity() <= 0) {
                    throw new BookingValidationException("INVALID_CART", "Invalid quantity in cart.");
                }
                AdminServiceEntity service = customerCatalogRepository.findById(cart.getServiceId()).orElse(null);
                double unitPrice = resolveUnitPrice(service);
                if (unitPrice <= 0) {
                    throw new BookingValidationException("INVALID_CART", "Cart price missing for cart item.");
                }

                OrderItem item = new OrderItem();
                item.setPartnerId(cart.getServiceId());
                item.setPartnerName(service == null || service.getName() == null ? "Service" : service.getName());
                item.setPrice(unitPrice);
                item.setQuantity(cart.getQuantity());
                item.setOrder(order);
                orderItems.add(item);

                total += unitPrice * cart.getQuantity();
            }
        }

        order.setItems(orderItems);
        order.setTotalAmount(total);

        Order savedOrder = orderRepository.save(order);

        try {
            notificationService.send(
                    userId,
                    "Order Placed",
                    "Your order has been placed successfully",
                    "ORDER",
                    savedOrder.getId()
            );
        } catch (Exception ignored) {
            // Keep order placement resilient even if notification pipeline fails.
        }

        if (!cartItems.isEmpty()) {
            cartRepository.deleteAll(cartItems);
        }
        if (!customerCartItems.isEmpty()) {
            customerCartItemRepository.deleteAll(customerCartItems);
        }
        return mapToDTO(savedOrder);
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

    public OrderResponseDTO updateStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!order.getStatus().canTransitionTo(newStatus)) {
            throw new IllegalStateException("Invalid status transition: " + order.getStatus() + " to " + newStatus);
        }

        order.setStatus(newStatus);
        Order updated = orderRepository.save(order);

        notificationService.send(
                order.getUserId(),
                "Order Update",
                "Your order is now " + newStatus.name(),
                "ORDER",
                order.getId()
        );

        return mapToDTO(updated);
    }

    public OrderResponseDTO cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        validateOrderAccess(order, userId);

        if (!order.getStatus().canCancel()) {
            throw new IllegalStateException("Order cannot be cancelled at this stage");
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order updated = orderRepository.save(order);

        notificationService.send(
                order.getUserId(),
                "Order Cancelled",
                "Your order has been cancelled",
                "ORDER",
                order.getId()
        );

        return mapToDTO(updated);
    }

    public OrderResponseDTO mapToDTO(Order order) {
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setId(order.getId());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setStatus(order.getStatus().name());
        dto.setPaymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().name() : "PENDING");
        dto.setAddress(order.getAddress());
        dto.setBookingType(order.getBookingType());
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
        if (!order.getUserId().equals(userId)) {
            throw new UnauthorizedException("You are not allowed to access this order");
        }
    }
}

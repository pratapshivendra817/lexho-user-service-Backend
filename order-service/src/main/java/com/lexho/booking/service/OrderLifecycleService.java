package com.lexho.booking.service;

import com.lexho.booking.entity.Order;
import com.lexho.booking.repository.OrderRepository;
import com.lexho.common.enums.OrderStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderLifecycleService {

    private static final String ORDER_NOT_FOUND_WITH_ID = "Order not found with id: ";

    private final OrderRepository orderRepository;

    public OrderLifecycleService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public Order transition(Long orderId, OrderStatus targetStatus) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalArgumentException(ORDER_NOT_FOUND_WITH_ID + orderId));
        return transition(order, targetStatus);
    }

    @Transactional
    public Order transitionIfCurrent(Long orderId, OrderStatus expectedCurrentStatus, OrderStatus targetStatus) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalArgumentException(ORDER_NOT_FOUND_WITH_ID + orderId));
        if (order.getStatus() != expectedCurrentStatus) {
            return order;
        }
        return transition(order, targetStatus);
    }

    private Order transition(Order order, OrderStatus targetStatus) {
        OrderStatus current = order.getStatus();
        if (current == targetStatus) {
            return order;
        }
        if (current == null || !current.canTransitionTo(targetStatus)) {
            throw new IllegalStateException("Invalid status transition: " + current + " to " + targetStatus);
        }
        order.setStatus(targetStatus);
        return orderRepository.save(order);
    }
}

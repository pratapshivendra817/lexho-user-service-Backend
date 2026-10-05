package com.lexho.user_service.booking.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.booking.entity.Order;
import com.lexho.common.enums.OrderStatus;
import com.lexho.user_service.dispatch.entity.BookingEventEntity;
import com.lexho.user_service.dispatch.repository.BookingEventRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class BookingLifecycleEventService {

    private final BookingEventRepository bookingEventRepository;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public BookingLifecycleEventService(BookingEventRepository bookingEventRepository,
                                        ObjectMapper objectMapper,
                                        SimpMessagingTemplate messagingTemplate) {
        this.bookingEventRepository = bookingEventRepository;
        this.objectMapper = objectMapper;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public void publish(Order order, String eventName, Map<String, Object> details) {
        if (order == null || order.getId() == null || eventName == null || eventName.isBlank()) {
            return;
        }
        BookingEventEntity event = new BookingEventEntity();
        event.setBookingId(order.getId());
        event.setEventType(eventName);
        event.setEventPayload(toJson(payload(order, eventName, null, details)));
        BookingEventEntity saved = bookingEventRepository.save(event);

        Map<String, Object> payload = payload(order, eventName, saved.getId(), details);
        saved.setEventPayload(toJson(payload));
        bookingEventRepository.save(saved);
        messagingTemplate.convertAndSend("/topic/customer/bookings/" + order.getId(), payload);
    }

    public void publish(Order order, OrderStatus status, Map<String, Object> details) {
        publish(order, status.name(), details);
    }

    private Map<String, Object> payload(Order order, String eventName, Long sequence, Map<String, Object> details) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("bookingId", order.getId());
        payload.put("orderId", order.getId());
        payload.put("event", eventName);
        payload.put("status", order.getStatus() == null ? null : order.getStatus().name());
        payload.put("paymentStatus", order.getPaymentStatus() == null ? null : order.getPaymentStatus().name());
        payload.put("sequence", sequence);
        payload.put("version", sequence);
        payload.put("timestamp", LocalDateTime.now().toString());
        payload.put("assignedPartnerId", order.getAssignedPartnerId());
        if (details != null && !details.isEmpty()) {
            payload.putAll(details);
        }
        return payload;
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            return "{\"error\":\"SERIALIZATION_FAILED\"}";
        }
    }
}

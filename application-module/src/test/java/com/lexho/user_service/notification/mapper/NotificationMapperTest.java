package com.lexho.user_service.notification.mapper;

import com.lexho.notification.entity.Notification;
import com.lexho.user_service.notification.dto.NotificationResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationMapperTest {

    @Test
    void shouldPopulateBookingIdForBookingReference() {
        NotificationMapper mapper = new NotificationMapper();
        Notification notification = new Notification();
        notification.setReferenceType("BOOKING");
        notification.setReferenceId(123L);

        NotificationResponse response = mapper.toResponse(notification);

        assertEquals("123", response.getBookingId());
    }
}

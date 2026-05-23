package com.lexho.user.client;

import com.lexho.user.dto.BookingSummaryClientResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "bookingServiceClient",
        url = "${app.booking-service.url:http://localhost:8082}",
        path = "/internal/bookings"
)
public interface BookingServiceClient {

    @GetMapping("/users/{userId}/summary")
    BookingSummaryClientResponse getUserBookingSummary(@PathVariable("userId") Long userId);
}

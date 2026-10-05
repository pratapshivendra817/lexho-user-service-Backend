package com.lexho.user_service.dispatch.service;

import com.lexho.booking.entity.Order;
import com.lexho.booking.repository.OrderRepository;
import com.lexho.common.enums.OrderStatus;
import com.lexho.user_service.booking.model.BookingType;
import com.lexho.user_service.config.DispatchProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class ScheduledBookingDispatchScheduler {
    private static final Logger LOG = LoggerFactory.getLogger(ScheduledBookingDispatchScheduler.class);
    private static final int BATCH_SIZE = 100;
    private static final List<OrderStatus> DISPATCHABLE_STATUSES = List.of(
            OrderStatus.BOOKING_CREATED,
            OrderStatus.CREATED,
            OrderStatus.CONFIRMED,
            OrderStatus.SCHEDULED,
            OrderStatus.PARTNER_SEARCH
    );

    private final OrderRepository orderRepository;
    private final DispatchOrchestrationService dispatchOrchestrationService;
    private final DispatchProperties dispatchProperties;

    public ScheduledBookingDispatchScheduler(OrderRepository orderRepository,
                                             DispatchOrchestrationService dispatchOrchestrationService,
                                             DispatchProperties dispatchProperties) {
        this.orderRepository = orderRepository;
        this.dispatchOrchestrationService = dispatchOrchestrationService;
        this.dispatchProperties = dispatchProperties;
    }

    @Scheduled(fixedDelayString = "${app.dispatch.scheduled-booking-scan-ms:60000}")
    public void dispatchDueScheduledBookings() {
        LocalDateTime scheduledBefore = LocalDateTime.now()
                .plusMinutes(dispatchProperties.getBooking().getPreDispatchMinutes());
        List<Order> dueBookings = orderRepository.findPaidScheduledBookingsAwaitingDispatch(
                BookingType.SCHEDULED.name(),
                DISPATCHABLE_STATUSES,
                scheduledBefore,
                PageRequest.of(0, BATCH_SIZE)
        );
        for (Order booking : dueBookings) {
            int createdRequests = dispatchOrchestrationService.dispatchPaidScheduledBooking(booking.getId());
            LOG.info("Scheduled booking dispatch scan processed bookingId={} createdRequests={}",
                    booking.getId(), createdRequests);
        }
    }
}

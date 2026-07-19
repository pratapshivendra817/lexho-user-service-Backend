package com.lexho.user_service.map.scheduler;

import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.map.dto.NavigationRouteResponse;
import com.lexho.user_service.map.service.NavigationEtaPushService;
import com.lexho.user_service.map.service.NavigationEtaTrackingRegistry;
import com.lexho.user_service.map.service.PartnerNavigationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NavigationEtaRefreshScheduler {

    private static final Logger log = LoggerFactory.getLogger(NavigationEtaRefreshScheduler.class);

    private final NavigationEtaTrackingRegistry navigationEtaTrackingRegistry;
    private final PartnerNavigationService partnerNavigationService;
    private final NavigationEtaPushService navigationEtaPushService;
    private final int maxBookingsPerRun;

    public NavigationEtaRefreshScheduler(NavigationEtaTrackingRegistry navigationEtaTrackingRegistry,
                                         PartnerNavigationService partnerNavigationService,
                                         NavigationEtaPushService navigationEtaPushService,
                                         @Value("${app.mapbox.live-eta-max-bookings-per-run:100}") int maxBookingsPerRun) {
        this.navigationEtaTrackingRegistry = navigationEtaTrackingRegistry;
        this.partnerNavigationService = partnerNavigationService;
        this.navigationEtaPushService = navigationEtaPushService;
        this.maxBookingsPerRun = Math.max(1, maxBookingsPerRun);
    }

    @Scheduled(fixedDelayString = "${app.mapbox.live-eta-refresh-ms:15000}")
    public void refreshTrackedBookingEtas() {
        List<Long> bookingIds = navigationEtaTrackingRegistry.activeBookingIds(maxBookingsPerRun);
        for (Long bookingId : bookingIds) {
            try {
                NavigationRouteResponse response = partnerNavigationService.etaForBookingSnapshot(bookingId);
                navigationEtaPushService.publish(bookingId, response);
            } catch (ResourceNotFoundException | BadRequestException ex) {
                navigationEtaTrackingRegistry.unregister(bookingId);
                log.info("navigation eta tracking removed for bookingId={} reason={}", bookingId, ex.getMessage());
            } catch (Exception ex) {
                log.warn("navigation eta refresh failed for bookingId={} error={}", bookingId, ex.getMessage());
            }
        }
    }
}

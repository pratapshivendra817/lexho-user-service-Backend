package com.lexho.user_service.map.scheduler;

import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.map.dto.NavigationRouteResponse;
import com.lexho.user_service.map.service.NavigationEtaPushService;
import com.lexho.user_service.map.service.NavigationEtaTrackingRegistry;
import com.lexho.user_service.map.service.PartnerNavigationService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NavigationEtaRefreshSchedulerTest {

    @Test
    void refreshPublishesTrackedBookingEtas() {
        NavigationEtaTrackingRegistry registry = mock(NavigationEtaTrackingRegistry.class);
        PartnerNavigationService navigationService = mock(PartnerNavigationService.class);
        NavigationEtaPushService pushService = mock(NavigationEtaPushService.class);
        NavigationEtaRefreshScheduler scheduler =
                new NavigationEtaRefreshScheduler(registry, navigationService, pushService, 50);
        when(registry.activeBookingIds(50)).thenReturn(List.of(45L));
        when(navigationService.etaForBookingSnapshot(45L)).thenReturn(new NavigationRouteResponse(
                45L, 3.5, 9, "poly", LocalDateTime.now().plusMinutes(9),
                true, "MAPBOX", false, true, false, null, 1, true,
                28.61, 77.20, LocalDateTime.now(),
                28.62, 77.21, LocalDateTime.now()
        ));

        scheduler.refreshTrackedBookingEtas();

        verify(pushService).publish(eq(45L), any(NavigationRouteResponse.class));
    }

    @Test
    void refreshRemovesBrokenBookingRegistrations() {
        NavigationEtaTrackingRegistry registry = mock(NavigationEtaTrackingRegistry.class);
        PartnerNavigationService navigationService = mock(PartnerNavigationService.class);
        NavigationEtaPushService pushService = mock(NavigationEtaPushService.class);
        NavigationEtaRefreshScheduler scheduler =
                new NavigationEtaRefreshScheduler(registry, navigationService, pushService, 50);
        when(registry.activeBookingIds(50)).thenReturn(List.of(99L));
        when(navigationService.etaForBookingSnapshot(99L)).thenThrow(new ResourceNotFoundException("missing"));

        scheduler.refreshTrackedBookingEtas();

        verify(registry).unregister(99L);
        verify(pushService, never()).publish(any(), any());
    }
}

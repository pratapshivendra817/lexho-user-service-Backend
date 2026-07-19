package com.lexho.user_service.map.controller;

import com.lexho.user_service.map.dto.NavigationRouteRequest;
import com.lexho.user_service.map.service.PartnerNavigationService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PartnerNavigationControllerTest {

    private final PartnerNavigationService partnerNavigationService = mock(PartnerNavigationService.class);
    private final PartnerNavigationController controller = new PartnerNavigationController(partnerNavigationService);

    @Test
    void delegatesRouteAndEtaEndpoints() {
        NavigationRouteRequest request = new NavigationRouteRequest(28.61, 77.20, 28.62, 77.21);

        assertThat(controller.route(request).isSuccess()).isTrue();
        assertThat(controller.eta(88L).isSuccess()).isTrue();

        verify(partnerNavigationService).route(request);
        verify(partnerNavigationService).etaForBooking(88L);
    }
}

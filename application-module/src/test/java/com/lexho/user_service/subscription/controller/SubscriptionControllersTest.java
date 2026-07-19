package com.lexho.user_service.subscription.controller;

import com.lexho.user_service.cartconfig.entity.CartConfigEntity;
import com.lexho.user_service.cartconfig.service.CartConfigService;
import com.lexho.user_service.exception.CodedValidationException;
import com.lexho.user_service.subscription.dto.*;
import com.lexho.user_service.subscription.enums.FamilySize;
import com.lexho.user_service.subscription.enums.SubscriptionStatus;
import com.lexho.user_service.subscription.exception.SubscriptionValidationException;
import com.lexho.user_service.subscription.service.AdminSubscriptionManagementService;
import com.lexho.user_service.subscription.service.SubscriptionBookingService;
import com.lexho.user_service.subscription.service.SubscriptionPricingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionControllersTest {

    @Mock private SubscriptionBookingService bookingService;
    @Mock private SubscriptionPricingService pricingService;
    @Mock private CartConfigService cartConfigService;
    @Mock private AdminSubscriptionManagementService adminService;

    private SubscriptionController controller;
    private AdminSubscriptionController adminController;

    @BeforeEach
    void setUp() {
        controller = new SubscriptionController(bookingService, pricingService, cartConfigService);
        adminController = new AdminSubscriptionController(adminService);
    }

    @Test
    void customerEndpointsDelegateAndWrapResponses() {
        CartConfigEntity config = new CartConfigEntity();
        when(cartConfigService.currentEntity()).thenReturn(config);
        CreateSubscriptionRequest createRequest = mock(CreateSubscriptionRequest.class);
        SubscriptionCreateResponse created = mock(SubscriptionCreateResponse.class);
        when(bookingService.create(7L, createRequest)).thenReturn(created);
        SubscriptionQuoteRequest quoteRequest = mock(SubscriptionQuoteRequest.class);
        SubscriptionQuoteResponse quote = mock(SubscriptionQuoteResponse.class);
        when(pricingService.quote(quoteRequest)).thenReturn(quote);
        when(pricingService.prices(3L)).thenReturn(List.of(mock(SubscriptionDurationPriceResponse.class)));
        when(pricingService.cookPrices(3L)).thenReturn(List.of(mock(CookSubscriptionPricingResponse.class)));
        SubscriptionPriceQuoteResponse priceQuote = mock(SubscriptionPriceQuoteResponse.class);
        when(pricingService.quote(3L, new BigDecimal("2.5"), 8)).thenReturn(priceQuote);
        CookCoverageResponse coverage = mock(CookCoverageResponse.class);
        when(pricingService.cookCoverage(null, FamilySize.SMALL)).thenReturn(coverage);
        when(bookingService.list(7L)).thenReturn(List.of(mock(SubscriptionListItemResponse.class)));
        when(bookingService.subscriptionCards(7L)).thenReturn(List.of(Map.of("id", 11L)));
        when(bookingService.subscriptionCard(7L, 11L)).thenReturn(Map.of("id", 11L));
        SubscriptionDetailResponse detail = mock(SubscriptionDetailResponse.class);
        when(bookingService.detail(7L, 11L)).thenReturn(detail);
        when(bookingService.pause(7L, 11L)).thenReturn(Map.of("status", "PAUSED"));
        when(bookingService.resume(7L, 11L)).thenReturn(Map.of("status", "ACTIVE"));
        when(bookingService.calendar(7L, 11L)).thenReturn(List.of(mock(SubscriptionCalendarItemResponse.class)));
        when(bookingService.visits(7L, 11L)).thenReturn(Map.of("count", 4));
        RescheduleVisitRequest reschedule = mock(RescheduleVisitRequest.class);

        assertThat(controller.create(7L, createRequest).getData()).isSameAs(created);
        assertThat(controller.createBookingAlias(7L, createRequest).getData()).isSameAs(created);
        assertThat(controller.quote(quoteRequest).getData()).isSameAs(quote);
        assertThat(controller.pricing(3L).getData()).hasSize(1);
        assertThat(controller.cookPricing(3L).getData()).hasSize(1);
        assertThat(controller.priceQuote(3L, new BigDecimal("2.5"), 8).getData()).isSameAs(priceQuote);
        assertThat(controller.cookCoverage(null, FamilySize.SMALL)).isSameAs(coverage);
        assertThat(controller.list(7L).getData()).hasSize(1);
        assertThat(controller.subscriptions(7L).getData()).containsExactly(Map.of("id", 11L));
        assertThat(controller.subscriptionCard(7L, 11L).getData()).containsEntry("id", 11L);
        assertThat(controller.detail(7L, 11L).getData()).isSameAs(detail);
        assertThat(controller.pause(7L, 11L).getData()).containsEntry("status", "PAUSED");
        assertThat(controller.resume(7L, 11L).getData()).containsEntry("status", "ACTIVE");
        assertThat(controller.calendar(7L, 11L)).hasSize(1);
        assertThat(controller.visits(7L, 11L).getData()).containsEntry("count", 4);
        assertThat(controller.cancel(7L, 11L).isSuccess()).isTrue();
        assertThat(controller.skipVisit(7L, 11L, 22L).isSuccess()).isTrue();
        assertThat(controller.rescheduleVisit(7L, 11L, 22L, reschedule).isSuccess()).isTrue();

        verify(bookingService).cancel(7L, 11L);
        verify(bookingService).skipVisit(7L, 11L, 22L);
        verify(bookingService).rescheduleVisit(7L, 11L, 22L, reschedule);
    }

    @Test
    void createRejectsDisabledSubscriptionConfiguration() {
        CartConfigEntity disabled = new CartConfigEntity();
        disabled.setSubscriptionBookingEnabled(false);
        when(cartConfigService.currentEntity()).thenReturn(disabled);

        assertThatThrownBy(() -> controller.create(7L, mock(CreateSubscriptionRequest.class)))
                .isInstanceOf(CodedValidationException.class)
                .hasMessage("Subscription booking is currently disabled");
        verifyNoInteractions(bookingService);
    }

    @Test
    void createRejectsDisabledCheckoutConfiguration() {
        CartConfigEntity disabled = new CartConfigEntity();
        disabled.setSubscriptionCheckoutEnabled(false);
        when(cartConfigService.currentEntity()).thenReturn(disabled);

        assertThatThrownBy(() -> controller.createBookingAlias(7L, mock(CreateSubscriptionRequest.class)))
                .isInstanceOf(CodedValidationException.class);
    }

    @Test
    void subscriptionValidationHandlerMapsErrorsToBadRequest() {
        var response = controller.handleSubscriptionValidation(
                new SubscriptionValidationException(List.of("startDate invalid", "frequency missing")));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.getBody().getData().get("errors"))
                .isEqualTo(List.of("startDate invalid", "frequency missing"));
    }

    @Test
    void adminEndpointsDelegateEveryOperation() {
        when(adminService.list(7L, SubscriptionStatus.ACTIVE, 1, 5)).thenReturn(new PageImpl<>(List.of()));
        AdminSubscriptionDetailResponse detail = mock(AdminSubscriptionDetailResponse.class);
        when(adminService.detail(11L)).thenReturn(detail);
        when(adminService.calendar(11L)).thenReturn(List.of(mock(SubscriptionCalendarItemResponse.class)));
        AdminSubscriptionStatusUpdateRequest statusRequest = mock(AdminSubscriptionStatusUpdateRequest.class);
        when(adminService.updateStatus(11L, statusRequest)).thenReturn(detail);
        AdminSubscriptionVisitStatusUpdateRequest visitStatus = mock(AdminSubscriptionVisitStatusUpdateRequest.class);
        SubscriptionCalendarItemResponse calendarItem = mock(SubscriptionCalendarItemResponse.class);
        when(adminService.updateVisitStatus(11L, 22L, visitStatus)).thenReturn(calendarItem);
        AdminSubscriptionAssignmentResponse assignment = mock(AdminSubscriptionAssignmentResponse.class);
        when(adminService.assignment(11L)).thenReturn(assignment);
        AdminPartnerAssignRequest assign = mock(AdminPartnerAssignRequest.class);
        when(adminService.assignPartner(11L, assign)).thenReturn(assignment);
        when(adminService.replacePartner(11L, assign)).thenReturn(assignment);
        AdminVisitAssignmentResponse visitAssignment = mock(AdminVisitAssignmentResponse.class);
        when(adminService.assignVisitPartner(11L, 22L, assign)).thenReturn(visitAssignment);

        assertThat(adminController.list(7L, SubscriptionStatus.ACTIVE, 1, 5).getData()).isEmpty();
        assertThat(adminController.detail(11L).getData()).isSameAs(detail);
        assertThat(adminController.calendar(11L).getData()).hasSize(1);
        assertThat(adminController.updateStatus(11L, statusRequest).getData()).isSameAs(detail);
        assertThat(adminController.updateVisitStatus(11L, 22L, visitStatus).getData()).isSameAs(calendarItem);
        assertThat(adminController.assignment(11L).getData()).isSameAs(assignment);
        assertThat(adminController.assignPartner(11L, assign).getData()).isSameAs(assignment);
        assertThat(adminController.replacePartner(11L, assign).getData()).isSameAs(assignment);
        assertThat(adminController.assignVisitPartner(11L, 22L, assign).getData()).isSameAs(visitAssignment);
    }
}

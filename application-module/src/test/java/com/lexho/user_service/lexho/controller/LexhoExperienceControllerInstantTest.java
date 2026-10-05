package com.lexho.user_service.lexho.controller;

import com.lexho.booking.entity.Order;
import com.lexho.booking.repository.OrderRepository;
import com.lexho.common.enums.OrderStatus;
import com.lexho.user_service.admin.repository.AdminCategoryRepository;
import com.lexho.user_service.admin.repository.AdminServiceEntityRepository;
import com.lexho.user_service.admin.repository.SupportFaqCategoryRepository;
import com.lexho.user_service.admin.repository.SupportFaqRepository;
import com.lexho.user_service.admin.repository.SupportTicketReplyRepository;
import com.lexho.user_service.admin.repository.SupportTicketRepository;
import com.lexho.user_service.admin.geo.service.GeoFenceValidationService;
import com.lexho.user_service.banner.repository.BannerPromotionRepository;
import com.lexho.user_service.cartconfig.entity.CartConfigEntity;
import com.lexho.user_service.cartconfig.service.CartConfigService;
import com.lexho.user_service.customer.location.entity.CustomerAddress;
import com.lexho.user_service.customer.location.repository.CustomerAddressRepository;
import com.lexho.user_service.customer.service.CustomerCartSlotService;
import com.lexho.user_service.customer.location.service.ServiceabilityService;
import com.lexho.user_service.customer.service.SlotDateTimePolicy;
import com.lexho.user_service.dispatch.service.DispatchOrchestrationService;
import com.lexho.user_service.dispatch.repository.BookingEventRepository;
import com.lexho.user_service.dispatch.repository.CurrentPartnerLocationRepository;
import com.lexho.user_service.dto.OrderResponseDTO;
import com.lexho.user_service.lexho.dto.LexhoDtos;
import com.lexho.user_service.lexho.entity.LexhoAddress;
import com.lexho.user_service.lexhopartner.repository.PartnerOperationalProfileRepository;
import com.lexho.user_service.lexho.repository.LexhoFavoriteRepository;
import com.lexho.user_service.lexho.repository.LexhoReferralCodeRepository;
import com.lexho.user_service.lexho.repository.LexhoReferralUseRepository;
import com.lexho.user_service.lexho.repository.LexhoAddressRepository;
import com.lexho.user_service.lexho.service.LexhoAddressService;
import com.lexho.user_service.review.repository.ReviewRepository;
import com.lexho.user_service.service.ApiRateLimiterService;
import com.lexho.user_service.service.CartService;
import com.lexho.user_service.service.HomeService;
import com.lexho.user_service.notification.service.NotificationManagementService;
import com.lexho.user_service.service.OrderService;
import com.lexho.user_service.util.MediaUrlResolver;
import com.lexho.catalog.repository.CategoryRepository;
import com.lexho.provider.repository.PartnerRepository;
import com.lexho.user_service.subscription.repository.SubscriptionFrequencyRepository;
import com.lexho.user_service.subscription.repository.SubscriptionMasterRepository;
import com.lexho.user_service.subscription.repository.SubscriptionVisitRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LexhoExperienceControllerInstantTest {

    @Test
    void createBookingInstantShouldWaitForPaymentBeforeDispatch() {
        HomeService homeService = mock(HomeService.class);
        CartService cartService = mock(CartService.class);
        OrderService orderService = mock(OrderService.class);
        OrderRepository orderRepository = mock(OrderRepository.class);
        NotificationManagementService notificationManagementService = mock(NotificationManagementService.class);
        LexhoAddressService addressService = mock(LexhoAddressService.class);
        BannerPromotionRepository bannerPromotionRepository = mock(BannerPromotionRepository.class);
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        AdminCategoryRepository adminCategoryRepository = mock(AdminCategoryRepository.class);
        AdminServiceEntityRepository adminServiceEntityRepository = mock(AdminServiceEntityRepository.class);
        PartnerRepository partnerRepository = mock(PartnerRepository.class);
        PartnerOperationalProfileRepository partnerProfileRepository = mock(PartnerOperationalProfileRepository.class);
        ReviewRepository reviewRepository = mock(ReviewRepository.class);
        LexhoFavoriteRepository favoriteRepository = mock(LexhoFavoriteRepository.class);
        LexhoReferralCodeRepository referralCodeRepository = mock(LexhoReferralCodeRepository.class);
        LexhoReferralUseRepository referralUseRepository = mock(LexhoReferralUseRepository.class);
        SupportTicketRepository supportTicketRepository = mock(SupportTicketRepository.class);
        SupportTicketReplyRepository supportTicketReplyRepository = mock(SupportTicketReplyRepository.class);
        SupportFaqCategoryRepository supportFaqCategoryRepository = mock(SupportFaqCategoryRepository.class);
        SupportFaqRepository supportFaqRepository = mock(SupportFaqRepository.class);
        MediaUrlResolver mediaUrlResolver = mock(MediaUrlResolver.class);
        DispatchOrchestrationService dispatchOrchestrationService = mock(DispatchOrchestrationService.class);
        ApiRateLimiterService apiRateLimiterService = mock(ApiRateLimiterService.class);
        SlotDateTimePolicy slotDateTimePolicy = mock(SlotDateTimePolicy.class);
        CustomerCartSlotService customerCartSlotService = mock(CustomerCartSlotService.class);
        GeoFenceValidationService geoFenceValidationService = mock(GeoFenceValidationService.class);
        ServiceabilityService serviceabilityService = mock(ServiceabilityService.class);
        CartConfigService cartConfigService = mock(CartConfigService.class);
        SubscriptionMasterRepository subscriptionMasterRepository = mock(SubscriptionMasterRepository.class);
        SubscriptionFrequencyRepository subscriptionFrequencyRepository = mock(SubscriptionFrequencyRepository.class);
        SubscriptionVisitRepository subscriptionVisitRepository = mock(SubscriptionVisitRepository.class);
        CurrentPartnerLocationRepository currentPartnerLocationRepository = mock(CurrentPartnerLocationRepository.class);
        BookingEventRepository bookingEventRepository = mock(BookingEventRepository.class);

        LexhoExperienceController controller = new LexhoExperienceController(
                homeService, cartService, orderService, orderRepository, notificationManagementService, addressService,
                bannerPromotionRepository, categoryRepository, adminCategoryRepository, adminServiceEntityRepository, partnerRepository,
                partnerProfileRepository,
                reviewRepository, favoriteRepository, referralCodeRepository, referralUseRepository,
                supportTicketRepository, supportTicketReplyRepository,
                supportFaqCategoryRepository, supportFaqRepository, mediaUrlResolver, dispatchOrchestrationService,
                apiRateLimiterService, slotDateTimePolicy, customerCartSlotService, geoFenceValidationService,
                serviceabilityService, cartConfigService, subscriptionMasterRepository, subscriptionFrequencyRepository,
                subscriptionVisitRepository, currentPartnerLocationRepository, bookingEventRepository
        );

        when(cartConfigService.currentEntity()).thenReturn(new CartConfigEntity());
        doNothing().when(apiRateLimiterService).check(any(), any(), anyInt(), any(), any());
        when(addressService.getById(5L, 20L)).thenReturn(new LexhoDtos.AddressResponse(20L, "H", "Line 1", "City", true));
        LexhoAddress rawAddress = new LexhoAddress();
        when(addressService.getRawById(5L, 20L)).thenReturn(rawAddress);

        OrderResponseDTO orderResponse = new OrderResponseDTO();
        orderResponse.setId(9001L);
        orderResponse.setStatus("CREATED");
        when(orderService.placeOrder(anyLong(), any())).thenReturn(orderResponse);
        Order order = new Order();
        order.setId(9001L);
        order.setStatus(OrderStatus.CREATED);
        when(orderRepository.findByIdForUpdate(9001L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        LexhoDtos.BookingCreateRequest request = new LexhoDtos.BookingCreateRequest(
                10L, 20L, "INSTANT", null, null, "note"
        );

        controller.createBooking(5L, request);

        assertEquals(OrderStatus.SEARCHING_PARTNER, order.getStatus());
        verify(dispatchOrchestrationService, never()).dispatchOrder(anyLong(), anyLong(), anyLong(), anyLong(), anyInt(), any());
        verify(dispatchOrchestrationService).saveEvent(eq(9001L), eq("INSTANT_DISPATCH_PENDING"), any());
        verify(orderRepository).save(order);
    }

    @Test
    void createBookingInstantShouldAcceptCustomerLocationUuidAndDispatchWithMirroredLegacyAddress() {
        HomeService homeService = mock(HomeService.class);
        CartService cartService = mock(CartService.class);
        OrderService orderService = mock(OrderService.class);
        OrderRepository orderRepository = mock(OrderRepository.class);
        NotificationManagementService notificationManagementService = mock(NotificationManagementService.class);
        LexhoAddressService addressService = mock(LexhoAddressService.class);
        BannerPromotionRepository bannerPromotionRepository = mock(BannerPromotionRepository.class);
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        AdminCategoryRepository adminCategoryRepository = mock(AdminCategoryRepository.class);
        AdminServiceEntityRepository adminServiceEntityRepository = mock(AdminServiceEntityRepository.class);
        PartnerRepository partnerRepository = mock(PartnerRepository.class);
        PartnerOperationalProfileRepository partnerProfileRepository = mock(PartnerOperationalProfileRepository.class);
        ReviewRepository reviewRepository = mock(ReviewRepository.class);
        LexhoFavoriteRepository favoriteRepository = mock(LexhoFavoriteRepository.class);
        LexhoReferralCodeRepository referralCodeRepository = mock(LexhoReferralCodeRepository.class);
        LexhoReferralUseRepository referralUseRepository = mock(LexhoReferralUseRepository.class);
        SupportTicketRepository supportTicketRepository = mock(SupportTicketRepository.class);
        SupportTicketReplyRepository supportTicketReplyRepository = mock(SupportTicketReplyRepository.class);
        SupportFaqCategoryRepository supportFaqCategoryRepository = mock(SupportFaqCategoryRepository.class);
        SupportFaqRepository supportFaqRepository = mock(SupportFaqRepository.class);
        MediaUrlResolver mediaUrlResolver = mock(MediaUrlResolver.class);
        DispatchOrchestrationService dispatchOrchestrationService = mock(DispatchOrchestrationService.class);
        ApiRateLimiterService apiRateLimiterService = mock(ApiRateLimiterService.class);
        SlotDateTimePolicy slotDateTimePolicy = mock(SlotDateTimePolicy.class);
        CustomerCartSlotService customerCartSlotService = mock(CustomerCartSlotService.class);
        GeoFenceValidationService geoFenceValidationService = mock(GeoFenceValidationService.class);
        ServiceabilityService serviceabilityService = mock(ServiceabilityService.class);
        CartConfigService cartConfigService = mock(CartConfigService.class);
        SubscriptionMasterRepository subscriptionMasterRepository = mock(SubscriptionMasterRepository.class);
        SubscriptionFrequencyRepository subscriptionFrequencyRepository = mock(SubscriptionFrequencyRepository.class);
        SubscriptionVisitRepository subscriptionVisitRepository = mock(SubscriptionVisitRepository.class);
        CurrentPartnerLocationRepository currentPartnerLocationRepository = mock(CurrentPartnerLocationRepository.class);
        BookingEventRepository bookingEventRepository = mock(BookingEventRepository.class);
        CustomerAddressRepository customerAddressRepository = mock(CustomerAddressRepository.class);
        LexhoAddressRepository lexhoAddressRepository = mock(LexhoAddressRepository.class);

        LexhoExperienceController controller = new LexhoExperienceController(
                homeService, cartService, orderService, orderRepository, notificationManagementService, addressService,
                bannerPromotionRepository, categoryRepository, adminCategoryRepository, adminServiceEntityRepository, partnerRepository,
                partnerProfileRepository,
                reviewRepository, favoriteRepository, referralCodeRepository, referralUseRepository,
                supportTicketRepository, supportTicketReplyRepository,
                supportFaqCategoryRepository, supportFaqRepository, mediaUrlResolver, dispatchOrchestrationService,
                apiRateLimiterService, slotDateTimePolicy, customerCartSlotService, geoFenceValidationService,
                serviceabilityService, cartConfigService, subscriptionMasterRepository, subscriptionFrequencyRepository,
                subscriptionVisitRepository, currentPartnerLocationRepository, bookingEventRepository
        );
        ReflectionTestUtils.setField(controller, "customerAddressRepository", customerAddressRepository);
        ReflectionTestUtils.setField(controller, "lexhoAddressRepository", lexhoAddressRepository);

        UUID customerLocationId = UUID.randomUUID();
        CustomerAddress customerAddress = new CustomerAddress();
        customerAddress.setFlatNo("12A");
        customerAddress.setFloor("2");
        customerAddress.setBuildingName("Test Building");
        customerAddress.setLandmark("Temple");
        customerAddress.setCountry("India");
        customerAddress.setAddressLine("Main Road, Gurugram, Haryana, 122001, India");
        customerAddress.setArea("Sector 45");
        customerAddress.setCity("Gurugram");
        customerAddress.setState("Haryana");
        customerAddress.setPincode("122001");
        customerAddress.setLatitude(BigDecimal.valueOf(28.4595));
        customerAddress.setLongitude(BigDecimal.valueOf(77.0266));

        when(cartConfigService.currentEntity()).thenReturn(new CartConfigEntity());
        doNothing().when(apiRateLimiterService).check(any(), any(), anyInt(), any(), any());
        when(customerAddressRepository.findByIdAndCustomerIdAndIsDeletedFalse(customerLocationId, 5L))
                .thenReturn(Optional.of(customerAddress));
        when(lexhoAddressRepository.findByUserIdAndCreatedByAndIsDeletedFalse(
                eq(5L), eq("CUSTOMER_LOCATION:" + customerLocationId))).thenReturn(Optional.empty());
        when(lexhoAddressRepository.save(any(LexhoAddress.class))).thenAnswer(invocation -> {
            LexhoAddress saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 700L);
            return saved;
        });

        OrderResponseDTO orderResponse = new OrderResponseDTO();
        orderResponse.setId(9002L);
        orderResponse.setStatus("CREATED");
        when(orderService.placeOrder(anyLong(), any())).thenReturn(orderResponse);
        Order order = new Order();
        order.setId(9002L);
        order.setStatus(OrderStatus.CREATED);
        when(orderRepository.findByIdForUpdate(9002L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LexhoDtos.BookingCreateRequest request = new LexhoDtos.BookingCreateRequest(
                10L, customerLocationId.toString(), "INSTANT", null, null, "note",
                null, null, null, null, null, null, null, null
        );

        var response = controller.createBooking(5L, request);

        assertEquals(customerLocationId.toString(), response.getBody().getData().get("addressId"));
        verify(orderService).placeOrder(eq(5L), org.mockito.ArgumentMatchers.argThat(placed ->
                "12A, Floor 2, Test Building, Near Temple, Main Road, Gurugram, Haryana, 122001, India, Sector 45"
                        .equals(placed.getAddress())));
        verify(customerAddressRepository).findByIdAndCustomerIdAndIsDeletedFalse(customerLocationId, 5L);
        verify(dispatchOrchestrationService, never()).dispatchOrder(eq(9002L), eq(5L), eq(700L), eq(10L), eq(1), any());
        verify(dispatchOrchestrationService).saveEvent(eq(9002L), eq("INSTANT_DISPATCH_PENDING"), any());
    }
}

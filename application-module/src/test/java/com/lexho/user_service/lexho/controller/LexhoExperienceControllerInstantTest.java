package com.lexho.user_service.lexho.controller;

import com.lexho.booking.entity.Order;
import com.lexho.booking.repository.OrderRepository;
import com.lexho.common.enums.OrderStatus;
import com.lexho.user_service.admin.repository.AdminCategoryRepository;
import com.lexho.user_service.admin.repository.SupportFaqCategoryRepository;
import com.lexho.user_service.admin.repository.SupportFaqRepository;
import com.lexho.user_service.admin.repository.SupportTicketReplyRepository;
import com.lexho.user_service.admin.repository.SupportTicketRepository;
import com.lexho.user_service.admin.geo.service.GeoFenceValidationService;
import com.lexho.user_service.banner.repository.BannerPromotionRepository;
import com.lexho.user_service.booking.exception.InstantBookingUnavailableException;
import com.lexho.user_service.customer.service.CustomerCartSlotService;
import com.lexho.user_service.customer.service.SlotDateTimePolicy;
import com.lexho.user_service.dispatch.service.DispatchOrchestrationService;
import com.lexho.user_service.dto.OrderResponseDTO;
import com.lexho.user_service.lexho.dto.LexhoDtos;
import com.lexho.user_service.lexho.entity.LexhoAddress;
import com.lexho.user_service.lexhopartner.repository.PartnerProfileRepository;
import com.lexho.user_service.lexho.repository.LexhoFavoriteRepository;
import com.lexho.user_service.lexho.repository.LexhoReferralCodeRepository;
import com.lexho.user_service.lexho.repository.LexhoReferralUseRepository;
import com.lexho.user_service.lexho.repository.LexhoSubscriptionRepository;
import com.lexho.user_service.lexho.service.LexhoAddressService;
import com.lexho.user_service.review.repository.ReviewRepository;
import com.lexho.user_service.service.ApiRateLimiterService;
import com.lexho.user_service.service.CartService;
import com.lexho.user_service.service.HomeService;
import com.lexho.user_service.service.NotificationService;
import com.lexho.user_service.service.OrderService;
import com.lexho.user_service.util.MediaUrlResolver;
import com.lexho.catalog.repository.CategoryRepository;
import com.lexho.partner.repository.PartnerRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LexhoExperienceControllerInstantTest {

    @Test
    void createBookingInstantShouldFailWhenNoPartnerDispatched() {
        HomeService homeService = mock(HomeService.class);
        CartService cartService = mock(CartService.class);
        OrderService orderService = mock(OrderService.class);
        OrderRepository orderRepository = mock(OrderRepository.class);
        NotificationService notificationService = mock(NotificationService.class);
        LexhoAddressService addressService = mock(LexhoAddressService.class);
        BannerPromotionRepository bannerPromotionRepository = mock(BannerPromotionRepository.class);
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        AdminCategoryRepository adminCategoryRepository = mock(AdminCategoryRepository.class);
        PartnerRepository partnerRepository = mock(PartnerRepository.class);
        PartnerProfileRepository partnerProfileRepository = mock(PartnerProfileRepository.class);
        ReviewRepository reviewRepository = mock(ReviewRepository.class);
        LexhoFavoriteRepository favoriteRepository = mock(LexhoFavoriteRepository.class);
        LexhoReferralCodeRepository referralCodeRepository = mock(LexhoReferralCodeRepository.class);
        LexhoReferralUseRepository referralUseRepository = mock(LexhoReferralUseRepository.class);
        LexhoSubscriptionRepository subscriptionRepository = mock(LexhoSubscriptionRepository.class);
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

        LexhoExperienceController controller = new LexhoExperienceController(
                homeService, cartService, orderService, orderRepository, notificationService, addressService,
                bannerPromotionRepository, categoryRepository, adminCategoryRepository, partnerRepository,
                partnerProfileRepository,
                reviewRepository, favoriteRepository, referralCodeRepository, referralUseRepository,
                subscriptionRepository, supportTicketRepository, supportTicketReplyRepository,
                supportFaqCategoryRepository, supportFaqRepository, mediaUrlResolver, dispatchOrchestrationService,
                apiRateLimiterService, slotDateTimePolicy, customerCartSlotService, geoFenceValidationService
        );

        doNothing().when(apiRateLimiterService).check(any(), any(), anyInt(), any(), any());
        when(addressService.getById(5L, 20L)).thenReturn(new LexhoDtos.AddressResponse(20L, "H", "Line 1", "City", true));
        LexhoAddress rawAddress = new LexhoAddress();
        when(addressService.getRawById(5L, 20L)).thenReturn(rawAddress);

        OrderResponseDTO orderResponse = new OrderResponseDTO();
        orderResponse.setId(9001L);
        orderResponse.setStatus("CREATED");
        when(orderService.placeOrder(anyLong(), any())).thenReturn(orderResponse);
        when(dispatchOrchestrationService.dispatchOrder(anyLong(), anyLong(), anyLong(), anyLong(), anyInt(), any()))
                .thenReturn(0);

        Order order = new Order();
        order.setId(9001L);
        order.setStatus(OrderStatus.CREATED);
        when(orderRepository.findById(9001L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        LexhoDtos.BookingCreateRequest request = new LexhoDtos.BookingCreateRequest(
                10L, 20L, "INSTANT", null, null, "note"
        );

        InstantBookingUnavailableException ex = assertThrows(
                InstantBookingUnavailableException.class,
                () -> controller.createBooking(5L, request)
        );

        assertEquals("INSTANT_UNAVAILABLE", ex.getErrorCode());
        assertEquals(OrderStatus.INSTANT_FAILED, order.getStatus());
        verify(orderRepository).save(order);
    }
}

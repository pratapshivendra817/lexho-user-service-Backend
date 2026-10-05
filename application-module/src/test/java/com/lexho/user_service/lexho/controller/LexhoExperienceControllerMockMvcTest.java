package com.lexho.user_service.lexho.controller;

import com.lexho.booking.repository.OrderRepository;
import com.lexho.booking.entity.Order;
import com.lexho.common.enums.OrderStatus;
import com.lexho.common.enums.PaymentStatus;
import com.lexho.catalog.repository.CategoryRepository;
import com.lexho.provider.entity.Partner;
import com.lexho.provider.repository.PartnerRepository;
import com.lexho.user_service.admin.entity.AdminCategoryEntity;
import com.lexho.user_service.admin.entity.AdminServiceEntity;
import com.lexho.user_service.admin.geo.service.GeoFenceValidationService;
import com.lexho.user_service.admin.entity.SupportFaq;
import com.lexho.user_service.admin.entity.SupportFaqCategory;
import com.lexho.user_service.admin.entity.SupportTicket;
import com.lexho.user_service.admin.entity.SupportTicketReply;
import com.lexho.user_service.admin.repository.*;
import com.lexho.user_service.banner.entity.BannerPromotion;
import com.lexho.user_service.banner.enums.BannerStatus;
import com.lexho.user_service.banner.repository.BannerPromotionRepository;
import com.lexho.user_service.booking.model.InstantUnavailableCode;
import com.lexho.user_service.cartconfig.entity.CartConfigEntity;
import com.lexho.user_service.cartconfig.service.CartConfigService;
import com.lexho.user_service.customer.location.dto.ReverseGeocodeResponse;
import com.lexho.user_service.customer.location.dto.ServiceabilityCheckResponse;
import com.lexho.user_service.customer.location.service.ServiceabilityService;
import com.lexho.user_service.customer.service.CustomerActivityFeedService;
import com.lexho.user_service.customer.service.CustomerCartSlotService;
import com.lexho.user_service.customer.service.SlotDateTimePolicy;
import com.lexho.user_service.dispatch.repository.BookingEventRepository;
import com.lexho.user_service.dispatch.repository.CurrentPartnerLocationRepository;
import com.lexho.user_service.dispatch.service.DispatchOrchestrationService;
import com.lexho.user_service.dto.CartResponseDTO;
import com.lexho.user_service.dto.CartSummaryDTO;
import com.lexho.user_service.dto.HomeResponseDTO;
import com.lexho.user_service.dto.OrderResponseDTO;
import com.lexho.user_service.lexho.repository.LexhoFavoriteRepository;
import com.lexho.user_service.lexho.repository.LexhoReferralCodeRepository;
import com.lexho.user_service.lexho.repository.LexhoReferralUseRepository;
import com.lexho.user_service.lexho.entity.LexhoFavorite;
import com.lexho.user_service.lexho.entity.LexhoAddress;
import com.lexho.user_service.lexho.entity.LexhoReferralCode;
import com.lexho.user_service.lexho.service.LexhoAddressService;
import com.lexho.user_service.lexhopartner.repository.PartnerOperationalProfileRepository;
import com.lexho.user_service.notification.service.NotificationManagementService;
import com.lexho.user_service.notification.service.NotificationRealtimeService;
import com.lexho.user_service.notification.dto.NotificationPageResponse;
import com.lexho.user_service.notification.dto.NotificationResponse;
import com.lexho.notification.enums.NotificationStatus;
import com.lexho.user_service.review.entity.ReviewEntity;
import com.lexho.user_service.review.repository.ReviewRepository;
import com.lexho.user_service.service.ApiRateLimiterService;
import com.lexho.user_service.service.CartService;
import com.lexho.user_service.service.HomeService;
import com.lexho.user_service.service.OrderService;
import com.lexho.user_service.subscription.repository.SubscriptionFrequencyRepository;
import com.lexho.user_service.subscription.repository.SubscriptionMasterRepository;
import com.lexho.user_service.subscription.repository.SubscriptionVisitRepository;
import com.lexho.user_service.support.service.SupportRealtimeService;
import com.lexho.user_service.util.MediaUrlResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LexhoExperienceControllerMockMvcTest {

    private HomeService homeService;
    private CartService cartService;
    private OrderService orderService;
    private BannerPromotionRepository bannerRepository;
    private AdminCategoryRepository categoryRepository;
    private AdminServiceEntityRepository adminServiceEntityRepository;
    private PartnerRepository partnerRepository;
    private ServiceabilityService serviceabilityService;
    private OrderRepository orderRepository;
    private NotificationManagementService notificationManagementService;
    private LexhoAddressService addressService;
    private DispatchOrchestrationService dispatchOrchestrationService;
    private ApiRateLimiterService apiRateLimiterService;
    private SlotDateTimePolicy slotDateTimePolicy;
    private GeoFenceValidationService geoFenceValidationService;
    private CartConfigService cartConfigService;
    private SubscriptionMasterRepository subscriptionMasterRepository;
    private SubscriptionFrequencyRepository subscriptionFrequencyRepository;
    private SubscriptionVisitRepository subscriptionVisitRepository;
    private CurrentPartnerLocationRepository currentPartnerLocationRepository;
    private BookingEventRepository bookingEventRepository;
    private ReviewRepository reviewRepository;
    private LexhoFavoriteRepository favoriteRepository;
    private LexhoReferralCodeRepository referralCodeRepository;
    private LexhoReferralUseRepository referralUseRepository;
    private SupportTicketRepository supportTicketRepository;
    private SupportTicketReplyRepository supportTicketReplyRepository;
    private SupportFaqCategoryRepository supportFaqCategoryRepository;
    private SupportFaqRepository supportFaqRepository;
    private CustomerCartSlotService customerCartSlotService;
    private MediaUrlResolver mediaUrlResolver;
    private SupportRealtimeService supportRealtimeService;
    private NotificationRealtimeService notificationRealtimeService;
    private CustomerActivityFeedService customerActivityFeedService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        homeService = mock(HomeService.class);
        cartService = mock(CartService.class);
        orderService = mock(OrderService.class);
        bannerRepository = mock(BannerPromotionRepository.class);
        categoryRepository = mock(AdminCategoryRepository.class);
        adminServiceEntityRepository = mock(AdminServiceEntityRepository.class);
        partnerRepository = mock(PartnerRepository.class);
        serviceabilityService = mock(ServiceabilityService.class);
        orderRepository = mock(OrderRepository.class);
        notificationManagementService = mock(NotificationManagementService.class);
        addressService = mock(LexhoAddressService.class);
        dispatchOrchestrationService = mock(DispatchOrchestrationService.class);
        apiRateLimiterService = mock(ApiRateLimiterService.class);
        slotDateTimePolicy = mock(SlotDateTimePolicy.class);
        geoFenceValidationService = mock(GeoFenceValidationService.class);
        cartConfigService = mock(CartConfigService.class);
        subscriptionMasterRepository = mock(SubscriptionMasterRepository.class);
        subscriptionFrequencyRepository = mock(SubscriptionFrequencyRepository.class);
        subscriptionVisitRepository = mock(SubscriptionVisitRepository.class);
        currentPartnerLocationRepository = mock(CurrentPartnerLocationRepository.class);
        bookingEventRepository = mock(BookingEventRepository.class);
        reviewRepository = mock(ReviewRepository.class);
        favoriteRepository = mock(LexhoFavoriteRepository.class);
        referralCodeRepository = mock(LexhoReferralCodeRepository.class);
        referralUseRepository = mock(LexhoReferralUseRepository.class);
        supportTicketRepository = mock(SupportTicketRepository.class);
        supportTicketReplyRepository = mock(SupportTicketReplyRepository.class);
        supportFaqCategoryRepository = mock(SupportFaqCategoryRepository.class);
        supportFaqRepository = mock(SupportFaqRepository.class);
        customerCartSlotService = mock(CustomerCartSlotService.class);
        mediaUrlResolver = mock(MediaUrlResolver.class);
        supportRealtimeService = new SupportRealtimeService();
        notificationRealtimeService = mock(NotificationRealtimeService.class);
        customerActivityFeedService = mock(CustomerActivityFeedService.class);

        ServiceabilityCheckResponse serviceable = mock(ServiceabilityCheckResponse.class);
        when(serviceable.isServiceable()).thenReturn(true);
        when(serviceabilityService.resolveForCustomer(nullable(Long.class))).thenReturn(serviceable);
        when(adminServiceEntityRepository.searchActiveCatalogForCustomer(nullable(String.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        LexhoExperienceController controller = new LexhoExperienceController(
                homeService,
                cartService,
                orderService,
                orderRepository,
                notificationManagementService,
                addressService,
                bannerRepository,
                mock(CategoryRepository.class),
                categoryRepository,
                adminServiceEntityRepository,
                partnerRepository,
                mock(PartnerOperationalProfileRepository.class),
                reviewRepository,
                favoriteRepository,
                referralCodeRepository,
                referralUseRepository,
                supportTicketRepository,
                supportTicketReplyRepository,
                supportFaqCategoryRepository,
                supportFaqRepository,
                mediaUrlResolver,
                dispatchOrchestrationService,
                apiRateLimiterService,
                slotDateTimePolicy,
                customerCartSlotService,
                geoFenceValidationService,
                serviceabilityService,
                cartConfigService,
                subscriptionMasterRepository,
                subscriptionFrequencyRepository,
                subscriptionVisitRepository,
                currentPartnerLocationRepository,
                bookingEventRepository
        );
        ReflectionTestUtils.setField(controller, "supportRealtimeService", supportRealtimeService);
        ReflectionTestUtils.setField(controller, "notificationRealtimeService", notificationRealtimeService);
        ReflectionTestUtils.setField(controller, "customerActivityFeedService", customerActivityFeedService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void dashboardShouldReturnGreetingAndRecentBooking() throws Exception {
        HomeResponseDTO home = new HomeResponseDTO();
        home.setGreeting("Good morning");
        OrderResponseDTO order = new OrderResponseDTO();
        order.setId(10L);
        order.setStatus("CREATED");
        order.setItems(List.of());
        when(homeService.getHomeData(7L)).thenReturn(home);
        when(orderService.getUserOrders(7L)).thenReturn(List.of(order));

        mockMvc.perform(get("/api/home/dashboard").requestAttr("userId", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.greeting").value("Good morning"))
                .andExpect(jsonPath("$.data.recentBookings[0].serviceName").value("Service"));
    }

    @Test
    void browsingEndpointsShouldReturnEmptyDataAndClampParameters() throws Exception {
        when(bannerRepository.findByStatusAndDeletedFalseAndStartDateLessThanEqualAndEndDateGreaterThanOrderByPriorityDescSlotNumberAsc(
                eq(BannerStatus.ACTIVE), any(), any(), any(Pageable.class))).thenReturn(List.of());
        when(categoryRepository.findActiveForCustomer(any(Pageable.class))).thenReturn(List.of());
        when(partnerRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/banners").param("page", "-2").param("size", "500"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.data", hasSize(0)));
        mockMvc.perform(get("/api/categories").requestAttr("userId", 7L))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.data", hasSize(0)));
        mockMvc.perform(get("/api/services/popular").requestAttr("userId", 7L))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.data", hasSize(0)));
        mockMvc.perform(get("/api/services/recommended").requestAttr("userId", 7L))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/services/quick").requestAttr("userId", 7L))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/search/services").param("keyword", " clean ").requestAttr("userId", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Search results fetched"));
    }

    @Test
    void browsingEndpointsShouldFallbackToAdminServicesWhenPartnersAreEmpty() throws Exception {
        AdminCategoryEntity category = new AdminCategoryEntity();
        ReflectionTestUtils.setField(category, "id", 5L);
        category.setName("Cleaning");

        AdminServiceEntity service = new AdminServiceEntity();
        ReflectionTestUtils.setField(service, "id", 21L);
        service.setName("Kitchen Cleaning");
        service.setCategory(category);
        service.setDescription("Deep kitchen cleaning");
        service.setBasePrice(BigDecimal.valueOf(499));
        service.setDurationMinutes(90);
        service.setImageUrl("/services/kitchen.png");

        when(partnerRepository.findAll()).thenReturn(List.of());
        when(adminServiceEntityRepository.searchActiveCatalogForCustomer(eq("clean"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(service)));
        when(mediaUrlResolver.toPublicUrl("/services/kitchen.png")).thenReturn("https://cdn.lexho/services/kitchen.png");

        mockMvc.perform(get("/api/search/services")
                        .param("keyword", "clean")
                        .requestAttr("userId", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data", hasSize(1)))
                .andExpect(jsonPath("$.data.data[0].serviceId").value("21"))
                .andExpect(jsonPath("$.data.data[0].name").value("Kitchen Cleaning"))
                .andExpect(jsonPath("$.data.data[0].imageUrl").value("https://cdn.lexho/services/kitchen.png"));
    }

    @Test
    void browsingEndpointsShouldPreferAdminServicesOverLegacyPartners() throws Exception {
        AdminCategoryEntity category = new AdminCategoryEntity();
        ReflectionTestUtils.setField(category, "id", 5L);
        category.setName("Cleaning");

        AdminServiceEntity service = new AdminServiceEntity();
        ReflectionTestUtils.setField(service, "id", 21L);
        service.setName("Kitchen Cleaning");
        service.setCategory(category);
        service.setDescription("Deep kitchen cleaning");
        service.setBasePrice(BigDecimal.valueOf(499));
        service.setDurationMinutes(90);
        service.setImageUrl("/services/kitchen.png");

        Partner available = new Partner();
        available.setId(11L);
        available.setName("Legacy Cleaner");
        available.setAvailable(true);
        available.setCategoryId(5L);
        available.setPrice(299.0);
        available.setRating(4.9);

        when(partnerRepository.findAll()).thenReturn(List.of(available));
        when(adminServiceEntityRepository.searchActiveCatalogForCustomer(isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(service)));
        when(mediaUrlResolver.toPublicUrl("/services/kitchen.png")).thenReturn("https://cdn.lexho/services/kitchen.png");

        mockMvc.perform(get("/api/services/popular").requestAttr("userId", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data", hasSize(1)))
                .andExpect(jsonPath("$.data.data[0].serviceId").value("21"))
                .andExpect(jsonPath("$.data.data[0].name").value("Kitchen Cleaning"));
    }

    @Test
    void addCartShouldMapRequestAndResponse() throws Exception {
        when(cartService.addToCart(eq(7L), any()))
                .thenReturn(new CartResponseDTO(4L, 20L, "Cleaning", "/image.png", 125.0, 2));

        mockMvc.perform(post("/api/cart/add")
                        .requestAttr("userId", 7L)
                        .contentType("application/json")
                        .content("{\"serviceId\":20,\"quantity\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("4"))
                .andExpect(jsonPath("$.data.price").value(125.0));

        verify(cartService).addToCart(eq(7L), argThat(request ->
                request.getPartnerId().equals(20L) && request.getQuantity() == 2));
    }

    @Test
    void cartSummaryShouldMapItemsAndTotal() throws Exception {
        when(cartService.getCart(7L)).thenReturn(new CartSummaryDTO(
                List.of(new CartResponseDTO(4L, 20L, "Cleaning", null, 125.0, 2)), 250.0));

        mockMvc.perform(get("/api/cart/summary").requestAttr("userId", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.totalAmount").value(250.0));
    }

    @Test
    void updateCartShouldReplaceItem() throws Exception {
        when(cartService.addToCart(eq(7L), any()))
                .thenReturn(new CartResponseDTO(4L, 20L, "Cleaning", null, 100.0, 3));

        mockMvc.perform(put("/api/cart/update")
                        .requestAttr("userId", 7L)
                        .contentType("application/json")
                        .content("{\"id\":20,\"quantity\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quantity").value(3));

        verify(cartService).removeFromCart(7L, 20L);
    }

    @Test
    void removeCartShouldDelegate() throws Exception {
        mockMvc.perform(delete("/api/cart/remove")
                        .requestAttr("userId", 7L)
                        .contentType("application/json")
                        .content("{\"id\":20}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cart item removed"));

        verify(cartService).removeFromCart(7L, 20L);
    }

    @Test
    void cartValidationShouldRejectNonPositiveValues() throws Exception {
        mockMvc.perform(post("/api/cart/add")
                        .requestAttr("userId", 7L)
                        .contentType("application/json")
                        .content("{\"serviceId\":0,\"quantity\":0}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(cartService);
    }

    @Test
    void bookingHistorySlotsAndLocationEndpointsShouldMapEmptyAndSimpleResults() throws Exception {
        when(orderRepository.findByUserId(7L)).thenReturn(List.of());
        when(customerCartSlotService.slotsForDate(any())).thenReturn(java.util.Map.of(
                "slots", List.of(
                        java.util.Map.of("slot", "10:00-11:00", "available", true, "etaMinutes", 15,
                                "isInstantEligible", true),
                        "invalid-row"
                )));
        Order order = new Order();
        order.setId(11L);
        order.setUserId(7L);
        order.setAssignedPartnerId(7L);
        when(orderRepository.findById(11L)).thenReturn(Optional.of(order));
        when(currentPartnerLocationRepository.findByPartnerIdAndDeletedFalse(7L)).thenReturn(Optional.empty());
        when(currentPartnerLocationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(get("/api/bookings/history").requestAttr("userId", 7L)
                        .param("page", "-1").param("size", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.instantBookings.items", hasSize(0)))
                .andExpect(jsonPath("$.data.scheduledBookings.page").value(1));
        mockMvc.perform(get("/api/slots/available").param("date", "2026-07-01"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].instantEligible").value(true))
                .andExpect(jsonPath("$[1].available").value(false));
        mockMvc.perform(get("/api/partner/live-location/11").requestAttr("userId", 7L))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.eta").value("N/A"));
        mockMvc.perform(put("/api/partner/update-location")
                        .requestAttr("userId", 7L)
                        .param("bookingId", "11").param("latitude", "28.6").param("longitude", "77.2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.bookingId").value(11));

        verify(currentPartnerLocationRepository).save(argThat(location ->
                location.getPartnerId().equals(7L)
                        && location.getLatitude().compareTo(new BigDecimal("28.6")) == 0
                        && location.getLongitude().compareTo(new BigDecimal("77.2")) == 0
                        && location.getLocationSeq() == 1L));
    }

    @Test
    void updateLocationShouldRejectForeignPartnerAndInvalidCoordinates() throws Exception {
        Order foreignOrder = new Order();
        foreignOrder.setId(12L);
        foreignOrder.setAssignedPartnerId(99L);
        when(orderRepository.findById(12L)).thenReturn(Optional.of(foreignOrder));

        assertThrows(jakarta.servlet.ServletException.class, () ->
                mockMvc.perform(put("/api/partner/update-location")
                        .requestAttr("userId", 7L)
                        .param("bookingId", "12")
                        .param("latitude", "28.6")
                        .param("longitude", "77.2")));

        Order ownOrder = new Order();
        ownOrder.setId(13L);
        ownOrder.setAssignedPartnerId(7L);
        when(orderRepository.findById(13L)).thenReturn(Optional.of(ownOrder));

        assertThrows(jakarta.servlet.ServletException.class, () ->
                mockMvc.perform(put("/api/partner/update-location")
                        .requestAttr("userId", 7L)
                        .param("bookingId", "13")
                        .param("latitude", "120")
                        .param("longitude", "77.2")));

        verify(currentPartnerLocationRepository, never()).save(any());
    }

    @Test
    void paymentEndpointsShouldCoverOptionsInitiationStatusAndRefund() throws Exception {
        Order order = new Order();
        order.setId(12L);
        order.setUserId(7L);
        order.setTotalAmount(499.0);
        when(orderRepository.findById(12L)).thenReturn(Optional.of(order));
        when(orderRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(get("/api/payments/legacy-options")
                        .param("bookingId", "12").param("amount", "499"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.methods", hasSize(2)))
                .andExpect(jsonPath("$.data.bookingId").value("12"))
                .andExpect(jsonPath("$.data.amount").value(499));
        mockMvc.perform(post("/api/payments/options").contentType("application/json")
                        .content("{\"bookingId\":\"12\",\"amount\":499}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.methods", hasSize(2)));
        mockMvc.perform(post("/api/payments/initiate").requestAttr("userId", 7L)
                        .contentType("application/json")
                        .content("{\"bookingId\":\"12\",\"mode\":\"upi\",\"amount\":499}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.razorpayOptions.currency").value("INR"));
        mockMvc.perform(get("/api/payments/status/12").requestAttr("userId", 7L))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.message").value("Payment pending"));
        mockMvc.perform(post("/api/payments/refund").requestAttr("userId", 7L)
                        .contentType("application/json")
                        .content("{\"bookingId\":\"12\",\"amount\":100}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("INITIATED"));
        assertEquals(PaymentStatus.FAILED, order.getPaymentStatus());
    }

    @Test
    void favoriteReferralAndSupportEndpointsShouldMapRepositoryResults() throws Exception {
        LexhoFavorite favorite = new LexhoFavorite();
        org.springframework.test.util.ReflectionTestUtils.setField(favorite, "id", 4L);
        favorite.setUserId(7L);
        favorite.setServiceId(20L);
        when(favoriteRepository.findByUserIdOrderByIdDesc(7L)).thenReturn(List.of(favorite));
        when(favoriteRepository.findByIdAndUserId(4L, 7L)).thenReturn(Optional.of(favorite));

        LexhoReferralCode code = new LexhoReferralCode();
        code.setUserId(7L);
        code.setCode("LEXHO7");
        code.setUsedCount(0);
        when(referralCodeRepository.findByUserId(7L)).thenReturn(Optional.of(code));

        when(supportFaqCategoryRepository.findByActiveTrueOrderByDisplayOrderAscIdAsc()).thenReturn(List.of());
        when(supportTicketRepository.findByUserIdOrderByCreatedAtDesc(eq(7L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(post("/api/favorites/add").requestAttr("userId", 7L)
                        .contentType("application/json").content("{\"serviceId\":20}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.serviceId").value("20"));
        mockMvc.perform(get("/api/favorites/list").requestAttr("userId", 7L))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.data", hasSize(1)));
        mockMvc.perform(delete("/api/favorites/remove").requestAttr("userId", 7L)
                        .contentType("application/json").content("{\"id\":4}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/referral/generate").requestAttr("userId", 7L))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.code").value("LEXHO7"));
        mockMvc.perform(get("/api/support/faqs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
        mockMvc.perform(get("/api/support/history").requestAttr("userId", 7L)
                        .param("page", "-1").param("limit", "500"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.meta.page").value(1))
                .andExpect(jsonPath("$.meta.limit").value(100));
    }

    @Test
    void notificationEndpointsShouldMapPageAndDelegateMutations() throws Exception {
        NotificationResponse notification = new NotificationResponse();
        notification.setId(8L);
        notification.setTitle("Booking");
        notification.setMessage("Confirmed");
        notification.setStatus(NotificationStatus.SENT);
        notification.setReferenceId(12L);
        notification.setReferenceType("BOOKING");
        when(notificationManagementService.getCustomerNotifications(0, 20))
                .thenReturn(new NotificationPageResponse<>(List.of(notification), 1, 1, 0));
        when(notificationManagementService.unreadCountForCurrentUser()).thenReturn(3L);

        mockMvc.perform(get("/api/notifications").requestAttr("userId", 7L))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.data", hasSize(1)))
                .andExpect(jsonPath("$.data.data[0].title").value("Booking"));
        mockMvc.perform(put("/api/notifications/read").requestAttr("userId", 7L)
                        .contentType("application/json").content("{\"id\":8}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/notifications/read").requestAttr("userId", 7L)
                        .contentType("application/json").content("{\"id\":8}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/customer/notifications/unread-count").requestAttr("userId", 7L))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.count").value(3));
        verify(notificationManagementService).markAsReadForCurrentUser(8L);
        verify(notificationManagementService).softDeleteForCurrentUser(8L);
    }

    @Test
    void createBookingDebugSlotAndBookingDetailsShouldCoverScheduledFlows() throws Exception {
        CartConfigEntity config = new CartConfigEntity();
        config.setInstantBookingEnabled(true);
        config.setScheduledBookingEnabled(true);
        config.setScheduledBookingWindowDays(7);
        when(cartConfigService.currentEntity()).thenReturn(config);
        when(slotDateTimePolicy.zoneId()).thenReturn(ZoneId.systemDefault());
        when(slotDateTimePolicy.now()).thenReturn(LocalDateTime.of(2026, 6, 20, 10, 0));
        when(slotDateTimePolicy.today()).thenReturn(LocalDate.of(2026, 6, 20));
        when(slotDateTimePolicy.parseSlotStart(LocalDate.of(2026, 6, 21), "10:00-11:00"))
                .thenReturn(LocalDateTime.of(2026, 6, 21, 10, 0));
        when(slotDateTimePolicy.isExpired(any())).thenReturn(false);
        when(customerCartSlotService.slotRowForDate(LocalDate.of(2026, 6, 21), "10:00-11:00"))
                .thenReturn(java.util.Map.of("available", true));
        doNothing().when(apiRateLimiterService).check(any(), any(), anyInt(), any(), any());

        when(addressService.getById(7L, 20L))
                .thenReturn(new com.lexho.user_service.lexho.dto.LexhoDtos.AddressResponse(20L, "Home", "Tower 21", "Gurugram", true));
        LexhoAddress rawAddress = new LexhoAddress();
        rawAddress.setPincode("122003");
        when(addressService.applyBookingCoordinatesFallback(eq(7L), eq(20L), any(), any())).thenReturn(rawAddress);

        OrderResponseDTO placed = new OrderResponseDTO();
        placed.setId(15L);
        placed.setStatus("CREATED");
        when(orderService.placeOrder(eq(7L), any())).thenReturn(placed);

        Order scheduled = new Order();
        scheduled.setId(15L);
        scheduled.setUserId(7L);
        scheduled.setStatus(OrderStatus.CREATED);
        scheduled.setCreatedAt(LocalDateTime.of(2026, 6, 20, 9, 0));
        scheduled.setScheduledAt(LocalDateTime.of(2026, 6, 21, 10, 0));
        when(orderRepository.findById(15L)).thenReturn(Optional.of(scheduled));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookingEventRepository.findByBookingIdOrderByCreatedAtAsc(15L)).thenReturn(List.of());

        OrderResponseDTO orderDetails = new OrderResponseDTO();
        orderDetails.setId(15L);
        orderDetails.setAddress("Tower 21, Gurugram");
        orderDetails.setTotalAmount(499.0);
        com.lexho.user_service.dto.OrderItemDTO item = new com.lexho.user_service.dto.OrderItemDTO();
        item.setPartnerId(20L);
        item.setPartnerName("Cleaning");
        item.setPrice(499.0);
        item.setQuantity(1);
        orderDetails.setItems(List.of(item));
        when(orderService.getOrder(15L, 7L)).thenReturn(orderDetails);

        mockMvc.perform(post("/api/bookings/create")
                        .requestAttr("userId", 7L)
                        .contentType("application/json")
                        .content("{\"serviceId\":10,\"addressId\":20,\"bookingType\":\"SCHEDULED\",\"date\":\"2026-06-21\",\"slot\":\"10:00-11:00\",\"note\":\"bring supplies\",\"expectedTotal\":499}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookingType").value("SCHEDULED"))
                .andExpect(jsonPath("$.data.assignmentStatus").value("SCHEDULED"))
                .andExpect(jsonPath("$.data.meta.trackingScreen").value("SCHEDULED_STATUS"));

        mockMvc.perform(post("/api/bookings/debug/validate-slot")
                        .contentType("application/json")
                        .content("{\"date\":\"2026-06-21\",\"slot\":\"10:00-11:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(true))
                .andExpect(jsonPath("$.data.reasonCode").value("OK"));

        mockMvc.perform(get("/api/bookings/details/15").requestAttr("userId", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookingType").value("SCHEDULED"))
                .andExpect(jsonPath("$.data.statusHistory", hasSize(1)))
                .andExpect(jsonPath("$.data.meta.canCancel").value(true));
    }

    @Test
    void geoBrowsingAndMappingShouldRespectServiceabilityAndMediaUrls() throws Exception {
        ServiceabilityCheckResponse blocked = mock(ServiceabilityCheckResponse.class);
        when(blocked.isServiceable()).thenReturn(false);
        when(serviceabilityService.resolve("122003", 28.6, 77.2)).thenReturn(blocked);

        mockMvc.perform(get("/api/categories")
                        .param("pincode", "122003")
                        .param("lat", "28.6")
                        .param("lng", "77.2")
                        .requestAttr("userId", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data", hasSize(0)));

        ServiceabilityCheckResponse allowed = mock(ServiceabilityCheckResponse.class);
        when(allowed.isServiceable()).thenReturn(true);
        when(serviceabilityService.resolve("122003", 28.6, 77.2)).thenReturn(allowed);

        AdminCategoryEntity category = new AdminCategoryEntity();
        ReflectionTestUtils.setField(category, "id", 5L);
        category.setName("Cleaning");
        category.setDisplayOrder(2);
        category.setIconUrl("/cat.png");
        when(categoryRepository.findActiveForCustomer(any(Pageable.class))).thenReturn(List.of(category));
        when(mediaUrlResolver.toPublicUrl("/cat.png")).thenReturn("https://cdn.lexho/cat.png");

        Partner available = new Partner();
        available.setId(11L);
        available.setName("Deep Clean");
        available.setAvailable(true);
        available.setCategoryId(5L);
        available.setPrice(299.0);
        available.setRating(4.9);
        available.setImage("/partner.png");
        Partner hidden = new Partner();
        hidden.setId(12L);
        hidden.setName("Ignore Me");
        hidden.setAvailable(false);
        when(partnerRepository.findAll()).thenReturn(List.of(hidden, available));

        mockMvc.perform(get("/api/categories")
                        .param("pincode", "122003")
                        .param("lat", "28.6")
                        .param("lng", "77.2")
                        .requestAttr("userId", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data[0].icon").value("https://cdn.lexho/cat.png"));

        mockMvc.perform(get("/api/services/popular")
                        .param("pincode", "122003")
                        .param("lat", "28.6")
                        .param("lng", "77.2")
                        .requestAttr("userId", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data", hasSize(1)))
                .andExpect(jsonPath("$.data.data[0].name").value("Deep Clean"));
    }

    @Test
    void bookingMutationEndpointsShouldCoverCancelAndRescheduleBranches() throws Exception {
        OrderResponseDTO cancelled = new OrderResponseDTO();
        cancelled.setStatus("CANCELLED");
        when(orderService.cancelOrder(25L, 7L)).thenReturn(cancelled);

        Order scheduled = new Order();
        scheduled.setId(26L);
        scheduled.setUserId(7L);
        scheduled.setBookingType("SCHEDULED");
        scheduled.setStatus(OrderStatus.CREATED);
        scheduled.setPaymentStatus(PaymentStatus.SUCCESS);
        scheduled.setScheduledAt(LocalDateTime.of(2026, 6, 20, 10, 0));
        when(orderRepository.findById(26L)).thenReturn(Optional.of(scheduled));
        when(orderRepository.findByIdForUpdate(26L)).thenReturn(Optional.of(scheduled));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(slotDateTimePolicy.zoneId()).thenReturn(ZoneId.systemDefault());
        when(slotDateTimePolicy.now()).thenReturn(LocalDateTime.of(2026, 6, 20, 10, 0));
        when(slotDateTimePolicy.today()).thenReturn(LocalDate.of(2026, 6, 20));
        CartConfigEntity config = new CartConfigEntity();
        config.setScheduledBookingWindowDays(7);
        when(cartConfigService.currentEntity()).thenReturn(config);
        when(slotDateTimePolicy.parseSlotStart(LocalDate.of(2026, 6, 21), "10:00-11:00"))
                .thenReturn(LocalDateTime.of(2026, 6, 21, 10, 0));
        when(slotDateTimePolicy.isExpired(any())).thenReturn(false);
        when(customerCartSlotService.slotsForDate(LocalDate.of(2026, 6, 21))).thenReturn(java.util.Map.of(
                "slots", List.of(java.util.Map.of("startTime", "10:00", "slot", "10:00-11:00"))
        ));
        when(customerCartSlotService.slotRowForDate(LocalDate.of(2026, 6, 21), "10:00-11:00"))
                .thenReturn(java.util.Map.of("available", true));

        mockMvc.perform(put("/api/bookings/cancel/25").requestAttr("userId", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignmentStatus").value("CANCELLED"))
                .andExpect(jsonPath("$.data.meta.canCancel").value(false));

        mockMvc.perform(put("/api/bookings/reschedule/26")
                        .requestAttr("userId", 7L)
                        .contentType("application/json")
                        .content("{\"date\":\"2026-06-21T10:00:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookingId").value(26))
                .andExpect(jsonPath("$.data.scheduledAt").exists());

        mockMvc.perform(post("/api/bookings/26/reschedule")
                        .requestAttr("userId", 7L)
                        .contentType("application/json")
                        .content("{\"date\":\"2026-06-21\",\"slot\":\"10:00-11:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookingId").value(26))
                .andExpect(jsonPath("$.data.scheduledAt").exists());

        mockMvc.perform(put("/api/bookings/reschedule/26")
                        .requestAttr("userId", 7L)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/bookings/26/reschedule")
                        .requestAttr("userId", 7L)
                        .contentType("application/json")
                        .content("{\"date\":\"2026-06-21\"}"))
                .andExpect(status().isBadRequest());

        verify(customerCartSlotService, atLeast(2)).lockSlotCapacity();
    }

    @Test
    void reviewReferralFavoriteAndSupportEndpointsShouldCoverAdditionalBranches() throws Exception {
        ReviewEntity review = new ReviewEntity();
        ReflectionTestUtils.setField(review, "id", 81L);
        review.setRating(4);
        review.setReviewComment("Solid");
        ReflectionTestUtils.setField(review, "createdAt", LocalDateTime.of(2026, 6, 20, 10, 0));
        when(reviewRepository.findByServiceIdAndDeletedFalseOrderByCreatedAtDesc(55L)).thenReturn(List.of(review));

        when(favoriteRepository.existsByUserIdAndServiceId(7L, 20L)).thenReturn(true);

        when(referralCodeRepository.findByUserId(7L)).thenReturn(Optional.empty());
        when(referralCodeRepository.existsByCodeIgnoreCase(anyString())).thenReturn(false);
        when(referralCodeRepository.save(any(LexhoReferralCode.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SupportTicket filtered = new SupportTicket();
        ReflectionTestUtils.setField(filtered, "id", 61L);
        filtered.setSubject("Filtered");
        filtered.setMessage("Only open");
        filtered.setStatus("OPEN");
        filtered.setPriority("MEDIUM");
        ReflectionTestUtils.setField(filtered, "createdAt", LocalDateTime.of(2026, 6, 20, 8, 0));
        ReflectionTestUtils.setField(filtered, "updatedAt", LocalDateTime.of(2026, 6, 20, 9, 0));
        when(supportTicketRepository.findByUserIdAndStatusIgnoreCaseOrderByCreatedAtDesc(eq(7L), eq("OPEN"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(filtered)));

        mockMvc.perform(get("/api/reviews/service/55").param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data[0].comment").value("Solid"));

        mockMvc.perform(post("/api/favorites/add").requestAttr("userId", 7L)
                        .contentType("application/json").content("{\"serviceId\":20}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.serviceId").value("20"));
        verify(favoriteRepository, never()).save(any(LexhoFavorite.class));

        mockMvc.perform(post("/api/referral/generate").requestAttr("userId", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code", org.hamcrest.Matchers.startsWith("LEX")));

        mockMvc.perform(get("/api/support/history").requestAttr("userId", 7L)
                        .param("status", "OPEN").param("page", "2").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.page").value(2))
                .andExpect(jsonPath("$.data[0].subject").value("Filtered"));
    }

    @Test
    void couponReviewReferralAndSupportEndpointsShouldCoverHappyAndErrorFlows() throws Exception {
        BannerPromotion coupon = new BannerPromotion();
        ReflectionTestUtils.setField(coupon, "id", 1L);
        coupon.setCouponCode("SAVE10");
        coupon.setDiscountPercentage(10.0);
        coupon.setStatus(BannerStatus.ACTIVE);
        coupon.setEndDate(LocalDateTime.now().plusDays(1));
        coupon.setDescription("Summer");
        when(bannerRepository.findByStatusAndDeletedFalse(BannerStatus.ACTIVE)).thenReturn(List.of(coupon));

        ReviewEntity review = new ReviewEntity();
        ReflectionTestUtils.setField(review, "id", 8L);
        when(reviewRepository.save(any(ReviewEntity.class))).thenAnswer(invocation -> {
            ReviewEntity saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 8L);
            return saved;
        });

        LexhoReferralCode code = new LexhoReferralCode();
        code.setUserId(9L);
        code.setCode("REF100");
        code.setUsedCount(0);
        when(referralUseRepository.existsByUserIdAndCodeIgnoreCase(7L, "REF100")).thenReturn(false);
        when(referralCodeRepository.findByCodeIgnoreCase("REF100")).thenReturn(Optional.of(code));

        SupportFaqCategory category = new SupportFaqCategory();
        category.setCategoryKey("payments");
        category.setLabel("Payments");
        category.setDisplayOrder(1);
        SupportFaq faq = new SupportFaq();
        faq.setFaqKey("f1");
        faq.setQuestion("How?");
        faq.setAnswer("Like this");
        faq.setDisplayOrder(1);
        when(supportFaqCategoryRepository.findByActiveTrueOrderByDisplayOrderAscIdAsc()).thenReturn(List.of(category));
        when(supportFaqRepository.findByCategoryAndActiveTrueOrderByDisplayOrderAscIdAsc(category)).thenReturn(List.of(faq));

        SupportTicket ticket = new SupportTicket();
        ReflectionTestUtils.setField(ticket, "id", 44L);
        ticket.setUserId(7L);
        ticket.setSubject("Help");
        ticket.setMessage("Need support");
        ticket.setStatus("OPEN");
        ticket.setPriority("HIGH");
        ReflectionTestUtils.setField(ticket, "createdAt", LocalDateTime.of(2026, 6, 20, 10, 0));
        ReflectionTestUtils.setField(ticket, "updatedAt", LocalDateTime.of(2026, 6, 20, 10, 5));
        when(supportTicketRepository.save(any(SupportTicket.class))).thenReturn(ticket);
        when(supportTicketRepository.findByUserIdOrderByCreatedAtDesc(eq(7L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ticket)));
        when(supportTicketRepository.findByUserIdAndId(7L, 44L)).thenReturn(Optional.of(ticket));
        SupportTicketReply reply = new SupportTicketReply();
        reply.setMessage("We are on it");
        reply.setSenderType("SUPPORT");
        ReflectionTestUtils.setField(reply, "createdAt", LocalDateTime.of(2026, 6, 20, 11, 0));
        when(supportTicketReplyRepository.findByTicket_IdOrderByCreatedAtAsc(44L)).thenReturn(List.of(reply));

        mockMvc.perform(post("/api/coupon/apply")
                        .contentType("application/json")
                        .content("{\"code\":\"SAVE10\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.discount").value(10));
        mockMvc.perform(get("/api/coupon/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data", hasSize(1)));

        mockMvc.perform(post("/api/reviews/add").requestAttr("userId", 7L)
                        .contentType("application/json")
                        .content("{\"serviceId\":\"10\",\"rating\":5,\"comment\":\"Great\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("8"));

        mockMvc.perform(post("/api/referral/apply").requestAttr("userId", 7L)
                        .contentType("application/json")
                        .content("{\"code\":\"REF100\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("REF100"));

        mockMvc.perform(get("/api/support/faqs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].faqs", hasSize(1)));
        mockMvc.perform(post("/api/support/create").requestAttr("userId", 7L)
                        .contentType("application/json")
                        .content("{\"subject\":\"Help\",\"message\":\"Need support\",\"priority\":\"HIGH\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("TCK-10044"));
        mockMvc.perform(get("/api/support/history").requestAttr("userId", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));
        mockMvc.perform(get("/api/support/tickets/TCK-10044").requestAttr("userId", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messages", hasSize(2)));

    }

    @Test
    void supportReplyStatusAndBookingValidationShouldWork() throws Exception {
        Order order = new Order();
        order.setId(55L);
        order.setUserId(7L);
        when(orderRepository.findById(55L)).thenReturn(Optional.of(order));

        SupportTicket ticket = new SupportTicket();
        ReflectionTestUtils.setField(ticket, "id", 44L);
        ticket.setUserId(7L);
        ticket.setSubject("Help");
        ticket.setMessage("Need support");
        ticket.setStatus("RESOLVED");
        ticket.setBookingId("55");
        ticket.setPriority("MEDIUM");
        ticket.setAttachmentUrls("[\"https://cdn.lexho.test/ticket.png\"]");
        ReflectionTestUtils.setField(ticket, "createdAt", LocalDateTime.of(2026, 7, 20, 10, 0));
        ReflectionTestUtils.setField(ticket, "updatedAt", LocalDateTime.of(2026, 7, 20, 10, 5));

        when(supportTicketRepository.save(any(SupportTicket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(supportTicketRepository.findByUserIdAndId(7L, 44L)).thenReturn(Optional.of(ticket));
        when(supportTicketRepository.findByUserIdOrderByCreatedAtDesc(eq(7L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ticket)));
        when(supportTicketReplyRepository.save(any(SupportTicketReply.class))).thenAnswer(invocation -> {
            SupportTicketReply saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "createdAt", LocalDateTime.of(2026, 7, 20, 10, 7));
            return saved;
        });

        mockMvc.perform(post("/api/support/create").requestAttr("userId", 7L)
                        .contentType("application/json")
                        .content("""
                                {"subject":"Booking issue","message":"Need invoice","bookingId":"55","attachmentUrls":["https://cdn.lexho.test/a.png"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookingId").value("55"))
                .andExpect(jsonPath("$.data.attachmentUrls", hasSize(1)));

        mockMvc.perform(post("/api/support/tickets/TCK-10044/reply").requestAttr("userId", 7L)
                        .contentType("application/json")
                        .content("""
                                {"message":"Here is the screenshot","attachmentUrls":["https://cdn.lexho.test/reply.png"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.message.sender").value("CUSTOMER"))
                .andExpect(jsonPath("$.data.message.attachmentUrls", hasSize(1)));

        mockMvc.perform(patch("/api/support/tickets/TCK-10044/status").requestAttr("userId", 7L)
                        .contentType("application/json")
                        .content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"));

        mockMvc.perform(patch("/api/support/tickets/TCK-10044/status").requestAttr("userId", 7L)
                        .contentType("application/json")
                        .content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("OPEN"));
    }

    @Test
    void supportTicketStreamShouldOpenForOwnedTicket() throws Exception {
        SupportTicket ticket = new SupportTicket();
        ReflectionTestUtils.setField(ticket, "id", 44L);
        ticket.setUserId(7L);
        when(supportTicketRepository.findByUserIdAndId(7L, 44L)).thenReturn(Optional.of(ticket));

        mockMvc.perform(get("/api/support/tickets/TCK-10044/stream").requestAttr("userId", 7L))
                .andExpect(status().isOk());
    }

    @Test
    void customerNotificationStreamAndActivityFeedShouldDelegate() throws Exception {
        when(notificationRealtimeService.subscribe(7L)).thenReturn(new org.springframework.web.servlet.mvc.method.annotation.SseEmitter());
        when(customerActivityFeedService.activity(7L, 2, 5)).thenReturn(java.util.Map.of(
                "data", java.util.List.of(java.util.Map.of("type", "NOTIFICATION", "id", "1")),
                "meta", java.util.Map.of("page", 2, "limit", 5, "total", 1, "totalPages", 1)
        ));

        mockMvc.perform(get("/api/customer/notifications/stream").requestAttr("userId", 7L))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/customer/inbox/activity").requestAttr("userId", 7L)
                        .param("page", "2").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].type").value("NOTIFICATION"))
                .andExpect(jsonPath("$.meta.page").value(2));
    }

    @Test
    void supportCreateShouldRejectForeignBooking() throws Exception {
        Order order = new Order();
        order.setId(99L);
        order.setUserId(8L);
        when(orderRepository.findById(99L)).thenReturn(Optional.of(order));

        assertThrows(jakarta.servlet.ServletException.class, () ->
                mockMvc.perform(post("/api/support/create").requestAttr("userId", 7L)
                                .contentType("application/json")
                                .content("""
                                        {"subject":"Booking issue","message":"Need help","bookingId":"99"}
                                        """))
        );
    }
}

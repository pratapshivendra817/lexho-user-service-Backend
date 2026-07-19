package com.lexho.user_service.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.booking.entity.Order;
import com.lexho.booking.repository.OrderRepository;
import com.lexho.catalog.repository.CategoryRepository;
import com.lexho.common.enums.OrderStatus;
import com.lexho.common.enums.PaymentStatus;
import com.lexho.provider.repository.PartnerRepository;
import com.lexho.testconfig.WebSliceBootTestConfig;
import com.lexho.user_service.admin.repository.AdminCategoryRepository;
import com.lexho.user_service.admin.repository.SupportFaqCategoryRepository;
import com.lexho.user_service.admin.repository.SupportFaqRepository;
import com.lexho.user_service.admin.repository.SupportTicketRepository;
import com.lexho.user_service.admin.repository.SupportTicketReplyRepository;
import com.lexho.user_service.admin.geo.service.GeoFenceValidationService;
import com.lexho.user_service.banner.repository.BannerPromotionRepository;
import com.lexho.user_service.config.JwtFilter;
import com.lexho.user_service.cartconfig.entity.CartConfigEntity;
import com.lexho.user_service.cartconfig.service.CartConfigService;
import com.lexho.user_service.customer.controller.CustomerServiceController;
import com.lexho.user_service.customer.location.service.ServiceabilityService;
import com.lexho.user_service.customer.service.CustomerCartService;
import com.lexho.user_service.customer.service.CustomerCartSlotService;
import com.lexho.user_service.customer.service.CustomerCatalogService;
import com.lexho.user_service.customer.service.CustomerPopularServiceService;
import com.lexho.user_service.customer.service.CustomerServiceCatalogService;
import com.lexho.user_service.customer.service.SlotDateTimePolicy;
import com.lexho.user_service.dispatch.repository.PartnerShiftRepository;
import com.lexho.user_service.dispatch.repository.BookingEventRepository;
import com.lexho.user_service.dispatch.repository.CurrentPartnerLocationRepository;
import com.lexho.user_service.dispatch.service.DispatchOrchestrationService;
import com.lexho.user_service.dto.OrderResponseDTO;
import com.lexho.user_service.lexho.controller.LexhoExperienceController;
import com.lexho.user_service.lexho.dto.LexhoDtos;
import com.lexho.user_service.lexhopartner.repository.PartnerProfileRepository;
import com.lexho.user_service.lexho.repository.LexhoFavoriteRepository;
import com.lexho.user_service.lexho.repository.LexhoReferralCodeRepository;
import com.lexho.user_service.lexho.repository.LexhoReferralUseRepository;
import com.lexho.user_service.lexho.service.LexhoAddressService;
import com.lexho.user_service.notification.service.NotificationManagementService;
import com.lexho.user_service.review.repository.ReviewRepository;
import com.lexho.user_service.service.ApiRateLimiterService;
import com.lexho.user_service.service.CartService;
import com.lexho.user_service.service.HomeService;
import com.lexho.user_service.service.NotificationService;
import com.lexho.user_service.service.OrderService;
import com.lexho.user_service.subscription.repository.SubscriptionFrequencyRepository;
import com.lexho.user_service.subscription.repository.SubscriptionMasterRepository;
import com.lexho.user_service.subscription.repository.SubscriptionVisitRepository;
import com.lexho.user_service.util.MediaUrlResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = WebSliceBootTestConfig.class,
        properties = "spring.cloud.compatibility-verifier.enabled=false"
)
@AutoConfigureMockMvc(addFilters = false)
@Import({CustomerServiceController.class, CustomerCartSlotService.class, SlotDateTimePolicy.class, LexhoExperienceController.class})
class SlotBookingPaymentContractTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JwtFilter jwtFilter;
    @MockBean
    private CustomerCatalogService customerCatalogService;
    @MockBean
    private CustomerCartService customerCartService;
    @MockBean
    private CustomerPopularServiceService customerPopularServiceService;
    @MockBean
    private CustomerServiceCatalogService customerServiceCatalogService;
    @MockBean
    private OrderRepository orderRepository;
    @MockBean
    private PartnerShiftRepository partnerShiftRepository;
    @MockBean
    private SlotDateTimePolicy slotDateTimePolicy;
    @MockBean
    private HomeService homeService;
    @MockBean
    private CartService cartService;
    @MockBean
    private OrderService orderService;
    @MockBean
    private NotificationService notificationService;
    @MockBean
    private NotificationManagementService notificationManagementService;
    @MockBean
    private LexhoAddressService addressService;
    @MockBean
    private BannerPromotionRepository bannerPromotionRepository;
    @MockBean
    private CategoryRepository categoryRepository;
    @MockBean
    private AdminCategoryRepository adminCategoryRepository;
    @MockBean
    private PartnerRepository partnerRepository;
    @MockBean
    private ReviewRepository reviewRepository;
    @MockBean
    private LexhoFavoriteRepository favoriteRepository;
    @MockBean
    private LexhoReferralCodeRepository referralCodeRepository;
    @MockBean
    private LexhoReferralUseRepository referralUseRepository;
    @MockBean
    private SupportTicketRepository supportTicketRepository;
    @MockBean
    private SupportTicketReplyRepository supportTicketReplyRepository;
    @MockBean
    private SupportFaqCategoryRepository supportFaqCategoryRepository;
    @MockBean
    private SupportFaqRepository supportFaqRepository;
    @MockBean
    private MediaUrlResolver mediaUrlResolver;
    @MockBean
    private DispatchOrchestrationService dispatchOrchestrationService;
    @MockBean
    private ApiRateLimiterService apiRateLimiterService;
    @MockBean
    private PartnerProfileRepository partnerProfileRepository;
    @MockBean
    private GeoFenceValidationService geoFenceValidationService;
    @MockBean
    private ServiceabilityService serviceabilityService;
    @MockBean
    private CartConfigService cartConfigService;
    @MockBean
    private SubscriptionMasterRepository subscriptionMasterRepository;
    @MockBean
    private SubscriptionFrequencyRepository subscriptionFrequencyRepository;
    @MockBean
    private SubscriptionVisitRepository subscriptionVisitRepository;
    @MockBean
    private CurrentPartnerLocationRepository currentPartnerLocationRepository;
    @MockBean
    private BookingEventRepository bookingEventRepository;
    private final Map<Long, Order> orders = new HashMap<>();
    private final AtomicLong orderSeq = new AtomicLong(2000L);

    @BeforeEach
    void setup() {
        orders.clear();
        when(cartConfigService.currentEntity()).thenReturn(new CartConfigEntity());
        SlotDateTimePolicy realPolicy = new SlotDateTimePolicy(cartConfigService);
        ZoneId ist = ZoneId.of("Asia/Kolkata");
        LocalDateTime fixedNow = LocalDateTime.of(2026, 5, 19, 12, 0);

        when(slotDateTimePolicy.zoneId()).thenReturn(ist);
        when(slotDateTimePolicy.now()).thenReturn(fixedNow);
        when(slotDateTimePolicy.today()).thenReturn(fixedNow.toLocalDate());
        when(slotDateTimePolicy.isExpired(any(LocalDateTime.class)))
                .thenAnswer(invocation -> {
                    LocalDateTime value = invocation.getArgument(0);
                    return !value.isAfter(fixedNow);
                });
        when(slotDateTimePolicy.parseSlotStart(any(LocalDate.class), any(String.class)))
                .thenAnswer(invocation -> realPolicy.parseSlotStart(invocation.getArgument(0), invocation.getArgument(1)));

        doNothing().when(apiRateLimiterService).check(any(), any(), anyInt(), any(), any());
        when(partnerShiftRepository.existsByOnlineTrue()).thenReturn(true);
        when(orderRepository.countByScheduledAtGreaterThanEqualAndScheduledAtLessThanAndStatusIn(any(), any(), anySet())).thenReturn(0L);
        when(orderRepository.findById(anyLong())).thenAnswer(invocation -> Optional.ofNullable(orders.get(invocation.getArgument(0))));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order value = invocation.getArgument(0);
            if (value.getId() == null) {
                value.setId(orderSeq.incrementAndGet());
            }
            orders.put(value.getId(), value);
            return value;
        });
        when(addressService.getById(anyLong(), anyLong())).thenReturn(new LexhoDtos.AddressResponse(6L, "Home", "Line 1", "Indore", true));
        when(orderService.placeOrder(anyLong(), any())).thenAnswer(invocation -> {
            Long userId = invocation.getArgument(0);
            Long bookingId = orderSeq.incrementAndGet();
            Order entity = new Order();
            entity.setId(bookingId);
            entity.setUserId(userId);
            entity.setStatus(OrderStatus.CREATED);
            entity.setPaymentStatus(PaymentStatus.PENDING);
            orders.put(bookingId, entity);

            OrderResponseDTO dto = new OrderResponseDTO();
            dto.setId(bookingId);
            dto.setStatus(OrderStatus.CREATED.name());
            dto.setTotalAmount(5250.0);
            return dto;
        });
    }

    @Test
    void nextFourDaysSlotsBookingAndPaymentChainShouldPass() throws Exception {
        LocalDate start = slotDateTimePolicy.today().plusDays(1);

        for (int i = 0; i < 4; i++) {
            String date = start.plusDays(i).toString();
            MvcResult slotsResult = mockMvc.perform(get("/api/customer/cart/slots").param("date", date))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.date").value(date))
                    .andExpect(jsonPath("$.data.timezone").value("Asia/Kolkata"))
                    .andReturn();

            JsonNode slots = objectMapper.readTree(slotsResult.getResponse().getContentAsString()).path("data").path("slots");
            assertTrue(slots.isArray());
            assertFalse(slots.isEmpty());

            JsonNode firstAvailable = null;
            for (JsonNode slot : slots) {
                if (slot.path("available").asBoolean(false)) {
                    firstAvailable = slot;
                    break;
                }
            }
            assertNotNull(firstAvailable, "No available slot for date " + date);

            String slotLabel = firstAvailable.path("slot").asText();
            assertFalse(slotLabel.isBlank());
            assertFalse(firstAvailable.path("startTime").asText().isBlank());
            assertFalse(firstAvailable.path("endTime").asText().isBlank());

            String bookingBody = objectMapper.writeValueAsString(Map.of(
                    "serviceId", 23,
                    "addressId", 6,
                    "date", date,
                    "slot", slotLabel,
                    "note", "QA validation"
            ));
            MvcResult bookingResult = mockMvc.perform(post("/api/bookings/create")
                            .requestAttr("userId", 101L)
                            .requestAttr("role", "CUSTOMER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bookingBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").isNotEmpty())
                    .andExpect(jsonPath("$.data.contractVersion").value(1))
                    .andExpect(jsonPath("$.data.statusHistory").isArray())
                    .andExpect(jsonPath("$.data.meta.trackingScreen").value("SCHEDULED_STATUS"))
                    .andExpect(jsonPath("$.data.meta.showMap").value(false))
                    .andExpect(jsonPath("$.data.meta.trackingRefreshSeconds").isNumber())
                    .andExpect(jsonPath("$.data.trackingScreen").doesNotExist())
                    .andExpect(jsonPath("$.data.showMap").doesNotExist())
                    .andExpect(jsonPath("$.data.partnerLatitude").doesNotExist())
                    .andExpect(jsonPath("$.data.partnerLongitude").doesNotExist())
                    .andReturn();

            String bookingId = objectMapper.readTree(bookingResult.getResponse().getContentAsString()).path("data").path("id").asText();

            mockMvc.perform(post("/api/payments/initiate")
                            .requestAttr("userId", 101L)
                            .requestAttr("role", "CUSTOMER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "mode", "UPI",
                                    "bookingId", "BK" + bookingId,
                                    "amount", 5250
                            ))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.bookingId").value(bookingId));

            mockMvc.perform(get("/api/payments/status/{bookingId}", "BK" + bookingId)
                            .requestAttr("userId", 101L)
                            .requestAttr("role", "CUSTOMER"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.bookingId").value(bookingId));
        }
    }

    @Test
    void bookingCreateShouldAcceptBoundarySlotsAndRejectPastSlot() throws Exception {
        when(slotDateTimePolicy.now()).thenReturn(LocalDateTime.of(2026, 5, 19, 5, 0));
        when(slotDateTimePolicy.isExpired(any(LocalDateTime.class)))
                .thenAnswer(invocation -> {
                    LocalDateTime value = invocation.getArgument(0);
                    return !value.isAfter(LocalDateTime.of(2026, 5, 19, 5, 0));
                });

        createBooking("2026-05-19", "09:00-09:15")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.contractVersion").value(1))
                .andExpect(jsonPath("$.data.meta.trackingScreen").value("SCHEDULED_STATUS"))
                .andExpect(jsonPath("$.data.trackingScreen").doesNotExist());

        createBooking("2026-05-19", "22:45-23:00")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.contractVersion").value(1))
                .andExpect(jsonPath("$.data.meta.trackingScreen").value("SCHEDULED_STATUS"))
                .andExpect(jsonPath("$.data.showMap").doesNotExist());

        when(slotDateTimePolicy.now()).thenReturn(LocalDateTime.of(2026, 5, 19, 12, 0));
        when(slotDateTimePolicy.isExpired(any(LocalDateTime.class)))
                .thenAnswer(invocation -> {
                    LocalDateTime value = invocation.getArgument(0);
                    return !value.isAfter(LocalDateTime.of(2026, 5, 19, 12, 0));
                });

        jakarta.servlet.ServletException ex = org.junit.jupiter.api.Assertions.assertThrows(jakarta.servlet.ServletException.class, () ->
                createBooking("2026-05-19", "11:45-12:00").andReturn()
        );
        org.junit.jupiter.api.Assertions.assertTrue(ex.getCause() instanceof com.lexho.user_service.booking.exception.BookingValidationException);
        org.junit.jupiter.api.Assertions.assertEquals("Selected slot is expired. Please choose a future slot.", ex.getCause().getMessage());
    }

    private ResultActions createBooking(String date, String slot) throws Exception {
        String bookingBody = objectMapper.writeValueAsString(Map.of(
                "serviceId", 23,
                "addressId", 6,
                "date", date,
                "slot", slot,
                "note", "QA validation"
        ));
        return mockMvc.perform(post("/api/bookings/create")
                .requestAttr("userId", 101L)
                .requestAttr("role", "CUSTOMER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookingBody));
    }
}

package com.lexho.user_service.lexho.controller;

import com.lexho.booking.entity.Order;
import com.lexho.booking.repository.OrderRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.catalog.entity.ServiceCategory;
import com.lexho.catalog.repository.CategoryRepository;
import com.lexho.common.enums.PaymentStatus;
import com.lexho.common.enums.OrderStatus;
import com.lexho.provider.entity.Partner;
import com.lexho.provider.repository.PartnerRepository;
import com.lexho.user_service.admin.entity.AdminCategoryEntity;
import com.lexho.user_service.admin.entity.SupportFaq;
import com.lexho.user_service.admin.entity.SupportFaqCategory;
import com.lexho.user_service.admin.entity.SupportTicketReply;
import com.lexho.user_service.admin.repository.AdminCategoryRepository;
import com.lexho.user_service.admin.repository.SupportFaqCategoryRepository;
import com.lexho.user_service.admin.repository.SupportFaqRepository;
import com.lexho.user_service.admin.entity.SupportTicket;
import com.lexho.user_service.admin.repository.SupportTicketReplyRepository;
import com.lexho.user_service.admin.repository.SupportTicketRepository;
import com.lexho.user_service.admin.geo.service.GeoFenceValidationService;
import com.lexho.user_service.audit.service.AuditLogService;
import com.lexho.user_service.banner.entity.BannerPromotion;
import com.lexho.user_service.banner.enums.BannerPlacement;
import com.lexho.user_service.banner.enums.BannerStatus;
import com.lexho.user_service.banner.repository.BannerPromotionRepository;
import com.lexho.user_service.dto.*;
import com.lexho.user_service.dispatch.entity.BookingEventEntity;
import com.lexho.user_service.dispatch.service.DispatchOrchestrationService;
import com.lexho.user_service.dispatch.entity.CurrentPartnerLocationEntity;
import com.lexho.user_service.dispatch.repository.BookingEventRepository;
import com.lexho.user_service.dispatch.repository.CurrentPartnerLocationRepository;
import com.lexho.user_service.cartconfig.entity.CartConfigEntity;
import com.lexho.user_service.cartconfig.service.CartConfigService;
import com.lexho.user_service.customer.service.CustomerActivityFeedService;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.customer.service.SlotDateTimePolicy;
import com.lexho.user_service.customer.service.CustomerCartSlotService;
import com.lexho.user_service.booking.exception.BookingValidationException;
import com.lexho.user_service.booking.exception.InstantBookingUnavailableException;
import com.lexho.user_service.booking.model.BookingType;
import com.lexho.user_service.booking.model.InstantUnavailableCode;
import com.lexho.user_service.customer.location.dto.ServiceabilityCheckResponse;
import com.lexho.user_service.customer.location.service.ServiceabilityService;
import com.lexho.user_service.lexho.dto.LexhoDtos;
import com.lexho.user_service.lexho.entity.*;
import com.lexho.user_service.lexho.repository.*;
import com.lexho.user_service.lexhopartner.entity.PartnerProfile;
import com.lexho.user_service.lexhopartner.repository.PartnerProfileRepository;
import com.lexho.user_service.lexho.service.LexhoAddressService;
import com.lexho.user_service.review.entity.ReviewEntity;
import com.lexho.user_service.review.enums.ReviewStatus;
import com.lexho.user_service.review.repository.ReviewRepository;
import com.lexho.user_service.service.CartService;
import com.lexho.user_service.service.HomeService;
import com.lexho.user_service.service.OrderService;
import com.lexho.user_service.service.ApiRateLimiterService;
import com.lexho.user_service.subscription.entity.SubscriptionFrequency;
import com.lexho.user_service.subscription.entity.SubscriptionMaster;
import com.lexho.user_service.subscription.entity.SubscriptionVisit;
import com.lexho.user_service.subscription.repository.SubscriptionFrequencyRepository;
import com.lexho.user_service.subscription.repository.SubscriptionMasterRepository;
import com.lexho.user_service.subscription.repository.SubscriptionVisitRepository;
import com.lexho.user_service.notification.dto.NotificationPageResponse;
import com.lexho.user_service.notification.dto.NotificationResponse;
import com.lexho.user_service.notification.service.NotificationRealtimeService;
import com.lexho.user_service.notification.service.NotificationManagementService;
import com.lexho.user_service.support.service.SupportRealtimeService;
import com.lexho.user_service.util.MediaUrlResolver;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@Slf4j
public class LexhoExperienceController {
    private static final ObjectMapper SUPPORT_OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};
    private static final int BOOKING_CONTRACT_VERSION = 1;
    private static final SecureRandom REFERRAL_CODE_RANDOM = new SecureRandom();
    private static final char[] REFERRAL_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int REFERRAL_CODE_LENGTH = 8;
    private static final int MAX_SUPPORT_ATTACHMENTS = 10;
    private static final int MAX_SUPPORT_ATTACHMENT_URL_LENGTH = 500;

    private final HomeService homeService;
    private final CartService cartService;
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final NotificationManagementService notificationManagementService;
    private final LexhoAddressService addressService;
    private final BannerPromotionRepository bannerPromotionRepository;
    private final CategoryRepository categoryRepository;
    private final AdminCategoryRepository adminCategoryRepository;
    private final PartnerRepository partnerRepository;
    private final PartnerProfileRepository partnerProfileRepository;
    private final ReviewRepository reviewRepository;
    private final LexhoFavoriteRepository favoriteRepository;
    private final LexhoReferralCodeRepository referralCodeRepository;
    private final LexhoReferralUseRepository referralUseRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final SupportTicketReplyRepository supportTicketReplyRepository;
    private final SupportFaqCategoryRepository supportFaqCategoryRepository;
    private final SupportFaqRepository supportFaqRepository;
    private final MediaUrlResolver mediaUrlResolver;
    private final DispatchOrchestrationService dispatchOrchestrationService;
    private final ApiRateLimiterService apiRateLimiterService;
    private final SlotDateTimePolicy slotDateTimePolicy;
    private final CustomerCartSlotService customerCartSlotService;
    private final GeoFenceValidationService geoFenceValidationService;
    private final ServiceabilityService serviceabilityService;
    private final CartConfigService cartConfigService;
    private final SubscriptionMasterRepository subscriptionMasterRepository;
    private final SubscriptionFrequencyRepository subscriptionFrequencyRepository;
    private final SubscriptionVisitRepository subscriptionVisitRepository;
    private final CurrentPartnerLocationRepository currentPartnerLocationRepository;
    private final BookingEventRepository bookingEventRepository;

    @Autowired(required = false)
    private AuditLogService auditLogService;

    @Autowired(required = false)
    private SupportRealtimeService supportRealtimeService;

    @Autowired(required = false)
    private NotificationRealtimeService notificationRealtimeService;

    @Autowired(required = false)
    private CustomerActivityFeedService customerActivityFeedService;

    public LexhoExperienceController(HomeService homeService,
                                     CartService cartService,
                                     OrderService orderService,
                                     OrderRepository orderRepository,
                                     NotificationManagementService notificationManagementService,
                                     LexhoAddressService addressService,
                                     BannerPromotionRepository bannerPromotionRepository,
                                     CategoryRepository categoryRepository,
                                     AdminCategoryRepository adminCategoryRepository,
                                     PartnerRepository partnerRepository,
                                     PartnerProfileRepository partnerProfileRepository,
                                     ReviewRepository reviewRepository,
                                     LexhoFavoriteRepository favoriteRepository,
                                     LexhoReferralCodeRepository referralCodeRepository,
                                     LexhoReferralUseRepository referralUseRepository,
                                     SupportTicketRepository supportTicketRepository,
                                     SupportTicketReplyRepository supportTicketReplyRepository,
                                     SupportFaqCategoryRepository supportFaqCategoryRepository,
                                     SupportFaqRepository supportFaqRepository,
                                     MediaUrlResolver mediaUrlResolver,
                                     DispatchOrchestrationService dispatchOrchestrationService,
                                     ApiRateLimiterService apiRateLimiterService,
                                     SlotDateTimePolicy slotDateTimePolicy,
                                     CustomerCartSlotService customerCartSlotService,
                                     GeoFenceValidationService geoFenceValidationService,
                                     ServiceabilityService serviceabilityService,
                                     CartConfigService cartConfigService,
                                     SubscriptionMasterRepository subscriptionMasterRepository,
                                     SubscriptionFrequencyRepository subscriptionFrequencyRepository,
                                     SubscriptionVisitRepository subscriptionVisitRepository,
                                     CurrentPartnerLocationRepository currentPartnerLocationRepository,
                                     BookingEventRepository bookingEventRepository) {
        this.homeService = homeService;
        this.cartService = cartService;
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.notificationManagementService = notificationManagementService;
        this.addressService = addressService;
        this.bannerPromotionRepository = bannerPromotionRepository;
        this.categoryRepository = categoryRepository;
        this.adminCategoryRepository = adminCategoryRepository;
        this.partnerRepository = partnerRepository;
        this.partnerProfileRepository = partnerProfileRepository;
        this.reviewRepository = reviewRepository;
        this.favoriteRepository = favoriteRepository;
        this.referralCodeRepository = referralCodeRepository;
        this.referralUseRepository = referralUseRepository;
        this.supportTicketRepository = supportTicketRepository;
        this.supportTicketReplyRepository = supportTicketReplyRepository;
        this.supportFaqCategoryRepository = supportFaqCategoryRepository;
        this.supportFaqRepository = supportFaqRepository;
        this.mediaUrlResolver = mediaUrlResolver;
        this.dispatchOrchestrationService = dispatchOrchestrationService;
        this.apiRateLimiterService = apiRateLimiterService;
        this.slotDateTimePolicy = slotDateTimePolicy;
        this.customerCartSlotService = customerCartSlotService;
        this.geoFenceValidationService = geoFenceValidationService;
        this.serviceabilityService = serviceabilityService;
        this.cartConfigService = cartConfigService;
        this.subscriptionMasterRepository = subscriptionMasterRepository;
        this.subscriptionFrequencyRepository = subscriptionFrequencyRepository;
        this.subscriptionVisitRepository = subscriptionVisitRepository;
        this.currentPartnerLocationRepository = currentPartnerLocationRepository;
        this.bookingEventRepository = bookingEventRepository;
    }

    @GetMapping("/api/home/dashboard")
    public ResponseEntity<ApiResponse<LexhoDtos.HomeDashboardResponse>> dashboard(@RequestAttribute("userId") Long userId) {
        HomeResponseDTO home = homeService.getHomeData(userId);
        List<Map<String, Object>> recentBookings = orderService.getUserOrders(userId).stream()
                .limit(5)
                .map(order -> Map.<String, Object>of(
                        "id", String.valueOf(order.getId()),
                        "status", order.getStatus(),
                        "serviceName", order.getItems().isEmpty() ? "Service" : order.getItems().get(0).getPartnerName(),
                        "createdAt", ""
                ))
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Home dashboard", new LexhoDtos.HomeDashboardResponse(
                home.getGreeting(),
                "Unknown",
                recentBookings
        )));
    }

    @GetMapping("/api/banners")
    public ResponseEntity<ApiResponse<Map<String, Object>>> banners(@RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "20") int size) {
        LocalDateTime now = LocalDateTime.now();
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        List<Map<String, Object>> data = bannerPromotionRepository
                .findByStatusAndDeletedFalseAndStartDateLessThanEqualAndEndDateGreaterThanOrderByPriorityDescSlotNumberAsc(
                        BannerStatus.ACTIVE, now, now, pageable
                ).stream()
                .sorted(Comparator
                        .comparingInt((BannerPromotion b) -> b.getPlacement() == BannerPlacement.TOP ? 0 : 1)
                        .thenComparing(BannerPromotion::getPriority, Comparator.reverseOrder())
                        .thenComparing(BannerPromotion::getSlotNumber))
                .map(this::mapBanner)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Banners fetched", Map.of("data", data)));
    }

    @GetMapping("/api/categories")
    public ResponseEntity<ApiResponse<Map<String, Object>>> categories(@RequestParam(defaultValue = "0") int page,
                                                                       @RequestParam(defaultValue = "20") int size,
                                                                       @RequestParam(required = false) Double lat,
                                                                       @RequestParam(required = false) Double lng,
                                                                       @RequestParam(required = false) String pincode,
                                                                       @RequestAttribute(value = "userId", required = false) Long userId) {
        if (!isGeoServiceable(userId, lat, lng, pincode)) {
            return ResponseEntity.ok(ApiResponse.success("Categories fetched", Map.of("data", List.of())));
        }
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        List<Map<String, Object>> data = adminCategoryRepository
                .findActiveForCustomer(pageable).stream()
                .map(this::mapCategory)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Categories fetched", Map.of("data", data)));
    }

    @GetMapping("/api/services/popular")
    public ResponseEntity<ApiResponse<Map<String, Object>>> popular(@RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "20") int size,
                                                                    @RequestParam(required = false) Double lat,
                                                                    @RequestParam(required = false) Double lng,
                                                                    @RequestParam(required = false) String pincode,
                                                                    @RequestAttribute(value = "userId", required = false) Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Popular services fetched",
                Map.of("data", partnerData(page, size, null, userId, lat, lng, pincode))));
    }

    @GetMapping("/api/services/recommended")
    public ResponseEntity<ApiResponse<Map<String, Object>>> recommended(@RequestParam(defaultValue = "0") int page,
                                                                        @RequestParam(defaultValue = "20") int size,
                                                                        @RequestParam(required = false) Double lat,
                                                                        @RequestParam(required = false) Double lng,
                                                                        @RequestParam(required = false) String pincode,
                                                                        @RequestAttribute(value = "userId", required = false) Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Recommended services fetched",
                Map.of("data", partnerData(page, size, null, userId, lat, lng, pincode))));
    }

    @GetMapping("/api/services/quick")
    public ResponseEntity<ApiResponse<Map<String, Object>>> quick(@RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "20") int size,
                                                                  @RequestParam(required = false) Double lat,
                                                                  @RequestParam(required = false) Double lng,
                                                                  @RequestParam(required = false) String pincode,
                                                                  @RequestAttribute(value = "userId", required = false) Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Quick services fetched",
                Map.of("data", partnerData(page, size, null, userId, lat, lng, pincode))));
    }

    @GetMapping("/api/search/services")
    public ResponseEntity<ApiResponse<Map<String, Object>>> search(@RequestParam String keyword,
                                                                   @RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "20") int size,
                                                                   @RequestParam(required = false) Double lat,
                                                                   @RequestParam(required = false) Double lng,
                                                                   @RequestParam(required = false) String pincode,
                                                                   @RequestAttribute(value = "userId", required = false) Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Search result for " + keyword,
                Map.of("data", partnerData(page, size, keyword, userId, lat, lng, pincode))));
    }

    @PostMapping("/api/cart/add")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addCart(@RequestAttribute("userId") Long userId,
                                                                    @Valid @RequestBody LexhoDtos.CartAddRequest request) {
        CartRequest cartRequest = new CartRequest();
        cartRequest.setPartnerId(request.serviceId());
        cartRequest.setQuantity(request.quantity());
        CartResponseDTO added = cartService.addToCart(userId, cartRequest);
        return ResponseEntity.ok(ApiResponse.success("Added to cart", mapCartItem(added)));
    }

    @GetMapping("/api/cart/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cart(@RequestAttribute("userId") Long userId) {
        CartSummaryDTO cart = cartService.getCart(userId);
        List<Map<String, Object>> items = cart.getItems().stream().map(this::mapCartItem).toList();
        return ResponseEntity.ok(ApiResponse.success("Cart fetched", Map.of("items", items, "totalAmount", cart.getTotal())));
    }

    @PutMapping("/api/cart/update")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateCart(@RequestAttribute("userId") Long userId,
                                                                       @Valid @RequestBody LexhoDtos.CartUpdateRequest request) {
        cartService.removeFromCart(userId, request.id());
        CartRequest cartRequest = new CartRequest();
        cartRequest.setPartnerId(request.id());
        cartRequest.setQuantity(request.quantity());
        CartResponseDTO updated = cartService.addToCart(userId, cartRequest);
        return ResponseEntity.ok(ApiResponse.success("Cart updated", mapCartItem(updated)));
    }

    @DeleteMapping("/api/cart/remove")
    public ResponseEntity<ApiResponse<Void>> removeCart(@RequestAttribute("userId") Long userId,
                                                        @Valid @RequestBody LexhoDtos.IdRequest request) {
        cartService.removeFromCart(userId, request.id());
        return ResponseEntity.ok(ApiResponse.success("Cart item removed", null));
    }

    @PostMapping({"/api/bookings/create", "/api/v1/bookings/create"})
    public ResponseEntity<ApiResponse<Map<String, Object>>> createBooking(@RequestAttribute("userId") Long userId,
                                                                          @Valid @RequestBody LexhoDtos.BookingCreateRequest request) {
        apiRateLimiterService.check("booking_create", String.valueOf(userId), 15, java.time.Duration.ofMinutes(5),
                "Too many booking attempts. Please try later");
        LexhoDtos.AddressResponse address;
        try {
            address = addressService.getById(userId, request.addressId());
            Double requestLatitude = resolveBookingLatitude(request);
            Double requestLongitude = resolveBookingLongitude(request);
            log.info("booking_create_coordinates userId={} addressId={} latitudePresent={} longitudePresent={}",
                    userId, request.addressId(), requestLatitude != null, requestLongitude != null);
             LexhoAddress rawAddress = addressService.applyBookingCoordinatesFallback(
                      userId,
                      request.addressId(),
                      requestLatitude,
                      requestLongitude
              );
             geoFenceValidationService.validateBookingAddress(rawAddress);
             ServiceabilityCheckResponse serviceability = serviceabilityService.resolveForCustomer(userId);
             if (serviceability != null && !serviceability.isServiceable()) {
                 throw new BookingValidationException("SERVICE_UNAVAILABLE",
                         "Service is unavailable in selected area");
             }
          } catch (BadRequestException ex) {
             throw new BookingValidationException("ADDRESS_NOT_FOUND", "Address not found for this customer");
          }
          PlaceOrderRequest placeOrderRequest = new PlaceOrderRequest();
          placeOrderRequest.setAddress(address.line1() + ", " + address.city());
          placeOrderRequest.setExpectedTotal(request.expectedTotal());
         BookingType bookingType = request.bookingType() == null || request.bookingType().isBlank()
                 ? BookingType.SCHEDULED
                 : BookingType.valueOf(request.bookingType());
         CartConfigEntity cartConfig = cartConfigService.currentEntity();
         if (bookingType == BookingType.INSTANT && !Boolean.TRUE.equals(cartConfig.getInstantBookingEnabled())) {
             throw new BookingValidationException("SERVICE_UNAVAILABLE", "Instant booking is currently disabled");
         }
         if (bookingType == BookingType.SCHEDULED && !Boolean.TRUE.equals(cartConfig.getScheduledBookingEnabled())) {
             throw new BookingValidationException("SERVICE_UNAVAILABLE", "Scheduled booking is currently disabled");
         }
         placeOrderRequest.setBookingType(bookingType.name());
         if (bookingType == BookingType.SCHEDULED) {
             placeOrderRequest.setScheduledAt(parseScheduledAt(request.date(), request.slot()));
        } else {
            placeOrderRequest.setScheduledAt(LocalDateTime.now());
        }
        OrderResponseDTO order = orderService.placeOrder(userId, placeOrderRequest);
        int dispatched = 0;
        if (bookingType == BookingType.INSTANT) {
            try {
                dispatched = dispatchOrchestrationService.dispatchOrder(
                        order.getId(), userId, request.addressId(), request.serviceId(), 1, placeOrderRequest.getScheduledAt());
            } catch (BadRequestException ex) {
                throw new InstantBookingUnavailableException(
                        InstantUnavailableCode.INSTANT_UNAVAILABLE.name(),
                        "No partner available right now. Please book a scheduled slot.");
            }
        } else {
            Order scheduledOrder = orderRepository.findById(order.getId()).orElse(null);
            if (scheduledOrder != null) {
                if (scheduledOrder.getBookingType() == null || scheduledOrder.getBookingType().isBlank()) {
                    scheduledOrder.setBookingType(bookingType.name());
                }
                if (scheduledOrder.getStatus() == OrderStatus.CREATED
                        && scheduledOrder.getStatus().canTransitionTo(OrderStatus.SCHEDULED)) {
                    scheduledOrder.setStatus(OrderStatus.SCHEDULED);
                }
                orderRepository.save(scheduledOrder);
            }
        }
        Order createdOrder = orderRepository.findById(order.getId()).orElse(null);
        boolean instantDispatchFailed = bookingType == BookingType.INSTANT && dispatched <= 0;
        if (!instantDispatchFailed
                && createdOrder != null
                && (createdOrder.getBookingType() == null || createdOrder.getBookingType().isBlank())) {
            createdOrder.setBookingType(bookingType.name());
            orderRepository.save(createdOrder);
        }
        if (instantDispatchFailed) {
            Order failedOrder = orderRepository.findById(order.getId()).orElse(null);
            if (failedOrder != null) {
                if (failedOrder.getBookingType() == null || failedOrder.getBookingType().isBlank()) {
                    failedOrder.setBookingType(bookingType.name());
                }
                failedOrder.setStatus(OrderStatus.INSTANT_FAILED);
                failedOrder.setUnavailableReason(InstantUnavailableCode.INSTANT_UNAVAILABLE.name());
                failedOrder.setSlaMinutes(DispatchOrchestrationService.INSTANT_SLA_MINUTES);
                orderRepository.save(failedOrder);
            }
            throw new InstantBookingUnavailableException(
                    InstantUnavailableCode.INSTANT_UNAVAILABLE.name(),
                    "No partner available right now. Please book a scheduled slot."
            );
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", String.valueOf(order.getId()));
        payload.put("bookingId", String.valueOf(order.getId()));
        payload.put("contractVersion", BOOKING_CONTRACT_VERSION);
        payload.put("serviceId", String.valueOf(request.serviceId()));
        payload.put("addressId", String.valueOf(request.addressId()));
        payload.put("bookingType", bookingType.name());
        payload.put("assignmentStatus", bookingType == BookingType.INSTANT ? "SEARCHING_PARTNER" : "SCHEDULED");
        payload.put("date", request.date() == null ? "" : request.date());
        payload.put("slot", request.slot());
        payload.put("note", request.note());
        payload.put("status", order.getStatus());
        payload.put("slaMinutes", bookingType == BookingType.INSTANT ? DispatchOrchestrationService.INSTANT_SLA_MINUTES : 0);
        Order persisted = orderRepository.findById(order.getId()).orElse(null);
        String assignmentStatus = persisted == null
                ? (bookingType == BookingType.INSTANT ? "SEARCHING_PARTNER" : "SCHEDULED")
                : deriveAssignmentStatus(persisted, persisted.getAssignedPartnerId() != null);
        Map<String, Object> trackingPayload = persisted == null ? Map.of("showMap", false) : mapTrackingPayload(persisted, assignmentStatus);
        Map<String, Object> permissions = persisted == null ? Map.of(
                "canCancel", true,
                "canReschedule", bookingType == BookingType.SCHEDULED,
                "canCallPartner", false,
                "canTrackPartner", false,
                "canRaiseIssue", false
        ) : actionPermissions(persisted, assignmentStatus, Boolean.TRUE.equals(trackingPayload.get("partnerAvailable")));
        payload.put("statusHistory", persisted == null ? List.of() : statusHistory(persisted));
        payload.put("meta", metaPayload(
                persisted == null ? (bookingType == BookingType.SCHEDULED ? "SCHEDULED_STATUS" : "INSTANT_STATUS") : trackingScreen(persisted, assignmentStatus),
                trackingRefreshSeconds(String.valueOf(order.getStatus()), assignmentStatus),
                trackingPayload,
                permissions
        ));
        audit(userId, null, order.getId(), "BOOKING_CREATED", null, bookingType.name());
        return ResponseEntity.ok(ApiResponse.success("Booking created", payload));
    }

    private Double resolveBookingLatitude(LexhoDtos.BookingCreateRequest request) {
        return firstValidCoordinate(
                request.latitude(),
                request.addressLatitude(),
                request.coordinates() == null ? null : request.coordinates().latitude(),
                request.addressCoordinates() == null ? null : request.addressCoordinates().latitude()
        );
    }

    private Double resolveBookingLongitude(LexhoDtos.BookingCreateRequest request) {
        return firstValidCoordinate(
                request.longitude(),
                request.addressLongitude(),
                request.coordinates() == null ? null : request.coordinates().longitude(),
                request.addressCoordinates() == null ? null : request.addressCoordinates().longitude()
        );
    }

    private Double firstValidCoordinate(Double... values) {
        for (Double value : values) {
            if (value != null
                    && Double.isFinite(value)
                    && BigDecimal.ZERO.compareTo(BigDecimal.valueOf(value)) != 0) {
                return value;
            }
        }
        return null;
    }

    @PostMapping("/api/bookings/debug/validate-slot")
    public ResponseEntity<ApiResponse<Map<String, Object>>> debugValidateSlot(@RequestBody LexhoDtos.SlotValidationDebugRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("requestedDate", request == null ? null : request.date());
        payload.put("requestedSlot", request == null ? null : request.slot());
        payload.put("serverTimezone", slotDateTimePolicy.zoneId().getId());
        payload.put("currentServerTime", slotDateTimePolicy.now().toString());

        if (request == null || request.date() == null || request.date().isBlank()) {
            payload.put("httpStatus", 400);
            payload.put("valid", false);
            payload.put("reasonCode", "INVALID_DATE_FORMAT");
            payload.put("message", "date is required");
            payload.put("slotLookupResult", "NOT_FOUND");
            payload.put("slotRow", null);
            return ResponseEntity.ok(ApiResponse.success("Slot validation debug", payload));
        }

        LocalDate bookingDate;
        try {
            bookingDate = LocalDate.parse(request.date());
        } catch (Exception ex) {
            payload.put("httpStatus", 400);
            payload.put("valid", false);
            payload.put("reasonCode", "INVALID_DATE_FORMAT");
            payload.put("message", "Invalid date format. Use YYYY-MM-DD");
            payload.put("slotLookupResult", "NOT_FOUND");
            payload.put("slotRow", null);
            return ResponseEntity.ok(ApiResponse.success("Slot validation debug", payload));
        }

        Map<String, Object> slotRow = customerCartSlotService.slotRowForDate(bookingDate, request.slot());
        payload.put("slotLookupResult", slotRow == null ? "NOT_FOUND" : "FOUND");
        payload.put("slotRow", slotRow);

        try {
            LocalDateTime parsedServerDateTime = parseScheduledAt(request.date(), request.slot());
            payload.put("httpStatus", 200);
            payload.put("valid", true);
            payload.put("reasonCode", "OK");
            payload.put("message", "Slot valid for booking");
            payload.put("parsedServerDateTime", parsedServerDateTime.toString());
        } catch (BookingValidationException ex) {
            payload.put("httpStatus", 400);
            payload.put("valid", false);
            payload.put("reasonCode", ex.getErrorCode());
            payload.put("message", ex.getMessage());
            payload.put("parsedServerDateTime", null);
        } catch (BadRequestException ex) {
            payload.put("httpStatus", 400);
            payload.put("valid", false);
            payload.put("reasonCode", "BAD_REQUEST");
            payload.put("message", ex.getMessage());
            payload.put("parsedServerDateTime", null);
        }

        log.info("[booking:slot:debug] {}", payload);
        return ResponseEntity.ok(ApiResponse.success("Slot validation debug", payload));
    }

    @GetMapping("/api/bookings/history")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bookingHistory(@RequestAttribute("userId") Long userId,
                                                                           @RequestParam(defaultValue = "0") int page,
                                                                           @RequestParam(defaultValue = "20") int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        List<Order> orders = orderRepository.findByUserId(userId).stream()
                .sorted(Comparator.comparing(Order::getId).reversed())
                .toList();
        List<Map<String, Object>> instantBookings = orders.stream()
                .filter(order -> "INSTANT".equalsIgnoreCase(order.getBookingType()))
                .map(this::mapOrderSummary)
                .toList();
        List<Map<String, Object>> scheduledBookings = orders.stream()
                .filter(order -> "SCHEDULED".equalsIgnoreCase(order.getBookingType())
                        || order.getBookingType() == null
                        || order.getBookingType().isBlank())
                .map(this::mapOrderSummary)
                .toList();
        List<Map<String, Object>> subscriptions = orders.stream()
                .filter(order -> "SUBSCRIPTION".equalsIgnoreCase(order.getBookingType()))
                .map(this::subscriptionSummaryPayload)
                .toList();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("instantBookings", pagedGroup(instantBookings, safePage, safeSize));
        payload.put("scheduledBookings", pagedGroup(scheduledBookings, safePage, safeSize));
        payload.put("subscriptions", pagedGroup(subscriptions, safePage, safeSize));
        return ResponseEntity.ok(ApiResponse.success("Booking history fetched", payload));
    }

    @GetMapping("/api/bookings/details/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bookingDetails(@RequestAttribute("userId") Long userId,
                                                                           @PathVariable Long id) {
        OrderResponseDTO order = orderService.getOrder(id, userId);
        Order persistedOrder = orderRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Booking not found"));
        if (!Objects.equals(persistedOrder.getUserId(), userId)) {
            throw new BadRequestException("You are not allowed to access this booking");
        }
        if ("SUBSCRIPTION".equalsIgnoreCase(persistedOrder.getBookingType())) {
            return ResponseEntity.ok(ApiResponse.success(
                    "Booking details fetched successfully",
                    subscriptionBookingPayload(userId, persistedOrder)
            ));
        }

        double itemTotal = order.getItems() == null ? 0d : order.getItems().stream()
                .mapToDouble(item -> {
                    double price = item.getPrice() == null ? 0d : item.getPrice();
                    int quantity = item.getQuantity() == null || item.getQuantity() <= 0 ? 1 : item.getQuantity();
                    return price * quantity;
                })
                .sum();
        itemTotal = round2(itemTotal);

        double totalAmount = order.getTotalAmount() == null ? itemTotal : round2(order.getTotalAmount());
        double serviceFee = round2(Math.max(0d, totalAmount - itemTotal));
        double gstAmount = 0d;
        double taxAmount = 0d;
        double discountAmount = 0d;
        double payableAmount = round2(itemTotal + serviceFee + gstAmount + taxAmount - discountAmount);

        Map<String, Object> payload = new LinkedHashMap<>();
        String bookingType = persistedOrder.getBookingType() == null || persistedOrder.getBookingType().isBlank()
                ? "SCHEDULED"
                : persistedOrder.getBookingType();
        boolean hasAssignedPartner = order.getItems() != null && !order.getItems().isEmpty();
        String assignmentStatus = deriveAssignmentStatus(persistedOrder, hasAssignedPartner);
        String status = responseStatus(persistedOrder, assignmentStatus);
        String trackingScreen = trackingScreen(persistedOrder, assignmentStatus);
        Long serviceId = order.getItems() == null || order.getItems().isEmpty()
                ? null
                : order.getItems().get(0).getPartnerId();
        payload.put("id", String.valueOf(order.getId()));
        payload.put("bookingId", String.valueOf(order.getId()));
        payload.put("contractVersion", BOOKING_CONTRACT_VERSION);
        payload.put("serviceId", serviceId == null ? "" : String.valueOf(serviceId));
        payload.put("status", status);
        payload.put("assignmentStatus", assignmentStatus);
        payload.put("bookingType", bookingType);
        payload.put("message", bookingMessage(status, assignmentStatus));
        Map<String, Object> partnerPayload = partnerSummary(serviceId, order, assignmentStatus);
        payload.put("partner", partnerPayload);
        payload.put("partnerId", partnerPayload == null ? "" : String.valueOf(partnerPayload.getOrDefault("id", "")));
        payload.put("partnerName", partnerPayload == null ? "" : String.valueOf(partnerPayload.getOrDefault("name", "")));
        payload.put("partnerPhone", partnerPayload == null ? "" : String.valueOf(partnerPayload.getOrDefault("phone", "")));
        payload.put("partnerRating", partnerPayload == null ? 0d : partnerPayload.getOrDefault("rating", 0d));
        int etaMinutes = deriveEtaMinutes(status, assignmentStatus);
        payload.put("etaMinutes", etaMinutes);
        payload.put("estimatedArrivalMinutes", etaMinutes);
        payload.put("estimatedCompletionTime", estimatedCompletionTime(persistedOrder));
        payload.put("slaMinutes", persistedOrder.getSlaMinutes() == null ? 0 : persistedOrder.getSlaMinutes());
        payload.put("promisedStartTime", persistedOrder.getPromisedStartTime() == null ? null : persistedOrder.getPromisedStartTime().toString());
        payload.put("serviceName", order.getItems().isEmpty() ? "Service" : order.getItems().get(0).getPartnerName());
        payload.put("address", order.getAddress());
        payload.put("fullAddress", order.getAddress());
        payload.put("createdAt", utc(persistedOrder.getCreatedAt()));
        payload.put("scheduledAt", utc(persistedOrder.getScheduledAt()));
        boolean isCompleted = "COMPLETED".equalsIgnoreCase(status);
        payload.put("arrivedAt", utc(persistedOrder.getArrivedAt()));
        payload.put("startedAt", utc(persistedOrder.getStartedAt()));
        payload.put("inProgressAt", utc(persistedOrder.getInProgressAt()));
        payload.put("completedAt", utc(isCompleted ? firstNonNull(persistedOrder.getCompletedAt(), persistedOrder.getUpdatedAt()) : persistedOrder.getCompletedAt()));
        payload.put("completionStatus", isCompleted ? "COMPLETED" : "NOT_COMPLETED_YET");
        payload.put("completedAtDisplay", isCompleted ? utc(firstNonNull(persistedOrder.getCompletedAt(), persistedOrder.getUpdatedAt())) : "Not completed yet");
        payload.put("completed", isCompleted ? utc(firstNonNull(persistedOrder.getCompletedAt(), persistedOrder.getUpdatedAt())) : "Not completed yet");
        payload.put("amount", payableAmount);
        payload.put("payableAmount", payableAmount);
        payload.put("currency", "INR");
        payload.put("itemTotal", itemTotal);
        payload.put("serviceFee", serviceFee);
        payload.put("gstAmount", gstAmount);
        payload.put("taxAmount", taxAmount);
        payload.put("discountAmount", discountAmount);
        payload.put("totalAmount", payableAmount);
        payload.put("paymentStatus", order.getPaymentStatus());
        Map<String, Object> trackingPayload = mapTrackingPayload(persistedOrder, assignmentStatus);
        payload.putAll(rootTrackingPayload(trackingPayload));
        int trackingRefreshSeconds = trackingRefreshSeconds(status, assignmentStatus);
        boolean partnerAvailable = Boolean.TRUE.equals(trackingPayload.get("partnerAvailable"));
        Map<String, Object> permissions = actionPermissions(persistedOrder, assignmentStatus, partnerAvailable);
        payload.put("retryPaymentAllowed", retryPaymentAllowed(persistedOrder));
        payload.put("statusHistory", statusHistory(persistedOrder));
        Map<String, Object> meta = metaPayload(trackingScreen, trackingRefreshSeconds, trackingPayload, permissions);
        addReviewMeta(meta, persistedOrder);
        payload.put("meta", meta);
        return ResponseEntity.ok(ApiResponse.success("Booking details fetched successfully", payload));
    }

    @PutMapping("/api/bookings/cancel/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cancelBooking(@RequestAttribute("userId") Long userId,
                                                                          @PathVariable Long id) {
        OrderResponseDTO order = orderService.cancelOrder(id, userId);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", String.valueOf(id));
        payload.put("bookingId", String.valueOf(id));
        payload.put("contractVersion", BOOKING_CONTRACT_VERSION);
        payload.put("status", order.getStatus());
        payload.put("assignmentStatus", "CANCELLED");
        payload.put("meta", metaPayload("INSTANT_STATUS", 0, Map.of("showMap", false), Map.of(
                "canCancel", false,
                "canReschedule", false,
                "canCallPartner", false,
                "canTrackPartner", false,
                "canRaiseIssue", false
        )));
        audit(userId, null, id, "BOOKING_CANCELLED", null, String.valueOf(order.getStatus()));
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled", payload));
    }

    @PutMapping("/api/bookings/reschedule/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> rescheduleBooking(@RequestAttribute("userId") Long userId,
                                                                              @PathVariable Long id,
                                                                              @Valid @RequestBody LexhoDtos.RescheduleRequest request) {
        Order order = orderRepository.findById(id).orElseThrow(() -> new BadRequestException("Booking not found"));
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new BadRequestException("You are not allowed to reschedule this booking");
        }
        LocalDateTime scheduledAt = parseDateTime(request.date());
        if (scheduledAt == null) {
            throw new BadRequestException("date is required");
        }
        String oldValue = order.getScheduledAt() == null ? null : utc(order.getScheduledAt());
        order.setScheduledAt(scheduledAt);
        if (order.getStatus() == OrderStatus.CREATED && order.getStatus().canTransitionTo(OrderStatus.SCHEDULED)) {
            order.setStatus(OrderStatus.SCHEDULED);
        }
        Order saved = orderRepository.save(order);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", String.valueOf(saved.getId()));
        payload.put("bookingId", String.valueOf(saved.getId()));
        payload.put("contractVersion", BOOKING_CONTRACT_VERSION);
        payload.put("status", saved.getStatus() == null ? "SCHEDULED" : saved.getStatus().name());
        payload.put("assignmentStatus", "SCHEDULED");
        payload.put("scheduledAt", utc(saved.getScheduledAt()));
        payload.put("meta", metaPayload("SCHEDULED_STATUS", 15, Map.of("showMap", false), actionPermissions(saved, "SCHEDULED", false)));
        audit(userId, null, saved.getId(), "BOOKING_RESCHEDULED", oldValue, utc(saved.getScheduledAt()));
        return ResponseEntity.ok(ApiResponse.success("Booking rescheduled", payload));
    }

    @PostMapping("/api/bookings/{id}/reschedule")
    public ResponseEntity<ApiResponse<Map<String, Object>>> rescheduleBookingV2(@RequestAttribute("userId") Long userId,
                                                                                @PathVariable Long id,
                                                                                @Valid @RequestBody LexhoDtos.RescheduleSlotRequest request) {
        Order order = orderRepository.findById(id).orElseThrow(() -> new BadRequestException("Booking not found"));
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new BadRequestException("You are not allowed to reschedule this booking");
        }
        boolean allowed = canReschedule(order);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("allowed", allowed);
        payload.put("remainingReschedules", allowed ? 2 : 0);
        payload.put("bookingId", id);
        payload.put("contractVersion", BOOKING_CONTRACT_VERSION);
        if (!allowed) {
            payload.put("meta", metaPayload("SCHEDULED_STATUS", 15, Map.of("showMap", false), actionPermissions(order, "SCHEDULED", false)));
            return ResponseEntity.ok(ApiResponse.success("Booking cannot be rescheduled in current state", payload));
        }
        String oldValue = order.getScheduledAt() == null ? null : utc(order.getScheduledAt());
        LocalDateTime scheduledAt = parseScheduledAt(request.date(), request.slot());
        order.setScheduledAt(scheduledAt);
        Order saved = orderRepository.save(order);
        payload.put("scheduledAt", utc(scheduledAt));
        payload.put("meta", metaPayload("SCHEDULED_STATUS", 15, Map.of("showMap", false), actionPermissions(saved, "SCHEDULED", false)));
        audit(userId, null, saved.getId(), "BOOKING_RESCHEDULED", oldValue, utc(saved.getScheduledAt()));
        return ResponseEntity.ok(ApiResponse.success("Booking rescheduled", payload));
    }

    @GetMapping("/api/slots/available")
    public ResponseEntity<List<Map<String, Object>>> availableSlots(@RequestParam String date) {
        LocalDate bookingDate;
        try {
            bookingDate = LocalDate.parse(date);
        } catch (Exception ex) {
            throw new BadRequestException("Invalid date format. Use YYYY-MM-DD");
        }
        Map<String, Object> slotResponse = customerCartSlotService.slotsForDate(bookingDate);
        Object rawRows = slotResponse.get("slots");
        List<?> rows = rawRows instanceof List<?> list ? list : List.of();
        List<Map<String, Object>> payload = rows.stream().map(raw -> {
            if (!(raw instanceof Map<?, ?> map)) {
                return Map.<String, Object>of(
                        "slot", "",
                        "available", false,
                        "etaMinutes", 0,
                        "instantEligible", false
                );
            }
            Map<String, Object> item = new LinkedHashMap<>();
            Object slotValue = map.containsKey("slot") ? map.get("slot") : "";
            item.put("slot", String.valueOf(slotValue));
            item.put("available", Boolean.TRUE.equals(map.get("available")));
            Object etaMinutes = map.get("etaMinutes");
            item.put("etaMinutes", etaMinutes instanceof Number ? ((Number) etaMinutes).intValue() : 0);
            item.put("instantEligible", Boolean.TRUE.equals(map.get("instantEligible"))
                    || Boolean.TRUE.equals(map.get("isInstantEligible")));
            return item;
        }).toList();
        return ResponseEntity.ok(payload);
    }

    @GetMapping("/api/partner/live-location/{bookingId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> liveLocation(@RequestAttribute("userId") Long userId,
                                                                         @PathVariable Long bookingId) {
        Order order = orderRepository.findById(bookingId).orElseThrow(() -> new BadRequestException("Booking not found"));
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new BadRequestException("You are not allowed to access this booking");
        }
        return ResponseEntity.ok(ApiResponse.success("Partner live location", Map.of("lat", 0.0, "lng", 0.0, "eta", "N/A")));
    }

    @PutMapping("/api/partner/update-location")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateLocation(@RequestAttribute("userId") Long partnerId,
                                                                           @RequestParam Long bookingId,
                                                                           @RequestParam Double latitude,
                                                                           @RequestParam Double longitude) {
        Order order = orderRepository.findById(bookingId)
                .orElseThrow(() -> new BadRequestException("Booking not found"));
        if (!Objects.equals(order.getAssignedPartnerId(), partnerId)) {
            throw new BadRequestException("You are not allowed to update this booking location");
        }
        validateCoordinates(latitude, longitude);

        CurrentPartnerLocationEntity location = currentPartnerLocationRepository.findByPartnerIdAndDeletedFalse(partnerId)
                .orElseGet(() -> {
                    CurrentPartnerLocationEntity created = new CurrentPartnerLocationEntity();
                    created.setPartnerId(partnerId);
                    created.setCreatedBy("partner:" + partnerId);
                    created.setLocationSeq(0L);
                    return created;
                });
        location.setLatitude(BigDecimal.valueOf(latitude));
        location.setLongitude(BigDecimal.valueOf(longitude));
        location.setSource("GPS");
        location.setDeleted(false);
        location.setLastHeartbeatAt(LocalDateTime.now());
        location.setLocationSeq(location.getLocationSeq() + 1);
        location.setUpdatedBy("partner:" + partnerId);
        currentPartnerLocationRepository.save(location);

        return ResponseEntity.ok(ApiResponse.success("Partner location updated", Map.of("bookingId", bookingId, "lat", latitude, "lng", longitude)));
    }

    @GetMapping("/api/payments/legacy-options")
    public ResponseEntity<ApiResponse<Map<String, Object>>> paymentOptions(@RequestParam(required = false) String bookingId,
                                                                           @RequestParam(required = false) Long cartId,
                                                                           @RequestParam(required = false) java.math.BigDecimal amount) {
        return ResponseEntity.ok(ApiResponse.success("Payment options fetched", paymentOptionsPayload(bookingId, cartId, amount)));
    }

    @PostMapping("/api/payments/options")
    public ResponseEntity<ApiResponse<Map<String, Object>>> paymentOptionsPost(@RequestBody(required = false) LexhoDtos.PaymentOptionsRequest request) {
        String bookingId = request == null ? null : request.bookingId();
        Long cartId = request == null ? null : request.cartId();
        java.math.BigDecimal amount = request == null ? null : request.amount();
        return ResponseEntity.ok(ApiResponse.success("Payment options fetched", paymentOptionsPayload(bookingId, cartId, amount)));
    }

    @PostMapping("/api/payments/initiate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> initiatePayment(@RequestAttribute("userId") Long userId,
                                                                            @Valid @RequestBody LexhoDtos.PaymentInitiateRequest request) {
        Long resolvedBookingId = resolveBookingId(request.bookingId());
        if (resolvedBookingId == null) {
            throw new BadRequestException("bookingId is required");
        }
        Order order = orderRepository.findById(resolvedBookingId).orElseThrow(() -> new BadRequestException("Booking not found"));
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new BadRequestException("You are not allowed to initiate payment for this booking");
        }
        String mode = normalizeMode(request.mode());
        PaymentStatus status = "COD".equalsIgnoreCase(mode) ? PaymentStatus.PENDING : PaymentStatus.SUCCESS;
        order.setPaymentStatus(status);
        orderRepository.save(order);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("bookingId", String.valueOf(order.getId()));
        data.put("mode", mode);
        data.put("amount", request.amount() != null ? request.amount() : order.getTotalAmount());
        data.put("status", status.name());
        data.put("message", status == PaymentStatus.SUCCESS ? "Payment completed successfully" : "Cash on delivery selected");
        if ("UPI".equalsIgnoreCase(mode) || "CARD".equalsIgnoreCase(mode)) {
            Map<String, Object> razorpayOptions = new LinkedHashMap<>();
            razorpayOptions.put("amount", amountInPaise(request.amount(), order.getTotalAmount()));
            razorpayOptions.put("currency", "INR");
            razorpayOptions.put("name", "Lexho");
            razorpayOptions.put("description", "Service payment");
            razorpayOptions.put("theme", Map.of("color", "#16A34A"));
            data.put("razorpayOptions", razorpayOptions);
        }
        return ResponseEntity.ok(ApiResponse.success("Payment initiated", data));
    }

    @GetMapping("/api/payments/status/{bookingId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> paymentStatus(@RequestAttribute("userId") Long userId,
                                                                          @PathVariable String bookingId) {
        Long resolvedBookingId = resolveBookingId(bookingId);
        if (resolvedBookingId == null) {
            throw new BadRequestException("Invalid bookingId");
        }
        Order order = orderRepository.findById(resolvedBookingId).orElseThrow(() -> new BadRequestException("Booking not found"));
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new BadRequestException("You are not allowed to access this booking");
        }
        PaymentStatus status = order.getPaymentStatus() == null ? PaymentStatus.PENDING : order.getPaymentStatus();
        return ResponseEntity.ok(ApiResponse.success("Payment status fetched", Map.of(
                "bookingId", String.valueOf(resolvedBookingId),
                "status", status.name(),
                "message", status == PaymentStatus.SUCCESS ? "Payment successful" : status == PaymentStatus.FAILED ? "Payment failed" : "Payment pending"
        )));
    }

    @PostMapping("/api/payments/refund")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refund(@RequestAttribute("userId") Long userId,
                                                                   @Valid @RequestBody LexhoDtos.PaymentRefundRequest request) {
        Long resolvedBookingId = resolveBookingId(request.bookingId());
        if (resolvedBookingId == null) {
            throw new BadRequestException("Invalid bookingId");
        }
        Order order = orderRepository.findById(resolvedBookingId).orElseThrow(() -> new BadRequestException("Booking not found"));
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new BadRequestException("You are not allowed to refund this booking");
        }
        order.setPaymentStatus(PaymentStatus.FAILED);
        orderRepository.save(order);
        return ResponseEntity.ok(ApiResponse.success("Refund requested", Map.of(
                "bookingId", String.valueOf(resolvedBookingId),
                "amount", request.amount(),
                "status", "INITIATED"
        )));
    }

    @PostMapping("/api/coupon/apply")
    public ResponseEntity<ApiResponse<Map<String, Object>>> applyCoupon(@Valid @RequestBody LexhoDtos.CouponApplyRequest request) {
        BannerPromotion coupon = bannerPromotionRepository.findByStatusAndDeletedFalse(BannerStatus.ACTIVE).stream()
                .filter(item -> item.getCouponCode() != null)
                .filter(item -> item.getCouponCode().equalsIgnoreCase(request.code()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Invalid coupon code"));
        return ResponseEntity.ok(ApiResponse.success("Coupon applied", Map.of(
                "code", coupon.getCouponCode(),
                "discount", coupon.getDiscountPercentage() == null ? 0 : coupon.getDiscountPercentage()
        )));
    }

    @GetMapping("/api/coupon/list")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listCoupons(@RequestParam(defaultValue = "0") int page,
                                                                        @RequestParam(defaultValue = "20") int size) {
        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> data = bannerPromotionRepository.findByStatusAndDeletedFalse(BannerStatus.ACTIVE).stream()
                .filter(item -> item.getCouponCode() != null && !item.getCouponCode().isBlank())
                .filter(item -> item.getEndDate() == null || item.getEndDate().isAfter(now))
                .skip((long) page * size)
                .limit(size)
                .map(item -> Map.<String, Object>of(
                        "id", String.valueOf(item.getId()),
                        "code", item.getCouponCode(),
                        "description", item.getDescription() == null ? "" : item.getDescription(),
                        "discount", item.getDiscountPercentage() == null ? 0 : item.getDiscountPercentage()
                ))
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Coupons fetched", Map.of("data", data)));
    }

    @PostMapping("/api/reviews/add")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addReview(@RequestAttribute("userId") Long userId,
                                                                      @Valid @RequestBody LexhoDtos.ReviewAddRequest request) {
        Long resolvedServiceId = resolveBookingId(request.serviceId());
        if (resolvedServiceId == null) {
            throw new BadRequestException("Invalid serviceId");
        }
        ReviewEntity review = new ReviewEntity();
        review.setCustomerId(userId);
        review.setServiceId(resolvedServiceId);
        review.setRating(request.rating());
        review.setReviewComment(request.comment());
        review.setReviewImages(request.imageUrls() == null ? null : String.join(",", request.imageUrls()));
        review.setStatus(ReviewStatus.APPROVED);
        review.setDeleted(false);
        review.setReportCount(0L);
        ReviewEntity saved = reviewRepository.save(review);
        return ResponseEntity.ok(ApiResponse.success("Review added", Map.of(
                "id", String.valueOf(saved.getId()),
                "serviceId", String.valueOf(saved.getServiceId()),
                "rating", saved.getRating(),
                "comment", saved.getReviewComment()
        )));
    }

    @GetMapping("/api/reviews/service/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> serviceReviews(@PathVariable Long id,
                                                                           @RequestParam(defaultValue = "0") int page,
                                                                           @RequestParam(defaultValue = "20") int size) {
        List<Map<String, Object>> data = reviewRepository.findByServiceIdAndDeletedFalseOrderByCreatedAtDesc(id).stream()
                .skip((long) page * size)
                .limit(size)
                .map(review -> Map.<String, Object>of(
                        "id", String.valueOf(review.getId()),
                        "rating", review.getRating(),
                        "comment", review.getReviewComment() == null ? "" : review.getReviewComment(),
                        "serviceName", "Service-" + id,
                        "createdAt", String.valueOf(review.getCreatedAt())
                ))
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Service reviews", Map.of("data", data)));
    }

    @PostMapping("/api/favorites/add")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addFavorite(@RequestAttribute("userId") Long userId,
                                                                        @Valid @RequestBody LexhoDtos.FavoriteAddRequest request) {
        if (!favoriteRepository.existsByUserIdAndServiceId(userId, request.serviceId())) {
            LexhoFavorite favorite = new LexhoFavorite();
            favorite.setUserId(userId);
            favorite.setServiceId(request.serviceId());
            favoriteRepository.save(favorite);
        }
        return ResponseEntity.ok(ApiResponse.success("Favorite added", Map.of("serviceId", String.valueOf(request.serviceId()))));
    }

    @GetMapping("/api/favorites/list")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listFavorites(@RequestAttribute("userId") Long userId,
                                                                          @RequestParam(defaultValue = "0") int page,
                                                                          @RequestParam(defaultValue = "20") int size) {
        List<Map<String, Object>> data = favoriteRepository.findByUserIdOrderByIdDesc(userId).stream()
                .skip((long) page * size)
                .limit(size)
                .map(item -> {
                    Partner partner = partnerRepository.findById(item.getServiceId()).orElse(null);
                    return Map.<String, Object>of(
                            "id", String.valueOf(item.getId()),
                            "serviceId", String.valueOf(item.getServiceId()),
                            "name", partner == null ? "" : partner.getName()
                    );
                })
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Favorites fetched", Map.of("data", data)));
    }

    @DeleteMapping("/api/favorites/remove")
    public ResponseEntity<ApiResponse<Void>> removeFavorite(@RequestAttribute("userId") Long userId,
                                                            @Valid @RequestBody LexhoDtos.IdRequest request) {
        LexhoFavorite favorite = favoriteRepository.findByIdAndUserId(request.id(), userId)
                .orElseThrow(() -> new BadRequestException("Favorite not found"));
        favoriteRepository.delete(favorite);
        return ResponseEntity.ok(ApiResponse.success("Favorite removed", null));
    }

    @GetMapping("/api/notifications")
    public ResponseEntity<ApiResponse<Map<String, Object>>> notifications(@RequestAttribute("userId") Long userId,
                                                                          @RequestParam(defaultValue = "0") int page,
                                                                          @RequestParam(defaultValue = "20") int size) {
        NotificationPageResponse<NotificationResponse> notifications =
                notificationManagementService.getCustomerNotifications(page, size);
        List<Map<String, Object>> data = notifications.getContent().stream()
                .map(this::toNotificationMap)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Notifications fetched successfully", Map.of(
                "data", data,
                "totalElements", notifications.getTotalElements(),
                "totalPages", notifications.getTotalPages(),
                "pageNumber", notifications.getPageNumber()
        )));
    }

    @PutMapping("/api/notifications/read")
    public ResponseEntity<ApiResponse<Void>> markNotificationRead(@RequestAttribute("userId") Long userId,
                                                                  @Valid @RequestBody LexhoDtos.IdRequest request) {
        notificationManagementService.markAsReadForCurrentUser(request.id());
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", null));
    }

    @DeleteMapping("/api/notifications/read")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(@RequestAttribute("userId") Long userId,
                                                                @Valid @RequestBody LexhoDtos.IdRequest request) {
        notificationManagementService.softDeleteForCurrentUser(request.id());
        return ResponseEntity.ok(ApiResponse.success("Notification deleted", null));
    }

    @GetMapping("/api/customer/notifications/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Object>>> unreadCount(@RequestAttribute("userId") Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Unread count", Map.of(
                "count", notificationManagementService.unreadCountForCurrentUser()
        )));
    }

    @GetMapping("/api/customer/notifications/stream")
    public SseEmitter customerNotificationStream(@RequestAttribute("userId") Long userId) {
        if (notificationRealtimeService == null) {
            throw new BadRequestException("Notification realtime stream is not available");
        }
        return notificationRealtimeService.subscribe(userId);
    }

    @GetMapping("/api/customer/inbox/activity")
    public ResponseEntity<Map<String, Object>> customerActivityFeed(@RequestAttribute("userId") Long userId,
                                                                    @RequestParam(defaultValue = "1") int page,
                                                                    @RequestParam(defaultValue = "20") int limit) {
        if (customerActivityFeedService == null) {
            throw new BadRequestException("Customer activity feed is not available");
        }
        Map<String, Object> payload = customerActivityFeedService.activity(userId, page, limit);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Customer activity feed",
                "data", payload.get("data"),
                "meta", payload.get("meta")
        ));
    }

    @PostMapping("/api/referral/generate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateReferral(@RequestAttribute("userId") Long userId) {
        LexhoReferralCode code = referralCodeRepository.findByUserId(userId).orElseGet(() -> {
            LexhoReferralCode created = new LexhoReferralCode();
            created.setUserId(userId);
            created.setCode(generateUniqueReferralCode(userId));
            created.setUsedCount(0);
            return referralCodeRepository.save(created);
        });
        return ResponseEntity.ok(ApiResponse.success("Referral generated", Map.of("code", code.getCode())));
    }

    @PostMapping("/api/referral/apply")
    public ResponseEntity<ApiResponse<Map<String, Object>>> applyReferral(@RequestAttribute("userId") Long userId,
                                                                          @Valid @RequestBody LexhoDtos.ReferralApplyRequest request) {
        if (referralUseRepository.existsByUserIdAndCodeIgnoreCase(userId, request.code())) {
            throw new BadRequestException("Referral code already applied");
        }
        LexhoReferralCode referralCode = referralCodeRepository.findByCodeIgnoreCase(request.code())
                .orElseThrow(() -> new BadRequestException("Invalid referral code"));
        if (Objects.equals(referralCode.getUserId(), userId)) {
            throw new BadRequestException("You cannot apply your own referral code");
        }
        LexhoReferralUse use = new LexhoReferralUse();
        use.setUserId(userId);
        use.setCode(referralCode.getCode());
        referralUseRepository.save(use);
        referralCode.setUsedCount(referralCode.getUsedCount() + 1);
        referralCodeRepository.save(referralCode);
        return ResponseEntity.ok(ApiResponse.success("Referral applied", Map.of("code", request.code())));
    }

    @GetMapping("/api/support/faqs")
    public ResponseEntity<Map<String, Object>> supportFaqs() {
        List<Map<String, Object>> data = supportFaqCategoryRepository.findByActiveTrueOrderByDisplayOrderAscIdAsc()
                .stream()
                .map(category -> {
                    List<Map<String, Object>> faqs = supportFaqRepository
                            .findByCategoryAndActiveTrueOrderByDisplayOrderAscIdAsc(category)
                            .stream()
                            .map(this::mapFaqRow)
                            .toList();
                    return Map.of(
                            "id", category.getCategoryKey(),
                            "label", category.getLabel(),
                            "displayOrder", category.getDisplayOrder(),
                            "faqs", faqs
                    );
                })
                .toList();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "FAQs fetched",
                "data", data
        ));
    }

    @PostMapping("/api/support/create")
    public ResponseEntity<Map<String, Object>> createTicket(@RequestAttribute("userId") Long userId,
                                                            @Valid @RequestBody LexhoDtos.SupportCreateRequest request) {
        String normalizedBookingId = normalizeSupportBookingId(userId, request.bookingId());
        SupportTicket ticket = new SupportTicket();
        ticket.setUserId(userId);
        ticket.setSubject(request.subject());
        ticket.setMessage(request.message());
        ticket.setStatus("OPEN");
        ticket.setCategoryId(blankToNull(request.categoryId()));
        ticket.setBookingId(normalizedBookingId);
        ticket.setPriority(request.priority() == null || request.priority().isBlank() ? "MEDIUM" : request.priority().trim().toUpperCase(Locale.ROOT));
        ticket.setAttachmentUrls(serializeAttachmentUrls(request.attachmentUrls()));
        SupportTicket saved = supportTicketRepository.save(ticket);
        publishSupportEvent(saved, "ticket-created", Map.of(
                "id", formatTicketId(saved.getId()),
                "status", saved.getStatus(),
                "updatedAt", String.valueOf(saved.getUpdatedAt())
        ));
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Ticket created",
                "data", Map.of(
                "id", formatTicketId(saved.getId()),
                "subject", saved.getSubject(),
                "message", saved.getMessage(),
                "status", saved.getStatus(),
                "categoryId", saved.getCategoryId() == null ? "" : saved.getCategoryId(),
                "bookingId", saved.getBookingId() == null ? "" : saved.getBookingId(),
                "priority", saved.getPriority() == null ? "MEDIUM" : saved.getPriority(),
                "attachmentUrls", deserializeAttachmentUrls(saved.getAttachmentUrls()),
                "createdAt", String.valueOf(saved.getCreatedAt())
        )));
    }

    @PostMapping("/api/support/tickets/{ticketId}/reply")
    public ResponseEntity<Map<String, Object>> replySupportTicket(@RequestAttribute("userId") Long userId,
                                                                  @PathVariable String ticketId,
                                                                  @Valid @RequestBody LexhoDtos.SupportReplyRequest request) {
        SupportTicket ticket = requireCustomerTicket(userId, ticketId);
        if ("CLOSED".equalsIgnoreCase(ticket.getStatus())) {
            throw new BadRequestException("Closed tickets cannot be replied to. Reopen the ticket first.");
        }
        SupportTicketReply reply = new SupportTicketReply();
        reply.setTicket(ticket);
        reply.setAdminId(null);
        reply.setSenderType("CUSTOMER");
        reply.setMessage(request.message().trim());
        reply.setAttachmentUrls(serializeAttachmentUrls(request.attachmentUrls()));
        supportTicketReplyRepository.save(reply);
        if ("RESOLVED".equalsIgnoreCase(ticket.getStatus())) {
            ticket.setStatus("OPEN");
            supportTicketRepository.save(ticket);
        }
        publishSupportEvent(ticket, "ticket-message", Map.of(
                "ticketId", formatTicketId(ticket.getId()),
                "status", ticket.getStatus(),
                "message", Map.of(
                        "sender", "CUSTOMER",
                        "text", reply.getMessage(),
                        "attachmentUrls", deserializeAttachmentUrls(reply.getAttachmentUrls()),
                        "createdAt", String.valueOf(reply.getCreatedAt())
                )
        ));
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Reply added",
                "data", Map.of(
                        "ticketId", formatTicketId(ticket.getId()),
                        "status", ticket.getStatus(),
                        "message", Map.of(
                                "sender", "CUSTOMER",
                                "text", reply.getMessage(),
                                "attachmentUrls", deserializeAttachmentUrls(reply.getAttachmentUrls()),
                                "createdAt", String.valueOf(reply.getCreatedAt())
                        )
                )
        ));
    }

    @PatchMapping("/api/support/tickets/{ticketId}/status")
    public ResponseEntity<Map<String, Object>> updateSupportTicketStatus(@RequestAttribute("userId") Long userId,
                                                                         @PathVariable String ticketId,
                                                                         @Valid @RequestBody LexhoDtos.SupportStatusUpdateRequest request) {
        SupportTicket ticket = requireCustomerTicket(userId, ticketId);
        String normalized = request.status().trim().toUpperCase(Locale.ROOT);
        if ("CLOSED".equals(normalized)) {
            if ("CLOSED".equalsIgnoreCase(ticket.getStatus())) {
                throw new BadRequestException("Ticket is already closed");
            }
            ticket.setStatus("CLOSED");
        } else {
            if (!Set.of("CLOSED", "RESOLVED").contains(ticket.getStatus() == null ? "" : ticket.getStatus().toUpperCase(Locale.ROOT))) {
              throw new BadRequestException("Only closed or resolved tickets can be reopened");
            }
            ticket.setStatus("OPEN");
        }
        SupportTicket saved = supportTicketRepository.save(ticket);
        publishSupportEvent(saved, "ticket-status", Map.of(
                "id", formatTicketId(saved.getId()),
                "status", saved.getStatus(),
                "updatedAt", String.valueOf(saved.getUpdatedAt())
        ));
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Ticket status updated",
                "data", Map.of(
                        "id", formatTicketId(saved.getId()),
                        "status", saved.getStatus(),
                        "updatedAt", String.valueOf(saved.getUpdatedAt())
                )
        ));
    }

    @GetMapping("/api/support/history")
    public ResponseEntity<Map<String, Object>> supportHistory(@RequestAttribute("userId") Long userId,
                                                              @RequestParam(required = false) String status,
                                                              @RequestParam(defaultValue = "1") int page,
                                                              @RequestParam(defaultValue = "20") int limit) {
        int safePage = Math.max(page, 1);
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        Pageable pageable = PageRequest.of(safePage - 1, safeLimit);
        Page<SupportTicket> ticketPage = (status == null || status.isBlank())
                ? supportTicketRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                : supportTicketRepository.findByUserIdAndStatusIgnoreCaseOrderByCreatedAtDesc(userId, status.trim(), pageable);

        List<Map<String, Object>> data = ticketPage.getContent().stream()
                .map(item -> Map.<String, Object>of(
                        "id", formatTicketId(item.getId()),
                        "subject", item.getSubject(),
                        "message", item.getMessage(),
                        "status", item.getStatus(),
                        "categoryId", item.getCategoryId() == null ? "" : item.getCategoryId(),
                        "bookingId", item.getBookingId() == null ? "" : item.getBookingId(),
                        "priority", item.getPriority() == null ? "MEDIUM" : item.getPriority(),
                        "attachmentUrls", deserializeAttachmentUrls(item.getAttachmentUrls()),
                        "createdAt", String.valueOf(item.getCreatedAt()),
                        "updatedAt", String.valueOf(item.getUpdatedAt())
                ))
                .toList();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Ticket history",
                "data", data,
                "meta", Map.of(
                        "page", safePage,
                        "limit", safeLimit,
                        "total", ticketPage.getTotalElements()
                )
        ));
    }

    @GetMapping("/api/support/tickets/{ticketId}")
    public ResponseEntity<Map<String, Object>> supportTicketDetail(@RequestAttribute("userId") Long userId,
                                                                   @PathVariable String ticketId) {
        Long parsedId = parseTicketId(ticketId);
        SupportTicket ticket = supportTicketRepository.findByUserIdAndId(userId, parsedId)
                .orElseThrow(() -> new BadRequestException("Ticket not found"));
        List<SupportTicketReply> replies = supportTicketReplyRepository.findByTicket_IdOrderByCreatedAtAsc(ticket.getId());

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of(
                "id", "MSG-1",
                "sender", "CUSTOMER",
                "message", ticket.getMessage(),
                "attachmentUrls", deserializeAttachmentUrls(ticket.getAttachmentUrls()),
                "createdAt", String.valueOf(ticket.getCreatedAt())
        ));
        for (int i = 0; i < replies.size(); i++) {
            SupportTicketReply reply = replies.get(i);
            messages.add(Map.of(
                    "id", "MSG-" + (i + 2),
                    "sender", supportSender(reply),
                    "message", reply.getMessage(),
                    "attachmentUrls", deserializeAttachmentUrls(reply.getAttachmentUrls()),
                    "createdAt", String.valueOf(reply.getCreatedAt())
            ));
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                        "id", formatTicketId(ticket.getId()),
                        "subject", ticket.getSubject(),
                        "status", ticket.getStatus(),
                        "bookingId", ticket.getBookingId() == null ? "" : ticket.getBookingId(),
                        "priority", ticket.getPriority() == null ? "MEDIUM" : ticket.getPriority(),
                        "messages", messages
                )
        ));
    }

    @GetMapping("/api/support/tickets/{ticketId}/stream")
    public SseEmitter supportTicketStream(@RequestAttribute("userId") Long userId,
                                          @PathVariable String ticketId) {
        SupportTicket ticket = requireCustomerTicket(userId, ticketId);
        if (supportRealtimeService == null) {
            throw new BadRequestException("Support realtime stream is not available");
        }
        return supportRealtimeService.subscribe("CUSTOMER", userId, ticket.getId());
    }

    private List<Map<String, Object>> partnerData(int page,
                                                  int size,
                                                  String keyword,
                                                  Long userId,
                                                  Double lat,
                                                  Double lng,
                                                  String pincode) {
        if (!isGeoServiceable(userId, lat, lng, pincode)) {
            return List.of();
        }
        String normalizedKeyword = keyword == null ? null : keyword.trim().toLowerCase(Locale.ROOT);
        return partnerRepository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getAvailable()))
                .filter(p -> normalizedKeyword == null || normalizedKeyword.isBlank() ||
                        (p.getName() != null && p.getName().toLowerCase(Locale.ROOT).contains(normalizedKeyword)))
                .sorted(Comparator.comparing(Partner::getRating, Comparator.nullsLast(Comparator.reverseOrder())))
                .skip((long) page * size)
                .limit(size)
                .map(this::mapPartner)
                .toList();
    }

    private boolean isGeoServiceable(Long userId, Double lat, Double lng, String pincode) {
        ServiceabilityCheckResponse resolution;
        if (lat != null || lng != null || (pincode != null && !pincode.isBlank())) {
            resolution = serviceabilityService.resolve(pincode, lat, lng);
        } else {
            resolution = serviceabilityService.resolveForCustomer(userId);
            if ("MISSING_CUSTOMER_CONTEXT".equals(resolution.getReason())
                    || "CURRENT_LOCATION_NOT_FOUND".equals(resolution.getReason())) {
                return true;
            }
        }
        return resolution.isServiceable();
    }

    private Map<String, Object> mapPartner(Partner p) {
        return Map.of(
                "id", String.valueOf(p.getId()),
                "name", p.getName() == null ? "" : p.getName(),
                "categoryId", p.getCategoryId() == null ? "" : String.valueOf(p.getCategoryId()),
                "description", "",
                "price", p.getPrice() == null ? 0 : p.getPrice(),
                "durationMinutes", 60,
                "imageUrl", p.getImage() == null ? "" : p.getImage()
        );
    }

    private Map<String, Object> mapCategory(ServiceCategory category) {
        return Map.of(
                "id", String.valueOf(category.getId()),
                "name", category.getName() == null ? "" : category.getName(),
                "icon", category.getIcon() == null ? "" : category.getIcon(),
                "imageUrl", ""
        );
    }

    private Map<String, Object> mapCategory(AdminCategoryEntity category) {
        String imageUrl = mediaUrlResolver.toPublicUrl(category.getIconUrl());
        String safeImageUrl = imageUrl == null ? "" : imageUrl;
        return Map.of(
                "id", String.valueOf(category.getId()),
                "name", category.getName() == null ? "" : category.getName(),
                "icon", safeImageUrl,
                "imageUrl", safeImageUrl,
                "displayOrder", category.getDisplayOrder()
        );
    }

    private Map<String, Object> mapBanner(BannerPromotion banner) {
        String imageUrl = mediaUrlResolver.toPublicUrl(banner.getBannerImageUrl());
        String safeImageUrl = imageUrl == null ? "" : imageUrl;
        return Map.of(
                "id", String.valueOf(banner.getId()),
                "title", banner.getTitle() == null ? "" : banner.getTitle(),
                "imageUrl", safeImageUrl,
                "placement", banner.getPlacement() == null ? "TOP" : banner.getPlacement().name(),
                "slotNumber", banner.getSlotNumber() == null ? 0 : banner.getSlotNumber(),
                "redirectUrl", "",
                "promoCode", banner.getCouponCode() == null ? "" : banner.getCouponCode()
        );
    }

    private Map<String, Object> mapCartItem(CartResponseDTO item) {
        return Map.of(
                "id", String.valueOf(item.getId()),
                "serviceId", String.valueOf(item.getPartnerId()),
                "name", item.getPartnerName() == null ? "" : item.getPartnerName(),
                "imageUrl", item.getImageUrl() == null ? "" : item.getImageUrl(),
                "quantity", item.getQuantity(),
                "price", item.getPrice() == null ? 0 : item.getPrice()
        );
    }

    private Map<String, Object> mapOrderSummary(Order order) {
        if ("SUBSCRIPTION".equalsIgnoreCase(order.getBookingType())) {
            return subscriptionSummaryPayload(order);
        }
        // Keep history resilient even when order items are lazily unavailable.
        String serviceName = "Service";
        String assignmentStatus = deriveAssignmentStatus(order, order.getAssignedPartnerId() != null);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", String.valueOf(order.getId()));
        payload.put("bookingId", String.valueOf(order.getId()));
        payload.put("contractVersion", BOOKING_CONTRACT_VERSION);
        payload.put("status", responseStatus(order, assignmentStatus));
        payload.put("assignmentStatus", assignmentStatus);
        payload.put("bookingType", order.getBookingType());
        payload.put("serviceName", serviceName);
        payload.put("createdAt", utc(order.getCreatedAt()));
        Map<String, Object> trackingPayload = mapTrackingPayload(order, assignmentStatus);
        Map<String, Object> permissions = actionPermissions(order, assignmentStatus, Boolean.TRUE.equals(trackingPayload.get("partnerAvailable")));
        Map<String, Object> meta = metaPayload(
                trackingScreen(order, assignmentStatus),
                trackingRefreshSeconds(responseStatus(order, assignmentStatus), assignmentStatus),
                trackingPayload,
                permissions
        );
        addReviewMeta(meta, order);
        payload.put("meta", meta);
        return payload;
    }

    private Map<String, Object> pagedGroup(List<Map<String, Object>> items, int page, int size) {
        int fromIndex = Math.min((page - 1) * size, items.size());
        int toIndex = Math.min(fromIndex + size, items.size());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("items", items.subList(fromIndex, toIndex));
        payload.put("page", page);
        payload.put("size", size);
        payload.put("hasMore", toIndex < items.size());
        return payload;
    }

    private Map<String, Object> subscriptionBookingPayload(Long userId, Order order) {
        SubscriptionMaster subscription = subscriptionMasterRepository
                .findByBookingIdAndCustomerId(order.getId(), userId)
                .orElseThrow(() -> new BadRequestException("Subscription not found"));
        List<String> frequencyDays = subscriptionFrequencyRepository.findBySubscriptionIdOrderByIdAsc(subscription.getId())
                .stream()
                .map(SubscriptionFrequency::getDayName)
                .toList();
        List<SubscriptionVisit> visits = subscriptionVisitRepository.findBySubscriptionIdOrderByVisitDateAscIdAsc(subscription.getId());
        LocalDate nextServiceDate = visits.stream()
                .filter(visit -> !visit.getVisitDate().isBefore(LocalDate.now()))
                .map(SubscriptionVisit::getVisitDate)
                .findFirst()
                .orElse(subscription.getStartDate());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", subscription.getId());
        payload.put("bookingId", order.getId());
        payload.put("contractVersion", BOOKING_CONTRACT_VERSION);
        payload.put("bookingType", "SUBSCRIPTION");
        payload.put("subscriptionId", subscription.getId());
        payload.put("subscriptionServiceType", subscription.getServiceType());
        payload.put("frequencyLabel", frequencyLabel(frequencyDays));
        payload.put("frequencyDays", frequencyDays);
        payload.put("startDate", subscription.getStartDate());
        payload.put("endDate", subscription.getEndDate());
        payload.put("visits", subscription.getVisits());
        payload.put("timeSlot", subscription.getTimeSlot());
        payload.put("fullAddress", order.getAddress());
        payload.put("totalAmount", subscription.getPrice());
        payload.put("nextServiceDate", nextServiceDate);
        payload.put("status", subscription.getStatus().name());
        payload.put("paymentStatus", order.getPaymentStatus() == null ? "PENDING" : order.getPaymentStatus().name());
        payload.put("map", Map.of("enabled", false, "reason", "SUBSCRIPTION_DETAILS"));
        payload.put("partnerAvailable", false);
        payload.put("retryPaymentAllowed", retryPaymentAllowed(order));
        payload.put("statusHistory", statusHistory(order));
        payload.put("meta", subscriptionBookingMeta());
        return payload;
    }

    private Map<String, Object> subscriptionSummaryPayload(Order order) {
        SubscriptionMaster subscription = subscriptionMasterRepository.findByBookingId(order.getId()).orElse(null);
        if (subscription == null) {
            return Map.of(
                    "id", String.valueOf(order.getId()),
                    "bookingId", String.valueOf(order.getId()),
                    "contractVersion", BOOKING_CONTRACT_VERSION,
                    "bookingType", "SUBSCRIPTION",
                    "status", order.getStatus() == null ? "CONFIRMED" : order.getStatus().name(),
                    "createdAt", utc(order.getCreatedAt()),
                    "meta", subscriptionBookingMeta()
            );
        }
        List<String> frequencyDays = subscriptionFrequencyRepository.findBySubscriptionIdOrderByIdAsc(subscription.getId())
                .stream()
                .map(SubscriptionFrequency::getDayName)
                .toList();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", subscription.getId());
        payload.put("bookingId", order.getId());
        payload.put("contractVersion", BOOKING_CONTRACT_VERSION);
        payload.put("bookingType", "SUBSCRIPTION");
        payload.put("subscriptionId", subscription.getId());
        payload.put("subscriptionServiceType", subscription.getServiceType());
        payload.put("frequencyLabel", frequencyLabel(frequencyDays));
        payload.put("frequencyDays", frequencyDays);
        payload.put("startDate", subscription.getStartDate());
        payload.put("endDate", subscription.getEndDate());
        payload.put("visits", subscription.getVisits());
        payload.put("timeSlot", subscription.getTimeSlot());
        payload.put("fullAddress", order.getAddress());
        payload.put("totalAmount", subscription.getPrice());
        payload.put("status", subscription.getStatus().name());
        payload.put("createdAt", utc(order.getCreatedAt()));
        payload.put("meta", subscriptionBookingMeta());
        return payload;
    }

    private String frequencyLabel(List<String> frequencyDays) {
        if (frequencyDays == null || frequencyDays.isEmpty()) {
            return "Custom";
        }
        if (frequencyDays.size() == 7) {
            return "Everyday";
        }
        if (frequencyDays.size() == 1) {
            return frequencyDays.get(0);
        }
        return String.join(", ", frequencyDays);
    }

    private Map<String, Object> mapTrackingPayload(Order order, String assignmentStatus) {
        Map<String, Object> payload = new LinkedHashMap<>();
        boolean paid = order.getPaymentStatus() == PaymentStatus.SUCCESS;
        boolean activeTracking = Set.of("PARTNER_ON_THE_WAY", "ARRIVED", "PARTNER_ARRIVED", "SERVICE_STARTED")
                .contains(assignmentStatus == null ? "" : assignmentStatus.toUpperCase(Locale.ROOT));
        Long partnerId = order.getAssignedPartnerId();
        Optional<CurrentPartnerLocationEntity> partnerLocation = partnerId == null
                ? Optional.empty()
                : currentPartnerLocationRepository.findByPartnerIdAndDeletedFalse(partnerId);
        boolean partnerAvailable = partnerLocation.isPresent();
        boolean showMap = paid && activeTracking && partnerAvailable;

        payload.put("showMap", showMap);
        payload.put("mapEnabled", showMap);
        payload.put("partnerAvailable", partnerAvailable);
        payload.put("trackingStatus", assignmentStatus);
        if (!showMap) {
            payload.put("map", Map.of(
                    "enabled", false,
                    "reason", mapDisabledReason(paid, activeTracking, partnerId, partnerAvailable)
            ));
            return payload;
        }

        CurrentPartnerLocationEntity location = partnerLocation.get();
        Map<String, Object> partner = new LinkedHashMap<>();
        partner.put("id", partnerId);
        partner.put("latitude", location.getLatitude() == null ? null : location.getLatitude().doubleValue());
        partner.put("longitude", location.getLongitude() == null ? null : location.getLongitude().doubleValue());
        partner.put("updatedAt", utc(location.getUpdatedAt()));
        partner.put("lastHeartbeatAt", utc(location.getLastHeartbeatAt()));

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("enabled", true);
        map.put("provider", "MAPBOX");
        map.put("partner", partner);
        map.put("customer", Map.of(
                "latitude", "",
                "longitude", ""
        ));
        payload.put("map", map);
        return payload;
    }

    private Map<String, Object> rootTrackingPayload(Map<String, Object> trackingPayload) {
        Map<String, Object> payload = new LinkedHashMap<>(trackingPayload);
        payload.remove("showMap");
        payload.remove("mapEnabled");
        return payload;
    }

    private Map<String, Object> actionPermissions(Order order, String assignmentStatus, boolean partnerAvailable) {
        Map<String, Object> permissions = new LinkedHashMap<>();
        permissions.put("canCancel", order != null && order.getStatus() != null && order.getStatus().canCancel());
        permissions.put("canReschedule", canReschedule(order));
        permissions.put("canCallPartner", canCallPartner(order, assignmentStatus));
        permissions.put("canTrackPartner", partnerAvailable && Set.of("PARTNER_ON_THE_WAY", "ARRIVED", "PARTNER_ARRIVED", "SERVICE_STARTED")
                .contains(assignmentStatus == null ? "" : assignmentStatus.toUpperCase(Locale.ROOT)));
        permissions.put("canRaiseIssue", canRaiseIssue(order));
        return permissions;
    }

    private boolean canReschedule(Order order) {
        if (order == null || order.getStatus() == null) {
            return false;
        }
        boolean scheduled = "SCHEDULED".equalsIgnoreCase(order.getBookingType()) || order.getStatus() == OrderStatus.SCHEDULED;
        return scheduled && Set.of(OrderStatus.CREATED, OrderStatus.CONFIRMED, OrderStatus.SCHEDULED, OrderStatus.BOOKING_CREATED)
                .contains(order.getStatus());
    }

    private boolean canCallPartner(Order order, String assignmentStatus) {
        if (order == null || order.getAssignedPartnerId() == null) {
            return false;
        }
        return Set.of("PARTNER_ASSIGNED", "PARTNER_ON_THE_WAY", "ARRIVED", "PARTNER_ARRIVED", "SERVICE_STARTED", "IN_PROGRESS")
                .contains(assignmentStatus == null ? "" : assignmentStatus.toUpperCase(Locale.ROOT));
    }

    private boolean canRaiseIssue(Order order) {
        if (order == null || order.getStatus() == null) {
            return false;
        }
        return Set.of(OrderStatus.SERVICE_STARTED, OrderStatus.SERVICE_IN_PROGRESS, OrderStatus.SERVICE_COMPLETED,
                OrderStatus.DELIVERED, OrderStatus.PAYMENT_COMPLETED).contains(order.getStatus());
    }

    private boolean retryPaymentAllowed(Order order) {
        return order != null && (order.getPaymentStatus() == PaymentStatus.FAILED || order.getPaymentStatus() == PaymentStatus.EXPIRED);
    }

    private int trackingRefreshSeconds(String status, String assignmentStatus) {
        String normalized = assignmentStatus == null ? "" : assignmentStatus.toUpperCase(Locale.ROOT);
        if ("PARTNER_ASSIGNED".equals(normalized)) {
            return 15;
        }
        if ("PARTNER_ON_THE_WAY".equals(normalized)) {
            return 5;
        }
        if ("ARRIVED".equals(normalized) || "PARTNER_ARRIVED".equals(normalized)) {
            return 5;
        }
        if ("SERVICE_STARTED".equals(normalized)) {
            return 15;
        }
        if ("IN_PROGRESS".equals(normalized) || "SERVICE_IN_PROGRESS".equalsIgnoreCase(status)) {
            return 30;
        }
        if ("COMPLETED".equalsIgnoreCase(status) || "COMPLETED".equals(normalized)) {
            return 0;
        }
        return 15;
    }

    private Map<String, Object> metaPayload(String trackingScreen,
                                            int trackingRefreshSeconds,
                                            Map<String, Object> trackingPayload,
                                            Map<String, Object> permissions) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("trackingScreen", trackingScreen);
        meta.put("trackingRefreshSeconds", trackingRefreshSeconds);
        meta.put("showMap", Boolean.TRUE.equals(trackingPayload.get("showMap")));
        meta.put("canCancel", Boolean.TRUE.equals(permissions.get("canCancel")));
        meta.put("canReschedule", Boolean.TRUE.equals(permissions.get("canReschedule")));
        meta.put("canCallPartner", Boolean.TRUE.equals(permissions.get("canCallPartner")));
        meta.put("canTrackPartner", Boolean.TRUE.equals(permissions.get("canTrackPartner")));
        meta.put("canRaiseIssue", Boolean.TRUE.equals(permissions.get("canRaiseIssue")));
        return meta;
    }

    private void addReviewMeta(Map<String, Object> meta, Order order) {
        boolean reviewSubmitted = order != null
                && order.getId() != null
                && reviewRepository.existsByBookingIdAndDeletedFalse(order.getId());
        boolean canReview = order != null && isCompletedStatus(order.getStatus()) && !reviewSubmitted;
        meta.put("canReview", canReview);
        meta.put("reviewSubmitted", reviewSubmitted);
    }

    private Map<String, Object> subscriptionBookingMeta() {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("trackingScreen", "SUBSCRIPTION_DETAILS");
        meta.put("showMap", false);
        meta.put("trackingRefreshSeconds", 0);
        meta.put("canCancel", false);
        meta.put("canReschedule", false);
        meta.put("canCallPartner", false);
        meta.put("canTrackPartner", false);
        meta.put("canRaiseIssue", false);
        return meta;
    }

    private List<Map<String, Object>> statusHistory(Order order) {
        if (order == null || order.getId() == null) {
            return List.of();
        }
        List<Map<String, Object>> events = bookingEventRepository.findByBookingIdOrderByCreatedAtAsc(order.getId()).stream()
                .map(this::statusHistoryEvent)
                .filter(Objects::nonNull)
                .toList();
        if (!events.isEmpty()) {
            return events;
        }
        List<Map<String, Object>> fallback = new ArrayList<>();
        addStatusHistory(fallback, "BOOKING_CREATED", order.getCreatedAt());
        addStatusHistory(fallback, "PARTNER_ASSIGNED", order.getUpdatedAt(), order.getAssignedPartnerId() != null);
        addStatusHistory(fallback, "ARRIVED", order.getArrivedAt());
        addStatusHistory(fallback, "SERVICE_STARTED", order.getStartedAt());
        addStatusHistory(fallback, "SERVICE_IN_PROGRESS", order.getInProgressAt());
        addStatusHistory(fallback, "COMPLETED", firstNonNull(order.getCompletedAt(), isCompletedStatus(order.getStatus()) ? order.getUpdatedAt() : null));
        return fallback;
    }

    private Map<String, Object> statusHistoryEvent(BookingEventEntity event) {
        if (event == null || event.getEventType() == null) {
            return null;
        }
        String status = timelineStatus(event.getEventType());
        if (status == null) {
            return null;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", status);
        payload.put("time", utc(event.getCreatedAt()));
        return payload;
    }

    private String timelineStatus(String eventType) {
        String normalized = eventType == null ? "" : eventType.toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "BOOKING_CREATED", "SEARCHING_PARTNER", "PARTNER_SEARCH", "PARTNER_ASSIGNED",
                 "SERVICE_STARTED", "SERVICE_IN_PROGRESS", "COMPLETED" -> normalized;
            case "PARTNER_ACCEPTED", "OUT_FOR_DELIVERY" -> "ON_THE_WAY";
            case "ARRIVED", "PARTNER_REACHED" -> "ARRIVED";
            case "SERVICE_COMPLETED", "PAYMENT_COMPLETED", "DELIVERED" -> "COMPLETED";
            default -> null;
        };
    }

    private void addStatusHistory(List<Map<String, Object>> history, String status, LocalDateTime time) {
        addStatusHistory(history, status, time, time != null);
    }

    private void addStatusHistory(List<Map<String, Object>> history, String status, LocalDateTime time, boolean include) {
        if (!include) {
            return;
        }
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("status", status);
        row.put("time", utc(time));
        history.add(row);
    }

    private boolean isCompletedStatus(OrderStatus status) {
        return status == OrderStatus.SERVICE_COMPLETED
                || status == OrderStatus.PAYMENT_COMPLETED
                || status == OrderStatus.DELIVERED;
    }

    private String assignmentStatusForMap(Order order) {
        return deriveAssignmentStatus(order, order != null && order.getAssignedPartnerId() != null);
    }

    private String mapDisabledReason(boolean paid, boolean activeTracking, Long partnerId, boolean hasPartnerLocation) {
        if (!paid) {
            return "PAYMENT_NOT_PAID";
        }
        if (!activeTracking) {
            return "TRACKING_NOT_ACTIVE";
        }
        if (partnerId == null) {
            return "PARTNER_NOT_ASSIGNED";
        }
        if (!hasPartnerLocation) {
            return "PARTNER_LOCATION_UNAVAILABLE";
        }
        return "UNAVAILABLE";
    }

    private Map<String, Object> toNotificationMap(NotificationResponse notification) {
        String bookingId = notification.getBookingId();
        return Map.of(
                "id", String.valueOf(notification.getId()),
                "title", notification.getTitle() == null ? "" : notification.getTitle(),
                "message", notification.getMessage() == null ? "" : notification.getMessage(),
                "redirectType", notification.getRedirectType() == null ? "" : notification.getRedirectType(),
                "bookingId", bookingId == null ? "" : bookingId,
                "read", "READ".equalsIgnoreCase(notification.getStatus().name()),
                "createdAt", String.valueOf(notification.getCreatedAt())
        );
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (Exception ex) {
            throw new BadRequestException("Invalid date format. Use ISO datetime");
        }
    }

    private double round2(double value) {
        return Math.round(value * 100d) / 100d;
    }

    private String utc(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.atZone(java.time.ZoneId.systemDefault())
                .withZoneSameInstant(java.time.ZoneOffset.UTC)
                .format(java.time.format.DateTimeFormatter.ISO_INSTANT);
    }

    private Map<String, Object> paymentOptionsPayload(String bookingId, Long cartId, java.math.BigDecimal amount) {
        List<Map<String, Object>> methods = List.of(
                paymentMethodRow("UPI", "UPI", "Pay instantly with any UPI app"),
                paymentMethodRow("CARD", "Card", "Credit/Debit card payment"),
                paymentMethodRow("COD", "Cash on Delivery", "Pay after service completion")
        );

        Map<String, Object> content = new LinkedHashMap<>();
        content.put("paymentOptions", methods);
        content.put("options", methods);
        content.put("methods", methods);
        content.put("paymentMethods", methods);
        content.put("content", methods);
        content.put("items", methods);
        if (bookingId != null) {
            content.put("bookingId", bookingId);
        }
        if (cartId != null) {
            content.put("cartId", cartId);
        }
        if (amount != null) {
            content.put("amount", amount);
        }
        return content;
    }

    private Map<String, Object> paymentMethodRow(String id, String label, String description) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("mode", id);
        row.put("label", label);
        row.put("description", description);
        return row;
    }

    private String normalizeMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return "UPI";
        }
        String normalized = mode.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("UPI", "CARD", "COD").contains(normalized)) {
            throw new BadRequestException("Unsupported payment mode");
        }
        return normalized;
    }

    private Long resolveBookingId(String rawBookingId) {
        if (rawBookingId == null || rawBookingId.isBlank()) {
            return null;
        }
        String value = rawBookingId.trim();
        if (value.chars().allMatch(Character::isDigit)) {
            return Long.parseLong(value);
        }
        Matcher matcher = Pattern.compile("(\\d+)").matcher(value);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }
        return null;
    }

    private int amountInPaise(java.math.BigDecimal requestedAmount, Double orderAmount) {
        java.math.BigDecimal amount = requestedAmount != null
                ? requestedAmount
                : java.math.BigDecimal.valueOf(orderAmount == null ? 0d : orderAmount);
        return amount.multiply(java.math.BigDecimal.valueOf(100L)).setScale(0, java.math.RoundingMode.HALF_UP).intValue();
    }

    private String deriveAssignmentStatus(Order order, boolean hasAssignedPartner) {
        if (order == null) {
            return "SEARCHING_PARTNER";
        }
        String status = order.getStatus() == null ? "" : order.getStatus().name();
        if ("SERVICE_IN_PROGRESS".equalsIgnoreCase(status)) {
            return "IN_PROGRESS";
        }
        if ("SERVICE_STARTED".equalsIgnoreCase(status)) {
            return "SERVICE_STARTED";
        }
        if ("SERVICE_COMPLETED".equalsIgnoreCase(status)
                || "DELIVERED".equalsIgnoreCase(status)
                || "PAYMENT_COMPLETED".equalsIgnoreCase(status)) {
            return "COMPLETED";
        }
        if ("ARRIVED".equalsIgnoreCase(status) || "PARTNER_REACHED".equalsIgnoreCase(status)) {
            return "ARRIVED";
        }
        if ("PARTNER_ACCEPTED".equalsIgnoreCase(status)
                || "PREPARING".equalsIgnoreCase(status)
                || "OUT_FOR_DELIVERY".equalsIgnoreCase(status)) {
            return "PARTNER_ON_THE_WAY";
        }
        if ("CANCELLED".equalsIgnoreCase(status)) {
            return "CANCELLED";
        }
        if ("INSTANT_FAILED".equalsIgnoreCase(String.valueOf(order.getStatus()))) {
            return "NO_PARTNER_AVAILABLE";
        }
        if (hasAssignedPartner) {
            return "PARTNER_ASSIGNED";
        }
        String bookingType = order.getBookingType();
        if ("SCHEDULED".equalsIgnoreCase(bookingType)) {
            return "SCHEDULED";
        }
        return "SEARCHING_PARTNER";
    }

    private String bookingMessage(String status, String assignmentStatus) {
        if ("PARTNER_ASSIGNED".equalsIgnoreCase(assignmentStatus)) {
            return "Partner assigned";
        }
        if ("NO_PARTNER_AVAILABLE".equalsIgnoreCase(assignmentStatus)) {
            return "No partner available right now";
        }
        if ("SCHEDULED".equalsIgnoreCase(assignmentStatus)) {
            return "Booking scheduled";
        }
        if ("COMPLETED".equalsIgnoreCase(status)) {
            return "Service completed";
        }
        return "Finding partner";
    }

    private String responseStatus(Order order, String assignmentStatus) {
        if (order == null || order.getStatus() == null) {
            return "PENDING_ASSIGNMENT";
        }
        return switch (order.getStatus()) {
            case SEARCHING_PARTNER, PARTNER_SEARCH, BOOKING_CREATED, CREATED, CONFIRMED -> {
                if ("SCHEDULED".equalsIgnoreCase(order.getBookingType())) {
                    yield "SCHEDULED";
                }
                yield "PENDING_ASSIGNMENT";
            }
            case PARTNER_ASSIGNED -> "PARTNER_ASSIGNED";
            case PARTNER_ACCEPTED, PREPARING, OUT_FOR_DELIVERY -> "ON_THE_WAY";
            case ARRIVED, PARTNER_REACHED -> "ARRIVED";
            case SERVICE_STARTED -> "SERVICE_STARTED";
            case SERVICE_IN_PROGRESS -> "SERVICE_IN_PROGRESS";
            case SERVICE_COMPLETED, PAYMENT_COMPLETED, DELIVERED -> "COMPLETED";
            case INSTANT_FAILED -> "EXPIRED";
            case CANCELLED -> "CANCELLED";
            case SCHEDULED -> "SCHEDULED";
        };
    }

    private String trackingScreen(Order order, String assignmentStatus) {
        if (order == null) {
            return "INSTANT_STATUS";
        }
        if ("SUBSCRIPTION".equalsIgnoreCase(order.getBookingType())) {
            return "SUBSCRIPTION_DETAILS";
        }
        if ("SCHEDULED".equalsIgnoreCase(order.getBookingType())
                && !Set.of("PARTNER_ASSIGNED", "PARTNER_ON_THE_WAY", "ARRIVED", "PARTNER_ARRIVED", "SERVICE_STARTED", "IN_PROGRESS")
                .contains(assignmentStatus == null ? "" : assignmentStatus.toUpperCase(Locale.ROOT))) {
            return "SCHEDULED_STATUS";
        }
        OrderStatus status = order.getStatus();
        if (status == null) {
            return "INSTANT_STATUS";
        }
        return switch (status) {
            case PARTNER_ASSIGNED -> "INSTANT_STATUS";
            case PARTNER_ACCEPTED, PREPARING, OUT_FOR_DELIVERY -> "LIVE_TRACKING";
            case ARRIVED, PARTNER_REACHED -> "PARTNER_ARRIVED";
            case SERVICE_STARTED -> "SERVICE_STARTED";
            case SERVICE_IN_PROGRESS -> "SERVICE_IN_PROGRESS";
            case SERVICE_COMPLETED, PAYMENT_COMPLETED, DELIVERED -> "SERVICE_COMPLETED";
            case SCHEDULED -> "SCHEDULED_STATUS";
            default -> "INSTANT_STATUS";
        };
    }

    private String estimatedCompletionTime(Order order) {
        LocalDateTime base = order == null ? null : firstNonNull(order.getInProgressAt(), order.getStartedAt(), order.getScheduledAt());
        if (base == null) {
            return "";
        }
        return base.plusHours(1).format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH));
    }

    @SafeVarargs
    private final <T> T firstNonNull(T... values) {
        if (values == null) {
            return null;
        }
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private int deriveEtaMinutes(String status, String assignmentStatus) {
        if ("COMPLETED".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(assignmentStatus)) {
            return 0;
        }
        if ("PARTNER_ASSIGNED".equalsIgnoreCase(assignmentStatus)
                || "PARTNER_ON_THE_WAY".equalsIgnoreCase(assignmentStatus)) {
            return 12;
        }
        return 0;
    }

    private Map<String, Object> partnerSummary(Long serviceId, OrderResponseDTO order, String assignmentStatus) {
        if (!shouldExposePartner(assignmentStatus)) {
            return null;
        }
        Long partnerId = serviceId;
        String fallbackName = order.getItems().isEmpty() ? "" : order.getItems().get(0).getPartnerName();
        String name = fallbackName;
        String phone = "";
        Double rating = 0d;
        String profileImage = "";

        if (partnerId != null) {
            Partner legacyPartner = partnerRepository.findById(partnerId).orElse(null);
            if (legacyPartner != null) {
                if (legacyPartner.getName() != null && !legacyPartner.getName().isBlank()) {
                    name = legacyPartner.getName();
                }
                rating = legacyPartner.getRating() == null ? 0d : legacyPartner.getRating();
                profileImage = legacyPartner.getImage() == null ? "" : legacyPartner.getImage();
            }
            PartnerProfile partnerProfile = partnerProfileRepository.findById(partnerId).orElse(null);
            if (partnerProfile != null) {
                if (partnerProfile.getName() != null && !partnerProfile.getName().isBlank()) {
                    name = partnerProfile.getName();
                }
                phone = sanitizePhone(partnerProfile.getMobileNumber());
                if (phone.isBlank()) {
                    phone = sanitizePhone(partnerProfile.getPhoneNumber());
                }
                rating = partnerProfile.getRating() == null ? rating : partnerProfile.getRating();
                if (partnerProfile.getProfileImage() != null && !partnerProfile.getProfileImage().isBlank()) {
                    profileImage = partnerProfile.getProfileImage();
                }
            }
        }

        Map<String, Object> partner = new LinkedHashMap<>();
        partner.put("id", partnerId == null ? "" : String.valueOf(partnerId));
        partner.put("name", name == null ? "" : name);
        partner.put("phone", phone);
        partner.put("rating", rating);
        partner.put("profileImage", profileImage);
        return partner;
    }

    private boolean shouldExposePartner(String assignmentStatus) {
        return Set.of("PARTNER_ASSIGNED", "PARTNER_ON_THE_WAY", "ARRIVED", "IN_PROGRESS", "COMPLETED")
                .contains(assignmentStatus == null ? "" : assignmentStatus.toUpperCase(Locale.ROOT));
    }

    private String sanitizePhone(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            return "";
        }
        String digits = rawPhone.replaceAll("[^0-9]", "");
        if (digits.length() == 10) {
            return digits;
        }
        if (digits.length() > 10) {
            return digits.substring(digits.length() - 10);
        }
        return "";
    }

    private void validateCoordinates(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            throw new BadRequestException("latitude and longitude are required");
        }
        if (latitude < -90d || latitude > 90d) {
            throw new BadRequestException("latitude must be between -90 and 90");
        }
        if (longitude < -180d || longitude > 180d) {
            throw new BadRequestException("longitude must be between -180 and 180");
        }
    }

    private Map<String, Object> mapFaqRow(SupportFaq faq) {
        return Map.of(
                "id", faq.getFaqKey(),
                "question", faq.getQuestion(),
                "answer", faq.getAnswer(),
                "displayOrder", faq.getDisplayOrder()
        );
    }

    private SupportTicket requireCustomerTicket(Long userId, String ticketId) {
        Long parsedId = parseTicketId(ticketId);
        return supportTicketRepository.findByUserIdAndId(userId, parsedId)
                .orElseThrow(() -> new BadRequestException("Ticket not found"));
    }

    private String normalizeSupportBookingId(Long userId, String bookingId) {
        String normalized = blankToNull(bookingId);
        if (normalized == null) {
            return null;
        }
        Long resolvedBookingId = resolveBookingId(normalized);
        if (resolvedBookingId == null) {
            throw new BadRequestException("Invalid bookingId");
        }
        Order order = orderRepository.findById(resolvedBookingId)
                .orElseThrow(() -> new BadRequestException("Booking not found"));
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new BadRequestException("You are not allowed to link this booking");
        }
        return String.valueOf(resolvedBookingId);
    }

    private String supportSender(SupportTicketReply reply) {
        String senderType = reply.getSenderType();
        if (senderType == null || senderType.isBlank()) {
            return "SUPPORT";
        }
        return senderType.trim().toUpperCase(Locale.ROOT);
    }

    private void publishSupportEvent(SupportTicket ticket, String eventName, Map<String, Object> payload) {
        if (supportRealtimeService != null) {
            supportRealtimeService.publish(ticket, "CUSTOMER", eventName, payload);
        }
    }

    private List<String> deserializeAttachmentUrls(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return List.of();
        }
        try {
            List<String> values = SUPPORT_OBJECT_MAPPER.readValue(rawValue, STRING_LIST_TYPE);
            return values == null ? List.of() : values;
        } catch (JsonProcessingException exception) {
            log.warn("Failed to parse support attachment urls", exception);
            return List.of();
        }
    }

    private String serializeAttachmentUrls(List<String> attachmentUrls) {
        List<String> sanitized = sanitizeAttachmentUrls(attachmentUrls);
        if (sanitized.isEmpty()) {
            return null;
        }
        try {
            return SUPPORT_OBJECT_MAPPER.writeValueAsString(sanitized);
        } catch (JsonProcessingException exception) {
            throw new BadRequestException("Invalid attachmentUrls");
        }
    }

    private List<String> sanitizeAttachmentUrls(List<String> attachmentUrls) {
        if (attachmentUrls == null || attachmentUrls.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> sanitized = new LinkedHashSet<>();
        for (String value : attachmentUrls) {
            String normalized = blankToNull(value);
            if (normalized == null) {
                continue;
            }
            if (normalized.length() > MAX_SUPPORT_ATTACHMENT_URL_LENGTH) {
                throw new BadRequestException("attachmentUrl is too long");
            }
            sanitized.add(normalized);
            if (sanitized.size() > MAX_SUPPORT_ATTACHMENTS) {
                throw new BadRequestException("No more than 10 attachments are allowed");
            }
        }
        return sanitized.isEmpty() ? List.of() : new ArrayList<>(sanitized);
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String formatTicketId(Long id) {
        if (id == null) {
            return "";
        }
        return "TCK-" + (10000 + id);
    }

    private Long parseTicketId(String ticketId) {
        if (ticketId == null || ticketId.isBlank()) {
            throw new BadRequestException("Ticket id is required");
        }
        String digits = ticketId.trim().replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            throw new BadRequestException("Invalid ticket id");
        }
        long numeric = Long.parseLong(digits);
        if (numeric >= 10000) {
            return numeric - 10000;
        }
        return numeric;
    }

    private LocalDateTime parseScheduledAt(String date, String slot) {
        if (date == null || date.isBlank() || slot == null || slot.isBlank()) {
            log.warn("Booking slot validation failed: requestedDate={}, requestedSlot={}, parsedServerDateTime=null, serverTimezone={}, currentServerTime={}, rejectionReasonCode=SLOT_REQUIRED, rejectionMessage=date and slot are required for scheduled booking, slotLookupResult=NOT_FOUND",
                    date, slot, slotDateTimePolicy.zoneId().getId(), slotDateTimePolicy.now());
            throw new BookingValidationException("SLOT_REQUIRED", "date and slot are required for scheduled booking");
        }

        LocalDate bookingDate;
        try {
            bookingDate = LocalDate.parse(date);
        } catch (Exception ex) {
            log.warn("Booking slot validation failed: requestedDate={}, requestedSlot={}, parsedServerDateTime=null, serverTimezone={}, currentServerTime={}, rejectionReasonCode=INVALID_DATE_FORMAT, rejectionMessage=Invalid date format. Use YYYY-MM-DD, slotLookupResult=NOT_FOUND",
                    date, slot, slotDateTimePolicy.zoneId().getId(), slotDateTimePolicy.now());
            throw new BadRequestException("Invalid date format. Use YYYY-MM-DD");
        }
         LocalDateTime scheduledStart = slotDateTimePolicy.parseSlotStart(bookingDate, slot);
        if (bookingDate.isAfter(slotDateTimePolicy.today().plusDays(cartConfigService.currentEntity().getScheduledBookingWindowDays()))) {
            throw new BookingValidationException("SLOT_UNAVAILABLE", "Selected date is outside scheduled booking window.");
        }
         Map<String, Object> slotRow = customerCartSlotService.slotRowForDate(bookingDate, slot);
        if (slotRow == null) {
            log.warn("Booking slot validation failed: requestedDate={}, requestedSlot={}, parsedServerDateTime={}, serverTimezone={}, currentServerTime={}, rejectionReasonCode=SLOT_NOT_LISTED, rejectionMessage=Selected slot is not available for requested date., slotLookupResult=NOT_FOUND",
                    date, slot, scheduledStart, slotDateTimePolicy.zoneId().getId(), slotDateTimePolicy.now());
            throw new BookingValidationException("SLOT_NOT_LISTED", "Selected slot is not available for requested date.");
        }
        if (slotDateTimePolicy.isExpired(scheduledStart)) {
            log.warn("Booking slot validation failed: requestedDate={}, requestedSlot={}, parsedServerDateTime={}, serverTimezone={}, currentServerTime={}, rejectionReasonCode={}, rejectionMessage=Selected slot is expired. Please choose a future slot., slotLookupResult=FOUND",
                    date, slot, scheduledStart, slotDateTimePolicy.zoneId().getId(), slotDateTimePolicy.now(), InstantUnavailableCode.SLOT_EXPIRED.name());
            throw new BookingValidationException(
                    InstantUnavailableCode.SLOT_EXPIRED.name(),
                    "Selected slot is expired. Please choose a future slot."
            );
        }
        Object available = slotRow.get("available");
        if (available instanceof Boolean bool && !bool) {
            log.warn("Booking slot validation failed: requestedDate={}, requestedSlot={}, parsedServerDateTime={}, serverTimezone={}, currentServerTime={}, rejectionReasonCode=SLOT_UNAVAILABLE, rejectionMessage=Selected slot is unavailable. Please choose another slot., slotLookupResult=FOUND",
                    date, slot, scheduledStart, slotDateTimePolicy.zoneId().getId(), slotDateTimePolicy.now());
            throw new BookingValidationException("SLOT_UNAVAILABLE", "Selected slot is unavailable. Please choose another slot.");
        }

        return scheduledStart;
    }

    private String generateUniqueReferralCode(Long userId) {
        for (int i = 0; i < 10; i++) {
            String code = nextReferralCode();
            if (!referralCodeRepository.existsByCodeIgnoreCase(code)) {
                return code;
            }
        }
        return "LEX" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
    }

    static String nextReferralCode() {
        StringBuilder builder = new StringBuilder(3 + REFERRAL_CODE_LENGTH);
        builder.append("LEX");
        for (int i = 0; i < REFERRAL_CODE_LENGTH; i++) {
            builder.append(REFERRAL_CODE_ALPHABET[REFERRAL_CODE_RANDOM.nextInt(REFERRAL_CODE_ALPHABET.length)]);
        }
        return builder.toString();
    }

    private void audit(Long userId, Long partnerId, Long bookingId, String action, String oldValue, String newValue) {
        if (auditLogService == null) {
            return;
        }
        try {
            auditLogService.log(userId, partnerId, bookingId, action, oldValue, newValue);
        } catch (RuntimeException exception) {
            log.warn("Audit log write failed for action={} bookingId={}", action, bookingId, exception);
        }
    }
}

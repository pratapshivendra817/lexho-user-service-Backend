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
import com.lexho.user_service.admin.entity.AdminServiceEntity;
import com.lexho.user_service.admin.entity.SupportFaq;
import com.lexho.user_service.admin.entity.SupportFaqCategory;
import com.lexho.user_service.admin.entity.SupportTicketReply;
import com.lexho.user_service.admin.repository.AdminCategoryRepository;
import com.lexho.user_service.admin.repository.AdminServiceEntityRepository;
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
import com.lexho.user_service.coupon.entity.Coupon;
import com.lexho.user_service.coupon.enums.CouponDiscountType;
import com.lexho.user_service.coupon.repository.CouponRepository;
import com.lexho.user_service.customer.service.CustomerActivityFeedService;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.customer.service.SlotDateTimePolicy;
import com.lexho.user_service.customer.service.CustomerCartSlotService;
import com.lexho.user_service.booking.exception.BookingValidationException;
import com.lexho.user_service.booking.exception.InstantBookingUnavailableException;
import com.lexho.user_service.booking.idempotency.BookingIdempotencyService;
import com.lexho.user_service.booking.model.BookingType;
import com.lexho.user_service.booking.model.InstantUnavailableCode;
import com.lexho.user_service.customer.location.entity.CustomerAddress;
import com.lexho.user_service.customer.location.repository.CustomerAddressRepository;
import com.lexho.user_service.customer.location.dto.ServiceabilityCheckResponse;
import com.lexho.user_service.customer.location.service.ServiceabilityService;
import com.lexho.user_service.lexho.dto.LexhoDtos;
import com.lexho.user_service.lexho.entity.*;
import com.lexho.user_service.lexho.repository.*;
import com.lexho.user_service.lexhopartner.entity.PartnerOperationalProfile;
import com.lexho.user_service.lexhopartner.repository.PartnerOperationalProfileRepository;
import com.lexho.user_service.lexho.service.LexhoAddressService;
import com.lexho.user_service.review.entity.ReviewEntity;
import com.lexho.user_service.review.enums.ReviewStatus;
import com.lexho.user_service.review.repository.ReviewRepository;
import com.lexho.user_service.service.CartService;
import com.lexho.user_service.service.HomeService;
import com.lexho.user_service.service.OrderService;
import com.lexho.user_service.service.ApiRateLimiterService;
import com.lexho.user_service.referral.dto.ReferralApplyRequest;
import com.lexho.user_service.referral.service.ReferralService;
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
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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
    private static final String ATTR_USER_ID = "userId";
    private static final String KEY_ASSIGNMENT_STATUS = "assignmentStatus";
    private static final String KEY_ATTACHMENT_URLS = "attachmentUrls";
    private static final String KEY_BOOKING_ID = "bookingId";
    private static final String KEY_BOOKING_TYPE = "bookingType";
    private static final String KEY_CATEGORY_ID = "categoryId";
    private static final String KEY_CONTRACT_VERSION = "contractVersion";
    private static final String KEY_CREATED_AT = "createdAt";
    private static final String KEY_DATA = "data";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_DISPLAY_ORDER = "displayOrder";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_IMAGE_URL = "imageUrl";
    private static final String KEY_ITEMS = "items";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_META = "meta";
    private static final String KEY_PARTNER_AVAILABLE = "partnerAvailable";
    private static final String KEY_SERVICE_ID = "serviceId";
    private static final String KEY_SERVICE_NAME = "serviceName";
    private static final String KEY_SHOW_MAP = "showMap";
    private static final String KEY_STATUS = "status";
    private static final String KEY_UPDATED_AT = "updatedAt";
    private static final String KEY_AMOUNT = "amount";
    private static final String KEY_AVAILABLE = "available";
    private static final String KEY_CAN_CALL_PARTNER = "canCallPartner";
    private static final String KEY_CAN_CANCEL = "canCancel";
    private static final String KEY_CAN_RAISE_ISSUE = "canRaiseIssue";
    private static final String KEY_CAN_RESCHEDULE = "canReschedule";
    private static final String KEY_CAN_TRACK_PARTNER = "canTrackPartner";
    private static final String KEY_ETA_MINUTES = "etaMinutes";
    private static final String KEY_HTTP_STATUS = "httpStatus";
    private static final String KEY_INSTANT_ELIGIBLE = "instantEligible";
    private static final String KEY_PARSED_SERVER_DATE_TIME = "parsedServerDateTime";
    private static final String KEY_PRICE = "price";
    private static final String KEY_PRIORITY = "priority";
    private static final String KEY_RATING = "rating";
    private static final String KEY_REASON_CODE = "reasonCode";
    private static final String KEY_SCHEDULED_AT = "scheduledAt";
    private static final String KEY_SENDER = "sender";
    private static final String KEY_SLOT_LOOKUP_RESULT = "slotLookupResult";
    private static final String KEY_SLOT_ROW = "slotRow";
    private static final String KEY_STATUS_HISTORY = "statusHistory";
    private static final String KEY_SUBJECT = "subject";
    private static final String KEY_SUCCESS = "success";
    private static final String KEY_TOTAL_AMOUNT = "totalAmount";
    private static final String KEY_VALID = "valid";
    private static final String MSG_SLOT_VALIDATION_DEBUG = "Slot validation debug";
    private static final String MSG_BOOKING_NOT_FOUND = "Booking not found";
    private static final String STATUS_ARRIVED = "ARRIVED";
    private static final String STATUS_BOOKING_CREATED = "BOOKING_CREATED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_CLOSED = "CLOSED";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_INSTANT_FAILED = "INSTANT_FAILED";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_NOT_FOUND = "NOT_FOUND";
    private static final String STATUS_PARTNER_ASSIGNED = "PARTNER_ASSIGNED";
    private static final String STATUS_PARTNER_ARRIVED = "PARTNER_ARRIVED";
    private static final String STATUS_PARTNER_ON_THE_WAY = "PARTNER_ON_THE_WAY";
    private static final String STATUS_SCHEDULED = "SCHEDULED";
    private static final String STATUS_SEARCHING_PARTNER = "SEARCHING_PARTNER";
    private static final String STATUS_SERVICE_COMPLETED = "SERVICE_COMPLETED";
    private static final String STATUS_SERVICE_IN_PROGRESS = "SERVICE_IN_PROGRESS";
    private static final String STATUS_SERVICE_STARTED = "SERVICE_STARTED";
    private static final String SUPPORT_PRIORITY_MEDIUM = "MEDIUM";
    private static final String TRACKING_SCREEN_INSTANT_STATUS = "INSTANT_STATUS";
    private static final String TRACKING_SCREEN_SCHEDULED_STATUS = "SCHEDULED_STATUS";
    private static final String TRACKING_SCREEN_SUBSCRIPTION_DETAILS = "SUBSCRIPTION_DETAILS";
    private static final String USER_TYPE_CUSTOMER = "CUSTOMER";
    private static final String TYPE_SUBSCRIPTION = "SUBSCRIPTION";
    private static final String CUSTOMER_LOCATION_MIRROR_PREFIX = "CUSTOMER_LOCATION:";

    private final HomeService homeService;
    private final CartService cartService;
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final NotificationManagementService notificationManagementService;
    private final LexhoAddressService addressService;
    private final BannerPromotionRepository bannerPromotionRepository;
    private final CategoryRepository categoryRepository;
    private final AdminCategoryRepository adminCategoryRepository;
    private final AdminServiceEntityRepository adminServiceEntityRepository;
    private final PartnerRepository partnerRepository;
    private final PartnerOperationalProfileRepository partnerProfileRepository;
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
    private final AuditLogService auditLogService;
    private final SupportRealtimeService supportRealtimeService;
    private final NotificationRealtimeService notificationRealtimeService;
    private final CustomerActivityFeedService customerActivityFeedService;
    private final ReferralService referralService;
    private final Optional<CouponRepository> couponRepository;

    @Autowired(required = false)
    private CustomerAddressRepository customerAddressRepository;

    @Autowired(required = false)
    private LexhoAddressRepository lexhoAddressRepository;

    @Autowired(required = false)
    private BookingIdempotencyService bookingIdempotencyService;

    public LexhoExperienceController(HomeService homeService,
                                     CartService cartService,
                                     OrderService orderService,
                                     OrderRepository orderRepository,
                                     NotificationManagementService notificationManagementService,
                                     LexhoAddressService addressService,
                                     BannerPromotionRepository bannerPromotionRepository,
                                     CategoryRepository categoryRepository,
                                     AdminCategoryRepository adminCategoryRepository,
                                     AdminServiceEntityRepository adminServiceEntityRepository,
                                     PartnerRepository partnerRepository,
                                     PartnerOperationalProfileRepository partnerProfileRepository,
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
        this(homeService, cartService, orderService, orderRepository, notificationManagementService, addressService,
                bannerPromotionRepository, categoryRepository, adminCategoryRepository, adminServiceEntityRepository,
                partnerRepository, partnerProfileRepository, reviewRepository, favoriteRepository, referralCodeRepository,
                referralUseRepository, supportTicketRepository, supportTicketReplyRepository, supportFaqCategoryRepository,
                supportFaqRepository, mediaUrlResolver, dispatchOrchestrationService, apiRateLimiterService,
                slotDateTimePolicy, customerCartSlotService, geoFenceValidationService, serviceabilityService,
                cartConfigService, subscriptionMasterRepository, subscriptionFrequencyRepository, subscriptionVisitRepository,
                currentPartnerLocationRepository, bookingEventRepository, Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    @Autowired
    public LexhoExperienceController(HomeService homeService,
                                     CartService cartService,
                                     OrderService orderService,
                                     OrderRepository orderRepository,
                                     NotificationManagementService notificationManagementService,
                                     LexhoAddressService addressService,
                                     BannerPromotionRepository bannerPromotionRepository,
                                     CategoryRepository categoryRepository,
                                     AdminCategoryRepository adminCategoryRepository,
                                     AdminServiceEntityRepository adminServiceEntityRepository,
                                     PartnerRepository partnerRepository,
                                     PartnerOperationalProfileRepository partnerProfileRepository,
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
                                     BookingEventRepository bookingEventRepository,
                                     Optional<AuditLogService> auditLogService,
                                     Optional<SupportRealtimeService> supportRealtimeService,
                                     Optional<NotificationRealtimeService> notificationRealtimeService,
                                     Optional<CustomerActivityFeedService> customerActivityFeedService,
                                     Optional<ReferralService> referralService,
                                     Optional<CouponRepository> couponRepository) {
        this.homeService = homeService;
        this.cartService = cartService;
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.notificationManagementService = notificationManagementService;
        this.addressService = addressService;
        this.bannerPromotionRepository = bannerPromotionRepository;
        this.categoryRepository = categoryRepository;
        this.adminCategoryRepository = adminCategoryRepository;
        this.adminServiceEntityRepository = adminServiceEntityRepository;
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
        this.auditLogService = auditLogService.orElse(null);
        this.supportRealtimeService = supportRealtimeService.orElse(null);
        this.notificationRealtimeService = notificationRealtimeService.orElse(null);
        this.customerActivityFeedService = customerActivityFeedService.orElse(null);
        this.referralService = referralService.orElse(null);
        this.couponRepository = couponRepository;
    }

    @GetMapping("/api/home/dashboard")
    public ResponseEntity<ApiResponse<LexhoDtos.HomeDashboardResponse>> dashboard(
            @RequestAttribute(value = ATTR_USER_ID, required = false) Long userId) {
        if (userId == null) {
            return ResponseEntity.ok(ApiResponse.success("Home dashboard", new LexhoDtos.HomeDashboardResponse(
                    "Hi",
                    "Unknown",
                    List.of()
            )));
        }
        HomeResponseDTO home = homeService.getHomeData(userId);
        List<Map<String, Object>> recentBookings = orderService.getUserOrders(userId).stream()
                .limit(5)
                .map(order -> Map.<String, Object>of(
                        "id", String.valueOf(order.getId()),
                        KEY_STATUS, order.getStatus(),
                        KEY_SERVICE_NAME, serviceName(order),
                        KEY_CREATED_AT, ""
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
        return ResponseEntity.ok(ApiResponse.success("Banners fetched", Map.of(KEY_DATA, data)));
    }

    @GetMapping("/api/categories")
    public ResponseEntity<ApiResponse<Map<String, Object>>> categories(@RequestParam(defaultValue = "0") int page,
                                                                       @RequestParam(defaultValue = "20") int size,
                                                                       @RequestParam(required = false) Double lat,
                                                                       @RequestParam(required = false) Double lng,
                                                                       @RequestParam(required = false) String pincode,
                                                                       @RequestAttribute(value = ATTR_USER_ID, required = false) Long userId) {
        if (!isGeoServiceable(userId, lat, lng, pincode)) {
            return ResponseEntity.ok(ApiResponse.success("Categories fetched", Map.of(KEY_DATA, List.of())));
        }
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        List<Map<String, Object>> data = adminCategoryRepository
                .findActiveForCustomer(pageable).stream()
                .map(this::mapCategory)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Categories fetched", Map.of(KEY_DATA, data)));
    }

    @GetMapping("/api/services/popular")
    public ResponseEntity<ApiResponse<Map<String, Object>>> popular(@RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "20") int size,
                                                                    @RequestParam(required = false) Double lat,
                                                                    @RequestParam(required = false) Double lng,
                                                                    @RequestParam(required = false) String pincode,
                                                                    @RequestAttribute(value = ATTR_USER_ID, required = false) Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Popular services fetched",
                Map.of(KEY_DATA, partnerData(page, size, null, userId, lat, lng, pincode))));
    }

    @GetMapping("/api/services/recommended")
    public ResponseEntity<ApiResponse<Map<String, Object>>> recommended(@RequestParam(defaultValue = "0") int page,
                                                                        @RequestParam(defaultValue = "20") int size,
                                                                        @RequestParam(required = false) Double lat,
                                                                        @RequestParam(required = false) Double lng,
                                                                        @RequestParam(required = false) String pincode,
                                                                        @RequestAttribute(value = ATTR_USER_ID, required = false) Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Recommended services fetched",
                Map.of(KEY_DATA, partnerData(page, size, null, userId, lat, lng, pincode))));
    }

    @GetMapping("/api/services/quick")
    public ResponseEntity<ApiResponse<Map<String, Object>>> quick(@RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "20") int size,
                                                                  @RequestParam(required = false) Double lat,
                                                                  @RequestParam(required = false) Double lng,
                                                                  @RequestParam(required = false) String pincode,
                                                                  @RequestAttribute(value = ATTR_USER_ID, required = false) Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Quick services fetched",
                Map.of(KEY_DATA, partnerData(page, size, null, userId, lat, lng, pincode))));
    }

    @GetMapping("/api/search/services")
    public ResponseEntity<ApiResponse<Map<String, Object>>> search(@RequestParam String keyword,
                                                                   @RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "20") int size,
                                                                   @RequestParam(required = false) Double lat,
                                                                   @RequestParam(required = false) Double lng,
                                                                   @RequestParam(required = false) String pincode,
                                                                   @RequestAttribute(value = ATTR_USER_ID, required = false) Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Search results fetched",
                Map.of(KEY_DATA, partnerData(page, size, keyword, userId, lat, lng, pincode))));
    }

    @PostMapping("/api/cart/add")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addCart(@RequestAttribute(ATTR_USER_ID) Long userId,
                                                                    @Valid @RequestBody LexhoDtos.CartAddRequest request) {
        CartRequest cartRequest = new CartRequest();
        cartRequest.setPartnerId(request.serviceId());
        cartRequest.setQuantity(request.quantity());
        CartResponseDTO added = cartService.addToCart(userId, cartRequest);
        return ResponseEntity.ok(ApiResponse.success("Added to cart", mapCartItem(added)));
    }

    @GetMapping("/api/cart/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cart(@RequestAttribute(ATTR_USER_ID) Long userId) {
        CartSummaryDTO cart = cartService.getCart(userId);
        List<Map<String, Object>> items = cart.getItems().stream().map(this::mapCartItem).toList();
        return ResponseEntity.ok(ApiResponse.success("Cart fetched", Map.of(KEY_ITEMS, items, KEY_TOTAL_AMOUNT, cart.getTotal())));
    }

    @PutMapping("/api/cart/update")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateCart(@RequestAttribute(ATTR_USER_ID) Long userId,
                                                                       @Valid @RequestBody LexhoDtos.CartUpdateRequest request) {
        cartService.removeFromCart(userId, request.id());
        CartRequest cartRequest = new CartRequest();
        cartRequest.setPartnerId(request.id());
        cartRequest.setQuantity(request.quantity());
        CartResponseDTO updated = cartService.addToCart(userId, cartRequest);
        return ResponseEntity.ok(ApiResponse.success("Cart updated", mapCartItem(updated)));
    }

    @DeleteMapping("/api/cart/remove")
    public ResponseEntity<ApiResponse<Void>> removeCart(@RequestAttribute(ATTR_USER_ID) Long userId,
                                                        @Valid @RequestBody LexhoDtos.IdRequest request) {
        cartService.removeFromCart(userId, request.id());
        return ResponseEntity.ok(ApiResponse.success("Cart item removed", null));
    }

    @Transactional
    @PostMapping({"/api/bookings/create", "/api/v1/bookings/create"})
    public ResponseEntity<ApiResponse<Map<String, Object>>> createBooking(@RequestAttribute(ATTR_USER_ID) Long userId,
                                                                          @Valid @RequestBody LexhoDtos.BookingCreateRequest request,
                                                                          @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        // Canonical customer booking entrypoint: keep order creation and dispatch orchestration together here.
        String normalizedIdempotencyKey = bookingIdempotencyService == null ? null : bookingIdempotencyService.normalizeKey(idempotencyKey);
        String requestHash = normalizedIdempotencyKey == null ? null : bookingIdempotencyService.requestHash(userId, request);
        if (normalizedIdempotencyKey != null) {
            Optional<ApiResponse<Map<String, Object>>> replay =
                    bookingIdempotencyService.reserveOrReplay(userId, normalizedIdempotencyKey, requestHash);
            if (replay.isPresent()) {
                return ResponseEntity.ok(replay.get());
            }
        }
        try {
            return createBookingInternal(userId, request, normalizedIdempotencyKey, requestHash);
        } catch (RuntimeException ex) {
            if (bookingIdempotencyService != null) {
                bookingIdempotencyService.clearInProgress(userId, normalizedIdempotencyKey);
            }
            throw ex;
        }
    }

    public ResponseEntity<ApiResponse<Map<String, Object>>> createBooking(Long userId,
                                                                          LexhoDtos.BookingCreateRequest request) {
        return createBooking(userId, request, null);
    }

    private ResponseEntity<ApiResponse<Map<String, Object>>> createBookingInternal(Long userId,
                                                                                  LexhoDtos.BookingCreateRequest request,
                                                                                  String idempotencyKey,
                                                                                  String requestHash) {
        apiRateLimiterService.check("booking_create", String.valueOf(userId), 15, java.time.Duration.ofMinutes(5),
                "Too many booking attempts. Please try later");
        BookingType bookingType = resolveBookingType(request);
        validateBookingTypeEnabled(bookingType);
        if (bookingType == BookingType.SCHEDULED) {
            customerCartSlotService.lockSlotCapacity();
        }
        BookingAddressSelection addressSelection = validateBookingAddress(userId, request);
        PlaceOrderRequest placeOrderRequest = placeOrderRequest(addressSelection, request, bookingType);
        OrderResponseDTO order = orderService.placeOrder(userId, placeOrderRequest);
        completeBookingDispatch(userId, addressSelection, request, placeOrderRequest, bookingType, order);
        Map<String, Object> payload = createdBookingPayload(request, addressSelection, bookingType, order);
        audit(userId, null, order.getId(), STATUS_BOOKING_CREATED, null, bookingType.name());
        registerBookingIdempotencySuccess(userId, idempotencyKey, requestHash, order.getId(), payload);
        return ResponseEntity.ok(ApiResponse.success("Booking created", payload));
    }

    private void registerBookingIdempotencySuccess(Long userId,
                                                   String idempotencyKey,
                                                   String requestHash,
                                                   Long bookingId,
                                                   Map<String, Object> payload) {
        if (bookingIdempotencyService == null || idempotencyKey == null) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            bookingIdempotencyService.markSucceeded(userId, idempotencyKey, requestHash, bookingId, payload);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                bookingIdempotencyService.markSucceeded(userId, idempotencyKey, requestHash, bookingId, payload);
            }
        });
    }

    private BookingAddressSelection validateBookingAddress(Long userId, LexhoDtos.BookingCreateRequest request) {
        return parseUuid(request.addressId())
                .map(addressId -> validateCustomerLocationAddress(userId, request, addressId))
                .orElseGet(() -> validateLegacyBookingAddress(userId, request));
    }

    private BookingAddressSelection validateLegacyBookingAddress(Long userId, LexhoDtos.BookingCreateRequest request) {
        try {
            Long legacyAddressId = parseLegacyAddressId(request.addressId());
            LexhoDtos.AddressResponse address = addressService.getById(userId, legacyAddressId);
            Double requestLatitude = resolveBookingLatitude(request);
            Double requestLongitude = resolveBookingLongitude(request);
            log.info("booking_create_coordinates userId={} addressId={} latitudePresent={} longitudePresent={}",
                    userId, legacyAddressId, requestLatitude != null, requestLongitude != null);
            LexhoAddress rawAddress = addressService.applyBookingCoordinatesFallback(
                    userId,
                    legacyAddressId,
                    requestLatitude,
                    requestLongitude
            );
            geoFenceValidationService.validateBookingAddress(rawAddress);
            validateCustomerServiceability(userId);
            return new BookingAddressSelection(address, legacyAddressId, request.addressId(),
                    rawAddress == null ? joinAddressParts(address.line1(), address.city())
                            : joinAddressParts(rawAddress.getHouseNo(), rawAddress.getArea(),
                                    rawAddress.getCity(), rawAddress.getState(), rawAddress.getPincode()));
        } catch (BadRequestException ex) {
            throw new BookingValidationException("ADDRESS_NOT_FOUND", "Address not found for this customer");
        }
    }

    private BookingAddressSelection validateCustomerLocationAddress(Long userId,
                                                                   LexhoDtos.BookingCreateRequest request,
                                                                   UUID addressId) {
        if (customerAddressRepository == null || lexhoAddressRepository == null) {
            throw new BookingValidationException("ADDRESS_NOT_FOUND", "Address not found for this customer");
        }
        CustomerAddress customerAddress = customerAddressRepository
                .findByIdAndCustomerIdAndIsDeletedFalse(addressId, userId)
                .orElseThrow(() -> new BookingValidationException("ADDRESS_NOT_FOUND",
                        "Address not found for this customer"));
        LexhoAddress dispatchAddress = mirrorCustomerAddress(userId, addressId, customerAddress);
        geoFenceValidationService.validateBookingAddress(dispatchAddress);
        validateCustomerServiceability(userId);
        return new BookingAddressSelection(toAddressResponse(dispatchAddress), dispatchAddress.getId(),
                request.addressId(), fullCustomerAddress(customerAddress));
    }

    private String fullCustomerAddress(CustomerAddress address) {
        return joinAddressParts(address.getFlatNo(),
                address.getFloor() == null || address.getFloor().isBlank() ? null : "Floor " + address.getFloor(),
                address.getBuildingName(),
                address.getLandmark() == null || address.getLandmark().isBlank() ? null : "Near " + address.getLandmark(),
                address.getAddressLine(), address.getArea(), address.getSector(),
                address.getCity(), address.getState(), address.getPincode(), address.getCountry());
    }

    private String joinAddressParts(String... values) {
        Map<String, String> parts = new LinkedHashMap<>();
        for (String value : values) {
            if (value == null || value.isBlank()) continue;
            for (String part : value.split(",")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) parts.putIfAbsent(trimmed.toLowerCase(Locale.ROOT), trimmed);
            }
        }
        return String.join(", ", parts.values());
    }

    private LexhoAddress mirrorCustomerAddress(Long userId, UUID sourceId, CustomerAddress source) {
        String sourceKey = CUSTOMER_LOCATION_MIRROR_PREFIX + sourceId;
        return lexhoAddressRepository.findByUserIdAndCreatedByAndIsDeletedFalse(userId, sourceKey)
                .map(existing -> updateMirroredAddress(existing, source, sourceKey))
                .orElseGet(() -> updateMirroredAddress(newMirroredAddress(userId), source, sourceKey));
    }

    private LexhoAddress newMirroredAddress(Long userId) {
        LexhoAddress address = new LexhoAddress();
        address.setUserId(userId);
        address.setDefault(false);
        return address;
    }

    private LexhoAddress updateMirroredAddress(LexhoAddress target, CustomerAddress source, String sourceKey) {
        target.setHouseNo(limit(firstNonBlank(source.getFlatNo(), source.getBuildingName(), source.getAddressLine()), 100));
        target.setArea(limit(firstNonBlank(source.getArea(), source.getSector(), source.getAddressLine()), 180));
        target.setCity(limit(firstNonBlank(source.getCity(), "Unknown"), 120));
        target.setState(limit(firstNonBlank(source.getState(), "Unknown"), 120));
        target.setPincode(limit(source.getPincode(), 10));
        target.setLatitude(source.getLatitude());
        target.setLongitude(source.getLongitude());
        target.setAddressResolved(source.getLatitude() != null && source.getLongitude() != null);
        target.setCreatedBy(sourceKey);
        target.setUpdatedBy("CUSTOMER_LOCATION_SYNC");
        return lexhoAddressRepository.save(target);
    }

    private LexhoDtos.AddressResponse toAddressResponse(LexhoAddress address) {
        return new LexhoDtos.AddressResponse(
                address.getId(),
                "Saved Address",
                firstNonBlank(address.getHouseNo(), address.getArea()),
                address.getCity(),
                address.isDefault()
        );
    }

    private Optional<UUID> parseUuid(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(raw.trim()));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private Long parseLegacyAddressId(String raw) {
        try {
            return Long.valueOf(raw.trim());
        } catch (RuntimeException ex) {
            throw new BookingValidationException("ADDRESS_NOT_FOUND", "Address not found for this customer");
        }
    }

    private void validateCustomerServiceability(Long userId) {
        ServiceabilityCheckResponse serviceability = serviceabilityService.resolveForCustomer(userId);
        if (serviceability != null && !serviceability.isServiceable()) {
            throw new BookingValidationException("SERVICE_UNAVAILABLE", "Service is unavailable in selected area");
        }
    }

    private BookingType resolveBookingType(LexhoDtos.BookingCreateRequest request) {
        return request.bookingType() == null || request.bookingType().isBlank()
                ? BookingType.SCHEDULED
                : BookingType.valueOf(request.bookingType());
    }

    private void validateBookingTypeEnabled(BookingType bookingType) {
        CartConfigEntity cartConfig = cartConfigService.currentEntity();
        if (bookingType == BookingType.INSTANT && !Boolean.TRUE.equals(cartConfig.getInstantBookingEnabled())) {
            throw new BookingValidationException("SERVICE_UNAVAILABLE", "Instant booking is currently disabled");
        }
        if (bookingType == BookingType.SCHEDULED && !Boolean.TRUE.equals(cartConfig.getScheduledBookingEnabled())) {
            throw new BookingValidationException("SERVICE_UNAVAILABLE", "Scheduled booking is currently disabled");
        }
    }

    private PlaceOrderRequest placeOrderRequest(BookingAddressSelection addressSelection,
                                                LexhoDtos.BookingCreateRequest request,
                                                BookingType bookingType) {
        PlaceOrderRequest placeOrderRequest = new PlaceOrderRequest();
        placeOrderRequest.setAddress(addressSelection.fullAddress());
        placeOrderRequest.setExpectedTotal(request.expectedTotal());
        placeOrderRequest.setCouponCode(request.couponCode());
        placeOrderRequest.setBookingType(bookingType.name());
        placeOrderRequest.setDispatchAddressId(addressSelection.dispatchAddressId());
        placeOrderRequest.setDispatchServiceId(request.serviceId());
        placeOrderRequest.setDispatchQuantity(1);
        placeOrderRequest.setScheduledAt(bookingType == BookingType.SCHEDULED
                ? parseScheduledAt(request.date(), request.slot())
                : LocalDateTime.now());
        return placeOrderRequest;
    }

    private void completeBookingDispatch(Long userId,
                                         BookingAddressSelection addressSelection,
                                         LexhoDtos.BookingCreateRequest request,
                                         PlaceOrderRequest placeOrderRequest,
                                         BookingType bookingType,
        OrderResponseDTO order) {
        if (bookingType == BookingType.INSTANT) {
            markInstantDispatchPending(order.getId(), bookingType);
            return;
        }
        markScheduledBooking(order.getId(), bookingType);
    }

    private int dispatchInstantBooking(Long userId,
                                       BookingAddressSelection addressSelection,
                                       LexhoDtos.BookingCreateRequest request,
                                       PlaceOrderRequest placeOrderRequest,
                                       OrderResponseDTO order) {
        try {
            return dispatchOrchestrationService.dispatchOrder(
                    order.getId(),
                    userId,
                    addressSelection.dispatchAddressId(),
                    request.serviceId(),
                    1,
                    placeOrderRequest.getScheduledAt()
            );
        } catch (BadRequestException ex) {
            throw instantUnavailable();
        }
    }

    private void markScheduledBooking(Long orderId, BookingType bookingType) {
        orderRepository.findByIdForUpdate(orderId).ifPresent(order -> {
            applyBookingTypeIfMissing(order, bookingType);
            if (order.getStatus() == OrderStatus.CREATED
                    && order.getStatus().canTransitionTo(OrderStatus.SCHEDULED)) {
                applyOrderStatusTransition(order, OrderStatus.SCHEDULED);
            }
            orderRepository.save(order);
        });
    }

    private void ensureInstantDispatched(Long orderId, BookingType bookingType, int dispatched) {
        if (dispatched > 0) {
            applyBookingTypeIfMissing(orderId, bookingType);
            return;
        }
        markInstantDispatchFailed(orderId, bookingType);
        throw instantUnavailable();
    }

    private void applyBookingTypeIfMissing(Long orderId, BookingType bookingType) {
        orderRepository.findByIdForUpdate(orderId).ifPresent(order -> {
            applyBookingTypeIfMissing(order, bookingType);
            orderRepository.save(order);
        });
    }

    private void applyBookingTypeIfMissing(Order order, BookingType bookingType) {
        if (order.getBookingType() == null || order.getBookingType().isBlank()) {
            order.setBookingType(bookingType.name());
        }
    }

    private void applyOrderStatusTransition(Order order, OrderStatus targetStatus) {
        OrderStatus current = order.getStatus();
        if (current == targetStatus) {
            return;
        }
        if (current == null || !current.canTransitionTo(targetStatus)) {
            throw new BadRequestException("Invalid booking status transition from " + current + " to " + targetStatus);
        }
        order.setStatus(targetStatus);
    }

    private void markInstantDispatchFailed(Long orderId, BookingType bookingType) {
        orderRepository.findByIdForUpdate(orderId).ifPresent(order -> {
            applyBookingTypeIfMissing(order, bookingType);
            applyOrderStatusTransition(order, OrderStatus.INSTANT_FAILED);
            order.setUnavailableReason(InstantUnavailableCode.INSTANT_UNAVAILABLE.name());
            order.setSlaMinutes(DispatchOrchestrationService.INSTANT_SLA_MINUTES);
            orderRepository.save(order);
        });
    }

    private void markInstantDispatchPending(Long orderId, BookingType bookingType) {
        orderRepository.findByIdForUpdate(orderId).ifPresent(order -> {
            applyBookingTypeIfMissing(order, bookingType);
            if (order.getStatus() == OrderStatus.CREATED
                    || order.getStatus() == OrderStatus.BOOKING_CREATED
                    || order.getStatus() == OrderStatus.CONFIRMED) {
                applyOrderStatusTransition(order, OrderStatus.SEARCHING_PARTNER);
            }
            order.setUnavailableReason(null);
            order.setSlaMinutes(DispatchOrchestrationService.INSTANT_SLA_MINUTES);
            orderRepository.save(order);
            dispatchOrchestrationService.saveEvent(orderId, "INSTANT_DISPATCH_PENDING",
                    Map.of("reason", "WAITING_FOR_PARTNER_ACCEPTANCE"));
        });
    }

    private InstantBookingUnavailableException instantUnavailable() {
        return new InstantBookingUnavailableException(
                InstantUnavailableCode.INSTANT_UNAVAILABLE.name(),
                "No partner available right now. Please book a scheduled slot."
        );
    }

    private Map<String, Object> createdBookingPayload(LexhoDtos.BookingCreateRequest request,
                                                      BookingAddressSelection addressSelection,
                                                      BookingType bookingType,
                                                      OrderResponseDTO order) {
        Map<String, Object> payload = baseCreatedBookingPayload(request, addressSelection, bookingType, order);
        Order persisted = orderRepository.findById(order.getId()).orElse(null);
        String assignmentStatus = assignmentStatusForCreatedBooking(bookingType, persisted);
        Map<String, Object> trackingPayload = persisted == null
                ? Map.of(KEY_SHOW_MAP, false)
                : mapTrackingPayload(persisted, assignmentStatus);
        Map<String, Object> permissions = permissionsForCreatedBooking(bookingType, persisted, assignmentStatus, trackingPayload);
        payload.put(KEY_STATUS_HISTORY, persisted == null ? List.of() : statusHistory(persisted));
        payload.put(KEY_META, metaPayload(
                trackingScreenForCreatedBooking(bookingType, persisted, assignmentStatus),
                trackingRefreshSeconds(String.valueOf(order.getStatus()), assignmentStatus),
                trackingPayload,
                permissions
        ));
        return payload;
    }

    private Map<String, Object> baseCreatedBookingPayload(LexhoDtos.BookingCreateRequest request,
                                                          BookingAddressSelection addressSelection,
                                                          BookingType bookingType,
                                                          OrderResponseDTO order) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", String.valueOf(order.getId()));
        payload.put(KEY_BOOKING_ID, String.valueOf(order.getId()));
        payload.put(KEY_CONTRACT_VERSION, BOOKING_CONTRACT_VERSION);
        payload.put(KEY_SERVICE_ID, String.valueOf(request.serviceId()));
        payload.put("addressId", addressSelection.responseAddressId());
        payload.put(KEY_BOOKING_TYPE, bookingType.name());
        payload.put(KEY_ASSIGNMENT_STATUS, defaultAssignmentStatus(bookingType));
        payload.put("date", request.date() == null ? "" : request.date());
        payload.put("slot", request.slot());
        payload.put("note", request.note());
        payload.put(KEY_STATUS, order.getStatus());
        payload.put("slaMinutes", bookingType == BookingType.INSTANT ? DispatchOrchestrationService.INSTANT_SLA_MINUTES : 0);
        return payload;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String limit(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private record BookingAddressSelection(
            LexhoDtos.AddressResponse address,
            Long dispatchAddressId,
            String responseAddressId,
            String fullAddress
    ) {
    }

    private String assignmentStatusForCreatedBooking(BookingType bookingType, Order persisted) {
        return persisted == null
                ? defaultAssignmentStatus(bookingType)
                : deriveAssignmentStatus(persisted, persisted.getAssignedPartnerId() != null);
    }

    private String defaultAssignmentStatus(BookingType bookingType) {
        return bookingType == BookingType.INSTANT ? STATUS_SEARCHING_PARTNER : STATUS_SCHEDULED;
    }

    private Map<String, Object> permissionsForCreatedBooking(BookingType bookingType,
                                                             Order persisted,
                                                             String assignmentStatus,
                                                             Map<String, Object> trackingPayload) {
        if (persisted != null) {
            return actionPermissions(
                    persisted,
                    assignmentStatus,
                    Boolean.TRUE.equals(trackingPayload.get(KEY_PARTNER_AVAILABLE))
            );
        }
        return Map.of(
                KEY_CAN_CANCEL, true,
                KEY_CAN_RESCHEDULE, bookingType == BookingType.SCHEDULED,
                KEY_CAN_CALL_PARTNER, false,
                KEY_CAN_TRACK_PARTNER, false,
                KEY_CAN_RAISE_ISSUE, false
        );
    }

    private String trackingScreenForCreatedBooking(BookingType bookingType, Order persisted, String assignmentStatus) {
        if (bookingType == BookingType.SCHEDULED
                && !Set.of(STATUS_PARTNER_ASSIGNED, STATUS_PARTNER_ON_THE_WAY, STATUS_ARRIVED,
                        STATUS_PARTNER_ARRIVED, STATUS_SERVICE_STARTED, STATUS_IN_PROGRESS)
                .contains(assignmentStatus == null ? "" : assignmentStatus.toUpperCase(Locale.ROOT))) {
            return TRACKING_SCREEN_SCHEDULED_STATUS;
        }
        if (persisted != null) {
            return trackingScreen(persisted, assignmentStatus);
        }
        return bookingType == BookingType.SCHEDULED ? TRACKING_SCREEN_SCHEDULED_STATUS : TRACKING_SCREEN_INSTANT_STATUS;
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
            payload.put(KEY_HTTP_STATUS, 400);
            payload.put(KEY_VALID, false);
            payload.put(KEY_REASON_CODE, "INVALID_DATE_FORMAT");
            payload.put(KEY_MESSAGE, "date is required");
            payload.put(KEY_SLOT_LOOKUP_RESULT, STATUS_NOT_FOUND);
            payload.put(KEY_SLOT_ROW, null);
            return ResponseEntity.ok(ApiResponse.success(MSG_SLOT_VALIDATION_DEBUG, payload));
        }

        LocalDate bookingDate;
        try {
            bookingDate = LocalDate.parse(request.date());
        } catch (Exception ex) {
            payload.put(KEY_HTTP_STATUS, 400);
            payload.put(KEY_VALID, false);
            payload.put(KEY_REASON_CODE, "INVALID_DATE_FORMAT");
            payload.put(KEY_MESSAGE, "Invalid date format. Use YYYY-MM-DD");
            payload.put(KEY_SLOT_LOOKUP_RESULT, STATUS_NOT_FOUND);
            payload.put(KEY_SLOT_ROW, null);
            return ResponseEntity.ok(ApiResponse.success(MSG_SLOT_VALIDATION_DEBUG, payload));
        }

        Map<String, Object> slotRow = customerCartSlotService.slotRowForDate(bookingDate, request.slot());
        payload.put(KEY_SLOT_LOOKUP_RESULT, slotRow == null ? STATUS_NOT_FOUND : "FOUND");
        payload.put(KEY_SLOT_ROW, slotRow);

        try {
            LocalDateTime parsedServerDateTime = parseScheduledAt(request.date(), request.slot());
            payload.put(KEY_HTTP_STATUS, 200);
            payload.put(KEY_VALID, true);
            payload.put(KEY_REASON_CODE, "OK");
            payload.put(KEY_MESSAGE, "Slot valid for booking");
            payload.put(KEY_PARSED_SERVER_DATE_TIME, parsedServerDateTime.toString());
        } catch (BookingValidationException ex) {
            payload.put(KEY_HTTP_STATUS, 400);
            payload.put(KEY_VALID, false);
            payload.put(KEY_REASON_CODE, ex.getErrorCode());
            payload.put(KEY_MESSAGE, ex.getMessage());
            payload.put(KEY_PARSED_SERVER_DATE_TIME, null);
        } catch (BadRequestException ex) {
            payload.put(KEY_HTTP_STATUS, 400);
            payload.put(KEY_VALID, false);
            payload.put(KEY_REASON_CODE, "BAD_REQUEST");
            payload.put(KEY_MESSAGE, ex.getMessage());
            payload.put(KEY_PARSED_SERVER_DATE_TIME, null);
        }

        log.info("[booking:slot:debug] {}", payload);
        return ResponseEntity.ok(ApiResponse.success(MSG_SLOT_VALIDATION_DEBUG, payload));
    }

    @GetMapping("/api/bookings/history")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bookingHistory(@RequestAttribute(ATTR_USER_ID) Long userId,
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
                .filter(order -> STATUS_SCHEDULED.equalsIgnoreCase(order.getBookingType())
                        || order.getBookingType() == null
                        || order.getBookingType().isBlank())
                .map(this::mapOrderSummary)
                .toList();
        List<Map<String, Object>> subscriptions = orders.stream()
                .filter(order -> TYPE_SUBSCRIPTION.equalsIgnoreCase(order.getBookingType()))
                .map(this::subscriptionSummaryPayload)
                .toList();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("instantBookings", pagedGroup(instantBookings, safePage, safeSize));
        payload.put("scheduledBookings", pagedGroup(scheduledBookings, safePage, safeSize));
        payload.put("subscriptions", pagedGroup(subscriptions, safePage, safeSize));
        return ResponseEntity.ok(ApiResponse.success("Booking history fetched", payload));
    }

    @GetMapping("/api/bookings/details/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bookingDetails(@RequestAttribute(ATTR_USER_ID) Long userId,
                                                                           @PathVariable Long id) {
        OrderResponseDTO order = orderService.getOrder(id, userId);
        Order persistedOrder = orderRepository.findById(id)
                .orElseThrow(() -> new BadRequestException(MSG_BOOKING_NOT_FOUND));
        if (!Objects.equals(persistedOrder.getUserId(), userId)) {
            throw new BadRequestException("You are not allowed to access this booking");
        }
        if (TYPE_SUBSCRIPTION.equalsIgnoreCase(persistedOrder.getBookingType())) {
            Map<String, Object> subscriptionPayload = subscriptionBookingPayload(userId, persistedOrder);
            com.lexho.user_service.util.CustomerServicePins.attach(subscriptionPayload, persistedOrder);
            return ResponseEntity.ok(ApiResponse.success(
                    "Booking details fetched successfully",
                    subscriptionPayload
            ));
        }

        Map<String, Object> payload = bookingDetailsPayload(userId, order, persistedOrder);
        com.lexho.user_service.util.CustomerServicePins.attach(payload, persistedOrder);
        return ResponseEntity.ok(ApiResponse.success("Booking details fetched successfully", payload));
    }

    private Map<String, Object> bookingDetailsPayload(Long userId, OrderResponseDTO order, Order persistedOrder) {
        double itemTotal = bookingItemTotal(order);
        double totalAmount = order.getTotalAmount() == null ? itemTotal : round2(order.getTotalAmount());
        double serviceFee = round2(Math.max(0d, totalAmount - itemTotal));
        double payableAmount = round2(itemTotal + serviceFee);
        Map<String, Object> payload = new LinkedHashMap<>();
        String bookingType = normalizedBookingType(persistedOrder);
        boolean hasAssignedPartner = persistedOrder.getAssignedPartnerId() != null;
        String assignmentStatus = deriveAssignmentStatus(persistedOrder, hasAssignedPartner);
        String status = responseStatus(persistedOrder, assignmentStatus);
        String trackingScreen = trackingScreen(persistedOrder, assignmentStatus);
        OrderItemDTO firstItem = firstOrderItem(order);
        Long serviceId = firstItem == null ? null : firstItem.getPartnerId();
        payload.put("id", String.valueOf(order.getId()));
        payload.put(KEY_BOOKING_ID, String.valueOf(order.getId()));
        payload.put(KEY_CONTRACT_VERSION, BOOKING_CONTRACT_VERSION);
        payload.put(KEY_SERVICE_ID, serviceId == null ? "" : String.valueOf(serviceId));
        payload.put(KEY_STATUS, status);
        payload.put(KEY_ASSIGNMENT_STATUS, assignmentStatus);
        payload.put(KEY_BOOKING_TYPE, bookingType);
        payload.put(KEY_MESSAGE, bookingMessage(status, assignmentStatus));
        Map<String, Object> partnerPayload = partnerSummary(persistedOrder, assignmentStatus);
        payload.put("partner", partnerPayload);
        payload.put("partnerId", partnerPayload == null ? "" : String.valueOf(partnerPayload.getOrDefault("id", "")));
        payload.put("partnerName", partnerPayload == null ? "" : String.valueOf(partnerPayload.getOrDefault("name", "")));
        payload.put("partnerPhone", partnerPayload == null ? "" : String.valueOf(partnerPayload.getOrDefault("phone", "")));
        payload.put("partnerRating", partnerPayload == null ? 0d : partnerPayload.getOrDefault(KEY_RATING, 0d));
        int etaMinutes = deriveEtaMinutes(status, assignmentStatus);
        payload.put(KEY_ETA_MINUTES, etaMinutes);
        payload.put("estimatedArrivalMinutes", etaMinutes);
        payload.put("estimatedCompletionTime", estimatedCompletionTime(persistedOrder));
        payload.put("slaMinutes", persistedOrder.getSlaMinutes() == null ? 0 : persistedOrder.getSlaMinutes());
        payload.put("promisedStartTime", persistedOrder.getPromisedStartTime() == null ? null : persistedOrder.getPromisedStartTime().toString());
        payload.put(KEY_SERVICE_NAME, serviceName(order));
        payload.put("address", order.getAddress());
        payload.put("fullAddress", order.getAddress());
        payload.put(KEY_CREATED_AT, utc(persistedOrder.getCreatedAt()));
        payload.put(KEY_SCHEDULED_AT, utc(persistedOrder.getScheduledAt()));
        boolean isCompleted = STATUS_COMPLETED.equalsIgnoreCase(status);
        payload.put("arrivedAt", utc(persistedOrder.getArrivedAt()));
        payload.put("startedAt", utc(persistedOrder.getStartedAt()));
        payload.put("inProgressAt", utc(persistedOrder.getInProgressAt()));
        payload.put("completedAt", utc(isCompleted ? firstNonNull(persistedOrder.getCompletedAt(), persistedOrder.getUpdatedAt()) : persistedOrder.getCompletedAt()));
        payload.put("completionStatus", isCompleted ? STATUS_COMPLETED : "NOT_COMPLETED_YET");
        payload.put("completedAtDisplay", isCompleted ? utc(firstNonNull(persistedOrder.getCompletedAt(), persistedOrder.getUpdatedAt())) : "Not completed yet");
        payload.put("completed", isCompleted ? utc(firstNonNull(persistedOrder.getCompletedAt(), persistedOrder.getUpdatedAt())) : "Not completed yet");
        payload.put(KEY_AMOUNT, payableAmount);
        payload.put("payableAmount", payableAmount);
        payload.put("currency", "INR");
        payload.put("itemTotal", itemTotal);
        payload.put("serviceFee", serviceFee);
        payload.put("gstAmount", 0d);
        payload.put("taxAmount", 0d);
        payload.put("discountAmount", 0d);
        payload.put(KEY_TOTAL_AMOUNT, payableAmount);
        payload.put("paymentStatus", order.getPaymentStatus());
        Map<String, Object> trackingPayload = mapTrackingPayload(persistedOrder, assignmentStatus);
        payload.putAll(rootTrackingPayload(trackingPayload));
        int trackingRefreshSeconds = trackingRefreshSeconds(status, assignmentStatus);
        boolean partnerAvailable = Boolean.TRUE.equals(trackingPayload.get(KEY_PARTNER_AVAILABLE));
        Map<String, Object> permissions = actionPermissions(persistedOrder, assignmentStatus, partnerAvailable);
        payload.put("retryPaymentAllowed", retryPaymentAllowed(persistedOrder));
        payload.put(KEY_STATUS_HISTORY, statusHistory(persistedOrder));
        Map<String, Object> meta = metaPayload(trackingScreen, trackingRefreshSeconds, trackingPayload, permissions);
        addReviewMeta(meta, persistedOrder);
        payload.put(KEY_META, meta);
        return payload;
    }

    private double bookingItemTotal(OrderResponseDTO order) {
        if (order.getItems() == null) {
            return 0d;
        }
        return round2(order.getItems().stream()
                .mapToDouble(this::lineTotal)
                .sum());
    }

    private double lineTotal(OrderItemDTO item) {
        double price = item.getPrice() == null ? 0d : item.getPrice();
        int quantity = item.getQuantity() == null || item.getQuantity() <= 0 ? 1 : item.getQuantity();
        return price * quantity;
    }

    private String normalizedBookingType(Order persistedOrder) {
        return persistedOrder.getBookingType() == null || persistedOrder.getBookingType().isBlank()
                ? STATUS_SCHEDULED
                : persistedOrder.getBookingType();
    }

    @PutMapping("/api/bookings/cancel/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cancelBooking(@RequestAttribute(ATTR_USER_ID) Long userId,
                                                                          @PathVariable Long id) {
        OrderResponseDTO order = orderService.cancelOrder(id, userId);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", String.valueOf(id));
        payload.put(KEY_BOOKING_ID, String.valueOf(id));
        payload.put(KEY_CONTRACT_VERSION, BOOKING_CONTRACT_VERSION);
        payload.put(KEY_STATUS, order.getStatus());
        payload.put(KEY_ASSIGNMENT_STATUS, STATUS_CANCELLED);
        payload.put(KEY_META, metaPayload(TRACKING_SCREEN_INSTANT_STATUS, 0, Map.of(KEY_SHOW_MAP, false), Map.of(
                KEY_CAN_CANCEL, false,
                KEY_CAN_RESCHEDULE, false,
                KEY_CAN_CALL_PARTNER, false,
                KEY_CAN_TRACK_PARTNER, false,
                KEY_CAN_RAISE_ISSUE, false
        )));
        audit(userId, null, id, "BOOKING_CANCELLED", null, String.valueOf(order.getStatus()));
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled", payload));
    }

    @Transactional
    @PutMapping("/api/bookings/reschedule/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> rescheduleBooking(@RequestAttribute(ATTR_USER_ID) Long userId,
                                                                              @PathVariable Long id,
                                                                              @Valid @RequestBody LexhoDtos.RescheduleRequest request) {
        LocalDateTime requestedAt = parseDateTime(request.date());
        if (requestedAt == null) {
            throw new BadRequestException("date is required");
        }
        String date = requestedAt.toLocalDate().toString();
        String time = requestedAt.toLocalTime().toString().substring(0, 5);
        Object rawRows = customerCartSlotService.slotsForDate(requestedAt.toLocalDate()).get("slots");
        if (!(rawRows instanceof List<?> rows)) {
            throw new BookingValidationException("SLOT_NOT_LISTED", "Selected slot is not available for requested date.");
        }
        String slot = rows.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .filter(row -> time.equals(String.valueOf(row.get("startTime"))))
                .map(row -> String.valueOf(row.get("slot")))
                .findFirst()
                .orElseThrow(() -> new BookingValidationException(
                        "SLOT_NOT_LISTED", "Selected slot is not available for requested date."));
        return applyBookingReschedule(userId, id, date, slot);
    }

    @Transactional
    @PostMapping("/api/bookings/{id}/reschedule")
    public ResponseEntity<ApiResponse<Map<String, Object>>> rescheduleBookingV2(@RequestAttribute(ATTR_USER_ID) Long userId,
                                                                                @PathVariable Long id,
                                                                                @Valid @RequestBody LexhoDtos.RescheduleSlotRequest request) {
        return applyBookingReschedule(userId, id, request.date(), request.slot());
    }

    private ResponseEntity<ApiResponse<Map<String, Object>>> applyBookingReschedule(Long userId,
                                                                                     Long id,
                                                                                     String date,
                                                                                     String slot) {
        customerCartSlotService.lockSlotCapacity();
        Order order = orderRepository.findByIdForUpdate(id).orElseThrow(() -> new BadRequestException(MSG_BOOKING_NOT_FOUND));
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new BadRequestException("You are not allowed to reschedule this booking");
        }
        if (!canReschedule(order) || order.getPaymentStatus() != PaymentStatus.SUCCESS
                || order.getAssignedPartnerId() != null) {
            throw new BadRequestException("Booking cannot be rescheduled in current state");
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(KEY_BOOKING_ID, id);
        payload.put(KEY_CONTRACT_VERSION, BOOKING_CONTRACT_VERSION);
        LocalDateTime scheduledAt = parseScheduledAt(date, slot);
        if (scheduledAt.equals(order.getScheduledAt())) {
            payload.put(KEY_SCHEDULED_AT, utc(scheduledAt));
            payload.put(KEY_META, metaPayload(TRACKING_SCREEN_SCHEDULED_STATUS, 15, Map.of(KEY_SHOW_MAP, false), actionPermissions(order, STATUS_SCHEDULED, false)));
            return ResponseEntity.ok(ApiResponse.success("Booking already has the selected slot", payload));
        }
        String oldValue = order.getScheduledAt() == null ? null : utc(order.getScheduledAt());
        order.setScheduledAt(scheduledAt);
        Order saved = orderRepository.save(order);
        payload.put(KEY_SCHEDULED_AT, utc(scheduledAt));
        payload.put(KEY_META, metaPayload(TRACKING_SCREEN_SCHEDULED_STATUS, 15, Map.of(KEY_SHOW_MAP, false), actionPermissions(saved, STATUS_SCHEDULED, false)));
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
                        KEY_AVAILABLE, false,
                        KEY_ETA_MINUTES, 0,
                        KEY_INSTANT_ELIGIBLE, false
                );
            }
            Map<String, Object> item = new LinkedHashMap<>();
            Object slotValue = map.containsKey("slot") ? map.get("slot") : "";
            item.put("slot", String.valueOf(slotValue));
            item.put(KEY_AVAILABLE, Boolean.TRUE.equals(map.get(KEY_AVAILABLE)));
            Object etaMinutes = map.get(KEY_ETA_MINUTES);
            item.put(KEY_ETA_MINUTES, etaMinutes instanceof Number ? ((Number) etaMinutes).intValue() : 0);
            item.put(KEY_INSTANT_ELIGIBLE, Boolean.TRUE.equals(map.get(KEY_INSTANT_ELIGIBLE))
                    || Boolean.TRUE.equals(map.get("isInstantEligible")));
            return item;
        }).toList();
        return ResponseEntity.ok(payload);
    }

    @GetMapping("/api/partner/live-location/{bookingId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> liveLocation(@RequestAttribute(ATTR_USER_ID) Long userId,
                                                                         @PathVariable Long bookingId) {
        Order order = orderRepository.findById(bookingId).orElseThrow(() -> new BadRequestException(MSG_BOOKING_NOT_FOUND));
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new BadRequestException("You are not allowed to access this booking");
        }
        return ResponseEntity.ok(ApiResponse.success("Partner live location", Map.of("lat", 0.0, "lng", 0.0, "eta", "N/A")));
    }

    @PutMapping("/api/partner/update-location")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateLocation(@RequestAttribute(ATTR_USER_ID) Long partnerId,
                                                                           @RequestParam Long bookingId,
                                                                           @RequestParam Double latitude,
                                                                           @RequestParam Double longitude) {
        Order order = orderRepository.findById(bookingId)
                .orElseThrow(() -> new BadRequestException(MSG_BOOKING_NOT_FOUND));
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

        return ResponseEntity.ok(ApiResponse.success("Partner location updated", Map.of(KEY_BOOKING_ID, bookingId, "lat", latitude, "lng", longitude)));
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
    public ResponseEntity<ApiResponse<Map<String, Object>>> initiatePayment(@RequestAttribute(ATTR_USER_ID) Long userId,
                                                                            @Valid @RequestBody LexhoDtos.PaymentInitiateRequest request) {
        Long resolvedBookingId = resolveBookingId(request.bookingId());
        if (resolvedBookingId == null) {
            throw new BadRequestException("bookingId is required");
        }
        Order order = orderRepository.findByIdForUpdate(resolvedBookingId).orElseThrow(() -> new BadRequestException(MSG_BOOKING_NOT_FOUND));
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new BadRequestException("You are not allowed to initiate payment for this booking");
        }
        String mode = normalizeMode(request.mode());
        PaymentStatus status = PaymentStatus.PENDING;
        order.setPaymentStatus(status);
        orderRepository.save(order);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put(KEY_BOOKING_ID, String.valueOf(order.getId()));
        data.put("mode", mode);
        data.put(KEY_AMOUNT, request.amount() != null ? request.amount() : order.getTotalAmount());
        data.put(KEY_STATUS, status.name());
        data.put(KEY_MESSAGE, "Payment pending");
        if ("UPI".equalsIgnoreCase(mode) || "CARD".equalsIgnoreCase(mode)) {
            Map<String, Object> razorpayOptions = new LinkedHashMap<>();
            razorpayOptions.put(KEY_AMOUNT, amountInPaise(request.amount(), order.getTotalAmount()));
            razorpayOptions.put("currency", "INR");
            razorpayOptions.put("name", "Lexho");
            razorpayOptions.put(KEY_DESCRIPTION, "Service payment");
            razorpayOptions.put("theme", Map.of("color", "#16A34A"));
            data.put("razorpayOptions", razorpayOptions);
        }
        return ResponseEntity.ok(ApiResponse.success("Payment initiated", data));
    }

    @GetMapping("/api/payments/status/{bookingId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> paymentStatus(@RequestAttribute(ATTR_USER_ID) Long userId,
                                                                          @PathVariable String bookingId) {
        Long resolvedBookingId = resolveBookingId(bookingId);
        if (resolvedBookingId == null) {
            throw new BadRequestException("Invalid bookingId");
        }
        Order order = orderRepository.findById(resolvedBookingId).orElseThrow(() -> new BadRequestException(MSG_BOOKING_NOT_FOUND));
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new BadRequestException("You are not allowed to access this booking");
        }
        PaymentStatus status = order.getPaymentStatus() == null ? PaymentStatus.PENDING : order.getPaymentStatus();
        return ResponseEntity.ok(ApiResponse.success("Payment status fetched", Map.of(
                KEY_BOOKING_ID, String.valueOf(resolvedBookingId),
                KEY_STATUS, status.name(),
                KEY_MESSAGE, status == PaymentStatus.SUCCESS ? "Payment successful" : status == PaymentStatus.FAILED ? "Payment failed" : "Payment pending"
        )));
    }

    @PostMapping("/api/payments/refund")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refund(@RequestAttribute(ATTR_USER_ID) Long userId,
                                                                   @Valid @RequestBody LexhoDtos.PaymentRefundRequest request) {
        Long resolvedBookingId = resolveBookingId(request.bookingId());
        if (resolvedBookingId == null) {
            throw new BadRequestException("Invalid bookingId");
        }
        Order order = orderRepository.findById(resolvedBookingId).orElseThrow(() -> new BadRequestException(MSG_BOOKING_NOT_FOUND));
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new BadRequestException("You are not allowed to refund this booking");
        }
        order.setPaymentStatus(PaymentStatus.FAILED);
        orderRepository.save(order);
        return ResponseEntity.ok(ApiResponse.success("Refund requested", Map.of(
                KEY_BOOKING_ID, String.valueOf(resolvedBookingId),
                KEY_AMOUNT, request.amount(),
                KEY_STATUS, "INITIATED"
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
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        List<Map<String, Object>> allCoupons = new ArrayList<>();
        Set<String> seenCodes = new HashSet<>();
        bannerPromotionRepository.findByStatusAndDeletedFalse(BannerStatus.ACTIVE).stream()
                .filter(item -> item.getCouponCode() != null && !item.getCouponCode().isBlank())
                .filter(item -> item.getEndDate() == null || item.getEndDate().isAfter(now))
                .forEach(item -> {
                    seenCodes.add(item.getCouponCode().trim().toUpperCase(Locale.ROOT));
                    allCoupons.add(Map.<String, Object>of(
                        "id", String.valueOf(item.getId()),
                        "code", item.getCouponCode(),
                        KEY_DESCRIPTION, item.getDescription() == null ? "" : item.getDescription(),
                        "discount", item.getDiscountPercentage() == null ? 0 : item.getDiscountPercentage()
                    ));
                });
        couponRepository.ifPresent(repository -> repository
                .findCustomerVisibleCandidates(null, now, Sort.by(Sort.Order.desc("id")))
                .stream()
                .filter(coupon -> seenCodes.add(coupon.getCode().trim().toUpperCase(Locale.ROOT)))
                .map(this::legacyCouponRow)
                .forEach(allCoupons::add));
        int fromIndex = Math.min(safePage * safeSize, allCoupons.size());
        int toIndex = Math.min(fromIndex + safeSize, allCoupons.size());
        return ResponseEntity.ok(ApiResponse.success("Coupons fetched",
                Map.of(KEY_DATA, allCoupons.subList(fromIndex, toIndex))));
    }

    private Map<String, Object> legacyCouponRow(Coupon coupon) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", String.valueOf(coupon.getId()));
        row.put("code", coupon.getCode());
        row.put("title", coupon.getTitle() == null ? "" : coupon.getTitle());
        row.put(KEY_DESCRIPTION, coupon.getDescription() == null ? "" : coupon.getDescription());
        row.put("discount", coupon.getDiscountValue() == null ? BigDecimal.ZERO : coupon.getDiscountValue());
        row.put("discountType", coupon.getDiscountType() == null ? CouponDiscountType.FIXED : coupon.getDiscountType());
        row.put("discountValue", coupon.getDiscountValue() == null ? BigDecimal.ZERO : coupon.getDiscountValue());
        row.put("maxDiscountAmount", coupon.getMaxDiscountAmount());
        row.put("minimumPurchaseAmount",
                coupon.getMinimumPurchaseAmount() == null ? BigDecimal.ZERO : coupon.getMinimumPurchaseAmount());
        row.put("firstBookingOnly", coupon.isFirstBookingOnly());
        return row;
    }

    @PostMapping("/api/reviews/add")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addReview(@RequestAttribute(ATTR_USER_ID) Long userId,
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
                KEY_SERVICE_ID, String.valueOf(saved.getServiceId()),
                KEY_RATING, saved.getRating(),
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
                        KEY_RATING, review.getRating(),
                        "comment", review.getReviewComment() == null ? "" : review.getReviewComment(),
                        KEY_SERVICE_NAME, "Service-" + id,
                        KEY_CREATED_AT, String.valueOf(review.getCreatedAt())
                ))
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Service reviews", Map.of(KEY_DATA, data)));
    }

    @PostMapping("/api/favorites/add")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addFavorite(@RequestAttribute(ATTR_USER_ID) Long userId,
                                                                        @Valid @RequestBody LexhoDtos.FavoriteAddRequest request) {
        if (!favoriteRepository.existsByUserIdAndServiceId(userId, request.serviceId())) {
            LexhoFavorite favorite = new LexhoFavorite();
            favorite.setUserId(userId);
            favorite.setServiceId(request.serviceId());
            favoriteRepository.save(favorite);
        }
        return ResponseEntity.ok(ApiResponse.success("Favorite added", Map.of(KEY_SERVICE_ID, String.valueOf(request.serviceId()))));
    }

    @GetMapping("/api/favorites/list")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listFavorites(@RequestAttribute(ATTR_USER_ID) Long userId,
                                                                          @RequestParam(defaultValue = "0") int page,
                                                                          @RequestParam(defaultValue = "20") int size) {
        List<Map<String, Object>> data = favoriteRepository.findByUserIdOrderByIdDesc(userId).stream()
                .skip((long) page * size)
                .limit(size)
                .map(item -> {
                    Partner partner = partnerRepository.findById(item.getServiceId()).orElse(null);
                    return Map.<String, Object>of(
                            "id", String.valueOf(item.getId()),
                            KEY_SERVICE_ID, String.valueOf(item.getServiceId()),
                            "name", partner == null ? "" : partner.getName()
                    );
                })
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Favorites fetched", Map.of(KEY_DATA, data)));
    }

    @DeleteMapping("/api/favorites/remove")
    public ResponseEntity<ApiResponse<Void>> removeFavorite(@RequestAttribute(ATTR_USER_ID) Long userId,
                                                            @Valid @RequestBody LexhoDtos.IdRequest request) {
        LexhoFavorite favorite = favoriteRepository.findByIdAndUserId(request.id(), userId)
                .orElseThrow(() -> new BadRequestException("Favorite not found"));
        favoriteRepository.delete(favorite);
        return ResponseEntity.ok(ApiResponse.success("Favorite removed", null));
    }

    @GetMapping("/api/notifications")
    public ResponseEntity<ApiResponse<Map<String, Object>>> notifications(@RequestAttribute(ATTR_USER_ID) Long userId,
                                                                          @RequestParam(defaultValue = "0") int page,
                                                                          @RequestParam(defaultValue = "20") int size) {
        NotificationPageResponse<NotificationResponse> notifications =
                notificationManagementService.getCustomerNotifications(page, size);
        List<Map<String, Object>> data = notifications.getContent().stream()
                .map(this::toNotificationMap)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Notifications fetched successfully", Map.of(
                KEY_DATA, data,
                "totalElements", notifications.getTotalElements(),
                "totalPages", notifications.getTotalPages(),
                "pageNumber", notifications.getPageNumber()
        )));
    }

    @PutMapping("/api/notifications/read")
    public ResponseEntity<ApiResponse<Void>> markNotificationRead(@RequestAttribute(ATTR_USER_ID) Long userId,
                                                                  @Valid @RequestBody LexhoDtos.IdRequest request) {
        notificationManagementService.markAsReadForCurrentUser(request.id());
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", null));
    }

    @DeleteMapping("/api/notifications/read")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(@RequestAttribute(ATTR_USER_ID) Long userId,
                                                                @Valid @RequestBody LexhoDtos.IdRequest request) {
        notificationManagementService.softDeleteForCurrentUser(request.id());
        return ResponseEntity.ok(ApiResponse.success("Notification deleted", null));
    }

    @GetMapping("/api/customer/notifications/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Object>>> unreadCount(@RequestAttribute(ATTR_USER_ID) Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Unread count", Map.of(
                "count", notificationManagementService.unreadCountForCurrentUser()
        )));
    }

    @GetMapping("/api/customer/notifications/stream")
    public SseEmitter customerNotificationStream(@RequestAttribute(ATTR_USER_ID) Long userId) {
        if (notificationRealtimeService == null) {
            throw new BadRequestException("Notification realtime stream is not available");
        }
        return notificationRealtimeService.subscribe(userId);
    }

    @GetMapping("/api/customer/inbox/activity")
    public ResponseEntity<Map<String, Object>> customerActivityFeed(@RequestAttribute(ATTR_USER_ID) Long userId,
                                                                    @RequestParam(defaultValue = "1") int page,
                                                                    @RequestParam(defaultValue = "20") int limit) {
        if (customerActivityFeedService == null) {
            throw new BadRequestException("Customer activity feed is not available");
        }
        Map<String, Object> payload = customerActivityFeedService.activity(userId, page, limit);
        return ResponseEntity.ok(Map.of(
                KEY_SUCCESS, true,
                KEY_MESSAGE, "Customer activity feed",
                KEY_DATA, payload.get(KEY_DATA),
                KEY_META, payload.get(KEY_META)
        ));
    }

    @PostMapping("/api/referral/generate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateReferral(@RequestAttribute(ATTR_USER_ID) Long userId) {
        if (referralService == null) {
            LexhoReferralCode code = referralCodeRepository.findByUserId(userId).orElseGet(() -> {
                LexhoReferralCode created = new LexhoReferralCode();
                created.setUserId(userId);
                created.setCode(generateUniqueReferralCode(userId));
                created.setUsedCount(0);
                return referralCodeRepository.save(created);
            });
            return ResponseEntity.ok(ApiResponse.success("Referral generated", Map.of("code", code.getCode())));
        }
        return ResponseEntity.ok(ApiResponse.success("Referral generated",
                Map.of("code", referralService.generateCode(userId).code())));
    }

    @PostMapping("/api/referral/apply")
    public ResponseEntity<ApiResponse<Map<String, Object>>> applyReferral(@RequestAttribute(ATTR_USER_ID) Long userId,
                                                                          @Valid @RequestBody LexhoDtos.ReferralApplyRequest request) {
        if (referralService == null) {
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
            use.setReferrerUserId(referralCode.getUserId());
            use.setCode(referralCode.getCode());
            referralUseRepository.save(use);
            referralCode.setUsedCount(referralCode.getUsedCount() + 1);
            referralCodeRepository.save(referralCode);
            return ResponseEntity.ok(ApiResponse.success("Referral applied", Map.of("code", request.code())));
        }
        referralService.apply(userId, new ReferralApplyRequest(request.code()));
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
                            KEY_DISPLAY_ORDER, category.getDisplayOrder(),
                            "faqs", faqs
                    );
                })
                .toList();
        return ResponseEntity.ok(Map.of(
                KEY_SUCCESS, true,
                KEY_MESSAGE, "FAQs fetched",
                KEY_DATA, data
        ));
    }

    @PostMapping("/api/support/create")
    public ResponseEntity<Map<String, Object>> createTicket(@RequestAttribute(ATTR_USER_ID) Long userId,
                                                            @Valid @RequestBody LexhoDtos.SupportCreateRequest request) {
        String normalizedBookingId = normalizeSupportBookingId(userId, request.bookingId());
        SupportTicket ticket = new SupportTicket();
        ticket.setUserId(userId);
        ticket.setSubject(request.subject());
        ticket.setMessage(request.message());
        ticket.setStatus("OPEN");
        ticket.setCategoryId(blankToNull(request.categoryId()));
        ticket.setBookingId(normalizedBookingId);
        ticket.setPriority(request.priority() == null || request.priority().isBlank() ? SUPPORT_PRIORITY_MEDIUM : request.priority().trim().toUpperCase(Locale.ROOT));
        ticket.setAttachmentUrls(serializeAttachmentUrls(request.attachmentUrls()));
        SupportTicket saved = supportTicketRepository.save(ticket);
        publishSupportEvent(saved, "ticket-created", Map.of(
                "id", formatTicketId(saved.getId()),
                KEY_STATUS, saved.getStatus(),
                KEY_UPDATED_AT, String.valueOf(saved.getUpdatedAt())
        ));
        return ResponseEntity.ok(Map.of(
                KEY_SUCCESS, true,
                KEY_MESSAGE, "Ticket created",
                KEY_DATA, Map.of(
                "id", formatTicketId(saved.getId()),
                KEY_SUBJECT, saved.getSubject(),
                KEY_MESSAGE, saved.getMessage(),
                KEY_STATUS, saved.getStatus(),
                KEY_CATEGORY_ID, saved.getCategoryId() == null ? "" : saved.getCategoryId(),
                KEY_BOOKING_ID, saved.getBookingId() == null ? "" : saved.getBookingId(),
                KEY_PRIORITY, saved.getPriority() == null ? SUPPORT_PRIORITY_MEDIUM : saved.getPriority(),
                KEY_ATTACHMENT_URLS, deserializeAttachmentUrls(saved.getAttachmentUrls()),
                KEY_CREATED_AT, String.valueOf(saved.getCreatedAt())
        )));
    }

    @PostMapping("/api/support/tickets/{ticketId}/reply")
    public ResponseEntity<Map<String, Object>> replySupportTicket(@RequestAttribute(ATTR_USER_ID) Long userId,
                                                                  @PathVariable String ticketId,
                                                                  @Valid @RequestBody LexhoDtos.SupportReplyRequest request) {
        SupportTicket ticket = requireCustomerTicket(userId, ticketId);
        if (STATUS_CLOSED.equalsIgnoreCase(ticket.getStatus())) {
            throw new BadRequestException("Closed tickets cannot be replied to. Reopen the ticket first.");
        }
        SupportTicketReply reply = new SupportTicketReply();
        reply.setTicket(ticket);
        reply.setAdminId(null);
        reply.setSenderType(USER_TYPE_CUSTOMER);
        reply.setMessage(request.message().trim());
        reply.setAttachmentUrls(serializeAttachmentUrls(request.attachmentUrls()));
        supportTicketReplyRepository.save(reply);
        if ("RESOLVED".equalsIgnoreCase(ticket.getStatus())) {
            ticket.setStatus("OPEN");
            supportTicketRepository.save(ticket);
        }
        publishSupportEvent(ticket, "ticket-message", Map.of(
                "ticketId", formatTicketId(ticket.getId()),
                KEY_STATUS, ticket.getStatus(),
                KEY_MESSAGE, Map.of(
                        KEY_SENDER, USER_TYPE_CUSTOMER,
                        "text", reply.getMessage(),
                        KEY_ATTACHMENT_URLS, deserializeAttachmentUrls(reply.getAttachmentUrls()),
                        KEY_CREATED_AT, String.valueOf(reply.getCreatedAt())
                )
        ));
        return ResponseEntity.ok(Map.of(
                KEY_SUCCESS, true,
                KEY_MESSAGE, "Reply added",
                KEY_DATA, Map.of(
                        "ticketId", formatTicketId(ticket.getId()),
                        KEY_STATUS, ticket.getStatus(),
                        KEY_MESSAGE, Map.of(
                                KEY_SENDER, USER_TYPE_CUSTOMER,
                                "text", reply.getMessage(),
                                KEY_ATTACHMENT_URLS, deserializeAttachmentUrls(reply.getAttachmentUrls()),
                                KEY_CREATED_AT, String.valueOf(reply.getCreatedAt())
                        )
                )
        ));
    }

    @PatchMapping("/api/support/tickets/{ticketId}/status")
    public ResponseEntity<Map<String, Object>> updateSupportTicketStatus(@RequestAttribute(ATTR_USER_ID) Long userId,
                                                                         @PathVariable String ticketId,
                                                                         @Valid @RequestBody LexhoDtos.SupportStatusUpdateRequest request) {
        SupportTicket ticket = requireCustomerTicket(userId, ticketId);
        String normalized = request.status().trim().toUpperCase(Locale.ROOT);
        if (STATUS_CLOSED.equals(normalized)) {
            if (STATUS_CLOSED.equalsIgnoreCase(ticket.getStatus())) {
                throw new BadRequestException("Ticket is already closed");
            }
            ticket.setStatus(STATUS_CLOSED);
        } else {
            if (!Set.of(STATUS_CLOSED, "RESOLVED").contains(ticket.getStatus() == null ? "" : ticket.getStatus().toUpperCase(Locale.ROOT))) {
              throw new BadRequestException("Only closed or resolved tickets can be reopened");
            }
            ticket.setStatus("OPEN");
        }
        SupportTicket saved = supportTicketRepository.save(ticket);
        publishSupportEvent(saved, "ticket-status", Map.of(
                "id", formatTicketId(saved.getId()),
                KEY_STATUS, saved.getStatus(),
                KEY_UPDATED_AT, String.valueOf(saved.getUpdatedAt())
        ));
        return ResponseEntity.ok(Map.of(
                KEY_SUCCESS, true,
                KEY_MESSAGE, "Ticket status updated",
                KEY_DATA, Map.of(
                        "id", formatTicketId(saved.getId()),
                        KEY_STATUS, saved.getStatus(),
                        KEY_UPDATED_AT, String.valueOf(saved.getUpdatedAt())
                )
        ));
    }

    @GetMapping("/api/support/history")
    public ResponseEntity<Map<String, Object>> supportHistory(@RequestAttribute(ATTR_USER_ID) Long userId,
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
                        KEY_SUBJECT, item.getSubject(),
                        KEY_MESSAGE, item.getMessage(),
                        KEY_STATUS, item.getStatus(),
                        KEY_CATEGORY_ID, item.getCategoryId() == null ? "" : item.getCategoryId(),
                        KEY_BOOKING_ID, item.getBookingId() == null ? "" : item.getBookingId(),
                        KEY_PRIORITY, item.getPriority() == null ? SUPPORT_PRIORITY_MEDIUM : item.getPriority(),
                        KEY_ATTACHMENT_URLS, deserializeAttachmentUrls(item.getAttachmentUrls()),
                        KEY_CREATED_AT, String.valueOf(item.getCreatedAt()),
                        KEY_UPDATED_AT, String.valueOf(item.getUpdatedAt())
                ))
                .toList();
        return ResponseEntity.ok(Map.of(
                KEY_SUCCESS, true,
                KEY_MESSAGE, "Ticket history",
                KEY_DATA, data,
                KEY_META, Map.of(
                        "page", safePage,
                        "limit", safeLimit,
                        "total", ticketPage.getTotalElements()
                )
        ));
    }

    @GetMapping("/api/support/tickets/{ticketId}")
    public ResponseEntity<Map<String, Object>> supportTicketDetail(@RequestAttribute(ATTR_USER_ID) Long userId,
                                                                   @PathVariable String ticketId) {
        Long parsedId = parseTicketId(ticketId);
        SupportTicket ticket = supportTicketRepository.findByUserIdAndId(userId, parsedId)
                .orElseThrow(() -> new BadRequestException("Ticket not found"));
        List<SupportTicketReply> replies = supportTicketReplyRepository.findByTicket_IdOrderByCreatedAtAsc(ticket.getId());

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of(
                "id", "MSG-1",
                KEY_SENDER, USER_TYPE_CUSTOMER,
                KEY_MESSAGE, ticket.getMessage(),
                KEY_ATTACHMENT_URLS, deserializeAttachmentUrls(ticket.getAttachmentUrls()),
                KEY_CREATED_AT, String.valueOf(ticket.getCreatedAt())
        ));
        for (int i = 0; i < replies.size(); i++) {
            SupportTicketReply reply = replies.get(i);
            messages.add(Map.of(
                    "id", "MSG-" + (i + 2),
                    KEY_SENDER, supportSender(reply),
                    KEY_MESSAGE, reply.getMessage(),
                    KEY_ATTACHMENT_URLS, deserializeAttachmentUrls(reply.getAttachmentUrls()),
                    KEY_CREATED_AT, String.valueOf(reply.getCreatedAt())
            ));
        }

        return ResponseEntity.ok(Map.of(
                KEY_SUCCESS, true,
                KEY_DATA, Map.of(
                        "id", formatTicketId(ticket.getId()),
                        KEY_SUBJECT, ticket.getSubject(),
                        KEY_STATUS, ticket.getStatus(),
                        KEY_BOOKING_ID, ticket.getBookingId() == null ? "" : ticket.getBookingId(),
                        KEY_PRIORITY, ticket.getPriority() == null ? SUPPORT_PRIORITY_MEDIUM : ticket.getPriority(),
                        "messages", messages
                )
        ));
    }

    @GetMapping("/api/support/tickets/{ticketId}/stream")
    public SseEmitter supportTicketStream(@RequestAttribute(ATTR_USER_ID) Long userId,
                                          @PathVariable String ticketId) {
        SupportTicket ticket = requireCustomerTicket(userId, ticketId);
        if (supportRealtimeService == null) {
            throw new BadRequestException("Support realtime stream is not available");
        }
        return supportRealtimeService.subscribe(USER_TYPE_CUSTOMER, userId, ticket.getId());
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
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        List<Map<String, Object>> adminServices = adminServiceEntityRepository
                .searchActiveCatalogForCustomer(keyword == null ? null : keyword.trim(), PageRequest.of(safePage, safeSize))
                .stream()
                .map(this::mapAdminService)
                .toList();
        if (!adminServices.isEmpty()) {
            return adminServices;
        }
        return partnerRepository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getAvailable()))
                .filter(p -> normalizedKeyword == null || normalizedKeyword.isBlank() ||
                        (p.getName() != null && p.getName().toLowerCase(Locale.ROOT).contains(normalizedKeyword)))
                .sorted(Comparator.comparing(Partner::getRating, Comparator.nullsLast(Comparator.reverseOrder())))
                .skip((long) safePage * safeSize)
                .limit(safeSize)
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
        String imageUrl = mediaUrlResolver.toPublicUrl(p.getImage());
        return Map.of(
                "id", String.valueOf(p.getId()),
                "name", p.getName() == null ? "" : p.getName(),
                KEY_CATEGORY_ID, p.getCategoryId() == null ? "" : String.valueOf(p.getCategoryId()),
                KEY_DESCRIPTION, "",
                KEY_PRICE, p.getPrice() == null ? 0 : p.getPrice(),
                "durationMinutes", 60,
                KEY_IMAGE_URL, imageUrl == null ? "" : imageUrl
        );
    }

    private Map<String, Object> mapAdminService(AdminServiceEntity service) {
        String imageUrl = mediaUrlResolver.toPublicUrl(service.getImageUrl());
        BigDecimal price = service.getDiscountedPrice() != null && service.getDiscountedPrice().signum() > 0
                ? service.getDiscountedPrice()
                : service.getBasePrice();
        return Map.of(
                "id", String.valueOf(service.getId()),
                KEY_SERVICE_ID, String.valueOf(service.getId()),
                "name", service.getName() == null ? "" : service.getName(),
                KEY_SERVICE_NAME, service.getName() == null ? "" : service.getName(),
                KEY_CATEGORY_ID, service.getCategory() == null || service.getCategory().getId() == null
                        ? ""
                        : String.valueOf(service.getCategory().getId()),
                KEY_DESCRIPTION, service.getDescription() == null ? "" : service.getDescription(),
                KEY_PRICE, price == null ? BigDecimal.ZERO : price,
                "durationMinutes", service.getDurationMinutes() == null ? 60 : service.getDurationMinutes(),
                KEY_IMAGE_URL, imageUrl == null ? "" : imageUrl
        );
    }

    private Map<String, Object> mapCategory(ServiceCategory category) {
        return Map.of(
                "id", String.valueOf(category.getId()),
                "name", category.getName() == null ? "" : category.getName(),
                "icon", category.getIcon() == null ? "" : category.getIcon(),
                KEY_IMAGE_URL, ""
        );
    }

    private Map<String, Object> mapCategory(AdminCategoryEntity category) {
        String imageUrl = mediaUrlResolver.toPublicUrl(category.getIconUrl());
        String safeImageUrl = imageUrl == null ? "" : imageUrl;
        return Map.of(
                "id", String.valueOf(category.getId()),
                "name", category.getName() == null ? "" : category.getName(),
                "icon", safeImageUrl,
                KEY_IMAGE_URL, safeImageUrl,
                KEY_DISPLAY_ORDER, category.getDisplayOrder()
        );
    }

    private Map<String, Object> mapBanner(BannerPromotion banner) {
        String imageUrl = mediaUrlResolver.toPublicUrl(banner.getBannerImageUrl());
        String safeImageUrl = imageUrl == null ? "" : imageUrl;
        return Map.of(
                "id", String.valueOf(banner.getId()),
                "title", banner.getTitle() == null ? "" : banner.getTitle(),
                KEY_IMAGE_URL, safeImageUrl,
                "placement", banner.getPlacement() == null ? "TOP" : banner.getPlacement().name(),
                "slotNumber", banner.getSlotNumber() == null ? 0 : banner.getSlotNumber(),
                "redirectUrl", "",
                "promoCode", banner.getCouponCode() == null ? "" : banner.getCouponCode()
        );
    }

    private Map<String, Object> mapCartItem(CartResponseDTO item) {
        return Map.of(
                "id", String.valueOf(item.getId()),
                KEY_SERVICE_ID, String.valueOf(item.getPartnerId()),
                "name", item.getPartnerName() == null ? "" : item.getPartnerName(),
                KEY_IMAGE_URL, item.getImageUrl() == null ? "" : item.getImageUrl(),
                "quantity", item.getQuantity(),
                KEY_PRICE, item.getPrice() == null ? 0 : item.getPrice()
        );
    }

    private Map<String, Object> mapOrderSummary(Order order) {
        if (TYPE_SUBSCRIPTION.equalsIgnoreCase(order.getBookingType())) {
            return subscriptionSummaryPayload(order);
        }
        // Keep history resilient even when order items are lazily unavailable.
        String serviceName = "Service";
        String assignmentStatus = deriveAssignmentStatus(order, order.getAssignedPartnerId() != null);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", String.valueOf(order.getId()));
        payload.put(KEY_BOOKING_ID, String.valueOf(order.getId()));
        payload.put(KEY_CONTRACT_VERSION, BOOKING_CONTRACT_VERSION);
        payload.put(KEY_STATUS, responseStatus(order, assignmentStatus));
        payload.put(KEY_ASSIGNMENT_STATUS, assignmentStatus);
        payload.put(KEY_BOOKING_TYPE, order.getBookingType());
        payload.put(KEY_SERVICE_NAME, serviceName);
        payload.put(KEY_CREATED_AT, utc(order.getCreatedAt()));
        Map<String, Object> trackingPayload = mapTrackingPayload(order, assignmentStatus);
        Map<String, Object> permissions = actionPermissions(order, assignmentStatus, Boolean.TRUE.equals(trackingPayload.get(KEY_PARTNER_AVAILABLE)));
        Map<String, Object> meta = metaPayload(
                trackingScreen(order, assignmentStatus),
                trackingRefreshSeconds(responseStatus(order, assignmentStatus), assignmentStatus),
                trackingPayload,
                permissions
        );
        addReviewMeta(meta, order);
        payload.put(KEY_META, meta);
        return payload;
    }

    private Map<String, Object> pagedGroup(List<Map<String, Object>> items, int page, int size) {
        int fromIndex = Math.min((page - 1) * size, items.size());
        int toIndex = Math.min(fromIndex + size, items.size());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(KEY_ITEMS, items.subList(fromIndex, toIndex));
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
        populateSubscriptionPayload(payload, order, subscription, frequencyDays);
        payload.put("nextServiceDate", nextServiceDate);
        payload.put(KEY_STATUS, subscription.getStatus().name());
        payload.put("paymentStatus", order.getPaymentStatus() == null ? "PENDING" : order.getPaymentStatus().name());
        payload.put("map", Map.of(KEY_ENABLED, false, "reason", TRACKING_SCREEN_SUBSCRIPTION_DETAILS));
        payload.put(KEY_PARTNER_AVAILABLE, false);
        payload.put("retryPaymentAllowed", retryPaymentAllowed(order));
        payload.put(KEY_STATUS_HISTORY, statusHistory(order));
        payload.put(KEY_META, subscriptionBookingMeta());
        return payload;
    }

    private Map<String, Object> subscriptionSummaryPayload(Order order) {
        SubscriptionMaster subscription = subscriptionMasterRepository.findByBookingId(order.getId()).orElse(null);
        if (subscription == null) {
            return Map.of(
                    "id", String.valueOf(order.getId()),
                    KEY_BOOKING_ID, String.valueOf(order.getId()),
                    KEY_CONTRACT_VERSION, BOOKING_CONTRACT_VERSION,
                    KEY_BOOKING_TYPE, TYPE_SUBSCRIPTION,
                    KEY_STATUS, order.getStatus() == null ? "CONFIRMED" : order.getStatus().name(),
                    KEY_CREATED_AT, utc(order.getCreatedAt()),
                    KEY_META, subscriptionBookingMeta()
            );
        }
        List<String> frequencyDays = subscriptionFrequencyRepository.findBySubscriptionIdOrderByIdAsc(subscription.getId())
                .stream()
                .map(SubscriptionFrequency::getDayName)
                .toList();
        Map<String, Object> payload = new LinkedHashMap<>();
        populateSubscriptionPayload(payload, order, subscription, frequencyDays);
        payload.put(KEY_STATUS, subscription.getStatus().name());
        payload.put(KEY_CREATED_AT, utc(order.getCreatedAt()));
        payload.put(KEY_META, subscriptionBookingMeta());
        return payload;
    }

    private void populateSubscriptionPayload(Map<String, Object> payload, Order order,
                                             SubscriptionMaster subscription, List<String> frequencyDays) {
        payload.put("id", subscription.getId());
        payload.put(KEY_BOOKING_ID, order.getId());
        payload.put(KEY_CONTRACT_VERSION, BOOKING_CONTRACT_VERSION);
        payload.put(KEY_BOOKING_TYPE, TYPE_SUBSCRIPTION);
        payload.put("subscriptionId", subscription.getId());
        payload.put("subscriptionServiceType", subscription.getServiceType());
        payload.put(KEY_SERVICE_NAME, subscription.getServiceType());
        payload.put("frequencyLabel", frequencyLabel(frequencyDays));
        payload.put("frequencyDays", frequencyDays);
        payload.put("startDate", subscription.getStartDate());
        payload.put("endDate", subscription.getEndDate());
        payload.put("visits", subscription.getVisits());
        payload.put("timeSlot", subscription.getTimeSlot());
        payload.put("fullAddress", order.getAddress());
        payload.put("address", order.getAddress());
        payload.put(KEY_CREATED_AT, utc(order.getCreatedAt()));
        payload.put(KEY_SCHEDULED_AT, utc(order.getScheduledAt()));
        payload.put("promisedStartTime", utc(order.getPromisedStartTime()));
        payload.put(KEY_TOTAL_AMOUNT, subscription.getPrice());
        payload.put("amount", subscription.getPrice());
        payload.put("paymentStatus", order.getPaymentStatus() == null ? "PENDING" : order.getPaymentStatus().name());
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
        boolean activeTracking = Set.of(STATUS_PARTNER_ASSIGNED, STATUS_PARTNER_ON_THE_WAY, STATUS_ARRIVED, STATUS_PARTNER_ARRIVED, STATUS_SERVICE_STARTED)
                .contains(assignmentStatus == null ? "" : assignmentStatus.toUpperCase(Locale.ROOT));
        Long partnerId = order.getAssignedPartnerId();
        Optional<CurrentPartnerLocationEntity> partnerLocation = partnerId == null
                ? Optional.empty()
                : currentPartnerLocationRepository.findByPartnerIdAndDeletedFalse(partnerId);
        boolean partnerAvailable = partnerLocation.isPresent();
        boolean showMap = paid && activeTracking && partnerAvailable;

        payload.put(KEY_SHOW_MAP, showMap);
        payload.put("mapEnabled", showMap);
        payload.put(KEY_PARTNER_AVAILABLE, partnerAvailable);
        payload.put("trackingStatus", assignmentStatus);
        if (!showMap) {
            payload.put("map", Map.of(
                    KEY_ENABLED, false,
                    "reason", mapDisabledReason(paid, activeTracking, partnerId, partnerAvailable)
            ));
            return payload;
        }

        CurrentPartnerLocationEntity location = partnerLocation.get();
        Map<String, Object> partner = new LinkedHashMap<>();
        partner.put("id", partnerId);
        partner.put("latitude", location.getLatitude() == null ? null : location.getLatitude().doubleValue());
        partner.put("longitude", location.getLongitude() == null ? null : location.getLongitude().doubleValue());
        partner.put(KEY_UPDATED_AT, utc(location.getUpdatedAt()));
        partner.put("lastHeartbeatAt", utc(location.getLastHeartbeatAt()));

        Map<String, Object> map = new LinkedHashMap<>();
        map.put(KEY_ENABLED, true);
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
        payload.remove(KEY_SHOW_MAP);
        payload.remove("mapEnabled");
        return payload;
    }

    private Map<String, Object> actionPermissions(Order order, String assignmentStatus, boolean partnerAvailable) {
        Map<String, Object> permissions = new LinkedHashMap<>();
        permissions.put(KEY_CAN_CANCEL, order != null && order.getStatus() != null && order.getStatus().canCancel());
        permissions.put(KEY_CAN_RESCHEDULE, canReschedule(order));
        permissions.put(KEY_CAN_CALL_PARTNER, canCallPartner(order, assignmentStatus));
        permissions.put(KEY_CAN_TRACK_PARTNER, partnerAvailable && Set.of(STATUS_PARTNER_ON_THE_WAY, STATUS_ARRIVED, STATUS_PARTNER_ARRIVED, STATUS_SERVICE_STARTED)
                .contains(assignmentStatus == null ? "" : assignmentStatus.toUpperCase(Locale.ROOT)));
        permissions.put(KEY_CAN_RAISE_ISSUE, canRaiseIssue(order));
        return permissions;
    }

    private boolean canReschedule(Order order) {
        if (order == null || order.getStatus() == null) {
            return false;
        }
        boolean scheduled = STATUS_SCHEDULED.equalsIgnoreCase(order.getBookingType()) || order.getStatus() == OrderStatus.SCHEDULED;
        return scheduled && Set.of(OrderStatus.CREATED, OrderStatus.CONFIRMED, OrderStatus.SCHEDULED, OrderStatus.BOOKING_CREATED)
                .contains(order.getStatus());
    }

    private boolean canCallPartner(Order order, String assignmentStatus) {
        if (order == null || order.getAssignedPartnerId() == null) {
            return false;
        }
        return Set.of(STATUS_PARTNER_ASSIGNED, STATUS_PARTNER_ON_THE_WAY, STATUS_ARRIVED, STATUS_PARTNER_ARRIVED, STATUS_SERVICE_STARTED, STATUS_IN_PROGRESS)
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
        if (STATUS_PARTNER_ASSIGNED.equals(normalized)) {
            return 15;
        }
        if (STATUS_PARTNER_ON_THE_WAY.equals(normalized)) {
            return 5;
        }
        if (STATUS_ARRIVED.equals(normalized) || STATUS_PARTNER_ARRIVED.equals(normalized)) {
            return 5;
        }
        if (STATUS_SERVICE_STARTED.equals(normalized)) {
            return 15;
        }
        if (STATUS_IN_PROGRESS.equals(normalized) || STATUS_SERVICE_IN_PROGRESS.equalsIgnoreCase(status)) {
            return 30;
        }
        if (STATUS_COMPLETED.equalsIgnoreCase(status) || STATUS_COMPLETED.equals(normalized)) {
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
        meta.put(KEY_SHOW_MAP, Boolean.TRUE.equals(trackingPayload.get(KEY_SHOW_MAP)));
        meta.put(KEY_CAN_CANCEL, Boolean.TRUE.equals(permissions.get(KEY_CAN_CANCEL)));
        meta.put(KEY_CAN_RESCHEDULE, Boolean.TRUE.equals(permissions.get(KEY_CAN_RESCHEDULE)));
        meta.put(KEY_CAN_CALL_PARTNER, Boolean.TRUE.equals(permissions.get(KEY_CAN_CALL_PARTNER)));
        meta.put(KEY_CAN_TRACK_PARTNER, Boolean.TRUE.equals(permissions.get(KEY_CAN_TRACK_PARTNER)));
        meta.put(KEY_CAN_RAISE_ISSUE, Boolean.TRUE.equals(permissions.get(KEY_CAN_RAISE_ISSUE)));
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
        meta.put("trackingScreen", TRACKING_SCREEN_SUBSCRIPTION_DETAILS);
        meta.put(KEY_SHOW_MAP, false);
        meta.put("trackingRefreshSeconds", 0);
        meta.put(KEY_CAN_CANCEL, false);
        meta.put(KEY_CAN_RESCHEDULE, false);
        meta.put(KEY_CAN_CALL_PARTNER, false);
        meta.put(KEY_CAN_TRACK_PARTNER, false);
        meta.put(KEY_CAN_RAISE_ISSUE, false);
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
        addStatusHistory(fallback, STATUS_BOOKING_CREATED, order.getCreatedAt());
        addStatusHistory(fallback, STATUS_PARTNER_ASSIGNED, order.getUpdatedAt(), order.getAssignedPartnerId() != null);
        addStatusHistory(fallback, STATUS_ARRIVED, order.getArrivedAt());
        addStatusHistory(fallback, STATUS_SERVICE_STARTED, order.getStartedAt());
        addStatusHistory(fallback, STATUS_SERVICE_IN_PROGRESS, order.getInProgressAt());
        addStatusHistory(fallback, STATUS_COMPLETED, firstNonNull(order.getCompletedAt(), isCompletedStatus(order.getStatus()) ? order.getUpdatedAt() : null));
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
        payload.put(KEY_STATUS, status);
        payload.put("time", utc(event.getCreatedAt()));
        return payload;
    }

    private String timelineStatus(String eventType) {
        String normalized = eventType == null ? "" : eventType.toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case STATUS_BOOKING_CREATED, STATUS_SEARCHING_PARTNER, "PARTNER_SEARCH", STATUS_PARTNER_ASSIGNED,
                 STATUS_SERVICE_STARTED, STATUS_SERVICE_IN_PROGRESS, STATUS_COMPLETED -> normalized;
            case "PARTNER_ACCEPTED", "OUT_FOR_DELIVERY" -> "ON_THE_WAY";
            case STATUS_ARRIVED, "PARTNER_REACHED" -> STATUS_ARRIVED;
            case STATUS_SERVICE_COMPLETED, "PAYMENT_COMPLETED", "DELIVERED" -> STATUS_COMPLETED;
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
        row.put(KEY_STATUS, status);
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
                KEY_MESSAGE, notification.getMessage() == null ? "" : notification.getMessage(),
                "redirectType", notification.getRedirectType() == null ? "" : notification.getRedirectType(),
                KEY_BOOKING_ID, bookingId == null ? "" : bookingId,
                "read", "READ".equalsIgnoreCase(notification.getStatus().name()),
                KEY_CREATED_AT, String.valueOf(notification.getCreatedAt())
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
                paymentMethodRow("CARD", "Card", "Credit/Debit card payment")
        );

        Map<String, Object> content = new LinkedHashMap<>();
        content.put("paymentOptions", methods);
        content.put("options", methods);
        content.put("methods", methods);
        content.put("paymentMethods", methods);
        content.put("content", methods);
        content.put(KEY_ITEMS, methods);
        if (bookingId != null) {
            content.put(KEY_BOOKING_ID, bookingId);
        }
        if (cartId != null) {
            content.put("cartId", cartId);
        }
        if (amount != null) {
            content.put(KEY_AMOUNT, amount);
        }
        return content;
    }

    private Map<String, Object> paymentMethodRow(String id, String label, String description) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("mode", id);
        row.put("label", label);
        row.put(KEY_DESCRIPTION, description);
        return row;
    }

    private String normalizeMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return "UPI";
        }
        String normalized = mode.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("UPI", "CARD").contains(normalized)) {
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
            return STATUS_SEARCHING_PARTNER;
        }
        String status = order.getStatus() == null ? "" : order.getStatus().name();
        if (STATUS_SERVICE_IN_PROGRESS.equalsIgnoreCase(status)) {
            return STATUS_IN_PROGRESS;
        }
        if (STATUS_SERVICE_STARTED.equalsIgnoreCase(status)) {
            return STATUS_SERVICE_STARTED;
        }
        if (STATUS_SERVICE_COMPLETED.equalsIgnoreCase(status)
                || "DELIVERED".equalsIgnoreCase(status)
                || "PAYMENT_COMPLETED".equalsIgnoreCase(status)) {
            return STATUS_COMPLETED;
        }
        if (STATUS_ARRIVED.equalsIgnoreCase(status) || "PARTNER_REACHED".equalsIgnoreCase(status)) {
            return STATUS_ARRIVED;
        }
        if ("PARTNER_ACCEPTED".equalsIgnoreCase(status)
                || "PREPARING".equalsIgnoreCase(status)
                || "OUT_FOR_DELIVERY".equalsIgnoreCase(status)) {
            return STATUS_PARTNER_ON_THE_WAY;
        }
        if (STATUS_CANCELLED.equalsIgnoreCase(status)) {
            return STATUS_CANCELLED;
        }
        if (STATUS_INSTANT_FAILED.equalsIgnoreCase(String.valueOf(order.getStatus()))) {
            return "NO_PARTNER_AVAILABLE";
        }
        if (hasAssignedPartner) {
            return STATUS_PARTNER_ASSIGNED;
        }
        String bookingType = order.getBookingType();
        if (STATUS_SCHEDULED.equalsIgnoreCase(bookingType)) {
            return STATUS_SCHEDULED;
        }
        return STATUS_SEARCHING_PARTNER;
    }

    private String bookingMessage(String status, String assignmentStatus) {
        if (STATUS_PARTNER_ASSIGNED.equalsIgnoreCase(assignmentStatus)) {
            return "Partner assigned";
        }
        if ("NO_PARTNER_AVAILABLE".equalsIgnoreCase(assignmentStatus)) {
            return "No partner available right now";
        }
        if (STATUS_SCHEDULED.equalsIgnoreCase(assignmentStatus)) {
            return "Booking scheduled";
        }
        if (STATUS_COMPLETED.equalsIgnoreCase(status)) {
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
                if (STATUS_SCHEDULED.equalsIgnoreCase(order.getBookingType())) {
                    yield STATUS_SCHEDULED;
                }
                yield "PENDING_ASSIGNMENT";
            }
            case PARTNER_ASSIGNED -> STATUS_PARTNER_ASSIGNED;
            case PARTNER_ACCEPTED, PREPARING, OUT_FOR_DELIVERY -> "ON_THE_WAY";
            case ARRIVED, PARTNER_REACHED -> STATUS_ARRIVED;
            case SERVICE_STARTED -> STATUS_SERVICE_STARTED;
            case SERVICE_IN_PROGRESS -> STATUS_SERVICE_IN_PROGRESS;
            case SERVICE_COMPLETED, PAYMENT_COMPLETED, DELIVERED -> STATUS_COMPLETED;
            case INSTANT_FAILED -> "EXPIRED";
            case CANCELLED -> STATUS_CANCELLED;
            case SCHEDULED -> STATUS_SCHEDULED;
        };
    }

    private String trackingScreen(Order order, String assignmentStatus) {
        if (order == null) {
            return TRACKING_SCREEN_INSTANT_STATUS;
        }
        if (TYPE_SUBSCRIPTION.equalsIgnoreCase(order.getBookingType())) {
            return TRACKING_SCREEN_SUBSCRIPTION_DETAILS;
        }
        if (STATUS_SCHEDULED.equalsIgnoreCase(order.getBookingType())
                && !Set.of(STATUS_PARTNER_ASSIGNED, STATUS_PARTNER_ON_THE_WAY, STATUS_ARRIVED, STATUS_PARTNER_ARRIVED, STATUS_SERVICE_STARTED, STATUS_IN_PROGRESS)
                .contains(assignmentStatus == null ? "" : assignmentStatus.toUpperCase(Locale.ROOT))) {
            return TRACKING_SCREEN_SCHEDULED_STATUS;
        }
        OrderStatus status = order.getStatus();
        if (status == null) {
            return TRACKING_SCREEN_INSTANT_STATUS;
        }
        return switch (status) {
            case PARTNER_ASSIGNED -> TRACKING_SCREEN_INSTANT_STATUS;
            case PARTNER_ACCEPTED, PREPARING, OUT_FOR_DELIVERY -> "LIVE_TRACKING";
            case ARRIVED, PARTNER_REACHED -> STATUS_PARTNER_ARRIVED;
            case SERVICE_STARTED -> STATUS_SERVICE_STARTED;
            case SERVICE_IN_PROGRESS -> STATUS_SERVICE_IN_PROGRESS;
            case SERVICE_COMPLETED, PAYMENT_COMPLETED, DELIVERED -> STATUS_SERVICE_COMPLETED;
            case SCHEDULED -> TRACKING_SCREEN_SCHEDULED_STATUS;
            default -> TRACKING_SCREEN_INSTANT_STATUS;
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
        if (STATUS_COMPLETED.equalsIgnoreCase(status) || STATUS_COMPLETED.equalsIgnoreCase(assignmentStatus)) {
            return 0;
        }
        if (STATUS_PARTNER_ASSIGNED.equalsIgnoreCase(assignmentStatus)
                || STATUS_PARTNER_ON_THE_WAY.equalsIgnoreCase(assignmentStatus)) {
            return 12;
        }
        return 0;
    }

    private String serviceName(OrderResponseDTO order) {
        OrderItemDTO firstItem = firstOrderItem(order);
        String name = firstItem == null ? null : firstItem.getPartnerName();
        return name == null || name.isBlank() ? "Service" : name;
    }

    private OrderItemDTO firstOrderItem(OrderResponseDTO order) {
        return order.getItems() == null || order.getItems().isEmpty() ? null : order.getItems().get(0);
    }

    private Map<String, Object> partnerSummary(Order order, String assignmentStatus) {
        if (!shouldExposePartner(assignmentStatus) || order.getAssignedPartnerId() == null) {
            return null;
        }
        Long partnerId = order.getAssignedPartnerId();
        PartnerSummaryDetails details = partnerSummaryDetails(partnerId);

        Map<String, Object> partner = new LinkedHashMap<>();
        partner.put("id", partnerId == null ? "" : String.valueOf(partnerId));
        partner.put("name", details.name() == null ? "" : details.name());
        partner.put("phone", details.phone());
        partner.put(KEY_RATING, details.rating());
        partner.put("profileImage", details.profileImage());
        return partner;
    }

    private Map<String, Object> partnerSummary(Long partnerId,
                                               OrderResponseDTO order,
                                               String assignmentStatus) {
        if (!shouldExposePartner(assignmentStatus) || partnerId == null) {
            return null;
        }
        PartnerSummaryDetails details = partnerSummaryDetails(partnerId);
        Map<String, Object> partner = new LinkedHashMap<>();
        partner.put("id", String.valueOf(partnerId));
        partner.put("name", details.name() == null ? "" : details.name());
        partner.put("phone", details.phone());
        partner.put(KEY_RATING, details.rating());
        partner.put("profileImage", details.profileImage());
        return partner;
    }

    private PartnerSummaryDetails partnerSummaryDetails(Long partnerId) {
        PartnerSummaryDetails details = new PartnerSummaryDetails("Partner", "", 0d, "");
        return applyPartnerOperationalProfile(details, partnerProfileRepository.findById(partnerId).orElse(null));
    }

    private PartnerSummaryDetails applyPartnerOperationalProfile(PartnerSummaryDetails details, PartnerOperationalProfile partnerProfile) {
        if (partnerProfile == null) {
            return details;
        }
        return new PartnerSummaryDetails(
                nonBlankOrDefault(partnerProfile.getName(), details.name()),
                preferredProfilePhone(partnerProfile),
                partnerProfile.getRating() == null ? details.rating() : partnerProfile.getRating(),
                details.profileImage()
        );
    }

    private String preferredProfilePhone(PartnerOperationalProfile partnerProfile) {
        String phone = sanitizePhone(partnerProfile.getMobileNumber());
        return phone.isBlank() ? sanitizePhone(partnerProfile.getPhoneNumber()) : phone;
    }

    private String nonBlankOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private record PartnerSummaryDetails(String name, String phone, Double rating, String profileImage) {
    }

    private boolean shouldExposePartner(String assignmentStatus) {
        return Set.of(STATUS_PARTNER_ASSIGNED, STATUS_PARTNER_ON_THE_WAY, STATUS_ARRIVED, STATUS_PARTNER_ARRIVED, STATUS_SERVICE_STARTED, STATUS_IN_PROGRESS, STATUS_COMPLETED)
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
                KEY_DISPLAY_ORDER, faq.getDisplayOrder()
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
                .orElseThrow(() -> new BadRequestException(MSG_BOOKING_NOT_FOUND));
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
            supportRealtimeService.publish(ticket, USER_TYPE_CUSTOMER, eventName, payload);
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
        Object available = slotRow.get(KEY_AVAILABLE);
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

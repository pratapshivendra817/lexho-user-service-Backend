package com.lexho.user_service.lexho.controller;

import com.lexho.booking.entity.Order;
import com.lexho.booking.repository.OrderRepository;
import com.lexho.catalog.entity.ServiceCategory;
import com.lexho.catalog.repository.CategoryRepository;
import com.lexho.common.enums.PaymentStatus;
import com.lexho.common.enums.OrderStatus;
import com.lexho.notification.entity.Notification;
import com.lexho.partner.entity.Partner;
import com.lexho.partner.repository.PartnerRepository;
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
import com.lexho.user_service.banner.entity.BannerPromotion;
import com.lexho.user_service.banner.enums.BannerPlacement;
import com.lexho.user_service.banner.enums.BannerStatus;
import com.lexho.user_service.banner.repository.BannerPromotionRepository;
import com.lexho.user_service.dto.*;
import com.lexho.user_service.dispatch.service.DispatchOrchestrationService;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.customer.service.SlotDateTimePolicy;
import com.lexho.user_service.customer.service.CustomerCartSlotService;
import com.lexho.user_service.booking.exception.BookingValidationException;
import com.lexho.user_service.booking.exception.InstantBookingUnavailableException;
import com.lexho.user_service.booking.model.BookingType;
import com.lexho.user_service.booking.model.InstantUnavailableCode;
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
import com.lexho.user_service.service.NotificationService;
import com.lexho.user_service.service.OrderService;
import com.lexho.user_service.service.ApiRateLimiterService;
import com.lexho.user_service.util.MediaUrlResolver;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@Slf4j
public class LexhoExperienceController {

    private final HomeService homeService;
    private final CartService cartService;
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;
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
    private final LexhoSubscriptionRepository subscriptionRepository;
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

    public LexhoExperienceController(HomeService homeService,
                                     CartService cartService,
                                     OrderService orderService,
                                     OrderRepository orderRepository,
                                     NotificationService notificationService,
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
                                     LexhoSubscriptionRepository subscriptionRepository,
                                     SupportTicketRepository supportTicketRepository,
                                     SupportTicketReplyRepository supportTicketReplyRepository,
                                     SupportFaqCategoryRepository supportFaqCategoryRepository,
                                     SupportFaqRepository supportFaqRepository,
                                     MediaUrlResolver mediaUrlResolver,
                                     DispatchOrchestrationService dispatchOrchestrationService,
                                     ApiRateLimiterService apiRateLimiterService,
                                     SlotDateTimePolicy slotDateTimePolicy,
                                     CustomerCartSlotService customerCartSlotService,
                                     GeoFenceValidationService geoFenceValidationService) {
        this.homeService = homeService;
        this.cartService = cartService;
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.notificationService = notificationService;
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
        this.subscriptionRepository = subscriptionRepository;
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
                                                                       @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        List<Map<String, Object>> data = adminCategoryRepository
                .findByActiveTrueAndDeletedFalseOrderByCreatedAtDesc(pageable).stream()
                .map(this::mapCategory)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Categories fetched", Map.of("data", data)));
    }

    @GetMapping("/api/services/popular")
    public ResponseEntity<ApiResponse<Map<String, Object>>> popular(@RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success("Popular services fetched", Map.of("data", partnerData(page, size, null))));
    }

    @GetMapping("/api/services/recommended")
    public ResponseEntity<ApiResponse<Map<String, Object>>> recommended(@RequestParam(defaultValue = "0") int page,
                                                                        @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success("Recommended services fetched", Map.of("data", partnerData(page, size, null))));
    }

    @GetMapping("/api/services/quick")
    public ResponseEntity<ApiResponse<Map<String, Object>>> quick(@RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success("Quick services fetched", Map.of("data", partnerData(page, size, null))));
    }

    @GetMapping("/api/search/services")
    public ResponseEntity<ApiResponse<Map<String, Object>>> search(@RequestParam String keyword,
                                                                   @RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success("Search result for " + keyword, Map.of("data", partnerData(page, size, keyword))));
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
            geoFenceValidationService.validateBookingAddress(addressService.getRawById(userId, request.addressId()));
        } catch (BadRequestException ex) {
            throw new BookingValidationException("ADDRESS_NOT_FOUND", "Address not found for this customer");
        }
        PlaceOrderRequest placeOrderRequest = new PlaceOrderRequest();
        placeOrderRequest.setAddress(address.line1() + ", " + address.city());
        BookingType bookingType = request.bookingType() == null || request.bookingType().isBlank()
                ? BookingType.SCHEDULED
                : BookingType.valueOf(request.bookingType());
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
            if (scheduledOrder != null && scheduledOrder.getStatus() == OrderStatus.CREATED
                    && scheduledOrder.getStatus().canTransitionTo(OrderStatus.SCHEDULED)) {
                scheduledOrder.setStatus(OrderStatus.SCHEDULED);
                orderRepository.save(scheduledOrder);
            }
        }
        if (bookingType == BookingType.INSTANT && dispatched <= 0) {
            Order failedOrder = orderRepository.findById(order.getId()).orElse(null);
            if (failedOrder != null) {
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
        payload.put("serviceId", String.valueOf(request.serviceId()));
        payload.put("addressId", String.valueOf(request.addressId()));
        payload.put("bookingType", bookingType.name());
        payload.put("assignmentStatus", bookingType == BookingType.INSTANT ? "SEARCHING_PARTNER" : "SCHEDULED");
        payload.put("date", request.date() == null ? "" : request.date());
        payload.put("slot", request.slot());
        payload.put("note", request.note());
        payload.put("status", order.getStatus());
        payload.put("slaMinutes", bookingType == BookingType.INSTANT ? DispatchOrchestrationService.INSTANT_SLA_MINUTES : 0);
        return ResponseEntity.ok(ApiResponse.success("Booking created", payload));
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
        List<Map<String, Object>> data = orderRepository.findByUserId(userId).stream()
                .sorted(Comparator.comparing(Order::getId).reversed())
                .skip((long) page * size)
                .limit(size)
                .map(this::mapOrderSummary)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Booking history fetched", Map.of("data", data)));
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
        String status = order.getStatus();
        Long serviceId = order.getItems() == null || order.getItems().isEmpty()
                ? null
                : order.getItems().get(0).getPartnerId();
        payload.put("id", String.valueOf(order.getId()));
        payload.put("bookingId", String.valueOf(order.getId()));
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
        payload.put("etaMinutes", deriveEtaMinutes(status, assignmentStatus));
        payload.put("slaMinutes", persistedOrder.getSlaMinutes() == null ? 0 : persistedOrder.getSlaMinutes());
        payload.put("promisedStartTime", persistedOrder.getPromisedStartTime() == null ? null : persistedOrder.getPromisedStartTime().toString());
        payload.put("serviceName", order.getItems().isEmpty() ? "Service" : order.getItems().get(0).getPartnerName());
        payload.put("address", order.getAddress());
        payload.put("fullAddress", order.getAddress());
        payload.put("createdAt", persistedOrder.getCreatedAt());
        payload.put("scheduledAt", persistedOrder.getScheduledAt());
        boolean isCompleted = "COMPLETED".equalsIgnoreCase(order.getStatus());
        payload.put("completedAt", isCompleted ? persistedOrder.getUpdatedAt() : null);
        payload.put("completionStatus", isCompleted ? "COMPLETED" : "NOT_COMPLETED_YET");
        payload.put("completedAtDisplay", isCompleted ? String.valueOf(persistedOrder.getUpdatedAt()) : "Not completed yet");
        payload.put("completed", isCompleted ? String.valueOf(persistedOrder.getUpdatedAt()) : "Not completed yet");
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
        return ResponseEntity.ok(ApiResponse.success("Booking details fetched successfully", payload));
    }

    @PutMapping("/api/bookings/cancel/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cancelBooking(@RequestAttribute("userId") Long userId,
                                                                          @PathVariable Long id) {
        OrderResponseDTO order = orderService.cancelOrder(id, userId);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", String.valueOf(id));
        payload.put("bookingId", String.valueOf(id));
        payload.put("status", order.getStatus());
        payload.put("assignmentStatus", "CANCELLED");
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled", payload));
    }

    @PutMapping("/api/bookings/reschedule/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> rescheduleBooking(@RequestAttribute("userId") Long userId,
                                                                              @PathVariable Long id,
                                                                              @RequestBody LexhoDtos.RescheduleRequest request) {
        Order order = orderRepository.findById(id).orElseThrow(() -> new BadRequestException("Booking not found"));
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new BadRequestException("You are not allowed to reschedule this booking");
        }
        LocalDateTime scheduledAt = parseDateTime(request.date());
        if (scheduledAt == null) {
            throw new BadRequestException("date is required");
        }
        order.setScheduledAt(scheduledAt);
        if (order.getStatus() == OrderStatus.CREATED && order.getStatus().canTransitionTo(OrderStatus.SCHEDULED)) {
            order.setStatus(OrderStatus.SCHEDULED);
        }
        Order saved = orderRepository.save(order);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", String.valueOf(saved.getId()));
        payload.put("bookingId", String.valueOf(saved.getId()));
        payload.put("status", saved.getStatus() == null ? "SCHEDULED" : saved.getStatus().name());
        payload.put("assignmentStatus", "SCHEDULED");
        payload.put("scheduledAt", saved.getScheduledAt());
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
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateLocation(@RequestParam Long bookingId,
                                                                           @RequestParam Double latitude,
                                                                           @RequestParam Double longitude) {
        return ResponseEntity.ok(ApiResponse.success("Partner location updated", Map.of("bookingId", bookingId, "lat", latitude, "lng", longitude)));
    }

    @GetMapping("/api/payments/options")
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
        List<Map<String, Object>> data = notificationService.getAll(userId).stream()
                .skip((long) page * size)
                .limit(size)
                .map(this::toNotificationMap)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Notifications fetched successfully", Map.of("data", data)));
    }

    @PutMapping("/api/notifications/read")
    public ResponseEntity<ApiResponse<Void>> markNotificationRead(@RequestAttribute("userId") Long userId,
                                                                  @Valid @RequestBody LexhoDtos.IdRequest request) {
        notificationService.markRead(userId, request.id());
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", null));
    }

    @DeleteMapping("/api/notifications/read")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(@RequestAttribute("userId") Long userId,
                                                                @Valid @RequestBody LexhoDtos.IdRequest request) {
        notificationService.deleteNotification(userId, request.id());
        return ResponseEntity.ok(ApiResponse.success("Notification deleted", null));
    }

    @GetMapping("/api/customer/notifications/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Object>>> unreadCount(@RequestAttribute("userId") Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Unread count", Map.of("count", notificationService.unreadCount(userId))));
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

    @PostMapping("/api/subscription/create")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createSubscription(@RequestAttribute("userId") Long userId,
                                                                               @Valid @RequestBody LexhoDtos.SubscriptionCreateRequest request) {
        LexhoSubscription sub = new LexhoSubscription();
        sub.setUserId(userId);
        sub.setPlanId(request.planId());
        sub.setStatus("ACTIVE");
        LexhoSubscription saved = subscriptionRepository.save(sub);
        return ResponseEntity.ok(ApiResponse.success("Subscription created", Map.of(
                "id", String.valueOf(saved.getId()),
                "planId", saved.getPlanId(),
                "status", saved.getStatus()
        )));
    }

    @GetMapping("/api/subscription/list")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listSubscriptions(@RequestAttribute("userId") Long userId,
                                                                              @RequestParam(defaultValue = "0") int page,
                                                                              @RequestParam(defaultValue = "20") int size) {
        List<Map<String, Object>> data = subscriptionRepository.findByUserIdOrderByIdDesc(userId).stream()
                .skip((long) page * size)
                .limit(size)
                .map(item -> Map.<String, Object>of(
                        "id", String.valueOf(item.getId()),
                        "planId", item.getPlanId(),
                        "status", item.getStatus(),
                        "createdAt", String.valueOf(item.getCreatedAt())
                ))
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Subscriptions fetched", Map.of("data", data)));
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
        SupportTicket ticket = new SupportTicket();
        ticket.setUserId(userId);
        ticket.setSubject(request.subject());
        ticket.setMessage(request.message());
        ticket.setStatus("OPEN");
        ticket.setCategoryId(request.categoryId());
        ticket.setBookingId(request.bookingId());
        ticket.setPriority(request.priority() == null || request.priority().isBlank() ? "MEDIUM" : request.priority().trim().toUpperCase(Locale.ROOT));
        SupportTicket saved = supportTicketRepository.save(ticket);
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
                "createdAt", String.valueOf(saved.getCreatedAt())
        )));
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
                "createdAt", String.valueOf(ticket.getCreatedAt())
        ));
        for (int i = 0; i < replies.size(); i++) {
            SupportTicketReply reply = replies.get(i);
            messages.add(Map.of(
                    "id", "MSG-" + (i + 2),
                    "sender", "SUPPORT",
                    "message", reply.getMessage(),
                    "createdAt", String.valueOf(reply.getCreatedAt())
            ));
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                        "id", formatTicketId(ticket.getId()),
                        "status", ticket.getStatus(),
                        "messages", messages
                )
        ));
    }

    private List<Map<String, Object>> partnerData(int page, int size, String keyword) {
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
                "imageUrl", safeImageUrl
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
                "quantity", item.getQuantity(),
                "price", item.getPrice() == null ? 0 : item.getPrice()
        );
    }

    private Map<String, Object> mapOrderSummary(Order order) {
        // Keep history resilient even when order items are lazily unavailable.
        String serviceName = "Service";
        return Map.of(
                "id", String.valueOf(order.getId()),
                "status", order.getStatus() == null ? "CREATED" : order.getStatus().name(),
                "serviceName", serviceName,
                "createdAt", String.valueOf(order.getCreatedAt())
        );
    }

    private Map<String, Object> toNotificationMap(Notification notification) {
        String bookingId = null;
        if ("BOOKING".equalsIgnoreCase(notification.getReferenceType())) {
            if (notification.getReferenceId() != null) {
                bookingId = String.valueOf(notification.getReferenceId());
            } else if (notification.getRedirectValue() != null && !notification.getRedirectValue().isBlank()) {
                bookingId = notification.getRedirectValue().trim();
            }
        }
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
        if ("SERVICE_STARTED".equalsIgnoreCase(status)) {
            return "IN_PROGRESS";
        }
        if ("SERVICE_COMPLETED".equalsIgnoreCase(status)
                || "DELIVERED".equalsIgnoreCase(status)
                || "PAYMENT_COMPLETED".equalsIgnoreCase(status)) {
            return "COMPLETED";
        }
        if ("PARTNER_REACHED".equalsIgnoreCase(status)) {
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

    private Map<String, Object> mapFaqRow(SupportFaq faq) {
        return Map.of(
                "id", faq.getFaqKey(),
                "question", faq.getQuestion(),
                "answer", faq.getAnswer(),
                "displayOrder", faq.getDisplayOrder()
        );
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
        for (int i = 0; i < 5; i++) {
            String code = "LEX" + userId + new Random().nextInt(9000);
            if (!referralCodeRepository.existsByCodeIgnoreCase(code)) {
                return code;
            }
        }
        return "LEX" + userId + System.currentTimeMillis() % 10000;
    }
}

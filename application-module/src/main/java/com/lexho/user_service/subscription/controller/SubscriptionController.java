package com.lexho.user_service.subscription.controller;

import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.cartconfig.service.CartConfigService;
import com.lexho.user_service.exception.CodedValidationException;
import com.lexho.user_service.subscription.dto.CookCoverageResponse;
import com.lexho.user_service.subscription.dto.CookSubscriptionPricingResponse;
import com.lexho.user_service.subscription.dto.CreateSubscriptionRequest;
import com.lexho.user_service.subscription.dto.RescheduleVisitRequest;
import com.lexho.user_service.subscription.dto.SubscriptionCalendarItemResponse;
import com.lexho.user_service.subscription.dto.SubscriptionCreateResponse;
import com.lexho.user_service.subscription.dto.SubscriptionDetailResponse;
import com.lexho.user_service.subscription.dto.SubscriptionDurationPriceResponse;
import com.lexho.user_service.subscription.dto.SubscriptionListItemResponse;
import com.lexho.user_service.subscription.dto.SubscriptionPriceQuoteResponse;
import com.lexho.user_service.subscription.dto.SubscriptionQuoteRequest;
import com.lexho.user_service.subscription.dto.SubscriptionQuoteResponse;
import com.lexho.user_service.subscription.enums.FamilySize;
import com.lexho.user_service.subscription.exception.SubscriptionValidationException;
import com.lexho.user_service.subscription.service.SubscriptionBookingService;
import com.lexho.user_service.subscription.service.SubscriptionPricingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/subscription", "/api/subscriptions"})
public class SubscriptionController {
    private static final String ATTR_USER_ID = "userId";

    private final SubscriptionBookingService subscriptionBookingService;
    private final SubscriptionPricingService subscriptionPricingService;
    private final CartConfigService cartConfigService;

    public SubscriptionController(SubscriptionBookingService subscriptionBookingService,
                                  SubscriptionPricingService subscriptionPricingService,
                                  CartConfigService cartConfigService) {
        this.subscriptionBookingService = subscriptionBookingService;
        this.subscriptionPricingService = subscriptionPricingService;
        this.cartConfigService = cartConfigService;
    }

    @PostMapping("/create")
    public ApiResponse<SubscriptionCreateResponse> create(@RequestAttribute(ATTR_USER_ID) Long customerId,
                                                          @Valid @RequestBody CreateSubscriptionRequest request,
                                                          @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        validateSubscriptionEnabled();
        return ApiResponse.success("Subscription created",
                subscriptionBookingService.create(customerId, request, idempotencyKey));
    }

    @PostMapping("/bookings")
    public ApiResponse<SubscriptionCreateResponse> createBookingAlias(@RequestAttribute(ATTR_USER_ID) Long customerId,
                                                                      @Valid @RequestBody CreateSubscriptionRequest request,
                                                                      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        validateSubscriptionEnabled();
        return ApiResponse.success("Subscription created",
                subscriptionBookingService.create(customerId, request, idempotencyKey));
    }

    private void validateSubscriptionEnabled() {
        if (!Boolean.TRUE.equals(cartConfigService.currentEntity().getSubscriptionBookingEnabled())
                || !Boolean.TRUE.equals(cartConfigService.currentEntity().getSubscriptionCheckoutEnabled())) {
            throw new CodedValidationException("SERVICE_UNAVAILABLE", "Subscription booking is currently disabled");
        }
    }

    @PostMapping("/quote")
    public ApiResponse<SubscriptionQuoteResponse> quote(@Valid @RequestBody SubscriptionQuoteRequest request) {
        return ApiResponse.success("Subscription quote calculated", subscriptionPricingService.quote(request));
    }

    @GetMapping("/pricing")
    public ApiResponse<List<SubscriptionDurationPriceResponse>> pricing(@RequestParam(required = false) Long serviceId) {
        return ApiResponse.success("Subscription pricing fetched", subscriptionPricingService.prices(serviceId));
    }

    @GetMapping("/cook-pricing")
    public ApiResponse<List<CookSubscriptionPricingResponse>> cookPricing(@RequestParam(required = false) Long serviceId) {
        return ApiResponse.success("Cook subscription pricing fetched", subscriptionPricingService.cookPrices(serviceId));
    }

    @GetMapping("/price-quote")
    public ApiResponse<SubscriptionPriceQuoteResponse> priceQuote(@RequestParam Long serviceId,
                                                                  @RequestParam java.math.BigDecimal durationHours,
                                                                  @RequestParam Integer visits) {
        return ApiResponse.success("Subscription price calculated",
                subscriptionPricingService.quote(serviceId, durationHours, visits));
    }

    @GetMapping("/cook-coverage")
    public CookCoverageResponse cookCoverage(@RequestParam(required = false) Long serviceId,
                                             @RequestParam(required = false) FamilySize familySize) {
        return subscriptionPricingService.cookCoverage(serviceId, familySize);
    }

    @GetMapping("/list")
    public ApiResponse<List<SubscriptionListItemResponse>> list(@RequestAttribute(ATTR_USER_ID) Long customerId) {
        return ApiResponse.success("Subscriptions fetched", subscriptionBookingService.list(customerId));
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> subscriptions(@RequestAttribute(ATTR_USER_ID) Long customerId) {
        return ApiResponse.success("Subscriptions fetched", subscriptionBookingService.subscriptionCards(customerId));
    }

    @GetMapping("/{subscriptionId}")
    public ApiResponse<Map<String, Object>> subscriptionCard(@RequestAttribute(ATTR_USER_ID) Long customerId,
                                                             @PathVariable Long subscriptionId) {
        return ApiResponse.success("Subscription fetched",
                subscriptionBookingService.subscriptionCard(customerId, subscriptionId));
    }

    @GetMapping("/{subscriptionId}/detail")
    public ApiResponse<SubscriptionDetailResponse> detail(@RequestAttribute(ATTR_USER_ID) Long customerId,
                                                          @PathVariable Long subscriptionId) {
        return ApiResponse.success("Subscription fetched",
                subscriptionBookingService.detail(customerId, subscriptionId));
    }

    @PostMapping("/{subscriptionId}/pause")
    public ApiResponse<Map<String, Object>> pause(@RequestAttribute(ATTR_USER_ID) Long customerId,
                                                  @PathVariable Long subscriptionId) {
        return ApiResponse.success("Subscription paused",
                subscriptionBookingService.pause(customerId, subscriptionId));
    }

    @PostMapping("/{subscriptionId}/resume")
    public ApiResponse<Map<String, Object>> resume(@RequestAttribute(ATTR_USER_ID) Long customerId,
                                                   @PathVariable Long subscriptionId) {
        return ApiResponse.success("Subscription resumed",
                subscriptionBookingService.resume(customerId, subscriptionId));
    }

    @GetMapping("/{subscriptionId}/calendar")
    public List<SubscriptionCalendarItemResponse> calendar(@RequestAttribute(ATTR_USER_ID) Long customerId,
                                                           @PathVariable Long subscriptionId) {
        return subscriptionBookingService.calendar(customerId, subscriptionId);
    }

    @GetMapping("/{subscriptionId}/visits")
    public ApiResponse<Map<String, Object>> visits(@RequestAttribute(ATTR_USER_ID) Long customerId,
                                                   @PathVariable Long subscriptionId) {
        return ApiResponse.success("Subscription visits fetched",
                subscriptionBookingService.visits(customerId, subscriptionId));
    }

    @PostMapping("/{subscriptionId}/cancel")
    public ApiResponse<Void> cancel(@RequestAttribute(ATTR_USER_ID) Long customerId,
                                    @PathVariable Long subscriptionId) {
        subscriptionBookingService.cancel(customerId, subscriptionId);
        return ApiResponse.success("Subscription cancelled", null);
    }

    @PostMapping("/{subscriptionId}/visits/{visitId}/skip")
    public ApiResponse<Void> skipVisit(@RequestAttribute(ATTR_USER_ID) Long customerId,
                                       @PathVariable Long subscriptionId,
                                       @PathVariable Long visitId) {
        subscriptionBookingService.skipVisit(customerId, subscriptionId, visitId);
        return ApiResponse.success("Visit skipped", null);
    }

    @PostMapping("/{subscriptionId}/visits/{visitId}/reschedule")
    public ApiResponse<Void> rescheduleVisit(@RequestAttribute(ATTR_USER_ID) Long customerId,
                                             @PathVariable Long subscriptionId,
                                             @PathVariable Long visitId,
                                             @Valid @RequestBody RescheduleVisitRequest request) {
        subscriptionBookingService.rescheduleVisit(customerId, subscriptionId, visitId, request);
        return ApiResponse.success("Visit rescheduled", null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .toList();
        return new ResponseEntity<>(
                new ApiResponse<>(false, "VALIDATION_FAILED", "Validation failed", Map.of("errors", errors)),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(SubscriptionValidationException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleSubscriptionValidation(SubscriptionValidationException ex) {
        return new ResponseEntity<>(
                new ApiResponse<>(false, "VALIDATION_FAILED", "Validation failed", Map.of("errors", ex.getErrors())),
                HttpStatus.BAD_REQUEST
        );
    }
}

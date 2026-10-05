package com.lexho.user_service.coupon.service;

import com.lexho.booking.entity.Order;
import com.lexho.booking.entity.OrderItem;
import com.lexho.booking.repository.OrderRepository;
import com.lexho.user_service.admin.repository.AdminServiceEntityRepository;
import com.lexho.user_service.admin.service.AdminAuditService;
import com.lexho.user_service.coupon.dto.CouponPageResponse;
import com.lexho.user_service.coupon.dto.CouponRequest;
import com.lexho.user_service.coupon.dto.CouponResponse;
import com.lexho.user_service.coupon.dto.CouponStatusRequest;
import com.lexho.user_service.coupon.dto.CouponValidationRequest;
import com.lexho.user_service.coupon.dto.CouponValidationResponse;
import com.lexho.user_service.coupon.entity.Coupon;
import com.lexho.user_service.coupon.entity.CouponRedemption;
import com.lexho.user_service.coupon.enums.CouponDiscountType;
import com.lexho.user_service.coupon.repository.CouponRedemptionRepository;
import com.lexho.user_service.coupon.repository.CouponRepository;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CouponService {
    private static final String ENTITY_NAME = "coupons";
    private static final String REFERRAL_REWARD_SOURCE = "REFERRAL_REWARD";
    private static final String CODE_AUDIT_PREFIX = "code=";
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private final CouponRepository couponRepository;
    private final CouponRedemptionRepository redemptionRepository;
    private final OrderRepository orderRepository;
    private final AdminServiceEntityRepository serviceRepository;
    private final AdminAuditService adminAuditService;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public CouponPageResponse list(int page, int size, Boolean active) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Order.desc("id")));
        Page<Coupon> result = active == null ? couponRepository.findAll(pageable)
                : couponRepository.findByActive(active, pageable);
        return new CouponPageResponse(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getTotalElements(),
                result.getTotalPages(),
                safePage,
                safeSize
        );
    }

    @Transactional(readOnly = true)
    public CouponResponse get(Long id) {
        return toResponse(findCoupon(id));
    }

    @Transactional(readOnly = true)
    public CouponPageResponse listMyRewardCoupons(Long userId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Order.desc("id")));
        Page<Coupon> result = couponRepository.findByAssignedUserIdAndSourceType(
                userId, REFERRAL_REWARD_SOURCE, pageable);
        return new CouponPageResponse(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getTotalElements(),
                result.getTotalPages(),
                safePage,
                safeSize
        );
    }

    @Transactional(readOnly = true)
    public CouponPageResponse listAvailableCoupons(Long userId, int page, int size, BigDecimal amount,
                                                   Long serviceId, Long categoryId, String bookingType) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        List<Coupon> eligibleCoupons = couponRepository
                .findCustomerVisibleCandidates(userId, LocalDateTime.now(), Sort.by(Sort.Order.desc("id")))
                .stream()
                .filter(coupon -> isEligibleForCartView(coupon, userId, serviceId, categoryId, bookingType))
                .toList();
        int fromIndex = Math.min(safePage * safeSize, eligibleCoupons.size());
        int toIndex = Math.min(fromIndex + safeSize, eligibleCoupons.size());
        return new CouponPageResponse(
                eligibleCoupons.subList(fromIndex, toIndex).stream().map(this::toResponse).toList(),
                eligibleCoupons.size(),
                (int) Math.ceil((double) eligibleCoupons.size() / safeSize),
                safePage,
                safeSize
        );
    }

    @Transactional
    public CouponResponse create(CouponRequest request) {
        validateCouponRequest(request);
        String code = normalizeCode(request.code());
        if (couponRepository.existsByCodeIgnoreCase(code)) {
            throw new BadRequestException("Coupon code already exists");
        }
        Long adminId = currentAdminId();
        Coupon coupon = new Coupon();
        applyRequest(coupon, request, code);
        coupon.setCreatedBy(adminId);
        coupon.setUpdatedBy(adminId);
        Coupon saved = couponRepository.save(coupon);
        audit("COUPON_CREATE", saved.getId(), CODE_AUDIT_PREFIX + saved.getCode());
        return toResponse(saved);
    }

    @Transactional
    public CouponResponse update(Long id, CouponRequest request) {
        validateCouponRequest(request);
        Coupon coupon = findCoupon(id);
        String code = normalizeCode(request.code());
        couponRepository.findByCodeIgnoreCase(code)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BadRequestException("Coupon code already exists");
                });
        applyRequest(coupon, request, code);
        coupon.setUpdatedBy(currentAdminId());
        Coupon saved = couponRepository.save(coupon);
        audit("COUPON_UPDATE", saved.getId(), CODE_AUDIT_PREFIX + saved.getCode());
        return toResponse(saved);
    }

    @Transactional
    public CouponResponse updateStatus(Long id, CouponStatusRequest request) {
        Coupon coupon = findCoupon(id);
        coupon.setActive(Boolean.TRUE.equals(request.active()));
        coupon.setUpdatedBy(currentAdminId());
        Coupon saved = couponRepository.save(coupon);
        audit("COUPON_STATUS_UPDATE", id, "active=" + saved.isActive());
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        Coupon coupon = findCoupon(id);
        couponRepository.delete(coupon);
        audit("COUPON_DELETE", id, CODE_AUDIT_PREFIX + coupon.getCode());
    }

    @Transactional
    public Coupon createReferralRewardCoupon(Long userId, BigDecimal amount, String sourceReferenceId) {
        String sourceReference = normalizeSourceReference(sourceReferenceId);
        Coupon existing = couponRepository
                .findBySourceTypeAndSourceReferenceId(REFERRAL_REWARD_SOURCE, sourceReference)
                .orElse(null);
        if (existing != null) {
            return existing;
        }

        Coupon coupon = new Coupon();
        coupon.setCode(generateUniqueRewardCode());
        coupon.setDiscountType(CouponDiscountType.FIXED);
        coupon.setDiscountValue(money(amount));
        coupon.setMaxDiscountAmount(money(amount));
        coupon.setMinimumPurchaseAmount(BigDecimal.ZERO.setScale(2));
        coupon.setTotalUsageLimit(1);
        coupon.setPerUserUsageLimit(1);
        coupon.setUsedCount(0);
        coupon.setActive(true);
        coupon.setAssignedUserId(userId);
        coupon.setSourceType(REFERRAL_REWARD_SOURCE);
        coupon.setSourceReferenceId(sourceReference);
        return couponRepository.save(coupon);
    }

    @Transactional(readOnly = true)
    public CouponValidationResponse validate(Long userId, CouponValidationRequest request) {
        AppliedCoupon applied = calculateCoupon(userId, request.code(), request.amount(), request.planId(),
                request.categoryId(), request.bookingType());
        return new CouponValidationResponse(true, applied.code(), applied.originalAmount(),
                applied.discountAmount(), applied.finalAmount());
    }

    @Transactional(readOnly = true)
    public AppliedCoupon calculate(Long userId, String code, BigDecimal amount, Long serviceId, String bookingType) {
        return calculateCoupon(userId, code, amount, serviceId, null, bookingType);
    }

    private AppliedCoupon calculateCoupon(Long userId, String code, BigDecimal amount, Long serviceId,
                                          Long categoryId, String bookingType) {
        Coupon coupon = couponRepository.findByCodeIgnoreCase(normalizeCode(code))
                .orElseThrow(() -> new BadRequestException("Invalid coupon code"));
        validateEligibility(coupon, userId, money(amount), serviceId, categoryId, bookingType, null);
        return appliedCoupon(coupon, amount);
    }

    @Transactional
    public AppliedCoupon applyToOrder(Order order, String couponCode, Long serviceId, String bookingType) {
        if (couponCode == null || couponCode.isBlank()) {
            BigDecimal total = money(order.getTotalAmount());
            order.setOriginalAmount(total.doubleValue());
            order.setDiscountAmount(BigDecimal.ZERO.setScale(2).doubleValue());
            return null;
        }
        AppliedCoupon applied = calculateCoupon(order.getUserId(), couponCode, money(order.getTotalAmount()),
                serviceId, null, bookingType);
        order.setOriginalAmount(applied.originalAmount().doubleValue());
        order.setDiscountAmount(applied.discountAmount().doubleValue());
        order.setTotalAmount(applied.finalAmount().doubleValue());
        order.setCouponCode(applied.code());
        return applied;
    }

    @Transactional
    public void recordRedemption(Long bookingId) {
        Order order = orderRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        if (order.getCouponCode() == null || order.getCouponCode().isBlank()) {
            return;
        }
        if (redemptionRepository.existsByBookingId(bookingId)) {
            return;
        }
        Coupon coupon = couponRepository.findByCodeForUpdate(order.getCouponCode())
                .orElseThrow(() -> new BadRequestException("Coupon no longer exists"));
        BigDecimal originalAmount = money(order.getOriginalAmount() == null ? order.getTotalAmount() : order.getOriginalAmount());
        Long serviceId = firstServiceId(order);
        validateEligibility(coupon, order.getUserId(), originalAmount, serviceId, resolveCategoryId(serviceId),
                order.getBookingType(),
                bookingId);
        String idempotencyKey = "coupon:" + coupon.getId() + ":booking:" + bookingId;
        if (redemptionRepository.existsByIdempotencyKey(idempotencyKey)) {
            return;
        }
        CouponRedemption redemption = new CouponRedemption();
        redemption.setCouponId(coupon.getId());
        redemption.setUserId(order.getUserId());
        redemption.setBookingId(bookingId);
        redemption.setDiscountAmount(money(order.getDiscountAmount()));
        redemption.setIdempotencyKey(idempotencyKey);
        redemptionRepository.save(redemption);
        coupon.setUsedCount(coupon.getUsedCount() + 1);
        couponRepository.save(coupon);
    }

    private void validateEligibility(Coupon coupon,
                                     Long userId,
                                     BigDecimal amount,
                                     Long serviceId,
                                     Long categoryId,
                                     String bookingType,
                                     Long excludedBookingId) {
        LocalDateTime now = LocalDateTime.now();
        if (!coupon.isActive()) {
            throw new BadRequestException("Coupon is inactive");
        }
        if (coupon.getValidFrom() != null && coupon.getValidFrom().isAfter(now)) {
            throw new BadRequestException("Coupon is not active yet");
        }
        if (coupon.getValidUntil() != null && coupon.getValidUntil().isBefore(now)) {
            throw new BadRequestException("Coupon has expired");
        }
        BigDecimal minimum = zeroIfNull(coupon.getMinimumPurchaseAmount());
        if (amount.compareTo(minimum) < 0) {
            throw new BadRequestException("Minimum purchase amount not met");
        }
        validateUsageLimits(coupon, userId);
        if (coupon.getApplicableServiceId() != null && serviceId != null && !coupon.getApplicableServiceId().equals(serviceId)) {
            throw new BadRequestException("Coupon is not applicable to this service");
        }
        if (coupon.getApplicableCategoryId() != null) {
            Long resolvedCategoryId = categoryId == null ? resolveCategoryId(serviceId) : categoryId;
            if (resolvedCategoryId == null || !coupon.getApplicableCategoryId().equals(resolvedCategoryId)) {
                throw new BadRequestException("Coupon is not applicable to this category");
            }
        }
        if (coupon.getAssignedUserId() != null && !coupon.getAssignedUserId().equals(userId)) {
            throw new BadRequestException("Coupon is not assigned to this user");
        }
        if (coupon.isFirstBookingOnly() && hasPreviousSuccessfulPaidBooking(userId, excludedBookingId)) {
            throw new BadRequestException("Coupon is valid only for first booking");
        }
        validateBookingType(coupon, bookingType);
    }

    private void validateUsageLimits(Coupon coupon, Long userId) {
        if (coupon.getTotalUsageLimit() != null && coupon.getUsedCount() >= coupon.getTotalUsageLimit()) {
            throw new BadRequestException("Coupon usage limit exhausted");
        }
        if (coupon.getPerUserUsageLimit() != null
                && redemptionRepository.countByCouponIdAndUserId(coupon.getId(), userId) >= coupon.getPerUserUsageLimit()) {
            throw new BadRequestException("Coupon usage limit exhausted for this user");
        }
        if (coupon.getMaxUsesPerDay() != null) {
            LocalDate today = LocalDate.now();
            long usesToday = redemptionRepository.countByCouponIdAndRedeemedAtBetween(
                    coupon.getId(), today.atStartOfDay(), today.plusDays(1).atStartOfDay());
            if (usesToday >= coupon.getMaxUsesPerDay()) {
                throw new BadRequestException("Coupon daily usage limit exhausted");
            }
        }
    }

    private boolean isEligibleForCartView(Coupon coupon, Long userId, Long serviceId,
                                          Long categoryId, String bookingType) {
        if (coupon.getPerUserUsageLimit() != null
                && redemptionRepository.countByCouponIdAndUserId(coupon.getId(), userId) >= coupon.getPerUserUsageLimit()) {
            return false;
        }
        if (coupon.getMaxUsesPerDay() != null) {
            LocalDate today = LocalDate.now();
            long usesToday = redemptionRepository.countByCouponIdAndRedeemedAtBetween(
                    coupon.getId(), today.atStartOfDay(), today.plusDays(1).atStartOfDay());
            if (usesToday >= coupon.getMaxUsesPerDay()) {
                return false;
            }
        }
        if (coupon.getApplicableServiceId() != null && serviceId != null
                && !coupon.getApplicableServiceId().equals(serviceId)) {
            return false;
        }
        if (coupon.getApplicableCategoryId() != null) {
            Long resolvedCategoryId = categoryId == null ? resolveCategoryId(serviceId) : categoryId;
            if (resolvedCategoryId == null || !coupon.getApplicableCategoryId().equals(resolvedCategoryId)) {
                return false;
            }
        }
        if (coupon.isFirstBookingOnly() && hasPreviousSuccessfulPaidBooking(userId, null)) {
            return false;
        }
        String applicableBookingType = normalizeNullable(coupon.getApplicableBookingType());
        String requestedBookingType = normalizeNullable(bookingType);
        return applicableBookingType == null || requestedBookingType == null
                || applicableBookingType.equals(requestedBookingType);
    }

    private void validateBookingType(Coupon coupon, String bookingType) {
        String applicableBookingType = normalizeNullable(coupon.getApplicableBookingType());
        String requestedBookingType = normalizeNullable(bookingType);
        if (applicableBookingType != null && requestedBookingType != null
                && !applicableBookingType.equals(requestedBookingType)) {
            throw new BadRequestException("Coupon is not applicable to this booking type");
        }
    }

    private boolean hasPreviousSuccessfulPaidBooking(Long userId, Long excludedBookingId) {
        if (userId == null) {
            return false;
        }
        if (excludedBookingId == null) {
            return orderRepository.countSuccessfulPaidBookingsByUserId(userId) > 0;
        }
        return orderRepository.countSuccessfulPaidBookingsByUserIdExcludingBooking(userId, excludedBookingId) > 0;
    }

    private AppliedCoupon appliedCoupon(Coupon coupon, BigDecimal amount) {
        BigDecimal original = money(amount);
        BigDecimal discount = switch (coupon.getDiscountType()) {
            case FIXED -> coupon.getDiscountValue();
            case PERCENTAGE -> original.multiply(coupon.getDiscountValue()).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
        };
        if (coupon.getMaxDiscountAmount() != null) {
            discount = discount.min(coupon.getMaxDiscountAmount());
        }
        discount = money(discount.min(original));
        return new AppliedCoupon(coupon.getId(), coupon.getCode(), original, discount, original.subtract(discount).setScale(2));
    }

    private void validateCouponRequest(CouponRequest request) {
        if (request.discountType() == CouponDiscountType.PERCENTAGE
                && request.discountValue().compareTo(ONE_HUNDRED) > 0) {
            throw new BadRequestException("Percentage discount cannot exceed 100");
        }
        if (request.validFrom() != null && request.validUntil() != null
                && request.validFrom().isAfter(request.validUntil())) {
            throw new BadRequestException("validFrom must not be after validUntil");
        }
    }

    private void applyRequest(Coupon coupon, CouponRequest request, String code) {
        coupon.setCode(code);
        coupon.setTitle(trimToNull(request.title()));
        coupon.setDescription(trimToNull(request.description()));
        coupon.setDiscountType(request.discountType());
        coupon.setDiscountValue(money(request.discountValue()));
        coupon.setMaxDiscountAmount(moneyOrNull(request.maxDiscountAmount()));
        coupon.setMinimumPurchaseAmount(zeroIfNull(request.minimumPurchaseAmount()));
        coupon.setTotalUsageLimit(request.totalUsageLimit());
        coupon.setPerUserUsageLimit(request.perUserUsageLimit());
        coupon.setMaxUsesPerDay(request.maxUsesPerDay());
        coupon.setValidFrom(request.validFrom());
        coupon.setValidUntil(request.validUntil());
        coupon.setActive(!Boolean.FALSE.equals(request.active()));
        coupon.setCustomerVisible(!Boolean.FALSE.equals(request.customerVisible()));
        coupon.setFirstBookingOnly(Boolean.TRUE.equals(request.firstBookingOnly()));
        coupon.setApplicableServiceId(request.applicableServiceId());
        coupon.setApplicableCategoryId(request.applicableCategoryId());
        coupon.setApplicableBookingType(normalizeNullable(request.applicableBookingType()));
    }

    private Coupon findCoupon(Long id) {
        return couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found: " + id));
    }

    private CouponResponse toResponse(Coupon coupon) {
        return new CouponResponse(coupon.getId(), coupon.getCode(), coupon.getTitle(), coupon.getDescription(), coupon.getDiscountType(),
                coupon.getDiscountValue(), coupon.getMaxDiscountAmount(), coupon.getMinimumPurchaseAmount(),
                coupon.getTotalUsageLimit(), coupon.getPerUserUsageLimit(), coupon.getMaxUsesPerDay(),
                coupon.getUsedCount(),
                coupon.getValidFrom(), coupon.getValidUntil(), coupon.isActive(), coupon.isCustomerVisible(),
                coupon.isFirstBookingOnly(),
                coupon.getApplicableServiceId(), coupon.getApplicableCategoryId(), coupon.getApplicableBookingType(), coupon.getAssignedUserId(),
                coupon.getSourceType(), coupon.getSourceReferenceId(),
                coupon.getCreatedAt(), coupon.getUpdatedAt());
    }

    private Long resolveCategoryId(Long serviceId) {
        if (serviceId == null) {
            return null;
        }
        return serviceRepository.findByIdAndDeletedFalse(serviceId)
                .map(service -> service.getCategory() == null ? null : service.getCategory().getId())
                .orElse(null);
    }

    private String generateUniqueRewardCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String code = "REF" + randomCodeSuffix();
            if (!couponRepository.existsByCodeIgnoreCase(code)) {
                return code;
            }
        }
        return "REF" + java.util.UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 16)
                .toUpperCase(Locale.ROOT);
    }

    private String randomCodeSuffix() {
        StringBuilder builder = new StringBuilder(10);
        for (int index = 0; index < 10; index++) {
            builder.append(CODE_ALPHABET[RANDOM.nextInt(CODE_ALPHABET.length)]);
        }
        return builder.toString();
    }

    private Long firstServiceId(Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            return null;
        }
        OrderItem first = order.getItems().get(0);
        return first == null ? null : first.getPartnerId();
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BadRequestException("code is required");
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeSourceReference(String sourceReferenceId) {
        if (sourceReferenceId == null || sourceReferenceId.isBlank()) {
            throw new BadRequestException("sourceReferenceId is required");
        }
        return sourceReferenceId.trim();
    }

    private BigDecimal money(Double amount) {
        return amount == null ? BigDecimal.ZERO.setScale(2) : money(BigDecimal.valueOf(amount));
    }

    private BigDecimal money(BigDecimal amount) {
        return zeroIfNull(amount).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal moneyOrNull(BigDecimal amount) {
        return amount == null ? null : money(amount);
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Long currentAdminId() {
        return securityUtils.getCurrentUserId().orElse(0L);
    }

    private void audit(String action, Long entityId, String metaData) {
        adminAuditService.log(currentAdminId(), action, ENTITY_NAME, entityId, metaData);
    }
}

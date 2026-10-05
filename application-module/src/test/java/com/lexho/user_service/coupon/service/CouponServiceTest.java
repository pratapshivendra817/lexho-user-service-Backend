package com.lexho.user_service.coupon.service;

import com.lexho.booking.entity.Order;
import com.lexho.booking.entity.OrderItem;
import com.lexho.booking.repository.OrderRepository;
import com.lexho.user_service.admin.repository.AdminServiceEntityRepository;
import com.lexho.user_service.admin.service.AdminAuditService;
import com.lexho.user_service.coupon.dto.CouponRequest;
import com.lexho.user_service.coupon.dto.CouponValidationRequest;
import com.lexho.user_service.coupon.entity.Coupon;
import com.lexho.user_service.coupon.enums.CouponDiscountType;
import com.lexho.user_service.coupon.repository.CouponRedemptionRepository;
import com.lexho.user_service.coupon.repository.CouponRepository;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {
    @Mock
    private CouponRepository couponRepository;
    @Mock
    private CouponRedemptionRepository redemptionRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private AdminServiceEntityRepository serviceRepository;
    @Mock
    private AdminAuditService adminAuditService;
    @Mock
    private SecurityUtils securityUtils;

    private CouponService couponService;

    @BeforeEach
    void setUp() {
        couponService = new CouponService(couponRepository, redemptionRepository, orderRepository,
                serviceRepository, adminAuditService, securityUtils);
    }

    @Test
    void calculateFixedDiscountCapsAtOriginalAmount() {
        Coupon coupon = coupon(1L, "LEXHO500", CouponDiscountType.FIXED, "500.00");
        when(couponRepository.findByCodeIgnoreCase("LEXHO500")).thenReturn(Optional.of(coupon));

        AppliedCoupon applied = couponService.calculate(7L, "lexho500", new BigDecimal("299.00"), 11L, "INSTANT");

        assertThat(applied.discountAmount()).isEqualByComparingTo("299.00");
        assertThat(applied.finalAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void calculatePercentageDiscountHonorsMaxDiscount() {
        Coupon coupon = coupon(1L, "WELCOME20", CouponDiscountType.PERCENTAGE, "20.00");
        coupon.setMaxDiscountAmount(new BigDecimal("75.00"));
        when(couponRepository.findByCodeIgnoreCase("WELCOME20")).thenReturn(Optional.of(coupon));

        AppliedCoupon applied = couponService.calculate(7L, "welcome20", new BigDecimal("499.00"), 11L, "SCHEDULED");

        assertThat(applied.discountAmount()).isEqualByComparingTo("75.00");
        assertThat(applied.finalAmount()).isEqualByComparingTo("424.00");
    }

    @Test
    void validateRejectsInactiveExpiredAndMinimumPurchaseFailures() {
        Coupon inactive = coupon(1L, "INACTIVE", CouponDiscountType.FIXED, "50.00");
        inactive.setActive(false);
        when(couponRepository.findByCodeIgnoreCase("INACTIVE")).thenReturn(Optional.of(inactive));
        CouponValidationRequest request = new CouponValidationRequest("INACTIVE", 11L, new BigDecimal("499.00"), null, "INSTANT");

        assertThatThrownBy(() -> couponService.validate(7L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Coupon is inactive");

        Coupon expired = coupon(2L, "EXPIRED", CouponDiscountType.FIXED, "50.00");
        expired.setValidUntil(LocalDateTime.now().minusDays(1));
        when(couponRepository.findByCodeIgnoreCase("EXPIRED")).thenReturn(Optional.of(expired));
        CouponValidationRequest expiredRequest = new CouponValidationRequest("EXPIRED", 11L,
                new BigDecimal("499.00"), null, "INSTANT");

        assertThatThrownBy(() -> couponService.validate(7L, expiredRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Coupon has expired");

        Coupon minimum = coupon(3L, "MINIMUM", CouponDiscountType.FIXED, "50.00");
        minimum.setMinimumPurchaseAmount(new BigDecimal("500.00"));
        when(couponRepository.findByCodeIgnoreCase("MINIMUM")).thenReturn(Optional.of(minimum));
        CouponValidationRequest minimumRequest = new CouponValidationRequest("MINIMUM", 11L,
                new BigDecimal("499.00"), null, "INSTANT");

        assertThatThrownBy(() -> couponService.validate(7L, minimumRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Minimum purchase amount not met");
    }

    @Test
    void recordRedemptionIsIdempotentPerBooking() {
        Order order = new Order();
        order.setUserId(7L);
        order.setCouponCode("LEXHO50");
        order.setOriginalAmount(499.00);
        order.setDiscountAmount(50.00);
        order.setTotalAmount(449.00);
        Coupon coupon = coupon(1L, "LEXHO50", CouponDiscountType.FIXED, "50.00");
        when(orderRepository.findByIdForUpdate(99L)).thenReturn(Optional.of(order));
        when(redemptionRepository.existsByBookingId(99L)).thenReturn(true);

        couponService.recordRedemption(99L);

        verify(couponRepository, never()).findByCodeForUpdate(any());
        assertThat(coupon.getUsedCount()).isZero();
    }

    @Test
    void recordRedemptionCreatesUsageOnlyAfterSuccessfulPaymentPath() {
        Order order = orderWithCoupon(7L, "LEXHO50", 11L);
        Coupon coupon = coupon(1L, "LEXHO50", CouponDiscountType.FIXED, "50.00");
        when(orderRepository.findByIdForUpdate(99L)).thenReturn(Optional.of(order));
        when(redemptionRepository.existsByBookingId(99L)).thenReturn(false);
        when(couponRepository.findByCodeForUpdate("LEXHO50")).thenReturn(Optional.of(coupon));
        when(redemptionRepository.existsByIdempotencyKey("coupon:1:booking:99")).thenReturn(false);

        couponService.recordRedemption(99L);

        assertThat(coupon.getUsedCount()).isEqualTo(1);
        verify(redemptionRepository).save(any());
    }

    @Test
    void recordRedemptionRevalidatesServiceApplicability() {
        Order order = orderWithCoupon(7L, "SERVICEONLY", 22L);
        Coupon coupon = coupon(1L, "SERVICEONLY", CouponDiscountType.FIXED, "50.00");
        coupon.setApplicableServiceId(11L);
        when(orderRepository.findByIdForUpdate(99L)).thenReturn(Optional.of(order));
        when(redemptionRepository.existsByBookingId(99L)).thenReturn(false);
        when(couponRepository.findByCodeForUpdate("SERVICEONLY")).thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> couponService.recordRedemption(99L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Coupon is not applicable to this service");

        verify(redemptionRepository, never()).save(any());
    }

    @Test
    void calculateRejectsTotalAndPerUserUsageExhaustion() {
        Coupon totalLimit = coupon(1L, "TOTAL", CouponDiscountType.FIXED, "50.00");
        totalLimit.setTotalUsageLimit(1);
        totalLimit.setUsedCount(1);
        when(couponRepository.findByCodeIgnoreCase("TOTAL")).thenReturn(Optional.of(totalLimit));

        assertThatThrownBy(() -> couponService.calculate(7L, "TOTAL", new BigDecimal("499.00"), 11L, "INSTANT"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Coupon usage limit exhausted");

        Coupon userLimit = coupon(2L, "USERLIMIT", CouponDiscountType.FIXED, "50.00");
        userLimit.setPerUserUsageLimit(1);
        when(couponRepository.findByCodeIgnoreCase("USERLIMIT")).thenReturn(Optional.of(userLimit));
        when(redemptionRepository.countByCouponIdAndUserId(2L, 7L)).thenReturn(1L);

        assertThatThrownBy(() -> couponService.calculate(7L, "USERLIMIT", new BigDecimal("499.00"), 11L, "INSTANT"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Coupon usage limit exhausted for this user");
    }

    @Test
    void calculateRejectsCouponAssignedToAnotherUser() {
        Coupon coupon = coupon(1L, "REFABC1234", CouponDiscountType.FIXED, "50.00");
        coupon.setAssignedUserId(99L);
        when(couponRepository.findByCodeIgnoreCase("REFABC1234")).thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> couponService.calculate(7L, "REFABC1234", new BigDecimal("499.00"), 11L, "INSTANT"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Coupon is not assigned to this user");
    }

    @Test
    void calculateAllowsAdminCouponAfterFirstSuccessfulPaidBookingByDefault() {
        Coupon coupon = coupon(1L, "CAMPAIGN", CouponDiscountType.FIXED, "50.00");
        when(couponRepository.findByCodeIgnoreCase("CAMPAIGN")).thenReturn(Optional.of(coupon));

        AppliedCoupon applied = couponService.calculate(7L, "CAMPAIGN", new BigDecimal("499.00"), 11L, "INSTANT");

        assertThat(applied.discountAmount()).isEqualByComparingTo("50.00");
        verify(orderRepository, never()).countSuccessfulPaidBookingsByUserId(any());
    }

    @Test
    void calculateRejectsFirstBookingOnlyCouponAfterFirstSuccessfulPaidBooking() {
        Coupon coupon = coupon(1L, "FIRSTONLY", CouponDiscountType.FIXED, "50.00");
        coupon.setFirstBookingOnly(true);
        when(couponRepository.findByCodeIgnoreCase("FIRSTONLY")).thenReturn(Optional.of(coupon));
        when(orderRepository.countSuccessfulPaidBookingsByUserId(7L)).thenReturn(1L);

        assertThatThrownBy(() -> couponService.calculate(7L, "FIRSTONLY", new BigDecimal("499.00"), 11L, "INSTANT"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Coupon is valid only for first booking");
    }

    @Test
    void calculateDoesNotApplyFirstBookingRuleToReferralRewardCoupon() {
        Coupon coupon = coupon(1L, "REFABC1234", CouponDiscountType.FIXED, "50.00");
        coupon.setSourceType("REFERRAL_REWARD");
        when(couponRepository.findByCodeIgnoreCase("REFABC1234")).thenReturn(Optional.of(coupon));

        AppliedCoupon applied = couponService.calculate(7L, "REFABC1234", new BigDecimal("499.00"), 11L, "INSTANT");

        assertThat(applied.discountAmount()).isEqualByComparingTo("50.00");
        verify(orderRepository, never()).countSuccessfulPaidBookingsByUserId(any());
    }

    @Test
    void createPersistsBusinessFields() {
        CouponRequest request = new CouponRequest("campaign10", "Campaign 10", "Festive offer",
                CouponDiscountType.PERCENTAGE, new BigDecimal("10.00"), new BigDecimal("100.00"),
                BigDecimal.ZERO, 500, 2, 25, null, null, true, false, true,
                11L, 3L, "instant");
        when(couponRepository.existsByCodeIgnoreCase("CAMPAIGN10")).thenReturn(false);
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(99L));
        when(couponRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = couponService.create(request);

        assertThat(response.code()).isEqualTo("CAMPAIGN10");
        assertThat(response.title()).isEqualTo("Campaign 10");
        assertThat(response.description()).isEqualTo("Festive offer");
        assertThat(response.customerVisible()).isFalse();
        assertThat(response.firstBookingOnly()).isTrue();
        assertThat(response.maxUsesPerDay()).isEqualTo(25);
        assertThat(response.applicableCategoryId()).isEqualTo(3L);
        assertThat(response.applicableBookingType()).isEqualTo("INSTANT");
    }

    @Test
    void calculateRejectsDailyUsageExhaustion() {
        Coupon coupon = coupon(1L, "DAILY", CouponDiscountType.FIXED, "50.00");
        coupon.setMaxUsesPerDay(2);
        when(couponRepository.findByCodeIgnoreCase("DAILY")).thenReturn(Optional.of(coupon));
        when(redemptionRepository.countByCouponIdAndRedeemedAtBetween(any(), any(), any())).thenReturn(2L);

        assertThatThrownBy(() -> couponService.calculate(7L, "DAILY", new BigDecimal("499.00"), 11L, "INSTANT"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Coupon daily usage limit exhausted");
    }

    @Test
    void createReferralRewardCouponCreatesSingleUseAssignedCoupon() {
        when(couponRepository.findBySourceTypeAndSourceReferenceId("REFERRAL_REWARD", "reward-55"))
                .thenReturn(Optional.empty());
        when(couponRepository.existsByCodeIgnoreCase(any())).thenReturn(false);
        when(couponRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Coupon coupon = couponService.createReferralRewardCoupon(7L, new BigDecimal("50.00"), "reward-55");

        assertThat(coupon.getCode()).startsWith("REF");
        assertThat(coupon.getAssignedUserId()).isEqualTo(7L);
        assertThat(coupon.getDiscountType()).isEqualTo(CouponDiscountType.FIXED);
        assertThat(coupon.getDiscountValue()).isEqualByComparingTo("50.00");
        assertThat(coupon.getTotalUsageLimit()).isEqualTo(1);
        assertThat(coupon.getPerUserUsageLimit()).isEqualTo(1);
        assertThat(coupon.getSourceType()).isEqualTo("REFERRAL_REWARD");
        assertThat(coupon.getSourceReferenceId()).isEqualTo("reward-55");
    }

    @Test
    void listMyRewardCouponsReturnsOnlyAssignedReferralRewards() {
        Coupon reward = coupon(1L, "REFABC1234", CouponDiscountType.FIXED, "50.00");
        reward.setAssignedUserId(7L);
        reward.setSourceType("REFERRAL_REWARD");
        when(couponRepository.findByAssignedUserIdAndSourceType(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(reward), PageRequest.of(0, 20), 1));

        var response = couponService.listMyRewardCoupons(7L, 0, 20);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).code()).isEqualTo("REFABC1234");
        assertThat(response.content().get(0).assignedUserId()).isEqualTo(7L);
        assertThat(response.content().get(0).sourceType()).isEqualTo("REFERRAL_REWARD");
        assertThat(response.totalElements()).isEqualTo(1);
        verify(couponRepository).findByAssignedUserIdAndSourceType(any(), any(), any());
    }

    @Test
    void listAvailableCouponsReturnsCustomerVisibleAdminCouponsForCart() {
        Coupon adminCoupon = coupon(1L, "CAMPAIGN50", CouponDiscountType.FIXED, "50.00");
        adminCoupon.setCustomerVisible(true);
        adminCoupon.setMinimumPurchaseAmount(new BigDecimal("300.00"));
        adminCoupon.setApplicableServiceId(11L);
        Coupon lowCartValueCoupon = coupon(2L, "MIN500", CouponDiscountType.FIXED, "25.00");
        lowCartValueCoupon.setCustomerVisible(true);
        lowCartValueCoupon.setMinimumPurchaseAmount(new BigDecimal("500.00"));
        when(couponRepository.findCustomerVisibleCandidates(any(), any(), any()))
                .thenReturn(List.of(adminCoupon, lowCartValueCoupon));

        var response = couponService.listAvailableCoupons(7L, 0, 20,
                new BigDecimal("499.00"), 11L, null, "INSTANT");

        assertThat(response.content()).hasSize(2);
        assertThat(response.content().get(0).code()).isEqualTo("CAMPAIGN50");
        assertThat(response.content().get(1).code()).isEqualTo("MIN500");
        assertThat(response.totalElements()).isEqualTo(2);
        verify(couponRepository).findCustomerVisibleCandidates(any(), any(), any());
    }

    @Test
    void createRejectsInvalidPercentage() {
        CouponRequest request = new CouponRequest("OVER", null, null, CouponDiscountType.PERCENTAGE, new BigDecimal("101.00"),
                null, BigDecimal.ZERO, null, null, null, null, null, true, null, null, null, null, null);

        assertThatThrownBy(() -> couponService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Percentage discount cannot exceed 100");
    }

    private Coupon coupon(Long id, String code, CouponDiscountType type, String value) {
        Coupon coupon = new Coupon();
        ReflectionTestUtils.setField(coupon, "id", id);
        coupon.setCode(code);
        coupon.setDiscountType(type);
        coupon.setDiscountValue(new BigDecimal(value));
        coupon.setMinimumPurchaseAmount(BigDecimal.ZERO);
        coupon.setUsedCount(0);
        coupon.setActive(true);
        return coupon;
    }

    private Order orderWithCoupon(Long userId, String couponCode, Long serviceId) {
        Order order = new Order();
        order.setUserId(userId);
        order.setCouponCode(couponCode);
        order.setOriginalAmount(499.00);
        order.setDiscountAmount(50.00);
        order.setTotalAmount(449.00);
        order.setBookingType("INSTANT");
        OrderItem item = new OrderItem();
        item.setPartnerId(serviceId);
        item.setOrder(order);
        order.setItems(java.util.List.of(item));
        return order;
    }
}

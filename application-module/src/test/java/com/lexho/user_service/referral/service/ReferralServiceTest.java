package com.lexho.user_service.referral.service;

import com.lexho.booking.repository.OrderRepository;
import com.lexho.user_service.admin.service.AdminAuditService;
import com.lexho.user_service.coupon.entity.Coupon;
import com.lexho.user_service.coupon.service.CouponService;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.lexho.entity.LexhoReferralCode;
import com.lexho.user_service.lexho.entity.LexhoReferralUse;
import com.lexho.user_service.lexho.repository.LexhoReferralCodeRepository;
import com.lexho.user_service.lexho.repository.LexhoReferralUseRepository;
import com.lexho.user_service.referral.dto.ReferralApplyRequest;
import com.lexho.user_service.referral.entity.ReferralConfiguration;
import com.lexho.user_service.referral.entity.ReferralRewardTransaction;
import com.lexho.user_service.referral.enums.ReferralStatus;
import com.lexho.user_service.referral.repository.ReferralConfigurationRepository;
import com.lexho.user_service.referral.repository.ReferralRewardTransactionRepository;
import com.lexho.user_service.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReferralServiceTest {
    @Mock
    private LexhoReferralCodeRepository referralCodeRepository;
    @Mock
    private LexhoReferralUseRepository referralUseRepository;
    @Mock
    private ReferralConfigurationRepository configurationRepository;
    @Mock
    private ReferralRewardTransactionRepository rewardTransactionRepository;
    @Mock
    private ReferralCodeGenerator codeGenerator;
    @Mock
    private CouponService couponService;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private AdminAuditService adminAuditService;
    @Mock
    private SecurityUtils securityUtils;

    private ReferralService referralService;

    @BeforeEach
    void setUp() {
        referralService = new ReferralService(referralCodeRepository, referralUseRepository,
                configurationRepository, rewardTransactionRepository, codeGenerator, couponService,
                orderRepository, adminAuditService, securityUtils);
        lenient().when(rewardTransactionRepository.save(any())).thenAnswer(invocation -> {
            ReferralRewardTransaction transaction = invocation.getArgument(0);
            if (transaction.getId() == null) {
                ReflectionTestUtils.setField(transaction, "id", transaction.getBeneficiary().ordinal() + 101L);
            }
            return transaction;
        });
    }

    @Test
    void generateCodeReusesExistingCode() {
        LexhoReferralCode code = referralCode(9L, 7L, "LEXHO8F4K2");
        when(referralCodeRepository.findByUserId(7L)).thenReturn(Optional.of(code));

        assertThat(referralService.generateCode(7L).code()).isEqualTo("LEXHO8F4K2");

        verify(referralCodeRepository, never()).save(any());
    }

    @Test
    void applyRejectsSelfReferral() {
        when(configurationRepository.findById(ReferralConfiguration.SINGLETON_ID))
                .thenReturn(Optional.of(activeConfiguration()));
        when(referralUseRepository.existsByUserId(7L)).thenReturn(false);
        when(referralCodeRepository.findByCodeForUpdate("LEXHO8F4K2"))
                .thenReturn(Optional.of(referralCode(1L, 7L, "LEXHO8F4K2")));

        assertThatThrownBy(() -> referralService.apply(7L, new ReferralApplyRequest("lexho8f4k2")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("You cannot apply your own referral code");
    }

    @Test
    void applyRejectsDuplicateAttribution() {
        when(configurationRepository.findById(ReferralConfiguration.SINGLETON_ID))
                .thenReturn(Optional.of(activeConfiguration()));
        when(referralUseRepository.existsByUserId(8L)).thenReturn(true);

        assertThatThrownBy(() -> referralService.apply(8L, new ReferralApplyRequest("LEXHO8F4K2")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Referral code already applied");

        verify(referralCodeRepository, never()).findByCodeForUpdate(any());
    }

    @Test
    void qualifyAndRewardIssuesBothRewardsOnce() {
        LexhoReferralUse referral = referralUse(55L, 10L, 20L, "LEXHO8F4K2");
        when(configurationRepository.findById(ReferralConfiguration.SINGLETON_ID))
                .thenReturn(Optional.of(activeConfiguration()));
        when(referralUseRepository.findByUserIdForUpdate(20L)).thenReturn(Optional.of(referral));
        when(orderRepository.countSuccessfulPaidBookingsByUserIdExcludingBooking(20L, 99L)).thenReturn(0L);
        when(rewardTransactionRepository.findByIdempotencyKey("referral:55:REFERRER")).thenReturn(Optional.empty());
        when(rewardTransactionRepository.findByIdempotencyKey("referral:55:REFERRED_USER")).thenReturn(Optional.empty());
        when(couponService.createReferralRewardCoupon(eq(10L), any(), any())).thenReturn(coupon(501L));
        when(couponService.createReferralRewardCoupon(eq(20L), any(), any())).thenReturn(coupon(502L));

        referralService.qualifyAndReward(20L, 99L, new BigDecimal("499.00"));

        assertThat(referral.getStatus()).isEqualTo(ReferralStatus.REWARDED);
        assertThat(referral.getQualifyingBookingId()).isEqualTo(99L);
        ArgumentCaptor<ReferralRewardTransaction> captor = ArgumentCaptor.forClass(ReferralRewardTransaction.class);
        verify(rewardTransactionRepository, org.mockito.Mockito.times(4)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(ReferralRewardTransaction::getIdempotencyKey)
                .contains("referral:55:REFERRER", "referral:55:REFERRED_USER");
        assertThat(captor.getAllValues())
                .extracting(ReferralRewardTransaction::getRewardCouponId)
                .contains(501L, 502L);
        verify(couponService).createReferralRewardCoupon(eq(10L), any(), eq("referral-reward-101"));
        verify(couponService).createReferralRewardCoupon(eq(20L), any(), eq("referral-reward-102"));
    }

    @Test
    void qualifyAndRewardDoesNotDuplicateExistingRewards() {
        LexhoReferralUse referral = referralUse(55L, 10L, 20L, "LEXHO8F4K2");
        when(configurationRepository.findById(ReferralConfiguration.SINGLETON_ID))
                .thenReturn(Optional.of(activeConfiguration()));
        when(referralUseRepository.findByUserIdForUpdate(20L)).thenReturn(Optional.of(referral));
        when(orderRepository.countSuccessfulPaidBookingsByUserIdExcludingBooking(20L, 99L)).thenReturn(0L);
        ReferralRewardTransaction referrerReward = rewardTransaction(1L, 501L);
        ReferralRewardTransaction referredReward = rewardTransaction(2L, 502L);
        when(rewardTransactionRepository.findByIdempotencyKey("referral:55:REFERRER"))
                .thenReturn(Optional.of(referrerReward));
        when(rewardTransactionRepository.findByIdempotencyKey("referral:55:REFERRED_USER"))
                .thenReturn(Optional.of(referredReward));

        referralService.qualifyAndReward(20L, 99L, new BigDecimal("499.00"));

        verify(couponService, never()).createReferralRewardCoupon(any(), any(), any());
        assertThat(referral.getStatus()).isEqualTo(ReferralStatus.REWARDED);
    }

    @Test
    void qualifyAndRewardBackfillsMissingRewardCouponForExistingRewardTransaction() {
        LexhoReferralUse referral = referralUse(55L, 10L, 20L, "LEXHO8F4K2");
        ReferralRewardTransaction existingReward = rewardTransaction(1L, null);
        existingReward.setBeneficiary(com.lexho.user_service.referral.enums.ReferralRewardBeneficiary.REFERRER);
        when(configurationRepository.findById(ReferralConfiguration.SINGLETON_ID))
                .thenReturn(Optional.of(activeConfiguration()));
        when(referralUseRepository.findByUserIdForUpdate(20L)).thenReturn(Optional.of(referral));
        when(orderRepository.countSuccessfulPaidBookingsByUserIdExcludingBooking(20L, 99L)).thenReturn(0L);
        when(rewardTransactionRepository.findByIdempotencyKey("referral:55:REFERRER"))
                .thenReturn(Optional.of(existingReward));
        when(rewardTransactionRepository.findByIdempotencyKey("referral:55:REFERRED_USER"))
                .thenReturn(Optional.of(rewardTransaction(2L, 502L)));
        when(couponService.createReferralRewardCoupon(eq(10L), any(), eq("referral-reward-1")))
                .thenReturn(coupon(501L));

        referralService.qualifyAndReward(20L, 99L, new BigDecimal("499.00"));

        assertThat(existingReward.getRewardCouponId()).isEqualTo(501L);
    }

    @Test
    void qualifyAndRewardRequiresReferredUserFirstSuccessfulPaidBooking() {
        LexhoReferralUse referral = referralUse(55L, 10L, 20L, "LEXHO8F4K2");
        when(configurationRepository.findById(ReferralConfiguration.SINGLETON_ID))
                .thenReturn(Optional.of(activeConfiguration()));
        when(referralUseRepository.findByUserIdForUpdate(20L)).thenReturn(Optional.of(referral));
        when(orderRepository.countSuccessfulPaidBookingsByUserIdExcludingBooking(20L, 99L)).thenReturn(1L);

        referralService.qualifyAndReward(20L, 99L, new BigDecimal("499.00"));

        assertThat(referral.getStatus()).isEqualTo(ReferralStatus.PENDING);
        verify(rewardTransactionRepository, never()).save(any());
        verify(couponService, never()).createReferralRewardCoupon(any(), any(), any());
    }

    private ReferralConfiguration activeConfiguration() {
        ReferralConfiguration config = new ReferralConfiguration();
        config.setId(ReferralConfiguration.SINGLETON_ID);
        config.setActive(true);
        config.setReferrerRewardAmount(new BigDecimal("50.00"));
        config.setReferredUserRewardAmount(new BigDecimal("20.00"));
        config.setMinimumQualifyingPurchaseAmount(new BigDecimal("100.00"));
        config.setQualificationCondition("SUCCESSFUL_PAYMENT");
        return config;
    }

    private LexhoReferralCode referralCode(Long id, Long userId, String codeValue) {
        LexhoReferralCode code = new LexhoReferralCode();
        ReflectionTestUtils.setField(code, "id", id);
        code.setUserId(userId);
        code.setCode(codeValue);
        code.setUsedCount(0);
        return code;
    }

    private LexhoReferralUse referralUse(Long id, Long referrerId, Long referredId, String code) {
        LexhoReferralUse use = new LexhoReferralUse();
        ReflectionTestUtils.setField(use, "id", id);
        use.setReferrerUserId(referrerId);
        use.setUserId(referredId);
        use.setCode(code);
        use.setStatus(ReferralStatus.PENDING);
        return use;
    }

    private ReferralRewardTransaction rewardTransaction(Long id, Long rewardCouponId) {
        ReferralRewardTransaction transaction = new ReferralRewardTransaction();
        ReflectionTestUtils.setField(transaction, "id", id);
        transaction.setRewardCouponId(rewardCouponId);
        return transaction;
    }

    private Coupon coupon(Long id) {
        Coupon coupon = new Coupon();
        ReflectionTestUtils.setField(coupon, "id", id);
        return coupon;
    }
}

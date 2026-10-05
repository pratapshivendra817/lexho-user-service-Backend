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
import com.lexho.user_service.referral.dto.ReferralCodeResponse;
import com.lexho.user_service.referral.dto.ReferralConfigurationRequest;
import com.lexho.user_service.referral.dto.ReferralConfigurationResponse;
import com.lexho.user_service.referral.dto.ReferralResponse;
import com.lexho.user_service.referral.entity.ReferralConfiguration;
import com.lexho.user_service.referral.entity.ReferralRewardTransaction;
import com.lexho.user_service.referral.enums.ReferralRewardBeneficiary;
import com.lexho.user_service.referral.enums.ReferralStatus;
import com.lexho.user_service.referral.repository.ReferralConfigurationRepository;
import com.lexho.user_service.referral.repository.ReferralRewardTransactionRepository;
import com.lexho.user_service.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ReferralService {
    private static final String ENTITY_NAME = "referrals";
    private static final String QUALIFICATION_SUCCESSFUL_PAYMENT = "SUCCESSFUL_PAYMENT";

    private final LexhoReferralCodeRepository referralCodeRepository;
    private final LexhoReferralUseRepository referralUseRepository;
    private final ReferralConfigurationRepository configurationRepository;
    private final ReferralRewardTransactionRepository rewardTransactionRepository;
    private final ReferralCodeGenerator codeGenerator;
    private final CouponService couponService;
    private final OrderRepository orderRepository;
    private final AdminAuditService adminAuditService;
    private final SecurityUtils securityUtils;

    @Transactional
    public ReferralCodeResponse generateCode(Long userId) {
        LexhoReferralCode code = referralCodeRepository.findByUserId(userId).orElseGet(() -> {
            LexhoReferralCode created = new LexhoReferralCode();
            created.setUserId(userId);
            created.setCode(generateUniqueCode());
            created.setUsedCount(0);
            return referralCodeRepository.save(created);
        });
        return new ReferralCodeResponse(code.getCode());
    }

    @Transactional
    public ReferralResponse apply(Long userId, ReferralApplyRequest request) {
        ReferralConfiguration config = currentConfiguration(false);
        if (!config.isActive()) {
            throw new BadRequestException("Referral program is inactive");
        }
        if (referralUseRepository.existsByUserId(userId)) {
            throw new BadRequestException("Referral code already applied");
        }
        LexhoReferralCode referralCode = referralCodeRepository.findByCodeForUpdate(normalizeCode(request.code()))
                .orElseThrow(() -> new BadRequestException("Invalid referral code"));
        if (Objects.equals(referralCode.getUserId(), userId)) {
            throw new BadRequestException("You cannot apply your own referral code");
        }
        LexhoReferralUse use = new LexhoReferralUse();
        use.setUserId(userId);
        use.setReferrerUserId(referralCode.getUserId());
        use.setCode(referralCode.getCode());
        use.setStatus(ReferralStatus.PENDING);
        if (config.getExpiryDays() != null) {
            use.setExpiresAt(LocalDateTime.now().plusDays(config.getExpiryDays()));
        }
        LexhoReferralUse saved = referralUseRepository.save(use);
        referralCode.setUsedCount(referralCode.getUsedCount() + 1);
        referralCodeRepository.save(referralCode);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ReferralResponse myReferral(Long userId) {
        return referralUseRepository.findByUserId(userId).map(this::toResponse).orElse(null);
    }

    @Transactional(readOnly = true)
    public ReferralConfigurationResponse getConfiguration() {
        return toResponse(currentConfiguration(false));
    }

    @Transactional
    public ReferralConfigurationResponse updateConfiguration(ReferralConfigurationRequest request) {
        ReferralConfiguration config = currentConfiguration(true);
        config.setActive(Boolean.TRUE.equals(request.active()));
        config.setReferrerRewardAmount(money(request.referrerRewardAmount()));
        config.setReferredUserRewardAmount(money(request.referredUserRewardAmount()));
        config.setMinimumQualifyingPurchaseAmount(money(request.minimumQualifyingPurchaseAmount()));
        config.setQualificationCondition(normalizeCondition(request.qualificationCondition()));
        config.setExpiryDays(request.expiryDays());
        config.setUpdatedBy(currentAdminId());
        ReferralConfiguration saved = configurationRepository.save(config);
        adminAuditService.log(currentAdminId(), "REFERRAL_CONFIG_UPDATE", ENTITY_NAME, saved.getId(),
                "condition=" + saved.getQualificationCondition());
        return toResponse(saved);
    }

    @Transactional
    public void qualifyAndReward(Long referredUserId, Long bookingId, BigDecimal paidAmount) {
        ReferralConfiguration config = currentConfiguration(false);
        if (!config.isActive() || !QUALIFICATION_SUCCESSFUL_PAYMENT.equals(config.getQualificationCondition())) {
            return;
        }
        LexhoReferralUse referral = referralUseRepository.findByUserIdForUpdate(referredUserId).orElse(null);
        if (referral == null || referral.getStatus() == ReferralStatus.REWARDED
                || referral.getStatus() == ReferralStatus.CANCELLED) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if (referral.getExpiresAt() != null && referral.getExpiresAt().isBefore(now)) {
            referral.setStatus(ReferralStatus.EXPIRED);
            referralUseRepository.save(referral);
            return;
        }
        if (money(paidAmount).compareTo(config.getMinimumQualifyingPurchaseAmount()) < 0) {
            return;
        }
        if (orderRepository.countSuccessfulPaidBookingsByUserIdExcludingBooking(referredUserId, bookingId) > 0) {
            return;
        }
        if (referral.getStatus() == ReferralStatus.PENDING) {
            referral.setStatus(ReferralStatus.QUALIFIED);
            referral.setQualifiedAt(now);
            referral.setQualifyingBookingId(bookingId);
        }
        issueReward(referral, referral.getReferrerUserId(), ReferralRewardBeneficiary.REFERRER,
                config.getReferrerRewardAmount(), bookingId);
        issueReward(referral, referral.getUserId(), ReferralRewardBeneficiary.REFERRED_USER,
                config.getReferredUserRewardAmount(), bookingId);
        referral.setStatus(ReferralStatus.REWARDED);
        referral.setRewardedAt(now);
        referralUseRepository.save(referral);
    }

    private void issueReward(LexhoReferralUse referral,
                             Long userId,
                             ReferralRewardBeneficiary beneficiary,
                             BigDecimal amount,
                             Long bookingId) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        String key = "referral:" + referral.getId() + ":" + beneficiary.name();
        ReferralRewardTransaction existing = rewardTransactionRepository.findByIdempotencyKey(key).orElse(null);
        if (existing != null) {
            ensureRewardCoupon(existing, userId, amount);
            return;
        }
        ReferralRewardTransaction transaction = new ReferralRewardTransaction();
        transaction.setReferralUseId(referral.getId());
        transaction.setUserId(userId);
        transaction.setBeneficiary(beneficiary);
        transaction.setAmount(money(amount));
        transaction.setQualifyingBookingId(bookingId);
        transaction.setIdempotencyKey(key);
        transaction.setStatus("ISSUED");
        ReferralRewardTransaction saved = rewardTransactionRepository.save(transaction);
        ensureRewardCoupon(saved, userId, amount);
    }

    private void ensureRewardCoupon(ReferralRewardTransaction transaction, Long userId, BigDecimal amount) {
        if (transaction.getRewardCouponId() != null) {
            return;
        }
        Coupon coupon = couponService.createReferralRewardCoupon(
                userId,
                amount,
                "referral-reward-" + transaction.getId()
        );
        transaction.setRewardCouponId(coupon.getId());
        rewardTransactionRepository.save(transaction);
    }

    private ReferralConfiguration currentConfiguration(boolean persistIfMissing) {
        return configurationRepository.findById(ReferralConfiguration.SINGLETON_ID)
                .orElseGet(() -> {
                    ReferralConfiguration config = defaultConfiguration();
                    return persistIfMissing ? configurationRepository.save(config) : config;
                });
    }

    private ReferralConfiguration defaultConfiguration() {
        ReferralConfiguration config = new ReferralConfiguration();
        config.setId(ReferralConfiguration.SINGLETON_ID);
        config.setActive(false);
        config.setReferrerRewardAmount(BigDecimal.ZERO.setScale(2));
        config.setReferredUserRewardAmount(BigDecimal.ZERO.setScale(2));
        config.setMinimumQualifyingPurchaseAmount(BigDecimal.ZERO.setScale(2));
        config.setQualificationCondition(QUALIFICATION_SUCCESSFUL_PAYMENT);
        return config;
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String code = codeGenerator.nextCode();
            if (!referralCodeRepository.existsByCodeIgnoreCase(code)) {
                return code;
            }
        }
        return "LEXHO" + java.util.UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 12)
                .toUpperCase(Locale.ROOT);
    }

    private ReferralResponse toResponse(LexhoReferralUse use) {
        return new ReferralResponse(use.getId(), use.getReferrerUserId(), use.getUserId(), use.getCode(),
                use.getStatus(), use.getCreatedAt(), use.getQualifiedAt(), use.getRewardedAt(),
                use.getQualifyingBookingId());
    }

    private ReferralConfigurationResponse toResponse(ReferralConfiguration config) {
        return new ReferralConfigurationResponse(config.isActive(), config.getReferrerRewardAmount(),
                config.getReferredUserRewardAmount(), config.getMinimumQualifyingPurchaseAmount(),
                config.getQualificationCondition(), config.getExpiryDays(), config.getUpdatedAt(),
                config.getUpdatedBy());
    }

    private BigDecimal money(BigDecimal amount) {
        return (amount == null ? BigDecimal.ZERO : amount).setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BadRequestException("code is required");
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeCondition(String condition) {
        String normalized = condition == null ? "" : condition.trim().toUpperCase(Locale.ROOT);
        if (!QUALIFICATION_SUCCESSFUL_PAYMENT.equals(normalized)) {
            throw new BadRequestException("Unsupported qualificationCondition");
        }
        return normalized;
    }

    private Long currentAdminId() {
        return securityUtils.getCurrentUserId().orElse(0L);
    }
}

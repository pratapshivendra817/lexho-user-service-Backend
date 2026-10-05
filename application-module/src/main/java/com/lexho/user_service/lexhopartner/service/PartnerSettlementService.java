package com.lexho.user_service.lexhopartner.service;

import com.lexho.common.enums.PaymentStatus;
import com.lexho.payment.event.PaymentCapturedEvent;
import com.lexho.payment.event.PaymentRefundProcessedEvent;
import com.lexho.user_service.admin.entity.Payout;
import com.lexho.user_service.admin.repository.PayoutRepository;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.lexhopartner.config.PartnerSettlementProperties;
import com.lexho.user_service.lexhopartner.dto.CreateWithdrawalRequest;
import com.lexho.user_service.lexhopartner.dto.PageResponseDto;
import com.lexho.user_service.lexhopartner.dto.PartnerEarningTransactionDto;
import com.lexho.user_service.lexhopartner.dto.PartnerEarningsSummaryDto;
import com.lexho.user_service.lexhopartner.dto.PartnerPayoutDto;
import com.lexho.user_service.lexhopartner.dto.PartnerWithdrawalDto;
import com.lexho.user_service.lexhopartner.entity.OrderStatus;
import com.lexho.user_service.lexhopartner.entity.PartnerOrder;
import com.lexho.user_service.lexhopartner.entity.PartnerSettlementRefund;
import com.lexho.user_service.lexhopartner.entity.PartnerWithdrawalRequest;
import com.lexho.user_service.lexhopartner.repository.PartnerOrderRepository;
import com.lexho.user_service.lexhopartner.repository.PartnerSettlementRefundRepository;
import com.lexho.user_service.lexhopartner.repository.PartnerWithdrawalRequestRepository;
import com.lexho.user_service.localization.dynamic.DynamicContentTranslationService;
import com.lexho.user_service.util.LexhoConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PartnerSettlementService {

    private static final List<String> RESERVED_WITHDRAWAL_STATUSES =
            List.of("REQUESTED", "APPROVED", "PROCESSING", "PAID");

    private final PayoutRepository payoutRepository;
    private final PartnerOrderRepository partnerOrderRepository;
    private final PartnerWithdrawalRequestRepository withdrawalRepository;
    private final PartnerSettlementRefundRepository settlementRefundRepository;
    private final PartnerSettlementProperties properties;
    private final DynamicContentTranslationService dynamicTranslationService;
    private final CompletionSettingsService completionSettingsService;

    public PartnerSettlementService(PayoutRepository payoutRepository,
                                    PartnerOrderRepository partnerOrderRepository,
                                    PartnerWithdrawalRequestRepository withdrawalRepository,
                                    PartnerSettlementRefundRepository settlementRefundRepository,
                                    PartnerSettlementProperties properties) {
        this(payoutRepository, partnerOrderRepository, withdrawalRepository, settlementRefundRepository, properties,
                Optional.empty(), Optional.empty());
    }

    public PartnerSettlementService(PayoutRepository payoutRepository,
                                    PartnerOrderRepository partnerOrderRepository,
                                    PartnerWithdrawalRequestRepository withdrawalRepository,
                                    PartnerSettlementRefundRepository settlementRefundRepository,
                                    PartnerSettlementProperties properties,
                                    DynamicContentTranslationService dynamicTranslationService) {
        this(payoutRepository, partnerOrderRepository, withdrawalRepository, settlementRefundRepository, properties,
                Optional.ofNullable(dynamicTranslationService), Optional.empty());
    }

    @Autowired
    public PartnerSettlementService(PayoutRepository payoutRepository,
                                    PartnerOrderRepository partnerOrderRepository,
                                    PartnerWithdrawalRequestRepository withdrawalRepository,
                                    PartnerSettlementRefundRepository settlementRefundRepository,
                                    PartnerSettlementProperties properties,
                                    Optional<DynamicContentTranslationService> dynamicTranslationService,
                                    Optional<CompletionSettingsService> completionSettingsService) {
        this.payoutRepository = payoutRepository;
        this.partnerOrderRepository = partnerOrderRepository;
        this.withdrawalRepository = withdrawalRepository;
        this.settlementRefundRepository = settlementRefundRepository;
        this.properties = properties;
        this.dynamicTranslationService = optionalValue(dynamicTranslationService);
        this.completionSettingsService = optionalValue(completionSettingsService);
    }

    private static <T> T optionalValue(Optional<T> optional) {
        return optional == null ? null : optional.orElse(null);
    }

    @EventListener
    @Transactional
    public void onPaymentCaptured(PaymentCapturedEvent event) {
        ensurePayoutInternal(event.bookingId());
    }

    @EventListener
    @Transactional
    public void onRefundProcessed(PaymentRefundProcessedEvent event) {
        reversePayout(event.bookingId(), event.razorpayRefundId(), event.amount());
    }

    @Transactional
    public void ensurePayout(Long bookingId) {
        ensurePayoutInternal(bookingId);
    }

    private void ensurePayoutInternal(Long bookingId) {
        if (payoutRepository.findTopByReferenceBookingIdOrderByCreatedAtDesc(bookingId).isPresent()) {
            return;
        }
        PartnerOrder order = partnerOrderRepository.findByIdForUpdate(bookingId).orElse(null);
        if (order == null || order.getAssignedPartnerId() == null
                || order.getStatus() != OrderStatus.COMPLETED
                || order.getPaymentStatus() != PaymentStatus.SUCCESS) {
            return;
        }
        if (payoutRepository.findTopByReferenceBookingIdOrderByCreatedAtDesc(bookingId).isPresent()) {
            return;
        }
        BigDecimal gross = money(order.getAmount());
        BigDecimal commissionPercent = completionSettingsService == null
                ? properties.getCommissionPercent()
                : completionSettingsService.commissionPercentFor(order);
        BigDecimal commission = gross.multiply(commissionPercent)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        Payout payout = new Payout();
        payout.setProviderId(order.getAssignedPartnerId());
        payout.setReferenceBookingId(order.getId());
        payout.setGrossAmount(gross);
        payout.setCommissionPercent(commissionPercent);
        payout.setCommissionAmount(commission);
        payout.setAmount(gross.subtract(commission).doubleValue());
        payout.setCurrency("INR");
        payout.setReversedAmount(BigDecimal.ZERO);
        payout.setStatus("PENDING");
        try {
            payoutRepository.saveAndFlush(payout);
        } catch (DataIntegrityViolationException ignored) {
            // Another transaction already materialized the same booking payout.
        }
    }

    @Transactional(readOnly = true)
    public Optional<BigDecimal> netEarningForOrder(Long bookingId) {
        return netEarningForOrderInternal(bookingId);
    }

    private Optional<BigDecimal> netEarningForOrderInternal(Long bookingId) {
        return payoutRepository.findTopByReferenceBookingIdOrderByCreatedAtDesc(bookingId)
                .map(this::availableAmount);
    }

    @Transactional(readOnly = true)
    public Double partnerVisibleAmountForOrder(PartnerOrder order) {
        if (order == null) {
            return null;
        }
        if (order.getStatus() == OrderStatus.COMPLETED && order.getId() != null) {
            Optional<BigDecimal> settledAmount = netEarningForOrderInternal(order.getId());
            if (settledAmount.isPresent()) {
                return settledAmount.get().doubleValue();
            }
        }
        BigDecimal gross = money(order.getAmount());
        BigDecimal commissionPercent = completionSettingsService == null
                ? properties.getCommissionPercent()
                : completionSettingsService.commissionPercentFor(order);
        BigDecimal commission = gross.multiply(commissionPercent)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        return gross.subtract(commission).max(BigDecimal.ZERO).doubleValue();
    }

    @Transactional(readOnly = true)
    public PageResponseDto<PartnerPayoutDto> payouts(Long partnerId, int page, int size) {
        var result = payoutRepository.findByProviderId(
                partnerId,
                PageRequest.of(Math.max(page, 0), limit(size), Sort.by(LexhoConstants.FIELD_CREATED_AT).descending())
        );
        return new PageResponseDto<>(result.map(this::toPayout).getContent(), result.getNumber(),
                result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public PartnerPayoutDto payout(Long partnerId, Long payoutId) {
        return payoutRepository.findByIdAndProviderId(payoutId, partnerId)
                .map(this::toPayout)
                .orElseThrow(() -> new ResourceNotFoundException("Payout not found"));
    }

    @Transactional(readOnly = true)
    public PartnerEarningsSummaryDto earningsSummary(Long partnerId) {
        List<Payout> payouts = payoutRepository.findByProviderId(partnerId);
        LocalDate today = LocalDate.now();
        LocalDateTime startToday = today.atStartOfDay();
        LocalDateTime startWeek = today.minusDays(today.getDayOfWeek().getValue() - 1L).atStartOfDay();
        LocalDateTime startMonth = today.withDayOfMonth(1).atStartOfDay();
        double lifetime = sumNet(payouts, LocalDateTime.MIN);
        double availableBalance = withdrawableBalanceInternal(partnerId).doubleValue();
        double pendingSettlement = payouts.stream()
                .filter(payout -> "PENDING".equals(payout.getStatus()) || "PROCESSING".equals(payout.getStatus()))
                .map(this::availableAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .doubleValue();
        long countLifetime = payouts.size();
        return new PartnerEarningsSummaryDto(
                sumNet(payouts, startToday),
                sumNet(payouts, startWeek),
                sumNet(payouts, startMonth),
                availableBalance,
                pendingSettlement,
                lifetime,
                countFrom(payouts, startToday),
                countFrom(payouts, startWeek),
                countFrom(payouts, startMonth),
                countLifetime,
                countLifetime == 0 ? 0d : round2(lifetime / countLifetime)
        );
    }

    @Transactional(readOnly = true)
    public PageResponseDto<PartnerEarningTransactionDto> earningTransactions(Long partnerId,
                                                                              LocalDate fromDate,
                                                                              LocalDate toDate,
                                                                              int page,
                                                                              int size) {
        LocalDateTime start = fromDate == null ? LocalDate.of(1970, 1, 1).atStartOfDay() : fromDate.atStartOfDay();
        LocalDateTime end = toDate == null ? LocalDateTime.now() : toDate.plusDays(1).atStartOfDay().minusNanos(1);
        if (end.isBefore(start)) {
            throw new BadRequestException("toDate must be greater than or equal to fromDate");
        }
        var result = payoutRepository.findByProviderIdAndCreatedAtBetween(
                partnerId, start, end,
                PageRequest.of(Math.max(page, 0), limit(size), Sort.by(LexhoConstants.FIELD_CREATED_AT).descending())
        );
        List<PartnerEarningTransactionDto> items = result.getContent().stream().map(payout -> {
            PartnerOrder order = partnerOrderRepository.findById(payout.getReferenceBookingId()).orElse(null);
            String serviceType = order == null ? null : order.getServiceType();
            String translatedServiceType = translatedServiceType(partnerId, order, serviceType);
            String description = serviceType == null || serviceType.isBlank()
                    ? "Booking #" + payout.getReferenceBookingId() + " settlement"
                    : translatedServiceType + " settlement";
            return new PartnerEarningTransactionDto(
                    payout.getId(),
                    payout.getReferenceBookingId(),
                    description,
                    availableAmount(payout).doubleValue(),
                    availableAmount(payout).doubleValue(),
                    availableAmount(payout).doubleValue(),
                    "EARNING",
                    payout.getStatus(),
                    payout.getCreatedAt(),
                    order == null ? null : order.getCustomerName(),
                    translatedServiceType
            );
        }).toList();
        return new PageResponseDto<>(items, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    private String translatedServiceType(Long partnerId, PartnerOrder order, String serviceType) {
        if (dynamicTranslationService == null || order == null || serviceType == null || serviceType.isBlank()) {
            return serviceType;
        }
        return dynamicTranslationService.translateForPartner(
                partnerId, null, "partner_order", order.getId(), "service_type", serviceType);
    }

    @Transactional
    public PartnerWithdrawalDto requestWithdrawal(Long partnerId, CreateWithdrawalRequest request) {
        BigDecimal amount = request.amount().setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(properties.getMinWithdrawalAmount()) < 0) {
            throw new BadRequestException("Minimum withdrawal amount is " + properties.getMinWithdrawalAmount());
        }
        if (amount.compareTo(withdrawableBalanceInternal(partnerId)) > 0) {
            throw new BadRequestException("Withdrawal amount exceeds available balance");
        }
        PartnerWithdrawalRequest withdrawal = new PartnerWithdrawalRequest();
        withdrawal.setPartnerId(partnerId);
        withdrawal.setAmount(amount);
        withdrawal.setStatus("REQUESTED");
        return toWithdrawal(withdrawalRepository.save(withdrawal));
    }

    @Transactional(readOnly = true)
    public PageResponseDto<PartnerWithdrawalDto> withdrawals(Long partnerId, int page, int size) {
        var result = withdrawalRepository.findByPartnerId(
                partnerId,
                PageRequest.of(Math.max(page, 0), limit(size), Sort.by(LexhoConstants.FIELD_CREATED_AT).descending())
        );
        return new PageResponseDto<>(result.map(this::toWithdrawal).getContent(), result.getNumber(),
                result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public BigDecimal withdrawableBalance(Long partnerId) {
        return withdrawableBalanceInternal(partnerId);
    }

    private BigDecimal withdrawableBalanceInternal(Long partnerId) {
        BigDecimal settled = payoutRepository.findByProviderId(partnerId).stream()
                .filter(payout -> "SETTLED".equals(payout.getStatus()))
                .map(this::availableAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal reserved = withdrawalRepository.findByPartnerIdAndStatusIn(partnerId, RESERVED_WITHDRAWAL_STATUSES)
                .stream().map(PartnerWithdrawalRequest::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return settled.subtract(reserved).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private void reversePayout(Long bookingId, String refundId, BigDecimal refundAmount) {
        if (refundId == null || refundId.isBlank() || settlementRefundRepository.existsById(refundId)) {
            return;
        }
        Payout payout = payoutRepository.findTopByReferenceBookingIdOrderByCreatedAtDesc(bookingId).orElse(null);
        if (payout == null) {
            return;
        }
        BigDecimal gross = gross(payout);
        BigDecimal net = money(payout.getAmount());
        BigDecimal reversal = gross.signum() == 0 ? BigDecimal.ZERO
                : refundAmount.multiply(net).divide(gross, 2, RoundingMode.HALF_UP);
        BigDecimal reversed = reversed(payout).add(reversal).min(net);
        payout.setReversedAmount(reversed);
        payout.setReversedAt(LocalDateTime.now());
        payout.setStatus(reversed.compareTo(net) >= 0 ? "REVERSED" : "PARTIALLY_REVERSED");
        payoutRepository.save(payout);

        PartnerSettlementRefund refund = new PartnerSettlementRefund();
        refund.setRazorpayRefundId(refundId);
        refund.setPayoutId(payout.getId());
        refund.setReversedAmount(reversal);
        refund.setCreatedAt(LocalDateTime.now());
        settlementRefundRepository.save(refund);
    }

    private PartnerPayoutDto toPayout(Payout payout) {
        return new PartnerPayoutDto(payout.getId(), payout.getReferenceBookingId(), money(payout.getAmount()),
                reversed(payout), payout.getCurrency(), payout.getStatus(), payout.getProcessedAt(),
                payout.getCreatedAt());
    }

    private PartnerWithdrawalDto toWithdrawal(PartnerWithdrawalRequest request) {
        return new PartnerWithdrawalDto(request.getId(), request.getAmount(), request.getStatus(),
                request.getRemarks(), request.getCreatedAt(), request.getProcessedAt());
    }

    private double sumNet(List<Payout> payouts, LocalDateTime from) {
        return payouts.stream().filter(payout -> !payout.getCreatedAt().isBefore(from))
                .map(this::availableAmount).reduce(BigDecimal.ZERO, BigDecimal::add).doubleValue();
    }

    private long countFrom(List<Payout> payouts, LocalDateTime from) {
        return payouts.stream().filter(payout -> !payout.getCreatedAt().isBefore(from)).count();
    }

    private BigDecimal availableAmount(Payout payout) {
        return money(payout.getAmount()).subtract(reversed(payout)).max(BigDecimal.ZERO);
    }

    private BigDecimal gross(Payout payout) {
        return payout.getGrossAmount() == null ? money(payout.getAmount()) : payout.getGrossAmount();
    }

    private BigDecimal reversed(Payout payout) {
        return payout.getReversedAmount() == null ? BigDecimal.ZERO : payout.getReversedAmount();
    }

    private BigDecimal money(Double value) {
        return value == null ? BigDecimal.ZERO : BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private int limit(int size) {
        return Math.min(Math.max(size, 1), 100);
    }

    private double round2(double value) {
        return Math.round(value * 100d) / 100d;
    }
}

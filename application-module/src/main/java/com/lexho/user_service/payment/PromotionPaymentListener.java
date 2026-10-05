package com.lexho.user_service.payment;

import com.lexho.booking.entity.Order;
import com.lexho.booking.repository.OrderRepository;
import com.lexho.payment.event.PaymentCapturedEvent;
import com.lexho.user_service.coupon.service.CouponService;
import com.lexho.user_service.referral.service.ReferralService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class PromotionPaymentListener {
    private final CouponService couponService;
    private final ReferralService referralService;
    private final OrderRepository orderRepository;

    public PromotionPaymentListener(CouponService couponService,
                                    ReferralService referralService,
                                    OrderRepository orderRepository) {
        this.couponService = couponService;
        this.referralService = referralService;
        this.orderRepository = orderRepository;
    }

    @EventListener
    @Transactional
    public void onPaymentCaptured(PaymentCapturedEvent event) {
        couponService.recordRedemption(event.bookingId());
        orderRepository.findById(event.bookingId()).ifPresent(order ->
                referralService.qualifyAndReward(order.getUserId(), order.getId(), paidAmount(order)));
    }

    private BigDecimal paidAmount(Order order) {
        if (order.getTotalAmount() == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(order.getTotalAmount()).setScale(2, RoundingMode.HALF_UP);
    }
}

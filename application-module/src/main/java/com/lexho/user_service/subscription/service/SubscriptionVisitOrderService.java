package com.lexho.user_service.subscription.service;

import com.lexho.booking.entity.OrderItem;
import com.lexho.booking.repository.OrderRepository;
import com.lexho.common.enums.PaymentStatus;
import com.lexho.user.repository.UserRepository;
import com.lexho.user_service.dispatch.repository.PartnerShiftRepository;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.lexhopartner.entity.OrderStatus;
import com.lexho.user_service.lexhopartner.entity.PartnerOrder;
import com.lexho.user_service.lexhopartner.entity.PaymentMode;
import com.lexho.user_service.lexhopartner.repository.PartnerOrderRepository;
import com.lexho.user_service.partnerrequest.entity.PartnerServiceRequestEntity;
import com.lexho.user_service.subscription.enums.SubscriptionStatus;
import com.lexho.user_service.subscription.enums.VisitStatus;
import com.lexho.user_service.subscription.repository.SubscriptionMasterRepository;
import com.lexho.user_service.subscription.repository.SubscriptionVisitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/** Each prepaid visit uses the existing order/PIN/settlement lifecycle. */
@Service
public class SubscriptionVisitOrderService {
    private final SubscriptionVisitRepository visits;
    private final SubscriptionMasterRepository subscriptions;
    private final PartnerOrderRepository partnerOrders;
    private final OrderRepository orders;
    private final UserRepository users;
    private final PartnerShiftRepository shifts;
    private final jakarta.persistence.EntityManager entityManager;

    public SubscriptionVisitOrderService(SubscriptionVisitRepository visits,
            SubscriptionMasterRepository subscriptions, PartnerOrderRepository partnerOrders,
            OrderRepository orders, UserRepository users, PartnerShiftRepository shifts,
            jakarta.persistence.EntityManager entityManager) {
        this.visits = visits;
        this.subscriptions = subscriptions;
        this.partnerOrders = partnerOrders;
        this.orders = orders;
        this.users = users;
        this.shifts = shifts;
        this.entityManager = entityManager;
    }

    @Transactional
    public Long accept(PartnerServiceRequestEntity request, Long partnerId) {
        var visit = visits.findByIdForUpdate(request.getSubscriptionVisitId())
                .orElseThrow(() -> new BadRequestException("Subscription visit not found"));
        if (!visit.getSubscriptionId().equals(request.getSubscriptionId())) {
            throw new BadRequestException("Subscription visit does not match request");
        }
        if (visit.getBookingId() != null) {
            var existing = partnerOrders.findById(visit.getBookingId()).orElseThrow();
            if (!partnerId.equals(existing.getAssignedPartnerId())) {
                throw new BadRequestException("Visit already accepted by another partner");
            }
            return existing.getId();
        }
        var subscription = subscriptions.findById(visit.getSubscriptionId()).orElseThrow();
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE
                || (visit.getStatus() != VisitStatus.SCHEDULED && visit.getStatus() != VisitStatus.RESCHEDULED)) {
            throw new BadRequestException("Subscription visit is not available");
        }
        var payment = orders.findById(subscription.getBookingId()).orElseThrow();
        if (payment.getPaymentStatus() != PaymentStatus.SUCCESS) {
            throw new BadRequestException("Subscription payment has not been verified");
        }
        var customer = users.findById(subscription.getCustomerId()).orElseThrow();
        var allVisits = visits.findBySubscriptionIdOrderByVisitDateAscIdAsc(subscription.getId());
        var total = BigDecimal.valueOf(payment.getTotalAmount()).setScale(2, RoundingMode.HALF_UP);
        var perVisit = total.divide(BigDecimal.valueOf(allVisits.size()), 2, RoundingMode.DOWN);
        // Allocate the remaining paise once, so visit payouts never exceed the paid plan amount.
        if (visit.getId().equals(allVisits.get(allVisits.size() - 1).getId())) {
            perVisit = total.subtract(perVisit.multiply(BigDecimal.valueOf(allVisits.size() - 1L)));
        }
        var order = new PartnerOrder();
        order.setCustomerId(subscription.getCustomerId());
        order.setCustomerName(customer.getFullName() == null ? "Customer" : customer.getFullName());
        order.setCustomerPhone(customer.getPhoneNumber() == null ? "" : customer.getPhoneNumber());
        order.setAddress(request.getFullAddress());
        order.setLatitude(request.getLatitude().doubleValue());
        order.setLongitude(request.getLongitude().doubleValue());
        order.setServiceType(request.getServiceType());
        order.setAmount(perVisit.doubleValue());
        order.setPaymentMode(PaymentMode.ONLINE);
        order.setPaymentStatus(PaymentStatus.SUCCESS);
        order.setStatus(OrderStatus.ACCEPTED);
        order.setAssignedPartnerId(partnerId);
        order.setAcceptedAt(LocalDateTime.now());
        var saved = partnerOrders.saveAndFlush(order);
        // Order and PartnerOrder map the same orders table; populate customer-facing fields too.
        var booking = orders.findById(saved.getId()).orElseThrow();
        entityManager.createNativeQuery("UPDATE orders SET total_amount = :amount, booking_type = 'SCHEDULED', scheduled_at = :scheduledAt WHERE id = :id")
                .setParameter("amount", perVisit.doubleValue())
                .setParameter("scheduledAt", request.getScheduledTime())
                .setParameter("id", saved.getId()).executeUpdate();
        var item = new OrderItem();
        item.setOrder(booking);
        item.setPartnerId(subscription.getServiceId());
        item.setPartnerName(request.getServiceType());
        item.setPrice(perVisit.doubleValue());
        item.setQuantity(1);
        entityManager.persist(item);
        visit.setBookingId(saved.getId());
        visit.setStatus(VisitStatus.ACCEPTED);
        visits.save(visit);
        shifts.findByPartnerIdForUpdate(partnerId).ifPresent(shift -> {
            shift.setActiveJobsCount(shift.getActiveJobsCount() + 1);
            shifts.save(shift);
        });
        return saved.getId();
    }

    @Transactional
    public void syncStatus(PartnerOrder order) {
        visits.findByBookingId(order.getId()).ifPresent(visit -> {
            VisitStatus next = switch (order.getStatus()) {
                case ACCEPTED -> VisitStatus.ACCEPTED;
                case ARRIVED -> VisitStatus.ARRIVED;
                case STARTED, IN_PROGRESS -> VisitStatus.IN_PROGRESS;
                case COMPLETED -> VisitStatus.COMPLETED;
                case CANCELLED -> VisitStatus.CANCELLED;
                default -> null;
            };
            if (next == null || visit.getStatus() == next) return;
            visit.setStatus(next);
            visits.saveAndFlush(visit);
            if (next == VisitStatus.COMPLETED || next == VisitStatus.CANCELLED) {
                shifts.findByPartnerIdForUpdate(order.getAssignedPartnerId()).ifPresent(shift -> {
                    shift.setActiveJobsCount(Math.max(0, shift.getActiveJobsCount() - 1));
                    shifts.save(shift);
                });
            }
            if (next == VisitStatus.COMPLETED) {
                var subscription = subscriptions.findById(visit.getSubscriptionId()).orElseThrow();
                if (visits.countBySubscriptionIdAndStatus(subscription.getId(), VisitStatus.COMPLETED) == subscription.getVisits()) {
                    subscription.setStatus(SubscriptionStatus.COMPLETED);
                    subscriptions.save(subscription);
                }
            }
        });
    }
}

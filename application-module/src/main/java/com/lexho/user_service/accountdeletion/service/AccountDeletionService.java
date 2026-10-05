package com.lexho.user_service.accountdeletion.service;

import com.lexho.booking.repository.OrderRepository;
import com.lexho.common.enums.OrderStatus;
import com.lexho.payment.entity.PaymentRefundStatus;
import com.lexho.payment.entity.PaymentTransactionStatus;
import com.lexho.payment.repository.PaymentRefundRepository;
import com.lexho.payment.repository.PaymentTransactionRepository;
import com.lexho.user_service.accountdeletion.dto.AccountDeletionCreateRequest;
import com.lexho.user_service.accountdeletion.dto.AccountDeletionRejectRequest;
import com.lexho.user_service.accountdeletion.dto.AccountDeletionResponse;
import com.lexho.user_service.accountdeletion.entity.AccountDeletionRequest;
import com.lexho.user_service.accountdeletion.repository.AccountDeletionRequestRepository;
import com.lexho.user_service.admin.entity.Payout;
import com.lexho.user_service.admin.repository.PayoutRepository;
import com.lexho.user_service.audit.service.AuditLogService;
import com.lexho.user_service.customer.profile.repository.CustomerRepository;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.lexhopartner.repository.PartnerOperationalProfileRepository;
import com.lexho.user_service.lexhopartner.repository.PartnerWithdrawalRequestRepository;
import com.lexho.user_service.notification.service.NotificationTriggerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class AccountDeletionService {
    private static final String STATUS_REQUESTED = "REQUESTED";
    private static final String STATUS_UNDER_REVIEW = "UNDER_REVIEW";
    private static final String STATUS_SCHEDULED = "SCHEDULED";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_DELETED = "DELETED";
    private static final String STATUS_PAID = "PAID";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PARTIALLY_REVERSED = "PARTIALLY_REVERSED";
    private static final String STATUS_SETTLED = "SETTLED";
    private static final String STATUS_NONE = "NONE";
    private static final String USER_TYPE_CUSTOMER = "CUSTOMER";
    private static final String USER_TYPE_PARTNER = "PARTNER";
    private static final String ACTOR_ROLE_ADMIN = "ADMIN";
    private static final String ACCOUNT_DELETION_RESOURCE = "ACCOUNT_DELETION_REQUEST";
    private static final String ACTION_REQUESTED = "ACCOUNT_DELETION_REQUESTED";
    private static final String ACTION_CANCELLED = "ACCOUNT_DELETION_CANCELLED";
    private static final String ACTION_APPROVED = "ACCOUNT_DELETION_APPROVED";
    private static final String ACTION_REJECTED = "ACCOUNT_DELETION_REJECTED";
    private static final String ACTION_PROCESSED = "ACCOUNT_DELETION_PROCESSED";
    private static final List<String> OPEN_STATUSES = List.of(STATUS_REQUESTED, STATUS_UNDER_REVIEW, STATUS_SCHEDULED);
    private static final List<String> APPROVABLE_STATUSES = List.of(STATUS_REQUESTED, STATUS_UNDER_REVIEW);
    private static final List<String> PENDING_PAYOUT_STATUSES = List.of(STATUS_PENDING, STATUS_PROCESSING,
            STATUS_PARTIALLY_REVERSED);
    private static final List<String> OPEN_WITHDRAWAL_STATUSES = List.of(STATUS_REQUESTED, STATUS_APPROVED,
            STATUS_PROCESSING);
    private static final List<String> RESERVED_WITHDRAWAL_STATUSES = List.of(STATUS_REQUESTED, STATUS_APPROVED,
            STATUS_PROCESSING, STATUS_PAID);
    private static final Set<OrderStatus> ACTIVE_BOOKING_STATUSES = Set.of(
            OrderStatus.SEARCHING_PARTNER, OrderStatus.SCHEDULED, OrderStatus.BOOKING_CREATED,
            OrderStatus.PARTNER_SEARCH, OrderStatus.PARTNER_ASSIGNED, OrderStatus.PARTNER_ACCEPTED,
            OrderStatus.ARRIVED, OrderStatus.PARTNER_REACHED, OrderStatus.SERVICE_STARTED,
            OrderStatus.SERVICE_IN_PROGRESS, OrderStatus.CREATED, OrderStatus.CONFIRMED,
            OrderStatus.PREPARING, OrderStatus.OUT_FOR_DELIVERY
    );

    private final AccountDeletionRequestRepository repository;
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final PartnerOperationalProfileRepository partnerProfileRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentRefundRepository paymentRefundRepository;
    private final PayoutRepository payoutRepository;
    private final PartnerWithdrawalRequestRepository withdrawalRepository;
    private final AuditLogService auditLogService;
    private final NotificationTriggerService notificationTriggerService;

    public AccountDeletionService(AccountDeletionRequestRepository repository,
                                  OrderRepository orderRepository,
                                  CustomerRepository customerRepository,
                                  PartnerOperationalProfileRepository partnerProfileRepository,
                                  PaymentTransactionRepository paymentTransactionRepository,
                                  PaymentRefundRepository paymentRefundRepository,
                                  PayoutRepository payoutRepository,
                                  PartnerWithdrawalRequestRepository withdrawalRepository) {
        this(repository, orderRepository, customerRepository, partnerProfileRepository, paymentTransactionRepository,
                paymentRefundRepository, payoutRepository, withdrawalRepository, Optional.empty(), Optional.empty());
    }

    @Autowired
    public AccountDeletionService(AccountDeletionRequestRepository repository,
                                  OrderRepository orderRepository,
                                  CustomerRepository customerRepository,
                                  PartnerOperationalProfileRepository partnerProfileRepository,
                                  PaymentTransactionRepository paymentTransactionRepository,
                                  PaymentRefundRepository paymentRefundRepository,
                                  PayoutRepository payoutRepository,
                                  PartnerWithdrawalRequestRepository withdrawalRepository,
                                  Optional<AuditLogService> auditLogService,
                                  Optional<NotificationTriggerService> notificationTriggerService) {
        this.repository = repository;
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.partnerProfileRepository = partnerProfileRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.paymentRefundRepository = paymentRefundRepository;
        this.payoutRepository = payoutRepository;
        this.withdrawalRepository = withdrawalRepository;
        this.auditLogService = auditLogService.orElse(null);
        this.notificationTriggerService = notificationTriggerService.orElse(null);
    }

    @Transactional
    public AccountDeletionResponse request(Long userId, String role, AccountDeletionCreateRequest request) {
        String userType = normalizeUserType(role);
        if (repository.existsByUserIdAndUserTypeAndStatusIn(userId, userType, OPEN_STATUSES)) {
            return statusFor(userId, userType);
        }
        AccountDeletionRequest entity = new AccountDeletionRequest();
        entity.setUserId(userId);
        entity.setUserType(userType);
        entity.setStatus(STATUS_REQUESTED);
        entity.setRequestReason(trim(request == null ? null : request.reason()));
        refreshChecks(entity);
        AccountDeletionRequest saved = repository.save(entity);
        audit(userId, userType, ACTION_REQUESTED, saved);
        notifyUser(userId, userType, "received", saved);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public AccountDeletionResponse status(Long userId, String role) {
        return statusFor(userId, normalizeUserType(role));
    }

    private AccountDeletionResponse statusFor(Long userId, String userType) {
        return repository.findTopByUserIdAndUserTypeOrderByRequestedAtDesc(userId, userType)
                .map(this::toResponse)
                .orElseGet(() -> new AccountDeletionResponse(null, userId, userType, STATUS_NONE,
                        null, null, 0, 0, 0, 0, null, null, null));
    }

    @Transactional
    public AccountDeletionResponse cancel(Long userId, String role) {
        AccountDeletionRequest entity = repository.findTopByUserIdAndUserTypeOrderByRequestedAtDesc(userId, normalizeUserType(role))
                .orElseThrow(() -> new ResourceNotFoundException("Account deletion request not found"));
        if (!OPEN_STATUSES.contains(entity.getStatus())) {
            throw new BadRequestException("Account deletion request cannot be cancelled in current status");
        }
        entity.setStatus(STATUS_CANCELLED);
        AccountDeletionRequest saved = repository.save(entity);
        audit(userId, entity.getUserType(), ACTION_CANCELLED, saved);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AccountDeletionResponse> adminList(String status) {
        List<AccountDeletionRequest> items = status == null || status.isBlank()
                ? repository.findAll()
                : repository.findByStatusOrderByRequestedAtDesc(status.trim().toUpperCase(Locale.ROOT));
        return items.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AccountDeletionResponse adminGet(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public AccountDeletionResponse approve(Long id, Long adminId) {
        AccountDeletionRequest entity = find(id);
        if (!APPROVABLE_STATUSES.contains(entity.getStatus())) {
            throw new BadRequestException("Account deletion request cannot be approved in current status");
        }
        refreshChecks(entity);
        if (entity.getActiveBookings() > 0 || entity.getPendingPayments() > 0 || entity.getPendingRefunds() > 0
                || entity.getPendingPartnerPayouts() > 0) {
            entity.setStatus(STATUS_UNDER_REVIEW);
            entity.setReviewedByAdminId(adminId);
            entity.setReviewedAt(LocalDateTime.now());
            repository.save(entity);
            throw new BadRequestException("Request has unresolved business conditions");
        }
        entity.setStatus(STATUS_SCHEDULED);
        entity.setReviewedByAdminId(adminId);
        entity.setReviewedAt(LocalDateTime.now());
        AccountDeletionRequest saved = repository.save(entity);
        audit(adminId, ACTOR_ROLE_ADMIN, ACTION_APPROVED, saved);
        notifyUser(saved.getUserId(), saved.getUserType(), "approved", saved);
        return toResponse(saved);
    }

    @Transactional
    public AccountDeletionResponse reject(Long id, Long adminId, AccountDeletionRejectRequest request) {
        AccountDeletionRequest entity = find(id);
        if (!OPEN_STATUSES.contains(entity.getStatus())) {
            throw new BadRequestException("Account deletion request cannot be rejected in current status");
        }
        entity.setStatus(STATUS_REJECTED);
        entity.setRejectionReason(request.reason().trim());
        entity.setReviewedByAdminId(adminId);
        entity.setReviewedAt(LocalDateTime.now());
        AccountDeletionRequest saved = repository.save(entity);
        audit(adminId, ACTOR_ROLE_ADMIN, ACTION_REJECTED, saved);
        notifyUser(saved.getUserId(), saved.getUserType(), "rejected", saved);
        return toResponse(saved);
    }

    @Transactional
    public AccountDeletionResponse process(Long id, Long adminId) {
        AccountDeletionRequest entity = find(id);
        if (!STATUS_SCHEDULED.equals(entity.getStatus())) {
            throw new BadRequestException("Only scheduled account deletion requests can be processed");
        }
        refreshChecks(entity);
        if (entity.getActiveBookings() > 0 || entity.getPendingPayments() > 0 || entity.getPendingRefunds() > 0
                || entity.getPendingPartnerPayouts() > 0) {
            throw new BadRequestException("Request has unresolved business conditions");
        }
        anonymizeProfile(entity);
        entity.setStatus(STATUS_DELETED);
        entity.setProcessedByAdminId(adminId);
        entity.setProcessedAt(LocalDateTime.now());
        AccountDeletionRequest saved = repository.save(entity);
        audit(adminId, ACTOR_ROLE_ADMIN, ACTION_PROCESSED, saved);
        notifyUser(saved.getUserId(), saved.getUserType(), "completed", saved);
        return toResponse(saved);
    }

    private void refreshChecks(AccountDeletionRequest entity) {
        long activeBookings = USER_TYPE_PARTNER.equals(entity.getUserType())
                ? orderRepository.countByAssignedPartnerIdAndStatusIn(entity.getUserId(), ACTIVE_BOOKING_STATUSES)
                : orderRepository.countByUserIdAndStatusIn(entity.getUserId(), ACTIVE_BOOKING_STATUSES);
        entity.setActiveBookings(activeBookings);
        entity.setPendingPayments(USER_TYPE_CUSTOMER.equals(entity.getUserType())
                ? paymentTransactionRepository.countByCustomerIdAndPaymentStatusIn(
                entity.getUserId(), List.of(PaymentTransactionStatus.CREATED))
                : 0);
        entity.setPendingRefunds(USER_TYPE_CUSTOMER.equals(entity.getUserType())
                ? paymentRefundRepository.countByCustomerIdAndRefundStatusIn(
                entity.getUserId(), List.of(PaymentRefundStatus.REQUESTED))
                : 0);
        entity.setPendingPartnerPayouts(USER_TYPE_PARTNER.equals(entity.getUserType())
                ? countPendingPartnerPayoutObligations(entity.getUserId())
                : 0);
    }

    private long countPendingPartnerPayoutObligations(Long partnerId) {
        long pendingPayouts = payoutRepository.countByProviderIdAndStatusIn(partnerId, PENDING_PAYOUT_STATUSES);
        long openWithdrawals = withdrawalRepository.countByPartnerIdAndStatusIn(partnerId, OPEN_WITHDRAWAL_STATUSES);
        long availableEarnings = withdrawableBalance(partnerId).signum() > 0 ? 1 : 0;
        return pendingPayouts + openWithdrawals + availableEarnings;
    }

    private BigDecimal withdrawableBalance(Long partnerId) {
        BigDecimal settled = payoutRepository.findByProviderId(partnerId).stream()
                .filter(payout -> STATUS_SETTLED.equals(payout.getStatus()))
                .map(this::availableAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal reserved = withdrawalRepository.findByPartnerIdAndStatusIn(partnerId, RESERVED_WITHDRAWAL_STATUSES)
                .stream()
                .map(withdrawal -> withdrawal.getAmount() == null ? BigDecimal.ZERO : withdrawal.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return settled.subtract(reserved).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal availableAmount(Payout payout) {
        BigDecimal amount = payout.getAmount() == null
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(payout.getAmount()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal reversed = payout.getReversedAmount() == null ? BigDecimal.ZERO : payout.getReversedAmount();
        return amount.subtract(reversed).max(BigDecimal.ZERO);
    }

    private void anonymizeProfile(AccountDeletionRequest entity) {
        String marker = "deleted-user-" + entity.getId();
        if (USER_TYPE_PARTNER.equals(entity.getUserType())) {
            partnerProfileRepository.findById(entity.getUserId()).ifPresent(partner -> {
                partner.setDeleted(true);
                partner.setPhoneNumber(marker);
                partner.setFullName("Deleted Partner");
                partnerProfileRepository.save(partner);
            });
        } else {
            customerRepository.findById(entity.getUserId()).ifPresent(customer -> {
                customer.setActive(false);
                customer.setEmail(null);
                customer.setPhoneNumber(marker);
                customer.setMobile(marker);
                customer.setFullName("Deleted Customer");
                customer.setProfileImageUrl(null);
                customerRepository.save(customer);
            });
        }
    }

    private AccountDeletionRequest find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account deletion request not found"));
    }

    private AccountDeletionResponse toResponse(AccountDeletionRequest entity) {
        return new AccountDeletionResponse(entity.getId(), entity.getUserId(), entity.getUserType(), entity.getStatus(),
                entity.getRequestReason(), entity.getRejectionReason(), entity.getActiveBookings(),
                entity.getPendingPayments(), entity.getPendingRefunds(), entity.getPendingPartnerPayouts(),
                entity.getRequestedAt(), entity.getReviewedAt(), entity.getProcessedAt());
    }

    private String normalizeUserType(String role) {
        String normalized = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
        if (!List.of(USER_TYPE_CUSTOMER, USER_TYPE_PARTNER).contains(normalized)) {
            throw new BadRequestException("Account deletion is available for customer and partner accounts");
        }
        return normalized;
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void audit(Long actorId, String actorRole, String action, AccountDeletionRequest request) {
        if (auditLogService != null) {
            auditLogService.log(actorId, actorRole, action, ACCOUNT_DELETION_RESOURCE,
                    request.getId() == null ? null : request.getId().toString(), null, request.getStatus());
        }
    }

    private void notifyUser(Long userId, String userType, String event, AccountDeletionRequest request) {
        if (notificationTriggerService != null) {
            notificationTriggerService.accountDeletion(userId, userType, event,
                    request.getId() == null ? null : request.getId().toString());
        }
    }
}

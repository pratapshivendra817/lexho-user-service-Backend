package com.lexho.user_service.subscription.service;

import com.lexho.booking.entity.Order;
import com.lexho.booking.repository.OrderRepository;
import com.lexho.common.enums.OrderStatus;
import com.lexho.common.enums.PaymentStatus;
import com.lexho.user_service.admin.entity.AdminServiceEntity;
import com.lexho.user_service.admin.repository.AdminServiceEntityRepository;
import com.lexho.user_service.admin.serviceablearea.repository.ServiceableAreaRepository;
import com.lexho.user_service.config.DispatchProperties;
import com.lexho.user_service.dispatch.entity.CurrentPartnerLocationEntity;
import com.lexho.user_service.dispatch.entity.PartnerShiftEntity;
import com.lexho.user_service.dispatch.repository.CurrentPartnerLocationRepository;
import com.lexho.user_service.dispatch.repository.PartnerServiceSkillRepository;
import com.lexho.user_service.dispatch.repository.PartnerShiftRepository;
import com.lexho.user_service.dispatch.service.PartnerDispatchEligibilityService;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.lexho.entity.LexhoAddress;
import com.lexho.user_service.lexho.repository.LexhoAddressRepository;
import com.lexho.user_service.lexhopartner.entity.PartnerOperationalProfile;
import com.lexho.user_service.lexhopartner.repository.PartnerOperationalProfileRepository;
import com.lexho.user_service.partnerrequest.entity.PartnerServiceRequestEntity;
import com.lexho.user_service.partnerrequest.model.PartnerServiceRequestStatus;
import com.lexho.user_service.partnerrequest.repository.PartnerServiceRequestRepository;
import com.lexho.user_service.subscription.dto.AdminPartnerAssignRequest;
import com.lexho.user_service.subscription.dto.AdminSubscriptionAssignmentResponse;
import com.lexho.user_service.subscription.dto.AdminSubscriptionDetailResponse;
import com.lexho.user_service.subscription.dto.AdminSubscriptionListResponse;
import com.lexho.user_service.subscription.dto.AdminSubscriptionStatusUpdateRequest;
import com.lexho.user_service.subscription.dto.AdminSubscriptionVisitStatusUpdateRequest;
import com.lexho.user_service.subscription.dto.AdminVisitAssignmentResponse;
import com.lexho.user_service.subscription.dto.SubscriptionCalendarItemResponse;
import com.lexho.user_service.subscription.entity.SubscriptionPartnerAssignment;
import com.lexho.user_service.subscription.entity.SubscriptionFrequency;
import com.lexho.user_service.subscription.entity.SubscriptionMaster;
import com.lexho.user_service.subscription.entity.SubscriptionVisit;
import com.lexho.user_service.subscription.entity.SubscriptionVisitPartnerAssignment;
import com.lexho.user_service.subscription.enums.PartnerAssignmentStatus;
import com.lexho.user_service.subscription.enums.SubscriptionStatus;
import com.lexho.user_service.subscription.enums.VisitStatus;
import com.lexho.user_service.subscription.repository.SubscriptionFrequencyRepository;
import com.lexho.user_service.subscription.repository.SubscriptionMasterRepository;
import com.lexho.user_service.subscription.repository.SubscriptionPartnerAssignmentRepository;
import com.lexho.user_service.subscription.repository.SubscriptionVisitRepository;
import com.lexho.user_service.subscription.repository.SubscriptionVisitPartnerAssignmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Duration;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminSubscriptionManagementService {

    private final SubscriptionMasterRepository subscriptionMasterRepository;
    private final OrderRepository orderRepository;
    private final SubscriptionFrequencyRepository subscriptionFrequencyRepository;
    private final SubscriptionVisitRepository subscriptionVisitRepository;
    private final AdminServiceEntityRepository serviceRepository;
    private final SubscriptionPartnerAssignmentRepository subscriptionPartnerAssignmentRepository;
    private final SubscriptionVisitPartnerAssignmentRepository visitPartnerAssignmentRepository;
    private final PartnerServiceRequestRepository partnerServiceRequestRepository;
    private final PartnerOperationalProfileRepository partnerProfileRepository;
    private final PartnerServiceSkillRepository partnerServiceSkillRepository;
    private final LexhoAddressRepository lexhoAddressRepository;
    private final PartnerShiftRepository partnerShiftRepository;
    private final CurrentPartnerLocationRepository currentPartnerLocationRepository;
    private final ServiceableAreaRepository serviceableAreaRepository;
    private final DispatchProperties dispatchProperties;
    private final PartnerDispatchEligibilityService partnerDispatchEligibilityService;

    public AdminSubscriptionManagementService(SubscriptionMasterRepository subscriptionMasterRepository,
                                              OrderRepository orderRepository,
                                              SubscriptionFrequencyRepository subscriptionFrequencyRepository,
                                              SubscriptionVisitRepository subscriptionVisitRepository,
                                              AdminServiceEntityRepository serviceRepository,
                                              SubscriptionPartnerAssignmentRepository subscriptionPartnerAssignmentRepository,
                                              SubscriptionVisitPartnerAssignmentRepository visitPartnerAssignmentRepository,
                                              PartnerServiceRequestRepository partnerServiceRequestRepository,
                                              PartnerOperationalProfileRepository partnerProfileRepository,
                                              PartnerServiceSkillRepository partnerServiceSkillRepository,
                                              LexhoAddressRepository lexhoAddressRepository,
                                              PartnerShiftRepository partnerShiftRepository,
                                              CurrentPartnerLocationRepository currentPartnerLocationRepository,
                                              ServiceableAreaRepository serviceableAreaRepository,
                                              DispatchProperties dispatchProperties,
                                              PartnerDispatchEligibilityService partnerDispatchEligibilityService) {
        this.subscriptionMasterRepository = subscriptionMasterRepository;
        this.orderRepository = orderRepository;
        this.subscriptionFrequencyRepository = subscriptionFrequencyRepository;
        this.subscriptionVisitRepository = subscriptionVisitRepository;
        this.serviceRepository = serviceRepository;
        this.subscriptionPartnerAssignmentRepository = subscriptionPartnerAssignmentRepository;
        this.visitPartnerAssignmentRepository = visitPartnerAssignmentRepository;
        this.partnerServiceRequestRepository = partnerServiceRequestRepository;
        this.partnerProfileRepository = partnerProfileRepository;
        this.partnerServiceSkillRepository = partnerServiceSkillRepository;
        this.lexhoAddressRepository = lexhoAddressRepository;
        this.partnerShiftRepository = partnerShiftRepository;
        this.currentPartnerLocationRepository = currentPartnerLocationRepository;
        this.serviceableAreaRepository = serviceableAreaRepository;
        this.dispatchProperties = dispatchProperties;
        this.partnerDispatchEligibilityService = partnerDispatchEligibilityService;
    }

    @Transactional(readOnly = true)
    public Page<AdminSubscriptionListResponse> list(Long customerId,
                                                    SubscriptionStatus status,
                                                    int page,
                                                    int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        Page<SubscriptionMaster> subscriptions = findSubscriptions(customerId, status, pageable);
        if (subscriptions.isEmpty()) {
            return Page.empty(pageable);
        }

        List<Long> subscriptionIds = subscriptions.stream().map(SubscriptionMaster::getId).toList();
        Map<Long, List<SubscriptionVisit>> visitsBySubscription = subscriptionVisitRepository
                .findBySubscriptionIdInOrderBySubscriptionIdAscVisitDateAscIdAsc(subscriptionIds)
                .stream()
                .collect(Collectors.groupingBy(SubscriptionVisit::getSubscriptionId));
        Map<Long, AdminServiceEntity> services = servicesById(subscriptions.stream()
                .map(SubscriptionMaster::getServiceId)
                .collect(Collectors.toSet())
                .stream()
                .toList());

        return subscriptions.map(subscription -> toListResponse(subscription,
                visitsBySubscription.getOrDefault(subscription.getId(), List.of()),
                services.get(subscription.getServiceId())));
    }

    @Transactional(readOnly = true)
    public AdminSubscriptionDetailResponse detail(Long subscriptionId) {
        return detailResponse(subscriptionId);
    }

    private AdminSubscriptionDetailResponse detailResponse(Long subscriptionId) {
        SubscriptionMaster subscription = getSubscription(subscriptionId);
        List<SubscriptionVisit> visits = subscriptionVisitRepository.findBySubscriptionIdOrderByVisitDateAscIdAsc(subscriptionId);
        List<String> frequencyDays = subscriptionFrequencyRepository.findBySubscriptionIdOrderByIdAsc(subscriptionId)
                .stream()
                .map(SubscriptionFrequency::getDayName)
                .toList();
        AdminServiceEntity service = serviceRepository.findById(subscription.getServiceId()).orElse(null);
        return toDetailResponse(subscription, service, frequencyDays, visits);
    }

    @Transactional(readOnly = true)
    public List<SubscriptionCalendarItemResponse> calendar(Long subscriptionId) {
        getSubscription(subscriptionId);
        return subscriptionVisitRepository.findBySubscriptionIdOrderByVisitDateAscIdAsc(subscriptionId)
                .stream()
                .map(this::toCalendarItem)
                .toList();
    }

    @Transactional
    public AdminSubscriptionDetailResponse updateStatus(Long subscriptionId,
                                                        AdminSubscriptionStatusUpdateRequest request) {
        SubscriptionMaster subscription = getSubscriptionForUpdate(subscriptionId);
        validateStatusTransition(subscription.getStatus(), request.status());
        if (request.status() == SubscriptionStatus.ACTIVE) {
            requirePaidMasterBooking(subscription);
        }
        subscription.setStatus(request.status());
        if (request.status() == SubscriptionStatus.CANCELLED) {
            subscriptionVisitRepository.cancelFutureScheduled(
                    subscriptionId,
                    VisitStatus.SCHEDULED,
                    VisitStatus.CANCELLED,
                    java.time.LocalDate.now()
            );
            subscriptionVisitRepository.cancelFutureScheduled(
                    subscriptionId,
                    VisitStatus.RESCHEDULED,
                    VisitStatus.CANCELLED,
                    java.time.LocalDate.now()
            );
        }
        subscriptionMasterRepository.save(subscription);
        return detailResponse(subscriptionId);
    }

    @Transactional
    public SubscriptionCalendarItemResponse updateVisitStatus(Long subscriptionId,
                                                              Long visitId,
                                                              AdminSubscriptionVisitStatusUpdateRequest request) {
        getSubscriptionForUpdate(subscriptionId);
        SubscriptionVisit visit = subscriptionVisitRepository.findByIdForUpdate(visitId)
                .filter(candidate -> candidate.getSubscriptionId().equals(subscriptionId))
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found"));
        validateVisitStatusChange(visit.getStatus(), request.status());
        visit.setStatus(request.status());
        SubscriptionVisit saved = subscriptionVisitRepository.save(visit);
        return toCalendarItem(saved);
    }

    @Transactional
    public AdminSubscriptionAssignmentResponse assignPartner(Long subscriptionId, AdminPartnerAssignRequest request) {
        SubscriptionMaster subscription = requireAssignableSubscription(subscriptionId);
        PartnerOperationalProfile partner = getValidPartner(request.partnerId());
        validatePartnerSkill(partner.getId(), subscription.getServiceId());
        validatePartnerDispatchEligibility(subscription, partner.getId());
        validateNoVisitScheduleConflict(subscription, partner.getId(),
                subscriptionVisitRepository.findBySubscriptionIdOrderByVisitDateAscIdAsc(subscriptionId));

        SubscriptionPartnerAssignment assignment = subscriptionPartnerAssignmentRepository
                .findBySubscriptionId(subscriptionId)
                .orElseGet(SubscriptionPartnerAssignment::new);
        assignment.setSubscriptionId(subscriptionId);
        assignment.setPartnerId(partner.getId());
        assignment.setStatus(PartnerAssignmentStatus.ASSIGNED);
        assignment.setReplacementPartnerId(null);
        assignment.setReplacementReason(normalizeReason(request.reason()));
        assignment.setReplacedAt(null);
        assignment.setAssignedAt(java.time.LocalDateTime.now());
        subscriptionPartnerAssignmentRepository.save(assignment);
        return assignmentResponse(subscriptionId);
    }

    @Transactional
    public AdminSubscriptionAssignmentResponse replacePartner(Long subscriptionId, AdminPartnerAssignRequest request) {
        SubscriptionMaster subscription = requireAssignableSubscription(subscriptionId);
        PartnerOperationalProfile replacement = getValidPartner(request.partnerId());
        validatePartnerSkill(replacement.getId(), subscription.getServiceId());
        validatePartnerDispatchEligibility(subscription, replacement.getId());
        List<SubscriptionVisit> replaceableVisits = subscriptionVisitRepository
                .findBySubscriptionIdOrderByVisitDateAscIdAsc(subscriptionId).stream()
                .filter(visit -> visit.getVisitDate().isAfter(LocalDate.now()))
                .filter(visit -> visit.getStatus() == VisitStatus.SCHEDULED || visit.getStatus() == VisitStatus.RESCHEDULED)
                .toList();
        validateNoVisitScheduleConflict(subscription, replacement.getId(), replaceableVisits);

        SubscriptionPartnerAssignment assignment = subscriptionPartnerAssignmentRepository
                .findBySubscriptionId(subscriptionId)
                .orElseThrow(() -> new BadRequestException("Assign a partner before replacement"));
        assignment.setReplacementPartnerId(replacement.getId());
        assignment.setReplacementReason(normalizeReason(request.reason()));
        assignment.setStatus(PartnerAssignmentStatus.REPLACED);
        assignment.setReplacedAt(java.time.LocalDateTime.now());
        subscriptionPartnerAssignmentRepository.save(assignment);
        replaceableVisits.forEach(this::expirePendingVisitRequests);
        return assignmentResponse(subscriptionId);
    }

    @Transactional
    public AdminVisitAssignmentResponse assignVisitPartner(Long subscriptionId,
                                                           Long visitId,
                                                           AdminPartnerAssignRequest request) {
        SubscriptionMaster subscription = requireAssignableSubscription(subscriptionId);
        SubscriptionVisit currentVisit = subscriptionVisitRepository.findByIdForUpdate(visitId)
                .filter(item -> item.getSubscriptionId().equals(subscriptionId))
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found"));
        if ((currentVisit.getStatus() != VisitStatus.SCHEDULED && currentVisit.getStatus() != VisitStatus.RESCHEDULED)
                || !currentVisit.getVisitDate().isAfter(LocalDate.now())) {
            throw new BadRequestException("Only a scheduled visit can have a preferred partner");
        }
        PartnerOperationalProfile partner = getValidPartner(request.partnerId());
        validatePartnerSkill(partner.getId(), subscription.getServiceId());
        validatePartnerDispatchEligibility(subscription, partner.getId());
        validateNoVisitScheduleConflict(subscription, partner.getId(), List.of(currentVisit));

        expirePendingVisitRequests(currentVisit);
        SubscriptionVisit visit = currentVisit;
        SubscriptionVisitPartnerAssignment assignment = visitPartnerAssignmentRepository.findByVisitId(visitId)
                .orElseGet(SubscriptionVisitPartnerAssignment::new);
        assignment.setSubscriptionId(subscriptionId);
        assignment.setVisitId(visitId);
        assignment.setPartnerId(partner.getId());
        assignment.setStatus(PartnerAssignmentStatus.ASSIGNED);
        assignment.setReason("ADMIN_PREFERRED_PARTNER: " + normalizeReason(request.reason()));
        assignment.setAssignedAt(java.time.LocalDateTime.now());
        SubscriptionVisitPartnerAssignment saved = visitPartnerAssignmentRepository.save(assignment);
        return toVisitAssignmentResponse(saved, Map.of(partner.getId(), partner));
    }

    private void expirePendingVisitRequests(SubscriptionVisit visit) {
        List<PartnerServiceRequestEntity> pendingRequests = partnerServiceRequestRepository
                .findBySubscriptionVisitIdAndStatus(visit.getId(), PartnerServiceRequestStatus.PENDING);
        for (PartnerServiceRequestEntity pendingRequest : pendingRequests) {
            pendingRequest.setStatus(PartnerServiceRequestStatus.EXPIRED);
            pendingRequest.setRejectionReason("Superseded by an administrative partner preference");
        }
        if (!pendingRequests.isEmpty()) {
            partnerServiceRequestRepository.saveAll(pendingRequests);
        }
    }

    @Transactional(readOnly = true)
    public AdminSubscriptionAssignmentResponse assignment(Long subscriptionId) {
        return assignmentResponse(subscriptionId);
    }

    private AdminSubscriptionAssignmentResponse assignmentResponse(Long subscriptionId) {
        getSubscription(subscriptionId);
        Optional<SubscriptionPartnerAssignment> subscriptionAssignment = subscriptionPartnerAssignmentRepository
                .findBySubscriptionId(subscriptionId);
        List<SubscriptionVisitPartnerAssignment> visitAssignments = visitPartnerAssignmentRepository
                .findBySubscriptionIdOrderByVisitIdAsc(subscriptionId);
        List<Long> partnerIds = new java.util.ArrayList<>();
        subscriptionAssignment.ifPresent(assignment -> {
            partnerIds.add(assignment.getPartnerId());
            if (assignment.getReplacementPartnerId() != null) {
                partnerIds.add(assignment.getReplacementPartnerId());
            }
        });
        visitAssignments.forEach(assignment -> partnerIds.add(assignment.getPartnerId()));
        Map<Long, PartnerOperationalProfile> partners = partnerProfileRepository.findAllById(partnerIds)
                .stream()
                .collect(Collectors.toMap(PartnerOperationalProfile::getId, Function.identity()));

        List<AdminVisitAssignmentResponse> visitResponses = visitAssignments.stream()
                .map(item -> toVisitAssignmentResponse(item, partners))
                .toList();
        if (subscriptionAssignment.isEmpty()) {
            return new AdminSubscriptionAssignmentResponse(subscriptionId, null, null, "PENDING",
                    null, null, null, null, null, visitResponses);
        }
        SubscriptionPartnerAssignment item = subscriptionAssignment.get();
        return new AdminSubscriptionAssignmentResponse(
                subscriptionId,
                item.getPartnerId(),
                partnerName(partners.get(item.getPartnerId())),
                item.getStatus().name(),
                item.getReplacementPartnerId(),
                partnerName(partners.get(item.getReplacementPartnerId())),
                item.getReplacementReason(),
                item.getAssignedAt(),
                item.getReplacedAt(),
                visitResponses
        );
    }

    private Page<SubscriptionMaster> findSubscriptions(Long customerId,
                                                       SubscriptionStatus status,
                                                       Pageable pageable) {
        if (customerId != null && status != null) {
            return subscriptionMasterRepository.findByCustomerIdAndStatusOrderByIdDesc(customerId, status, pageable);
        }
        if (customerId != null) {
            return subscriptionMasterRepository.findByCustomerIdOrderByIdDesc(customerId, pageable);
        }
        if (status != null) {
            return subscriptionMasterRepository.findByStatusOrderByIdDesc(status, pageable);
        }
        return subscriptionMasterRepository.findByOrderByIdDesc(pageable);
    }

    private SubscriptionMaster getSubscription(Long subscriptionId) {
        return subscriptionMasterRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
    }

    private SubscriptionMaster getSubscriptionForUpdate(Long subscriptionId) {
        return subscriptionMasterRepository.findByIdForUpdate(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
    }

    private SubscriptionMaster requireAssignableSubscription(Long subscriptionId) {
        SubscriptionMaster subscription = getSubscriptionForUpdate(subscriptionId);
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new BadRequestException("Only ACTIVE subscriptions can be assigned");
        }
        requirePaidMasterBooking(subscription);
        return subscription;
    }

    private void requirePaidMasterBooking(SubscriptionMaster subscription) {
        Order masterOrder = orderRepository.findByIdForUpdate(subscription.getBookingId())
                .orElseThrow(() -> new BadRequestException("Subscription payment booking not found"));
        if (masterOrder.getPaymentStatus() != PaymentStatus.SUCCESS) {
            throw new BadRequestException("Subscription payment is not confirmed");
        }
    }

    private void validateNoVisitScheduleConflict(SubscriptionMaster subscription,
                                                 Long partnerId,
                                                 List<SubscriptionVisit> visits) {
        Set<OrderStatus> activeStatuses = Set.of(
                OrderStatus.PARTNER_SEARCH, OrderStatus.PARTNER_ASSIGNED, OrderStatus.PARTNER_ACCEPTED,
                OrderStatus.ARRIVED, OrderStatus.PARTNER_REACHED, OrderStatus.SERVICE_STARTED,
                OrderStatus.SERVICE_IN_PROGRESS
        );
        for (SubscriptionVisit visit : visits) {
            if (!visit.getVisitDate().isAfter(LocalDate.now())) {
                continue;
            }
            LocalTime start = slotStart(visit.getTimeSlot());
            LocalDateTime from = LocalDateTime.of(visit.getVisitDate(), start);
            long durationMinutes = Math.max(1L, subscription.getDurationHours() == null
                    ? 60L
                    : subscription.getDurationHours().multiply(BigDecimal.valueOf(60))
                            .setScale(0, java.math.RoundingMode.CEILING).longValue());
            LocalDateTime to = from.plusMinutes(durationMinutes);
            LocalDateTime conflictFrom = from.minusMinutes(durationMinutes);
            if (orderRepository.existsByAssignedPartnerIdAndScheduledAtGreaterThanEqualAndScheduledAtLessThanAndStatusInAndIdNot(
                    partnerId, conflictFrom, to, activeStatuses, visit.getBookingId() == null ? -1L : visit.getBookingId())) {
                throw new BadRequestException("Partner has a conflicting booking during subscription visit "
                        + visit.getId());
            }
        }
    }

    private LocalTime slotStart(String timeSlot) {
        try {
            String start = timeSlot == null ? "" : timeSlot.split("-", 2)[0].trim();
            return LocalTime.parse(start);
        } catch (RuntimeException ex) {
            throw new BadRequestException("Subscription visit has an invalid time slot");
        }
    }

    private Map<Long, AdminServiceEntity> servicesById(List<Long> serviceIds) {
        return serviceRepository.findAllById(serviceIds)
                .stream()
                .collect(Collectors.toMap(AdminServiceEntity::getId, Function.identity()));
    }

    private void validateVisitStatusChange(VisitStatus current, VisitStatus target) {
        if (target == VisitStatus.RESCHEDULED) {
            throw new BadRequestException("Use reschedule API for RESCHEDULED status");
        }
        if (current == target) {
            return;
        }
        boolean allowed = switch (current) {
            case SCHEDULED, RESCHEDULED -> target == VisitStatus.CANCELLED || target == VisitStatus.SKIPPED;
            case ACCEPTED -> target == VisitStatus.ARRIVED || target == VisitStatus.CANCELLED;
            case ARRIVED -> target == VisitStatus.IN_PROGRESS || target == VisitStatus.CANCELLED;
            case IN_PROGRESS -> target == VisitStatus.COMPLETED;
            case COMPLETED, CANCELLED, SKIPPED -> false;
        };
        if (!allowed) {
            throw new BadRequestException("Cannot change visit status from " + current + " to " + target);
        }
    }

    private void validateStatusTransition(SubscriptionStatus currentStatus, SubscriptionStatus requestedStatus) {
        if (currentStatus == requestedStatus) {
            return;
        }
        boolean allowed = switch (currentStatus) {
            case PENDING -> requestedStatus == SubscriptionStatus.ACTIVE
                    || requestedStatus == SubscriptionStatus.CANCELLED;
            case ACTIVE -> requestedStatus == SubscriptionStatus.PAUSED
                    || requestedStatus == SubscriptionStatus.CANCELLED
                    || requestedStatus == SubscriptionStatus.COMPLETED;
            case PAUSED -> requestedStatus == SubscriptionStatus.ACTIVE
                    || requestedStatus == SubscriptionStatus.CANCELLED;
            case CANCELLED, COMPLETED -> false;
        };
        if (!allowed) {
            throw new BadRequestException("Cannot change subscription status from "
                    + currentStatus + " to " + requestedStatus
                    + ". Allowed transitions: " + allowedStatusValues(currentStatus));
        }
    }

    private String allowedStatusValues(SubscriptionStatus currentStatus) {
        return switch (currentStatus) {
            case PENDING -> "ACTIVE, CANCELLED";
            case ACTIVE -> "PAUSED, CANCELLED, COMPLETED";
            case PAUSED -> "ACTIVE, CANCELLED";
            case CANCELLED, COMPLETED -> "none (terminal status)";
        };
    }

    private PartnerOperationalProfile getValidPartner(Long partnerId) {
        PartnerOperationalProfile partner = partnerProfileRepository.findById(partnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Partner not found"));
        if (partner.isDeleted()) {
            throw new BadRequestException("Partner is deleted");
        }
        if (!partner.isVerified()) {
            throw new BadRequestException("Partner is not verified");
        }
        return partner;
    }

    private void validatePartnerSkill(Long partnerId, Long serviceId) {
        if (partnerServiceSkillRepository.existsByPartnerIdAndSubServiceId(partnerId, serviceId)) {
            return;
        }
        Long categoryId = serviceRepository.findByIdAndDeletedFalse(serviceId)
                .map(AdminServiceEntity::getCategory)
                .map(category -> category == null ? null : category.getId())
                .orElse(null);
        if (categoryId != null && partnerServiceSkillRepository.existsByPartnerIdAndCategoryId(partnerId, categoryId)) {
            return;
        }
        throw new BadRequestException("Partner is not mapped to this subscription service");
    }

    private void validatePartnerDispatchEligibility(SubscriptionMaster subscription, Long partnerId) {
        LexhoAddress address = lexhoAddressRepository.findById(subscription.getAddressId())
                .orElseThrow(() -> new BadRequestException("Subscription address not found"));
        if (address.getLatitude() == null || address.getLongitude() == null) {
            throw new BadRequestException("Subscription address coordinates are missing");
        }
        if (address.getPincode() == null
                || address.getPincode().isBlank()
                || !serviceableAreaRepository.existsByPincodeAndActiveTrueAndDeletedFalse(address.getPincode().trim())) {
            throw new BadRequestException("Subscription address is not serviceable");
        }

        PartnerShiftEntity shift = partnerShiftRepository.findByPartnerId(partnerId)
                .orElseThrow(() -> new BadRequestException("Partner shift not found"));
        PartnerOperationalProfile partner = partnerProfileRepository.findById(partnerId)
                .orElseThrow(() -> new BadRequestException("Partner not found"));
        partnerDispatchEligibilityService.requireEligibleForNewDispatch(partner, shift);

        CurrentPartnerLocationEntity location = currentPartnerLocationRepository.findByPartnerId(partnerId)
                .orElseThrow(() -> new BadRequestException("Partner live location not available"));
        BigDecimal distanceKm = SubscriptionDistanceSupport.calculateDistanceKm(address, location);
        if (distanceKm.compareTo(BigDecimal.valueOf(getMaxSearchRadiusKm())) > 0) {
            throw new BadRequestException("Partner is outside maximum dispatch radius");
        }
        if (SubscriptionDistanceSupport.estimateEtaMinutes(distanceKm) > getMaxEtaMinutes()) {
            throw new BadRequestException("Partner ETA exceeds allowed dispatch limit");
        }
    }

    private double getMaxSearchRadiusKm() {
        return Math.max(0.1d, Math.min(25.0d, dispatchProperties.getPartner().getMaxSearchRadiusKm()));
    }

    private int getMaxEtaMinutes() {
        return Math.max(1, Math.min(120, dispatchProperties.getBooking().getMaxEtaMinutes()));
    }

    private String normalizeReason(String reason) {
        if (reason == null) {
            return null;
        }
        String trimmed = reason.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private AdminSubscriptionListResponse toListResponse(SubscriptionMaster subscription,
                                                         List<SubscriptionVisit> visits,
                                                         AdminServiceEntity service) {
        long completed = countVisits(visits, VisitStatus.COMPLETED);
        return new AdminSubscriptionListResponse(
                subscription.getId(),
                subscription.getCustomerId(),
                subscription.getServiceId(),
                serviceName(service),
                subscription.getAddressId(),
                subscription.getBookingMode(),
                subscription.getServiceType(),
                subscription.getStartDate(),
                subscription.getEndDate(),
                subscription.getVisits(),
                completed,
                Math.max(subscription.getVisits() - completed, 0),
                subscription.getDurationHours(),
                subscription.getFamilySize() == null ? null : subscription.getFamilySize().name(),
                familySizeLabel(subscription.getFamilySize()),
                subscription.getPerVisitPrice(),
                subscription.getTimeSlot(),
                subscription.getPrice(),
                subscription.getStatus().name()
        );
    }

    private AdminSubscriptionDetailResponse toDetailResponse(SubscriptionMaster subscription,
                                                             AdminServiceEntity service,
                                                             List<String> frequencyDays,
                                                             List<SubscriptionVisit> visits) {
        long completed = countVisits(visits, VisitStatus.COMPLETED);
        return new AdminSubscriptionDetailResponse(
                subscription.getId(),
                subscription.getCustomerId(),
                subscription.getServiceId(),
                serviceName(service),
                subscription.getAddressId(),
                subscription.getBookingMode(),
                subscription.getServiceType(),
                frequencyDays,
                subscription.getStartDate(),
                subscription.getEndDate(),
                subscription.getVisits(),
                completed,
                Math.max(subscription.getVisits() - completed, 0),
                subscription.getDurationHours(),
                subscription.getFamilySize() == null ? null : subscription.getFamilySize().name(),
                familySizeLabel(subscription.getFamilySize()),
                subscription.getPerVisitPrice(),
                subscription.getTimeSlot(),
                subscription.getPrice(),
                subscription.getStatus().name(),
                subscription.getCreatedAt(),
                subscription.getUpdatedAt(),
                visits.stream().map(this::toCalendarItem).toList()
        );
    }

    private long countVisits(List<SubscriptionVisit> visits, VisitStatus status) {
        return visits.stream().filter(visit -> visit.getStatus() == status).count();
    }

    private String serviceName(AdminServiceEntity service) {
        return service == null || service.getName() == null ? "" : service.getName();
    }

    private String partnerName(PartnerOperationalProfile partner) {
        if (partner == null) {
            return null;
        }
        if (partner.getName() != null && !partner.getName().isBlank()) {
            return partner.getName();
        }
        return partner.getFullName();
    }

    private String familySizeLabel(com.lexho.user_service.subscription.enums.FamilySize familySize) {
        if (familySize == null) {
            return null;
        }
        return switch (familySize) {
            case SMALL -> "1-2 Members";
            case MEDIUM -> "3-4 Members";
            case LARGE -> "5-6 Members";
            case XLARGE -> "6-8 Members";
        };
    }

    private AdminVisitAssignmentResponse toVisitAssignmentResponse(SubscriptionVisitPartnerAssignment assignment,
                                                                   Map<Long, PartnerOperationalProfile> partners) {
        return new AdminVisitAssignmentResponse(
                assignment.getId(),
                assignment.getVisitId(),
                assignment.getPartnerId(),
                partnerName(partners.get(assignment.getPartnerId())),
                assignment.getStatus().name(),
                assignment.getReason(),
                assignment.getAssignedAt()
        );
    }

    private SubscriptionCalendarItemResponse toCalendarItem(SubscriptionVisit visit) {
        return new SubscriptionCalendarItemResponse(
                visit.getId(),
                visit.getVisitDate(),
                visit.getTimeSlot(),
                visit.getStatus().name()
        );
    }
}

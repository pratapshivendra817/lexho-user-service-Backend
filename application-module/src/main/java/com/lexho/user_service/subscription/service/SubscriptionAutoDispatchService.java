package com.lexho.user_service.subscription.service;

import com.lexho.booking.entity.Order;
import com.lexho.booking.repository.OrderRepository;
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
import com.lexho.user_service.dispatch.service.PartnerDispatchEligibilityResult;
import com.lexho.user_service.dispatch.service.PartnerDispatchEligibilityService;
import com.lexho.user_service.lexho.entity.LexhoAddress;
import com.lexho.user_service.lexho.repository.LexhoAddressRepository;
import com.lexho.user_service.lexhopartner.entity.PartnerOperationalProfile;
import com.lexho.user_service.lexhopartner.repository.PartnerOperationalProfileRepository;
import com.lexho.user_service.partnerrequest.entity.PartnerServiceRequestEntity;
import com.lexho.user_service.partnerrequest.model.PartnerServiceRequestStatus;
import com.lexho.user_service.partnerrequest.repository.PartnerServiceRequestRepository;
import com.lexho.user_service.subscription.entity.SubscriptionMaster;
import com.lexho.user_service.subscription.entity.SubscriptionPartnerAssignment;
import com.lexho.user_service.subscription.entity.SubscriptionVisit;
import com.lexho.user_service.subscription.entity.SubscriptionVisitPartnerAssignment;
import com.lexho.user_service.subscription.enums.SubscriptionStatus;
import com.lexho.user_service.subscription.enums.VisitStatus;
import com.lexho.user_service.subscription.repository.SubscriptionMasterRepository;
import com.lexho.user_service.subscription.repository.SubscriptionPartnerAssignmentRepository;
import com.lexho.user_service.subscription.repository.SubscriptionVisitPartnerAssignmentRepository;
import com.lexho.user_service.subscription.repository.SubscriptionVisitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SubscriptionAutoDispatchService {
    private static final List<DateTimeFormatter> TIME_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("H:mm"),
            DateTimeFormatter.ofPattern("HH:mm"),
            DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)
    );
    private static final int PARTNER_RESPONSE_WINDOW_MINUTES = 2;

    private final SubscriptionMasterRepository subscriptionMasterRepository;
    private final OrderRepository orderRepository;
    private final SubscriptionVisitRepository subscriptionVisitRepository;
    private final SubscriptionPartnerAssignmentRepository subscriptionPartnerAssignmentRepository;
    private final SubscriptionVisitPartnerAssignmentRepository visitPartnerAssignmentRepository;
    private final LexhoAddressRepository lexhoAddressRepository;
    private final PartnerOperationalProfileRepository partnerProfileRepository;
    private final PartnerShiftRepository partnerShiftRepository;
    private final CurrentPartnerLocationRepository currentPartnerLocationRepository;
    private final PartnerServiceSkillRepository partnerServiceSkillRepository;
    private final PartnerServiceRequestRepository partnerServiceRequestRepository;
    private final ServiceableAreaRepository serviceableAreaRepository;
    private final AdminServiceEntityRepository adminServiceEntityRepository;
    private final DispatchProperties dispatchProperties;
    private final PartnerDispatchEligibilityService partnerDispatchEligibilityService;

    public SubscriptionAutoDispatchService(SubscriptionMasterRepository subscriptionMasterRepository,
                                           OrderRepository orderRepository,
                                           SubscriptionVisitRepository subscriptionVisitRepository,
                                           SubscriptionPartnerAssignmentRepository subscriptionPartnerAssignmentRepository,
                                           SubscriptionVisitPartnerAssignmentRepository visitPartnerAssignmentRepository,
                                           LexhoAddressRepository lexhoAddressRepository,
                                           PartnerOperationalProfileRepository partnerProfileRepository,
                                           PartnerShiftRepository partnerShiftRepository,
                                           CurrentPartnerLocationRepository currentPartnerLocationRepository,
                                           PartnerServiceSkillRepository partnerServiceSkillRepository,
                                           PartnerServiceRequestRepository partnerServiceRequestRepository,
                                           ServiceableAreaRepository serviceableAreaRepository,
                                           AdminServiceEntityRepository adminServiceEntityRepository,
                                           DispatchProperties dispatchProperties,
                                           PartnerDispatchEligibilityService partnerDispatchEligibilityService) {
        this.subscriptionMasterRepository = subscriptionMasterRepository;
        this.orderRepository = orderRepository;
        this.subscriptionVisitRepository = subscriptionVisitRepository;
        this.subscriptionPartnerAssignmentRepository = subscriptionPartnerAssignmentRepository;
        this.visitPartnerAssignmentRepository = visitPartnerAssignmentRepository;
        this.lexhoAddressRepository = lexhoAddressRepository;
        this.partnerProfileRepository = partnerProfileRepository;
        this.partnerShiftRepository = partnerShiftRepository;
        this.currentPartnerLocationRepository = currentPartnerLocationRepository;
        this.partnerServiceSkillRepository = partnerServiceSkillRepository;
        this.partnerServiceRequestRepository = partnerServiceRequestRepository;
        this.serviceableAreaRepository = serviceableAreaRepository;
        this.adminServiceEntityRepository = adminServiceEntityRepository;
        this.dispatchProperties = dispatchProperties;
        this.partnerDispatchEligibilityService = partnerDispatchEligibilityService;
    }

    @Transactional
    public int autoAssignDueVisits() {
        List<SubscriptionMaster> activeSubscriptions = subscriptionMasterRepository.findByStatusOrderByIdDesc(SubscriptionStatus.ACTIVE);
        if (activeSubscriptions.isEmpty()) {
            return 0;
        }
        Map<Long, SubscriptionMaster> subscriptionsById = activeSubscriptions.stream()
                .collect(Collectors.toMap(SubscriptionMaster::getId, item -> item));
        Collection<Long> subscriptionIds = subscriptionsById.keySet();
        List<SubscriptionVisit> candidateVisits = subscriptionVisitRepository
                .findBySubscriptionIdInAndStatusInOrderByVisitDateAscIdAsc(subscriptionIds, List.of(VisitStatus.SCHEDULED, VisitStatus.RESCHEDULED));

        int assigned = 0;
        for (SubscriptionVisit visit : candidateVisits) {
            if (dispatchClaimedVisit(visit.getSubscriptionId(), visit.getId())) {
                assigned++;
            }
        }
        return assigned;
    }

    @Transactional
    public boolean autoAssignVisit(Long subscriptionId, Long visitId) {
        return dispatchClaimedVisit(subscriptionId, visitId);
    }

    @Transactional
    public void markPartnerAccepted(Long subscriptionId, Long visitId, Long partnerId) {
        SubscriptionVisit visit = subscriptionVisitRepository.findByIdForUpdate(visitId)
                .filter(item -> item.getSubscriptionId().equals(subscriptionId))
                .orElseThrow(() -> new IllegalStateException("Subscription visit is not available"));
        SubscriptionVisitPartnerAssignment assignment = visitPartnerAssignmentRepository.findByVisitIdForUpdate(visitId)
                .orElseGet(SubscriptionVisitPartnerAssignment::new);
        if (assignment.getId() != null && assignment.getPartnerId() != null && !assignment.getPartnerId().equals(partnerId)) {
            throw new IllegalStateException("Subscription visit is already assigned to another partner");
        }
        assignment.setSubscriptionId(subscriptionId);
        assignment.setVisitId(visitId);
        assignment.setPartnerId(partnerId);
        assignment.setStatus(com.lexho.user_service.subscription.enums.PartnerAssignmentStatus.ASSIGNED);
        assignment.setReason("PARTNER_ACCEPTED");
        assignment.setAssignedAt(LocalDateTime.now());
        visitPartnerAssignmentRepository.save(assignment);
    }

    @Transactional
    public int retryDispatch(Long subscriptionId, Long visitId) {
        return dispatchClaimedVisit(subscriptionId, visitId) ? 1 : 0;
    }

    /** Payment activation can request dispatch only when the visit is inside the configured window. */
    @Transactional
    public int dispatchForBooking(Long bookingId) {
        if (bookingId == null) {
            return 0;
        }
        SubscriptionMaster subscription = subscriptionMasterRepository.findByBookingId(bookingId).orElse(null);
        if (subscription == null || subscription.getStatus() != SubscriptionStatus.ACTIVE) return 0;
        return subscriptionVisitRepository
                .findBySubscriptionIdInAndStatusInOrderByVisitDateAscIdAsc(
                        List.of(subscription.getId()), List.of(VisitStatus.SCHEDULED, VisitStatus.RESCHEDULED))
                .stream()
                .findFirst()
                .map(visit -> dispatchClaimedVisit(subscription.getId(), visit.getId()) ? 1 : 0)
                .orElse(0);
    }

    private boolean dispatchClaimedVisit(Long subscriptionId, Long visitId) {
        SubscriptionMaster subscription = subscriptionMasterRepository.findByIdForUpdate(subscriptionId).orElse(null);
        SubscriptionVisit visit = subscriptionVisitRepository.findByIdForUpdate(visitId)
                .filter(item -> item.getSubscriptionId().equals(subscriptionId))
                .orElse(null);
        if (subscription == null
                || subscription.getStatus() != SubscriptionStatus.ACTIVE
                || visit == null
                || (visit.getStatus() != VisitStatus.SCHEDULED && visit.getStatus() != VisitStatus.RESCHEDULED)
                || visit.getBookingId() != null
                || !isPaid(subscription)
                || !isDueForDispatch(visit)
                || hasActiveRequest(visitId)
                || hasConfirmedVisitAssignment(visitId)) {
            return false;
        }
        return createDispatchRequests(subscription, visit) > 0;
    }

    private boolean isPaid(SubscriptionMaster subscription) {
        if (subscription.getBookingId() == null) {
            return false;
        }
        return orderRepository.findByIdForUpdate(subscription.getBookingId())
                .map(Order::getPaymentStatus)
                .filter(status -> status == PaymentStatus.SUCCESS)
                .isPresent();
    }

    private boolean hasConfirmedVisitAssignment(Long visitId) {
        return visitPartnerAssignmentRepository.findByVisitId(visitId)
                .filter(assignment -> assignment.getReason() == null
                        || !assignment.getReason().startsWith("ADMIN_PREFERRED_PARTNER:"))
                .isPresent();
    }

    private int createDispatchRequests(SubscriptionMaster subscription, SubscriptionVisit visit) {
        LexhoAddress address = lexhoAddressRepository.findById(subscription.getAddressId()).orElse(null);
        if (address == null || address.getLatitude() == null || address.getLongitude() == null) {
            return 0;
        }
        if (!isServiceable(address)) {
            return 0;
        }

        List<PartnerServiceRequestEntity> existingRequests = partnerServiceRequestRepository.findBySubscriptionVisitId(visit.getId());
        Set<Long> requestedPartnerIds = existingRequests.stream()
                .map(PartnerServiceRequestEntity::getPartnerId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        int nextAttempt = existingRequests.stream()
                .map(PartnerServiceRequestEntity::getDispatchAttempt)
                .max(Integer::compareTo)
                .orElse(0) + 1;

        Optional<SubscriptionVisitPartnerAssignment> visitPreference =
                visitPartnerAssignmentRepository.findByVisitId(visit.getId())
                        .filter(item -> item.getReason() != null
                                && item.getReason().startsWith("ADMIN_PREFERRED_PARTNER:"));
        Optional<SubscriptionPartnerAssignment> baseAssignment = subscriptionPartnerAssignmentRepository
                .findBySubscriptionId(subscription.getId());
        Long preferredPartnerId = visitPreference.map(SubscriptionVisitPartnerAssignment::getPartnerId)
                .orElseGet(() -> preferredPartnerId(baseAssignment.orElse(null)));
        if (preferredPartnerId != null && !requestedPartnerIds.contains(preferredPartnerId)) {
            Optional<PartnerCandidate> preferredCandidate = isEligiblePartner(subscription, address, preferredPartnerId);
            if (preferredCandidate.isPresent()) {
                partnerServiceRequestRepository.save(toPartnerServiceRequest(subscription, visit, preferredCandidate.get(), nextAttempt, "AUTO_PRIMARY_PARTNER"));
                return 1;
            }
        }

        Set<Long> excludedPartnerIds = new java.util.HashSet<>(requestedPartnerIds);
        if (preferredPartnerId != null) {
            excludedPartnerIds.add(preferredPartnerId);
        }
        Optional<Long> fallbackPartnerId = findNearestEligiblePartner(subscription, address, excludedPartnerIds);
        if (fallbackPartnerId.isPresent()) {
            Optional<PartnerCandidate> candidate = isEligiblePartner(subscription, address, fallbackPartnerId.get());
            if (candidate.isPresent()) {
                partnerServiceRequestRepository.save(toPartnerServiceRequest(subscription, visit, candidate.get(), nextAttempt, "AUTO_FALLBACK_DISPATCH"));
                return 1;
            }
        }
        return 0;
    }

    private Optional<Long> findNearestEligiblePartner(SubscriptionMaster subscription,
                                                      LexhoAddress address,
                                                      Set<Long> excludedPartnerIds) {
        List<PartnerCandidate> initial = nearbyCandidates(subscription, address, getInitialSearchRadiusKm(), excludedPartnerIds);
        if (!initial.isEmpty()) {
            return Optional.of(initial.get(0).partnerId());
        }
        if (getMaxSearchRadiusKm() <= getInitialSearchRadiusKm()) {
            return Optional.empty();
        }
        List<PartnerCandidate> expanded = nearbyCandidates(subscription, address, getMaxSearchRadiusKm(), excludedPartnerIds);
        return expanded.isEmpty() ? Optional.empty() : Optional.of(expanded.get(0).partnerId());
    }

    private List<PartnerCandidate> nearbyCandidates(SubscriptionMaster subscription,
                                                    LexhoAddress address,
                                                    double radiusKm,
                                                    Set<Long> excludedPartnerIds) {
        List<Long> partnerIds = currentPartnerLocationRepository.findNearbyPartnerIds(
                address.getLatitude().doubleValue(),
                address.getLongitude().doubleValue(),
                radiusKm,
                200
        );
        if (partnerIds.isEmpty()) {
            return List.of();
        }

        Map<Long, PartnerShiftEntity> shifts = partnerShiftRepository.findByPartnerIdInAndDeletedFalse(partnerIds).stream()
                .collect(Collectors.toMap(PartnerShiftEntity::getPartnerId, item -> item));
        Map<Long, PartnerOperationalProfile> profiles = partnerProfileRepository.findAllById(partnerIds).stream()
                .collect(Collectors.toMap(PartnerOperationalProfile::getId, item -> item));

        List<PartnerCandidate> eligible = new ArrayList<>();
        for (Long partnerId : partnerIds) {
            if (excludedPartnerIds.contains(partnerId)) {
                continue;
            }
            Optional<PartnerCandidate> candidate = isEligiblePartner(subscription, address, partnerId, profiles.get(partnerId), shifts.get(partnerId));
            candidate.ifPresent(eligible::add);
        }
        eligible.sort(Comparator
                .comparing(PartnerCandidate::distanceKm)
                .thenComparing(PartnerCandidate::rating, Comparator.reverseOrder())
                .thenComparing(PartnerCandidate::activeJobs));
        return eligible;
    }

    private Optional<PartnerCandidate> isEligiblePartner(SubscriptionMaster subscription,
                                                         LexhoAddress address,
                                                         Long partnerId) {
        PartnerOperationalProfile profile = partnerProfileRepository.findById(partnerId).orElse(null);
        PartnerShiftEntity shift = partnerShiftRepository.findByPartnerId(partnerId).orElse(null);
        return isEligiblePartner(subscription, address, partnerId, profile, shift);
    }

    private Optional<PartnerCandidate> isEligiblePartner(SubscriptionMaster subscription,
                                                         LexhoAddress address,
                                                         Long partnerId,
                                                         PartnerOperationalProfile profile,
                                                         PartnerShiftEntity shift) {
        if (profile == null || shift == null) {
            return Optional.empty();
        }
        PartnerDispatchEligibilityResult eligibility =
                partnerDispatchEligibilityService.canReceiveNewBookings(profile, shift);
        if (!eligibility.eligible()) {
            return Optional.empty();
        }
        if (!hasRequiredSkill(partnerId, subscription.getServiceId())) {
            return Optional.empty();
        }
        CurrentPartnerLocationEntity location = currentPartnerLocationRepository.findByPartnerId(partnerId).orElse(null);
        if (location == null || location.getLatitude() == null || location.getLongitude() == null) {
            return Optional.empty();
        }
        BigDecimal distanceKm = SubscriptionDistanceSupport.calculateDistanceKm(address, location);
        if (distanceKm.compareTo(BigDecimal.valueOf(getMaxSearchRadiusKm())) > 0) {
            return Optional.empty();
        }
        int etaMinutes = SubscriptionDistanceSupport.estimateEtaMinutes(distanceKm);
        if (etaMinutes > getMaxEtaMinutes()) {
            return Optional.empty();
        }
        return Optional.of(new PartnerCandidate(
                partnerId,
                distanceKm.doubleValue(),
                profile.getRating() == null ? 0.0d : profile.getRating(),
                shift.getActiveJobsCount()
        ));
    }

    private PartnerServiceRequestEntity toPartnerServiceRequest(SubscriptionMaster subscription,
                                                                SubscriptionVisit visit,
                                                                PartnerCandidate candidate,
                                                                int dispatchAttempt,
                                                                String reason) {
        LexhoAddress address = lexhoAddressRepository.findById(subscription.getAddressId()).orElseThrow();
        PartnerServiceRequestEntity request = new PartnerServiceRequestEntity();
        request.setPartnerId(candidate.partnerId());
        request.setCartId(subscription.getBookingId() == null ? subscription.getId() : subscription.getBookingId());
        request.setSubscriptionId(subscription.getId());
        request.setSubscriptionVisitId(visit.getId());
        request.setCustomerId(subscription.getCustomerId());
        request.setServiceId(subscription.getServiceId());
        request.setServiceType(subscription.getServiceType() == null || subscription.getServiceType().isBlank()
                ? "SUBSCRIPTION"
                : subscription.getServiceType());
        request.setQuantity(1);
        request.setScheduledTime(scheduledAt(visit));
        request.setFullAddress(fullAddress(address));
        request.setCity(address.getCity());
        request.setLatitude(address.getLatitude());
        request.setLongitude(address.getLongitude());
        request.setTargetPincode(address.getPincode());
        request.setDispatchAttempt(dispatchAttempt);
        request.setDistanceKm(BigDecimal.valueOf(candidate.distanceKm()).setScale(3, RoundingMode.HALF_UP));
        request.setEtaMinutes(SubscriptionDistanceSupport.estimateEtaMinutes(BigDecimal.valueOf(candidate.distanceKm())));
        request.setExpiresAt(LocalDateTime.now().plusMinutes(PARTNER_RESPONSE_WINDOW_MINUTES));
        request.setStatus(PartnerServiceRequestStatus.PENDING);
        request.setRejectionReason(reason);
        return request;
    }

    private boolean hasActiveRequest(Long visitId) {
        return partnerServiceRequestRepository.countBySubscriptionVisitIdAndStatus(visitId, PartnerServiceRequestStatus.PENDING) > 0
                || partnerServiceRequestRepository.countBySubscriptionVisitIdAndStatus(visitId, PartnerServiceRequestStatus.ACCEPTED) > 0;
    }

    private boolean isServiceable(LexhoAddress address) {
        String pincode = address.getPincode();
        return pincode != null
                && !pincode.isBlank()
                && serviceableAreaRepository.existsByPincodeAndActiveTrueAndDeletedFalse(pincode.trim());
    }

    private boolean hasRequiredSkill(Long partnerId, Long serviceId) {
        if (partnerId == null || serviceId == null) {
            return false;
        }
        if (partnerServiceSkillRepository.existsByPartnerIdAndSubServiceId(partnerId, serviceId)) {
            return true;
        }
        Long categoryId = adminServiceEntityRepository.findByIdAndDeletedFalse(serviceId)
                .map(AdminServiceEntity::getCategory)
                .map(category -> category == null ? null : category.getId())
                .orElse(null);
        return categoryId != null
                && partnerServiceSkillRepository.existsByPartnerIdAndCategoryId(partnerId, categoryId);
    }

    private String fullAddress(LexhoAddress address) {
        return address.getHouseNo() + ", " + address.getArea() + ", " + address.getCity();
    }

    private boolean isDueForDispatch(SubscriptionVisit visit) {
        LocalDateTime scheduledAt = scheduledAt(visit);
        if (scheduledAt == null) {
            return false;
        }
        return !LocalDateTime.now().isBefore(scheduledAt.minusMinutes(getPreDispatchMinutes()));
    }

    private LocalDateTime scheduledAt(SubscriptionVisit visit) {
        LocalTime startTime = parseStartTime(visit.getTimeSlot());
        return startTime == null ? null : LocalDateTime.of(visit.getVisitDate(), startTime);
    }

    private LocalTime parseStartTime(String timeSlot) {
        if (timeSlot == null || timeSlot.isBlank()) {
            return null;
        }
        String value = timeSlot.trim();
        String firstPart = value.split("-")[0].trim().toUpperCase(Locale.ENGLISH);
        for (DateTimeFormatter formatter : TIME_FORMATTERS) {
            try {
                return LocalTime.parse(firstPart, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private Long preferredPartnerId(SubscriptionPartnerAssignment assignment) {
        if (assignment == null) {
            return null;
        }
        return assignment.getReplacementPartnerId() != null ? assignment.getReplacementPartnerId() : assignment.getPartnerId();
    }

    private double getInitialSearchRadiusKm() {
        return Math.max(0.1d, Math.min(getMaxSearchRadiusKm(), dispatchProperties.getPartner().getInitialSearchRadiusKm()));
    }

    private double getMaxSearchRadiusKm() {
        return Math.max(0.1d, Math.min(25.0d, dispatchProperties.getPartner().getMaxSearchRadiusKm()));
    }

    private int getMaxEtaMinutes() {
        return Math.max(1, Math.min(120, dispatchProperties.getBooking().getMaxEtaMinutes()));
    }

    private int getPreDispatchMinutes() {
        return Math.max(5, Math.min(240, dispatchProperties.getBooking().getPreDispatchMinutes()));
    }

    private record PartnerCandidate(Long partnerId, double distanceKm, double rating, int activeJobs) {}
}

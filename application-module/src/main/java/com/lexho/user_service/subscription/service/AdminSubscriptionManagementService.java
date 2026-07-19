package com.lexho.user_service.subscription.service;

import com.lexho.user_service.admin.entity.AdminServiceEntity;
import com.lexho.user_service.admin.repository.AdminServiceEntityRepository;
import com.lexho.user_service.dispatch.repository.PartnerServiceSkillRepository;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.lexhopartner.entity.PartnerProfile;
import com.lexho.user_service.lexhopartner.repository.PartnerProfileRepository;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminSubscriptionManagementService {

    private final SubscriptionMasterRepository subscriptionMasterRepository;
    private final SubscriptionFrequencyRepository subscriptionFrequencyRepository;
    private final SubscriptionVisitRepository subscriptionVisitRepository;
    private final AdminServiceEntityRepository serviceRepository;
    private final SubscriptionPartnerAssignmentRepository subscriptionPartnerAssignmentRepository;
    private final SubscriptionVisitPartnerAssignmentRepository visitPartnerAssignmentRepository;
    private final PartnerProfileRepository partnerProfileRepository;
    private final PartnerServiceSkillRepository partnerServiceSkillRepository;

    public AdminSubscriptionManagementService(SubscriptionMasterRepository subscriptionMasterRepository,
                                              SubscriptionFrequencyRepository subscriptionFrequencyRepository,
                                              SubscriptionVisitRepository subscriptionVisitRepository,
                                              AdminServiceEntityRepository serviceRepository,
                                              SubscriptionPartnerAssignmentRepository subscriptionPartnerAssignmentRepository,
                                              SubscriptionVisitPartnerAssignmentRepository visitPartnerAssignmentRepository,
                                              PartnerProfileRepository partnerProfileRepository,
                                              PartnerServiceSkillRepository partnerServiceSkillRepository) {
        this.subscriptionMasterRepository = subscriptionMasterRepository;
        this.subscriptionFrequencyRepository = subscriptionFrequencyRepository;
        this.subscriptionVisitRepository = subscriptionVisitRepository;
        this.serviceRepository = serviceRepository;
        this.subscriptionPartnerAssignmentRepository = subscriptionPartnerAssignmentRepository;
        this.visitPartnerAssignmentRepository = visitPartnerAssignmentRepository;
        this.partnerProfileRepository = partnerProfileRepository;
        this.partnerServiceSkillRepository = partnerServiceSkillRepository;
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
        SubscriptionMaster subscription = getSubscription(subscriptionId);
        subscription.setStatus(request.status());
        if (request.status() == SubscriptionStatus.CANCELLED) {
            subscriptionVisitRepository.cancelFutureScheduled(
                    subscriptionId,
                    VisitStatus.SCHEDULED,
                    VisitStatus.CANCELLED,
                    java.time.LocalDate.now()
            );
        }
        subscriptionMasterRepository.save(subscription);
        return detail(subscriptionId);
    }

    @Transactional
    public SubscriptionCalendarItemResponse updateVisitStatus(Long subscriptionId,
                                                              Long visitId,
                                                              AdminSubscriptionVisitStatusUpdateRequest request) {
        getSubscription(subscriptionId);
        SubscriptionVisit visit = subscriptionVisitRepository.findByIdAndSubscriptionId(visitId, subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found"));
        validateVisitStatusChange(request.status());
        visit.setStatus(request.status());
        SubscriptionVisit saved = subscriptionVisitRepository.save(visit);
        return toCalendarItem(saved);
    }

    @Transactional
    public AdminSubscriptionAssignmentResponse assignPartner(Long subscriptionId, AdminPartnerAssignRequest request) {
        SubscriptionMaster subscription = getSubscription(subscriptionId);
        PartnerProfile partner = getValidPartner(request.partnerId());
        validatePartnerSkill(partner.getId(), subscription.getServiceId());

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
        return assignment(subscriptionId);
    }

    @Transactional
    public AdminSubscriptionAssignmentResponse replacePartner(Long subscriptionId, AdminPartnerAssignRequest request) {
        SubscriptionMaster subscription = getSubscription(subscriptionId);
        PartnerProfile replacement = getValidPartner(request.partnerId());
        validatePartnerSkill(replacement.getId(), subscription.getServiceId());

        SubscriptionPartnerAssignment assignment = subscriptionPartnerAssignmentRepository
                .findBySubscriptionId(subscriptionId)
                .orElseThrow(() -> new BadRequestException("Assign a partner before replacement"));
        assignment.setReplacementPartnerId(replacement.getId());
        assignment.setReplacementReason(normalizeReason(request.reason()));
        assignment.setStatus(PartnerAssignmentStatus.REPLACED);
        assignment.setReplacedAt(java.time.LocalDateTime.now());
        subscriptionPartnerAssignmentRepository.save(assignment);
        return assignment(subscriptionId);
    }

    @Transactional
    public AdminVisitAssignmentResponse assignVisitPartner(Long subscriptionId,
                                                           Long visitId,
                                                           AdminPartnerAssignRequest request) {
        SubscriptionMaster subscription = getSubscription(subscriptionId);
        subscriptionVisitRepository.findByIdAndSubscriptionId(visitId, subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found"));
        PartnerProfile partner = getValidPartner(request.partnerId());
        validatePartnerSkill(partner.getId(), subscription.getServiceId());

        SubscriptionVisitPartnerAssignment assignment = visitPartnerAssignmentRepository.findByVisitId(visitId)
                .orElseGet(SubscriptionVisitPartnerAssignment::new);
        assignment.setSubscriptionId(subscriptionId);
        assignment.setVisitId(visitId);
        assignment.setPartnerId(partner.getId());
        assignment.setStatus(PartnerAssignmentStatus.ASSIGNED);
        assignment.setReason(normalizeReason(request.reason()));
        assignment.setAssignedAt(java.time.LocalDateTime.now());
        SubscriptionVisitPartnerAssignment saved = visitPartnerAssignmentRepository.save(assignment);
        return toVisitAssignmentResponse(saved, Map.of(partner.getId(), partner));
    }

    @Transactional(readOnly = true)
    public AdminSubscriptionAssignmentResponse assignment(Long subscriptionId) {
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
        Map<Long, PartnerProfile> partners = partnerProfileRepository.findAllById(partnerIds)
                .stream()
                .collect(Collectors.toMap(PartnerProfile::getId, Function.identity()));

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

    private Map<Long, AdminServiceEntity> servicesById(List<Long> serviceIds) {
        return serviceRepository.findAllById(serviceIds)
                .stream()
                .collect(Collectors.toMap(AdminServiceEntity::getId, Function.identity()));
    }

    private void validateVisitStatusChange(VisitStatus status) {
        if (status == VisitStatus.RESCHEDULED) {
            throw new BadRequestException("Use reschedule API for RESCHEDULED status");
        }
    }

    private PartnerProfile getValidPartner(Long partnerId) {
        PartnerProfile partner = partnerProfileRepository.findById(partnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Partner not found"));
        if (partner.isDeleted()) {
            throw new BadRequestException("Partner is deleted");
        }
        return partner;
    }

    private void validatePartnerSkill(Long partnerId, Long serviceId) {
        if (!partnerServiceSkillRepository.existsByPartnerIdAndSubServiceId(partnerId, serviceId)) {
            throw new BadRequestException("Partner is not mapped to this subscription service");
        }
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

    private String partnerName(PartnerProfile partner) {
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
                                                                   Map<Long, PartnerProfile> partners) {
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

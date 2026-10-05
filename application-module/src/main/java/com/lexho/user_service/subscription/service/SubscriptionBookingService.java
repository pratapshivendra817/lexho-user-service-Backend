package com.lexho.user_service.subscription.service;

import com.lexho.booking.entity.Order;
import com.lexho.booking.entity.OrderItem;
import com.lexho.booking.repository.OrderRepository;
import com.lexho.common.enums.OrderStatus;
import com.lexho.common.enums.PaymentStatus;
import com.lexho.user_service.admin.geo.service.GeoFenceValidationService;
import com.lexho.user_service.admin.entity.AdminServiceEntity;
import com.lexho.user_service.admin.repository.AdminServiceEntityRepository;
import com.lexho.user_service.audit.service.AuditLogService;
import com.lexho.user_service.coupon.service.CouponService;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.lexho.repository.LexhoAddressRepository;
import com.lexho.user_service.customer.location.entity.CustomerAddress;
import com.lexho.user_service.customer.location.repository.CustomerAddressRepository;
import com.lexho.user_service.subscription.dto.CreateSubscriptionRequest;
import com.lexho.user_service.subscription.dto.RescheduleVisitRequest;
import com.lexho.user_service.subscription.dto.SubscriptionCalendarItemResponse;
import com.lexho.user_service.subscription.dto.SubscriptionCreateResponse;
import com.lexho.user_service.subscription.dto.SubscriptionDetailResponse;
import com.lexho.user_service.subscription.dto.SubscriptionListItemResponse;
import com.lexho.user_service.subscription.entity.SubscriptionFrequency;
import com.lexho.user_service.subscription.entity.SubscriptionMaster;
import com.lexho.user_service.subscription.entity.SubscriptionVisit;
import com.lexho.user_service.subscription.enums.FamilySize;
import com.lexho.user_service.subscription.enums.SubscriptionStatus;
import com.lexho.user_service.subscription.enums.VisitStatus;
import com.lexho.user_service.subscription.exception.SubscriptionValidationException;
import com.lexho.user_service.subscription.repository.SubscriptionFrequencyRepository;
import com.lexho.user_service.subscription.repository.SubscriptionMasterRepository;
import com.lexho.user_service.subscription.repository.SubscriptionVisitRepository;
import com.lexho.user_service.util.LexhoConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SubscriptionBookingService {
    private static final Logger LOG = LoggerFactory.getLogger(SubscriptionBookingService.class);

    private final SubscriptionMasterRepository subscriptionMasterRepository;
    private final SubscriptionFrequencyRepository subscriptionFrequencyRepository;
    private final SubscriptionVisitRepository subscriptionVisitRepository;
    private final AdminServiceEntityRepository serviceRepository;
    private final LexhoAddressRepository addressRepository;
    private final CustomerAddressRepository customerAddressRepository;
    private final SubscriptionPricingService subscriptionPricingService;
    private final OrderRepository orderRepository;
    private final GeoFenceValidationService geoFenceValidationService;
    private final CouponService couponService;
    private final AuditLogService auditLogService;

    public SubscriptionBookingService(SubscriptionMasterRepository subscriptionMasterRepository,
                                      SubscriptionFrequencyRepository subscriptionFrequencyRepository,
                                      SubscriptionVisitRepository subscriptionVisitRepository,
                                      AdminServiceEntityRepository serviceRepository,
                                      LexhoAddressRepository addressRepository,
                                      SubscriptionPricingService subscriptionPricingService,
                                      OrderRepository orderRepository,
                                      GeoFenceValidationService geoFenceValidationService,
                                      CouponService couponService) {
        this(subscriptionMasterRepository, subscriptionFrequencyRepository, subscriptionVisitRepository,
                serviceRepository, addressRepository, subscriptionPricingService, orderRepository,
                geoFenceValidationService, couponService, Optional.empty(), null);
    }

    @Autowired
    public SubscriptionBookingService(SubscriptionMasterRepository subscriptionMasterRepository,
                                      SubscriptionFrequencyRepository subscriptionFrequencyRepository,
                                      SubscriptionVisitRepository subscriptionVisitRepository,
                                      AdminServiceEntityRepository serviceRepository,
                                      LexhoAddressRepository addressRepository,
                                      SubscriptionPricingService subscriptionPricingService,
                                      OrderRepository orderRepository,
                                      GeoFenceValidationService geoFenceValidationService,
                                      CouponService couponService,
                                      Optional<AuditLogService> auditLogService) {
        this(subscriptionMasterRepository, subscriptionFrequencyRepository, subscriptionVisitRepository,
                serviceRepository, addressRepository, subscriptionPricingService, orderRepository,
                geoFenceValidationService, couponService, auditLogService, null);
    }

    public SubscriptionBookingService(SubscriptionMasterRepository subscriptionMasterRepository,
                                      SubscriptionFrequencyRepository subscriptionFrequencyRepository,
                                      SubscriptionVisitRepository subscriptionVisitRepository,
                                      AdminServiceEntityRepository serviceRepository,
                                      LexhoAddressRepository addressRepository,
                                      SubscriptionPricingService subscriptionPricingService,
                                      OrderRepository orderRepository,
                                      GeoFenceValidationService geoFenceValidationService,
                                      CouponService couponService,
                                      Optional<AuditLogService> auditLogService,
                                      CustomerAddressRepository customerAddressRepository) {
        this.subscriptionMasterRepository = subscriptionMasterRepository;
        this.subscriptionFrequencyRepository = subscriptionFrequencyRepository;
        this.subscriptionVisitRepository = subscriptionVisitRepository;
        this.serviceRepository = serviceRepository;
        this.addressRepository = addressRepository;
        this.customerAddressRepository = customerAddressRepository;
        this.subscriptionPricingService = subscriptionPricingService;
        this.orderRepository = orderRepository;
        this.geoFenceValidationService = geoFenceValidationService;
        this.couponService = couponService;
        this.auditLogService = optionalValue(auditLogService);
    }

    private static <T> T optionalValue(Optional<T> optional) {
        return optional == null ? null : optional.orElse(null);
    }

    @Transactional
    public SubscriptionCreateResponse create(Long customerId, CreateSubscriptionRequest request) {
        return create(customerId, request, null);
    }

    @Transactional
    public SubscriptionCreateResponse create(Long customerId,
                                             CreateSubscriptionRequest request,
                                             String idempotencyKey) {
        List<String> frequencyDays = SubscriptionScheduleSupport.normalizeDays(request.frequencyDays());
        validateBasicRequest(request, frequencyDays);
        String normalizedKey = normalizeIdempotencyKey(idempotencyKey);
        String requestHash = requestHash(request, frequencyDays);
        if (normalizedKey != null) {
            addressRepository.findByUserIdAndIsDeletedFalseForUpdate(customerId);
            SubscriptionMaster existing = subscriptionMasterRepository
                    .findByCustomerIdAndIdempotencyKey(customerId, normalizedKey).orElse(null);
            if (existing != null) {
                if (!requestHash.equals(existing.getRequestHash())) {
                    throw new SubscriptionValidationException("Idempotency key was already used for a different subscription request");
                }
                return subscriptionCreateResponse(existing);
            }
        }

        AdminServiceEntity service = serviceRepository.findByIdAndDeletedFalse(request.serviceId())
                .orElseThrow(() -> new SubscriptionValidationException("Service not found"));
        if (!service.isActive() || !supportsSubscription(service)) {
            throw new SubscriptionValidationException("Service does not support SUBSCRIPTION booking mode");
        }

        com.lexho.user_service.lexho.entity.LexhoAddress address = resolveAddress(customerId, request.addressId(), request.latitude(), request.longitude());
        if (address == null) {
            throw new SubscriptionValidationException("Address does not belong to logged-in customer");
        }
        try {
            geoFenceValidationService.validateBookingAddress(address);
        } catch (RuntimeException ex) {
            throw new SubscriptionValidationException(ex.getMessage());
        }

        String serviceType = resolveServiceType(request, service);
        List<LocalDate> generatedDates = SubscriptionScheduleSupport.generateVisitDates(
                request.startDate(),
                request.endDate(),
                frequencyDays
        );
        int visitCount = generatedDates.size();
        PriceSelection priceSelection = resolvePrice(request, serviceType, visitCount);

        Order paymentBooking = new Order();
        paymentBooking.setUserId(customerId);
        paymentBooking.setStatus(OrderStatus.CONFIRMED);
        paymentBooking.setPaymentStatus(PaymentStatus.PENDING);
        paymentBooking.setBookingType(LexhoConstants.TYPE_SUBSCRIPTION);
        paymentBooking.setScheduledAt(request.startDate().atStartOfDay());
        paymentBooking.setAddress(formatAddress(address));
        paymentBooking.setTotalAmount(priceSelection.totalPrice().doubleValue());
        couponService.applyToOrder(paymentBooking, request.couponCode(), request.serviceId(), LexhoConstants.TYPE_SUBSCRIPTION);
        OrderItem item = new OrderItem();
        item.setPartnerId(service.getId());
        item.setPartnerName(service.getName());
        item.setPrice(paymentBooking.getTotalAmount());
        item.setQuantity(1);
        item.setOrder(paymentBooking);
        paymentBooking.setItems(new ArrayList<>(List.of(item)));
        Order savedPaymentBooking = orderRepository.save(paymentBooking);

        SubscriptionMaster master = new SubscriptionMaster();
        master.setCustomerId(customerId);
        master.setServiceId(request.serviceId());
        master.setAddressId(address.getId());
        master.setBookingId(savedPaymentBooking.getId());
        master.setBookingMode(normalizeBookingMode(request.bookingMode()));
        master.setServiceType(serviceType);
        master.setStartDate(request.startDate());
        master.setEndDate(request.endDate());
        master.setVisits(visitCount);
        master.setDurationHours(priceSelection.durationHours());
        master.setFamilySize(priceSelection.familySize());
        master.setPerVisitPrice(priceSelection.perVisitPrice());
        master.setTimeSlot(request.timeSlot().trim());
        master.setPrice(priceSelection.totalPrice());
        master.setStatus(SubscriptionStatus.PENDING);
        master.setIdempotencyKey(normalizedKey);
        master.setRequestHash(normalizedKey == null ? null : requestHash);
        SubscriptionMaster savedMaster = subscriptionMasterRepository.save(master);

        List<SubscriptionFrequency> frequencies = frequencyDays.stream()
                .map(day -> {
                    SubscriptionFrequency frequency = new SubscriptionFrequency();
                    frequency.setSubscriptionId(savedMaster.getId());
                    frequency.setDayName(day);
                    return frequency;
                })
                .toList();
        subscriptionFrequencyRepository.saveAll(frequencies);

        List<SubscriptionVisit> visits = generatedDates.stream()
                .map(date -> {
                    SubscriptionVisit visit = new SubscriptionVisit();
                    visit.setSubscriptionId(savedMaster.getId());
                    visit.setVisitDate(date);
                    visit.setTimeSlot(request.timeSlot().trim());
                    visit.setStatus(VisitStatus.SCHEDULED);
                    return visit;
                })
                .toList();
        List<SubscriptionVisit> savedVisits = subscriptionVisitRepository.saveAll(visits);

        return subscriptionCreateResponse(savedMaster, frequencyDays, savedVisits);
    }

    private SubscriptionCreateResponse subscriptionCreateResponse(SubscriptionMaster master) {
        List<String> frequencyDays = subscriptionFrequencyRepository
                .findBySubscriptionIdOrderByIdAsc(master.getId()).stream()
                .map(SubscriptionFrequency::getDayName)
                .toList();
        List<SubscriptionVisit> visits = subscriptionVisitRepository
                .findBySubscriptionIdOrderByVisitDateAscIdAsc(master.getId());
        return subscriptionCreateResponse(master, frequencyDays, visits);
    }

    private SubscriptionCreateResponse subscriptionCreateResponse(SubscriptionMaster master,
                                                                  List<String> frequencyDays,
                                                                  List<SubscriptionVisit> visits) {
        return new SubscriptionCreateResponse(
                master.getId(), master.getId(), master.getBookingId(), master.getServiceId(),
                master.getAddressId(), master.getBookingMode(), master.getServiceType(), frequencyDays,
                master.getStartDate(), master.getEndDate(), master.getVisits(), master.getDurationHours(),
                master.getFamilySize() == null ? null : master.getFamilySize().name(),
                subscriptionPricingService.familySizeLabel(master.getFamilySize()),
                master.getPerVisitPrice(), master.getTimeSlot(), master.getPrice(), master.getStatus().name(),
                visits.stream().map(this::toCalendarItem).toList()
        );
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }
        String normalized = idempotencyKey.trim();
        if (normalized.length() > 100) {
            throw new SubscriptionValidationException("Idempotency-Key must not exceed 100 characters");
        }
        return normalized;
    }

    private String requestHash(CreateSubscriptionRequest request, List<String> frequencyDays) {
        String canonical = String.join("|",
                String.valueOf(request.serviceId()),
                String.valueOf(request.addressId()),
                String.valueOf(request.latitude()),
                String.valueOf(request.longitude()),
                String.valueOf(request.bookingMode()),
                String.valueOf(request.serviceType()),
                String.join(",", frequencyDays),
                String.valueOf(request.startDate()),
                String.valueOf(request.endDate()),
                String.valueOf(request.durationHours()),
                String.valueOf(request.familySize()),
                String.valueOf(request.timeSlot() == null ? null : request.timeSlot().trim()),
                String.valueOf(request.couponCode()));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private com.lexho.user_service.lexho.entity.LexhoAddress resolveAddress(Long customerId, String addressId, Double latitude, Double longitude) {
        try {
            Long legacyId = Long.valueOf(addressId);
            return addressRepository.findByIdAndUserIdAndIsDeletedFalse(legacyId, customerId).orElse(null);
        } catch (NumberFormatException ignored) {
            // The customer location API uses UUIDs. Mirror that address into the legacy table,
            // exactly as the scheduled booking flow does, so subscription bookings use one ID space.
            if (customerAddressRepository == null) return null;
            final UUID uuid;
            try { uuid = UUID.fromString(addressId); } catch (IllegalArgumentException ex) { return null; }
            CustomerAddress source = customerAddressRepository
                    .findByIdAndCustomerIdAndIsDeletedFalse(uuid, customerId).orElse(null);
            if (source == null) {
                // Some deployments expose customer locations from a separate store. Use the
                // customer's legacy address with the closest coordinates as a compatibility fallback.
                return addressRepository.findByUserIdAndIsDeletedFalse(customerId).stream()
                        .filter(a -> latitude == null || longitude == null || a.getLatitude() == null || a.getLongitude() == null
                                || (Math.abs(a.getLatitude().doubleValue() - latitude) < 0.001
                                && Math.abs(a.getLongitude().doubleValue() - longitude) < 0.001))
                        .sorted((a, b) -> Boolean.compare(!a.isDefault(), !b.isDefault()))
                        .findFirst().orElse(null);
            }
            String sourceKey = "CUSTOMER_LOCATION:" + uuid;
            com.lexho.user_service.lexho.entity.LexhoAddress target = addressRepository
                    .findByUserIdAndCreatedByAndIsDeletedFalse(customerId, sourceKey)
                    .orElseGet(() -> { var a = new com.lexho.user_service.lexho.entity.LexhoAddress(); a.setUserId(customerId); return a; });
            target.setHouseNo(firstNonBlank(source.getFlatNo(), source.getBuildingName(), source.getAddressLine()));
            target.setArea(firstNonBlank(source.getArea(), source.getSector(), source.getAddressLine()));
            target.setCity(source.getCity()); target.setState(source.getState()); target.setPincode(source.getPincode());
            target.setLatitude(source.getLatitude()); target.setLongitude(source.getLongitude());
            target.setAddressResolved(source.getLatitude() != null && source.getLongitude() != null);
            target.setCreatedBy(sourceKey); target.setUpdatedBy("CUSTOMER_LOCATION_SYNC");
            return addressRepository.save(target);
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value;
        return "Unknown";
    }

    @Transactional
    public void activateByBookingId(Long bookingId) {
        subscriptionMasterRepository.findByBookingId(bookingId).ifPresent(subscription -> {
            if (subscription.getStatus() == SubscriptionStatus.PENDING) {
                subscription.setStatus(SubscriptionStatus.ACTIVE);
                subscriptionMasterRepository.save(subscription);
            }
        });
    }

    @Transactional(readOnly = true)
    public List<SubscriptionListItemResponse> list(Long customerId) {
        List<SubscriptionMaster> subscriptions = subscriptionMasterRepository.findByCustomerIdOrderByIdDesc(customerId);
        if (subscriptions.isEmpty()) {
            return List.of();
        }
        List<Long> subscriptionIds = subscriptions.stream().map(SubscriptionMaster::getId).toList();
        Map<Long, List<SubscriptionVisit>> visitsBySubscription = subscriptionVisitRepository
                .findBySubscriptionIdInOrderBySubscriptionIdAscVisitDateAscIdAsc(subscriptionIds)
                .stream()
                .collect(Collectors.groupingBy(
                        SubscriptionVisit::getSubscriptionId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        Map<Long, AdminServiceEntity> services = serviceRepository.findAllById(
                        subscriptions.stream().map(SubscriptionMaster::getServiceId).collect(Collectors.toSet())
                )
                .stream()
                .collect(Collectors.toMap(AdminServiceEntity::getId, Function.identity()));

        return subscriptions.stream()
                .map(subscription -> {
                    List<SubscriptionVisit> visits = visitsBySubscription.getOrDefault(subscription.getId(), List.of());
                    long completed = countVisits(visits, VisitStatus.COMPLETED);
                    return new SubscriptionListItemResponse(
                            subscription.getId(),
                            serviceName(services.get(subscription.getServiceId())),
                            subscription.getStartDate(),
                            subscription.getEndDate(),
                            subscription.getVisits(),
                            completed,
                            Math.max(subscription.getVisits() - completed, 0),
                            subscription.getStatus().name()
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> subscriptionCards(Long customerId) {
        List<SubscriptionMaster> subscriptions = subscriptionMasterRepository.findByCustomerIdOrderByIdDesc(customerId);
        if (subscriptions.isEmpty()) {
            return List.of();
        }
        Map<Long, AdminServiceEntity> services = serviceRepository.findAllById(
                        subscriptions.stream().map(SubscriptionMaster::getServiceId).collect(Collectors.toSet())
                )
                .stream()
                .collect(Collectors.toMap(AdminServiceEntity::getId, Function.identity()));
        return subscriptions.stream()
                .map(subscription -> subscriptionCardPayload(subscription, services.get(subscription.getServiceId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> subscriptionCard(Long customerId, Long subscriptionId) {
        SubscriptionMaster subscription = getOwnedSubscription(customerId, subscriptionId);
        AdminServiceEntity service = serviceRepository.findById(subscription.getServiceId()).orElse(null);
        return subscriptionCardPayload(subscription, service);
    }

    @Transactional(readOnly = true)
    public SubscriptionDetailResponse detail(Long customerId, Long subscriptionId) {
        SubscriptionMaster subscription = getOwnedSubscription(customerId, subscriptionId);
        List<String> frequencyDays = subscriptionFrequencyRepository.findBySubscriptionIdOrderByIdAsc(subscriptionId)
                .stream()
                .map(SubscriptionFrequency::getDayName)
                .toList();
        List<SubscriptionVisit> visits = subscriptionVisitRepository.findBySubscriptionIdOrderByVisitDateAscIdAsc(subscriptionId);
        long completed = countVisits(visits, VisitStatus.COMPLETED);
        AdminServiceEntity service = serviceRepository.findById(subscription.getServiceId()).orElse(null);

        return new SubscriptionDetailResponse(
                subscription.getId(),
                subscription.getServiceId(),
                serviceName(service),
                subscription.getAddressId(),
                subscription.getBookingMode(),
                subscription.getServiceType(),
                frequencyDays,
                subscription.getStartDate(),
                subscription.getEndDate(),
                subscription.getVisits(),
                subscription.getDurationHours(),
                subscription.getFamilySize() == null ? null : subscription.getFamilySize().name(),
                subscriptionPricingService.familySizeLabel(subscription.getFamilySize()),
                subscription.getPerVisitPrice(),
                subscription.getTimeSlot(),
                subscription.getPrice(),
                subscription.getStatus().name(),
                completed,
                Math.max(subscription.getVisits() - completed, 0),
                visits.stream().map(this::toCalendarItem).toList()
        );
    }

    @Transactional(readOnly = true)
    public List<SubscriptionCalendarItemResponse> calendar(Long customerId, Long subscriptionId) {
        getOwnedSubscription(customerId, subscriptionId);
        return subscriptionVisitRepository.findBySubscriptionIdOrderByVisitDateAscIdAsc(subscriptionId)
                .stream()
                .map(this::toCalendarItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> visits(Long customerId, Long subscriptionId) {
        getOwnedSubscription(customerId, subscriptionId);
        List<SubscriptionVisit> visits = subscriptionVisitRepository.findBySubscriptionIdOrderByVisitDateAscIdAsc(subscriptionId);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("upcomingVisits", visits.stream()
                .filter(visit -> visit.getStatus() == VisitStatus.SCHEDULED || visit.getStatus() == VisitStatus.RESCHEDULED
                        || visit.getStatus() == VisitStatus.ACCEPTED || visit.getStatus() == VisitStatus.ARRIVED
                        || visit.getStatus() == VisitStatus.IN_PROGRESS)
                .map(this::visitPayload)
                .toList());
        payload.put("completedVisits", visits.stream()
                .filter(visit -> visit.getStatus() == VisitStatus.COMPLETED)
                .map(this::visitPayload)
                .toList());
        payload.put("cancelledVisits", visits.stream()
                .filter(visit -> visit.getStatus() == VisitStatus.CANCELLED || visit.getStatus() == VisitStatus.SKIPPED)
                .map(this::visitPayload)
                .toList());
        return payload;
    }

    @Transactional
    public void cancel(Long customerId, Long subscriptionId) {
        SubscriptionMaster subscription = getOwnedSubscriptionForUpdate(customerId, subscriptionId);
        if (subscription.getStatus() == SubscriptionStatus.CANCELLED) {
            return;
        }
        if (subscription.getStatus() == SubscriptionStatus.COMPLETED) {
            throw new SubscriptionValidationException("Subscription cannot be cancelled in current state");
        }
        String oldStatus = subscription.getStatus().name();
        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscriptionMasterRepository.save(subscription);
        subscriptionVisitRepository.cancelFutureScheduled(
                subscriptionId,
                VisitStatus.SCHEDULED,
                VisitStatus.CANCELLED,
                LocalDate.now()
        );
        subscriptionVisitRepository.cancelFutureScheduled(
                subscriptionId,
                VisitStatus.RESCHEDULED,
                VisitStatus.CANCELLED,
                LocalDate.now()
        );
        audit(customerId, subscription.getBookingId(), "SUBSCRIPTION_CANCELLED", oldStatus, "CANCELLED");
    }

    @Transactional
    public Map<String, Object> pause(Long customerId, Long subscriptionId) {
        SubscriptionMaster subscription = getOwnedSubscriptionForUpdate(customerId, subscriptionId);
        if (subscription.getStatus() == SubscriptionStatus.PAUSED) {
            return subscriptionCardPayload(subscription, serviceRepository.findById(subscription.getServiceId()).orElse(null));
        }
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new SubscriptionValidationException("Only ACTIVE subscription can be paused");
        }
        String oldStatus = subscription.getStatus().name();
        subscription.setStatus(SubscriptionStatus.PAUSED);
        SubscriptionMaster saved = subscriptionMasterRepository.save(subscription);
        audit(customerId, saved.getBookingId(), "SUBSCRIPTION_PAUSED", oldStatus, "PAUSED");
        return subscriptionCardPayload(saved, serviceRepository.findById(saved.getServiceId()).orElse(null));
    }

    @Transactional
    public Map<String, Object> resume(Long customerId, Long subscriptionId) {
        SubscriptionMaster subscription = getOwnedSubscriptionForUpdate(customerId, subscriptionId);
        if (subscription.getStatus() == SubscriptionStatus.ACTIVE) {
            return subscriptionCardPayload(subscription, serviceRepository.findById(subscription.getServiceId()).orElse(null));
        }
        if (subscription.getStatus() != SubscriptionStatus.PAUSED) {
            throw new SubscriptionValidationException("Only PAUSED subscription can be resumed");
        }
        String oldStatus = subscription.getStatus().name();
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        SubscriptionMaster saved = subscriptionMasterRepository.save(subscription);
        audit(customerId, saved.getBookingId(), "SUBSCRIPTION_RESUMED", oldStatus, "ACTIVE");
        return subscriptionCardPayload(saved, serviceRepository.findById(saved.getServiceId()).orElse(null));
    }

    @Transactional
    public void skipVisit(Long customerId, Long subscriptionId, Long visitId) {
        getOwnedSubscriptionForUpdate(customerId, subscriptionId);
        SubscriptionVisit visit = getVisitForUpdate(subscriptionId, visitId);
        if (!visit.getVisitDate().isAfter(LocalDate.now())) {
            throw new SubscriptionValidationException("Only future visit can be skipped");
        }
        if (visit.getStatus() != VisitStatus.SCHEDULED) {
            throw new SubscriptionValidationException("Only SCHEDULED visit can be skipped");
        }
        visit.setStatus(VisitStatus.SKIPPED);
        subscriptionVisitRepository.save(visit);
    }

    @Transactional
    public void rescheduleVisit(Long customerId,
                                Long subscriptionId,
                                Long visitId,
                                RescheduleVisitRequest request) {
        SubscriptionMaster subscription = getOwnedSubscriptionForUpdate(customerId, subscriptionId);
        SubscriptionVisit visit = getVisitForUpdate(subscriptionId, visitId);
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new SubscriptionValidationException("Only ACTIVE subscription visits can be rescheduled");
        }
        if (!request.newDate().isAfter(LocalDate.now())) {
            throw new SubscriptionValidationException("New date must be future date");
        }
        if (visit.getStatus() == VisitStatus.RESCHEDULED
                && request.newDate().equals(visit.getVisitDate())) {
            return;
        }
        if (visit.getStatus() != VisitStatus.SCHEDULED) {
            throw new SubscriptionValidationException("Only SCHEDULED visit can be rescheduled");
        }
        if (subscriptionVisitRepository.existsBySubscriptionIdAndVisitDateAndIdNot(subscriptionId, request.newDate(), visitId)) {
            throw new SubscriptionValidationException("Visit already exists on selected date");
        }
        visit.setVisitDate(request.newDate());
        visit.setRescheduledDate(request.newDate());
        visit.setStatus(VisitStatus.RESCHEDULED);
        subscriptionVisitRepository.save(visit);
    }

    private void validateBasicRequest(CreateSubscriptionRequest request, List<String> frequencyDays) {
        List<String> errors = new ArrayList<>();
        if (request.startDate() != null && request.endDate() != null && request.startDate().isAfter(request.endDate())) {
            errors.add("startDate must not be after endDate");
        }
        if (request.timeSlot() != null && request.timeSlot().isBlank()) {
            errors.add("timeSlot cannot be blank");
        }
        if (frequencyDays.isEmpty()) {
            errors.add("frequencyDays cannot be empty");
        }
        if (!errors.isEmpty()) {
            throw new SubscriptionValidationException(errors);
        }
    }

    private boolean supportsSubscription(AdminServiceEntity service) {
        String serviceType = service.getServiceType();
        if (serviceType != null && serviceType.toUpperCase(Locale.ROOT).contains(LexhoConstants.TYPE_SUBSCRIPTION)) {
            return true;
        }
        // Home repeat booking uses the same catalog services as scheduled booking.
        // Actual availability remains gated by subscription settings and configured pricing.
        String category = String.valueOf(service.getCategoryName()).toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
        return category.equals("cleaningservice") || category.equals("cleaning")
                || category.equals("cookathome") || category.equals("cook")
                || category.equals("vehiclewash") || category.equals("vehicalwash")
                || category.equals("fitnesswellness") || category.equals("fitnessandwellness");
    }

    private PriceSelection resolvePrice(CreateSubscriptionRequest request, String serviceType, int visits) {
        if (visits <= 0) {
            throw new SubscriptionValidationException("No visits generated for selected weekdays and date range");
        }
        if ("COOK".equals(serviceType)) {
            if (request.familySize() == null) {
                throw new SubscriptionValidationException("familySize is required for COOK subscription");
            }
            java.math.BigDecimal perVisitPrice = subscriptionPricingService
                    .resolveCookPricePerVisit(request.serviceId(), request.familySize());
            return new PriceSelection(null, request.familySize(), perVisitPrice,
                    perVisitPrice.multiply(java.math.BigDecimal.valueOf(visits)));
        }
        if (request.durationHours() == null) {
            throw new SubscriptionValidationException("durationHours is required for CLEANING subscription");
        }
        java.math.BigDecimal perVisitPrice = subscriptionPricingService
                .resolvePerVisitPrice(request.serviceId(), request.durationHours());
        return new PriceSelection(request.durationHours(), null, perVisitPrice,
                perVisitPrice.multiply(java.math.BigDecimal.valueOf(visits)));
    }

    private String resolveServiceType(CreateSubscriptionRequest request, AdminServiceEntity service) {
        if (request.serviceType() != null && !request.serviceType().isBlank()) {
            return request.serviceType().trim().toUpperCase(Locale.ROOT);
        }
        String category = service.getCategoryName();
        if (category != null && category.toUpperCase(Locale.ROOT).contains("COOK")) {
            return "COOK";
        }
        String name = service.getName();
        if (name != null && name.toUpperCase(Locale.ROOT).contains("COOK")) {
            return "COOK";
        }
        return "CLEANING";
    }

    private String normalizeBookingMode(String bookingMode) {
        return bookingMode == null || bookingMode.isBlank()
                ? LexhoConstants.TYPE_SUBSCRIPTION
                : bookingMode.trim().toUpperCase(Locale.ROOT);
    }

    private String formatAddress(com.lexho.user_service.lexho.entity.LexhoAddress address) {
        return java.util.stream.Stream.of(address.getHouseNo(), address.getArea(), address.getCity(), address.getState(), address.getPincode())
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(", "));
    }

    private SubscriptionMaster getOwnedSubscription(Long customerId, Long subscriptionId) {
        return subscriptionMasterRepository.findByIdAndCustomerId(subscriptionId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
    }

    private SubscriptionMaster getOwnedSubscriptionForUpdate(Long customerId, Long subscriptionId) {
        SubscriptionMaster subscription = subscriptionMasterRepository.findByIdForUpdate(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
        if (!subscription.getCustomerId().equals(customerId)) {
            throw new ResourceNotFoundException("Subscription not found");
        }
        return subscription;
    }

    private SubscriptionVisit getVisit(Long subscriptionId, Long visitId) {
        return subscriptionVisitRepository.findByIdAndSubscriptionId(visitId, subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found"));
    }

    private SubscriptionVisit getVisitForUpdate(Long subscriptionId, Long visitId) {
        SubscriptionVisit visit = subscriptionVisitRepository.findByIdForUpdate(visitId)
                .filter(candidate -> candidate.getSubscriptionId().equals(subscriptionId))
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found"));
        return visit;
    }

    private long countVisits(List<SubscriptionVisit> visits, VisitStatus status) {
        return visits.stream().filter(visit -> visit.getStatus() == status).count();
    }

    private String serviceName(AdminServiceEntity service) {
        return service == null || service.getName() == null ? "" : service.getName();
    }

    private Map<String, Object> subscriptionCardPayload(SubscriptionMaster subscription, AdminServiceEntity service) {
        List<String> frequencyDays = subscriptionFrequencyRepository.findBySubscriptionIdOrderByIdAsc(subscription.getId())
                .stream()
                .map(SubscriptionFrequency::getDayName)
                .toList();
        List<SubscriptionVisit> visits = subscriptionVisitRepository.findBySubscriptionIdOrderByVisitDateAscIdAsc(subscription.getId());
        long completed = countVisits(visits, VisitStatus.COMPLETED);
        LocalDate nextVisitDate = visits.stream()
                .filter(visit -> !visit.getVisitDate().isBefore(LocalDate.now()))
                .map(SubscriptionVisit::getVisitDate)
                .findFirst()
                .orElse(null);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("subscriptionId", subscription.getId());
        payload.put("serviceType", subscription.getServiceType());
        payload.put("serviceName", serviceName(service));
        payload.put("visits", subscription.getVisits());
        addressRepository.findById(subscription.getAddressId()).ifPresent(address -> payload.put("fullAddress", formatAddress(address)));
        payload.put("planName", planName(subscription, service));
        payload.put("frequency", String.join(",", frequencyDays).toUpperCase(Locale.ROOT));
        payload.put("frequencyDays", frequencyDays);
        payload.put("nextVisitDate", nextVisitDate);
        payload.put("remainingVisits", Math.max(subscription.getVisits() - completed, 0));
        payload.put("status", subscription.getStatus().name());
        payload.put("bookingId", subscription.getBookingId());
        payload.put("startDate", subscription.getStartDate());
        payload.put("endDate", subscription.getEndDate());
        payload.put("timeSlot", subscription.getTimeSlot());
        payload.put("totalAmount", subscription.getPrice());
        payload.putAll(subscriptionActionPermissions(subscription));
        return payload;
    }

    private Map<String, Object> subscriptionActionPermissions(SubscriptionMaster subscription) {
        Map<String, Object> payload = new LinkedHashMap<>();
        SubscriptionStatus status = subscription == null ? null : subscription.getStatus();
        payload.put("canPause", status == SubscriptionStatus.ACTIVE);
        payload.put("canResume", status == SubscriptionStatus.PAUSED);
        payload.put("canCancel", status == SubscriptionStatus.ACTIVE || status == SubscriptionStatus.PAUSED || status == SubscriptionStatus.PENDING);
        return payload;
    }

    private String planName(SubscriptionMaster subscription, AdminServiceEntity service) {
        String serviceType = subscription.getServiceType() == null ? "Subscription" : subscription.getServiceType();
        String serviceName = serviceName(service);
        if (!serviceName.isBlank()) {
            return serviceName;
        }
        return switch (serviceType.toUpperCase(Locale.ROOT)) {
            case "CLEANING" -> "Daily Cleaning";
            case "COOK" -> "Daily Cook";
            default -> serviceType + " Plan";
        };
    }

    private SubscriptionCalendarItemResponse toCalendarItem(SubscriptionVisit visit) {
        return new SubscriptionCalendarItemResponse(
                visit.getId(),
                visit.getVisitDate(),
                visit.getTimeSlot(),
                visit.getStatus().name()
        );
    }

    private Map<String, Object> visitPayload(SubscriptionVisit visit) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("visitId", visit.getId());
        payload.put("bookingId", visit.getBookingId());
        payload.put("date", visit.getVisitDate());
        payload.put("timeSlot", visit.getTimeSlot());
        payload.put("status", visit.getStatus().name());
        payload.put("rescheduledDate", visit.getRescheduledDate());
        return payload;
    }

    private void audit(Long customerId, Long bookingId, String action, String oldValue, String newValue) {
        if (auditLogService == null) {
            return;
        }
        try {
            auditLogService.log(customerId, null, bookingId, action, oldValue, newValue);
        } catch (RuntimeException ex) {
            LOG.warn("Subscription lifecycle audit failed bookingId={} action={}", bookingId, action, ex);
        }
    }

    private record PriceSelection(java.math.BigDecimal durationHours,
                                  FamilySize familySize,
                                  java.math.BigDecimal perVisitPrice,
                                  java.math.BigDecimal totalPrice) {
    }
}

package com.lexho.user_service.subscription.service;

import com.lexho.booking.entity.Order;
import com.lexho.booking.entity.OrderItem;
import com.lexho.booking.repository.OrderRepository;
import com.lexho.common.enums.OrderStatus;
import com.lexho.common.enums.PaymentStatus;
import com.lexho.user_service.admin.entity.AdminServiceEntity;
import com.lexho.user_service.admin.repository.AdminServiceEntityRepository;
import com.lexho.user_service.audit.service.AuditLogService;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.lexho.repository.LexhoAddressRepository;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SubscriptionBookingService {

    private final SubscriptionMasterRepository subscriptionMasterRepository;
    private final SubscriptionFrequencyRepository subscriptionFrequencyRepository;
    private final SubscriptionVisitRepository subscriptionVisitRepository;
    private final AdminServiceEntityRepository serviceRepository;
    private final LexhoAddressRepository addressRepository;
    private final SubscriptionPricingService subscriptionPricingService;
    private final OrderRepository orderRepository;

    @Autowired(required = false)
    private AuditLogService auditLogService;

    public SubscriptionBookingService(SubscriptionMasterRepository subscriptionMasterRepository,
                                      SubscriptionFrequencyRepository subscriptionFrequencyRepository,
                                      SubscriptionVisitRepository subscriptionVisitRepository,
                                      AdminServiceEntityRepository serviceRepository,
                                      LexhoAddressRepository addressRepository,
                                      SubscriptionPricingService subscriptionPricingService,
                                      OrderRepository orderRepository) {
        this.subscriptionMasterRepository = subscriptionMasterRepository;
        this.subscriptionFrequencyRepository = subscriptionFrequencyRepository;
        this.subscriptionVisitRepository = subscriptionVisitRepository;
        this.serviceRepository = serviceRepository;
        this.addressRepository = addressRepository;
        this.subscriptionPricingService = subscriptionPricingService;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public SubscriptionCreateResponse create(Long customerId, CreateSubscriptionRequest request) {
        List<String> frequencyDays = normalizeDays(request.frequencyDays());
        validateBasicRequest(request, frequencyDays);

        AdminServiceEntity service = serviceRepository.findByIdAndDeletedFalse(request.serviceId())
                .orElseThrow(() -> new SubscriptionValidationException("Service not found"));
        if (!service.isActive() || !supportsSubscription(service)) {
            throw new SubscriptionValidationException("Service does not support SUBSCRIPTION booking mode");
        }

        com.lexho.user_service.lexho.entity.LexhoAddress address = addressRepository
                .findByIdAndUserIdAndIsDeletedFalse(request.addressId(), customerId)
                .orElse(null);
        if (address == null) {
            throw new SubscriptionValidationException("Address does not belong to logged-in customer");
        }

        String serviceType = resolveServiceType(request, service);
        List<LocalDate> generatedDates = generateVisitDates(request.startDate(), request.endDate(), frequencyDays);
        int visitCount = generatedDates.size();
        PriceSelection priceSelection = resolvePrice(request, serviceType, visitCount);

        Order paymentBooking = new Order();
        paymentBooking.setUserId(customerId);
        paymentBooking.setStatus(OrderStatus.CONFIRMED);
        paymentBooking.setPaymentStatus(PaymentStatus.PENDING);
        paymentBooking.setBookingType("SUBSCRIPTION");
        paymentBooking.setScheduledAt(request.startDate().atStartOfDay());
        paymentBooking.setAddress(formatAddress(address));
        paymentBooking.setTotalAmount(priceSelection.totalPrice().doubleValue());
        OrderItem item = new OrderItem();
        item.setPartnerId(service.getId());
        item.setPartnerName(service.getName());
        item.setPrice(priceSelection.totalPrice().doubleValue());
        item.setQuantity(1);
        item.setOrder(paymentBooking);
        paymentBooking.setItems(new ArrayList<>(List.of(item)));
        Order savedPaymentBooking = orderRepository.save(paymentBooking);

        SubscriptionMaster master = new SubscriptionMaster();
        master.setCustomerId(customerId);
        master.setServiceId(request.serviceId());
        master.setAddressId(request.addressId());
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

        return new SubscriptionCreateResponse(
                savedMaster.getId(),
                savedMaster.getId(),
                savedMaster.getBookingId(),
                savedMaster.getServiceId(),
                savedMaster.getAddressId(),
                savedMaster.getBookingMode(),
                savedMaster.getServiceType(),
                frequencyDays,
                savedMaster.getStartDate(),
                savedMaster.getEndDate(),
                savedMaster.getVisits(),
                savedMaster.getDurationHours(),
                savedMaster.getFamilySize() == null ? null : savedMaster.getFamilySize().name(),
                subscriptionPricingService.familySizeLabel(savedMaster.getFamilySize()),
                savedMaster.getPerVisitPrice(),
                savedMaster.getTimeSlot(),
                savedMaster.getPrice(),
                savedMaster.getStatus().name(),
                savedVisits.stream().map(this::toCalendarItem).toList()
        );
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
                .filter(visit -> visit.getStatus() == VisitStatus.SCHEDULED || visit.getStatus() == VisitStatus.RESCHEDULED)
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
        SubscriptionMaster subscription = getOwnedSubscription(customerId, subscriptionId);
        if (subscription.getStatus() == SubscriptionStatus.CANCELLED || subscription.getStatus() == SubscriptionStatus.COMPLETED) {
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
        audit(customerId, subscription.getBookingId(), "SUBSCRIPTION_CANCELLED", oldStatus, "CANCELLED");
    }

    @Transactional
    public Map<String, Object> pause(Long customerId, Long subscriptionId) {
        SubscriptionMaster subscription = getOwnedSubscription(customerId, subscriptionId);
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
        SubscriptionMaster subscription = getOwnedSubscription(customerId, subscriptionId);
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
        getOwnedSubscription(customerId, subscriptionId);
        SubscriptionVisit visit = getVisit(subscriptionId, visitId);
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
        getOwnedSubscription(customerId, subscriptionId);
        SubscriptionVisit visit = getVisit(subscriptionId, visitId);
        if (!request.newDate().isAfter(LocalDate.now())) {
            throw new SubscriptionValidationException("New date must be future date");
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

    private List<String> normalizeDays(List<String> days) {
        if (days == null) {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String rawDay : days) {
            if (rawDay == null || rawDay.isBlank()) {
                continue;
            }
            DayOfWeek dayOfWeek = parseDay(rawDay);
            normalized.add(dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH));
        }
        return List.copyOf(normalized);
    }

    private DayOfWeek parseDay(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (DayOfWeek day : DayOfWeek.values()) {
            if (day.name().equals(normalized)
                    || day.getDisplayName(TextStyle.FULL, Locale.ENGLISH).toUpperCase(Locale.ROOT).equals(normalized)) {
                return day;
            }
        }
        throw new SubscriptionValidationException("Invalid frequency day: " + value);
    }

    private List<LocalDate> generateVisitDates(LocalDate startDate, LocalDate endDate, List<String> frequencyDays) {
        Set<DayOfWeek> weekdays = frequencyDays.stream()
                .map(this::parseDay)
                .collect(Collectors.toSet());
        List<LocalDate> dates = new ArrayList<>();
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            if (weekdays.contains(cursor.getDayOfWeek())) {
                dates.add(cursor);
            }
            cursor = cursor.plusDays(1);
        }
        dates.sort(Comparator.naturalOrder());
        return dates;
    }

    private boolean supportsSubscription(AdminServiceEntity service) {
        String serviceType = service.getServiceType();
        return serviceType != null && serviceType.toUpperCase(Locale.ROOT).contains("SUBSCRIPTION");
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
        return bookingMode == null || bookingMode.isBlank() ? "SUBSCRIPTION" : bookingMode.trim().toUpperCase(Locale.ROOT);
    }

    private String formatAddress(com.lexho.user_service.lexho.entity.LexhoAddress address) {
        return List.of(address.getHouseNo(), address.getArea(), address.getCity(), address.getState(), address.getPincode())
                .stream()
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(", "));
    }

    private SubscriptionMaster getOwnedSubscription(Long customerId, Long subscriptionId) {
        return subscriptionMasterRepository.findByIdAndCustomerId(subscriptionId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
    }

    private SubscriptionVisit getVisit(Long subscriptionId, Long visitId) {
        return subscriptionVisitRepository.findByIdAndSubscriptionId(visitId, subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found"));
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
        } catch (RuntimeException ignored) {
            // Audit failures must not break customer-facing subscription actions.
        }
    }

    private record PriceSelection(java.math.BigDecimal durationHours,
                                  FamilySize familySize,
                                  java.math.BigDecimal perVisitPrice,
                                  java.math.BigDecimal totalPrice) {
    }
}

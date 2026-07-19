package com.lexho.user_service.booking.service;

import com.lexho.booking.entity.Order;
import com.lexho.booking.entity.OrderItem;
import com.lexho.booking.repository.OrderRepository;
import com.lexho.common.enums.OrderStatus;
import com.lexho.common.enums.PaymentStatus;
import com.lexho.user.repository.UserRepository;
import com.lexho.user_service.admin.entity.AdminServiceEntity;
import com.lexho.user_service.booking.dto.BookingRescheduleRequest;
import com.lexho.user_service.booking.dto.BookingStatusResponse;
import com.lexho.user_service.booking.dto.InstantBookingCreateRequest;
import com.lexho.user_service.booking.exception.BookingValidationException;
import com.lexho.user_service.customer.location.dto.ReverseGeocodeResponse;
import com.lexho.user_service.customer.location.entity.CustomerAddress;
import com.lexho.user_service.customer.location.repository.CustomerAddressRepository;
import com.lexho.user_service.customer.location.service.ReverseGeocodeService;
import com.lexho.user_service.customer.repository.CustomerCatalogRepository;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.exception.UnauthorizedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class InstantBookingService {
    private static final int INSTANT_SLA_MINUTES = 30;
    private static final ZoneId INDIA_ZONE = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter SLOT_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final OrderRepository orderRepository;
    private final CustomerCatalogRepository catalogRepository;
    private final CustomerAddressRepository addressRepository;
    private final ReverseGeocodeService reverseGeocodeService;
    private final UserRepository userRepository;

    public InstantBookingService(OrderRepository orderRepository,
                                 CustomerCatalogRepository catalogRepository,
                                 CustomerAddressRepository addressRepository,
                                 ReverseGeocodeService reverseGeocodeService,
                                 UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.catalogRepository = catalogRepository;
        this.addressRepository = addressRepository;
        this.reverseGeocodeService = reverseGeocodeService;
        this.userRepository = userRepository;
    }

    @Transactional
    public BookingStatusResponse create(Long customerId, InstantBookingCreateRequest request) {
        double latitude = request.coordinates != null && request.coordinates.lat != null ? request.coordinates.lat : request.latitude;
        double longitude = request.coordinates != null && request.coordinates.lng != null ? request.coordinates.lng : request.longitude;
        AdminServiceEntity service = catalogRepository.findById(request.serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found: " + request.serviceId));
        if (!service.isActive() || service.isDeleted()) {
            throw new BookingValidationException("SERVICE_UNAVAILABLE", "Service is unavailable");
        }

        Order order = new Order();
        order.setUserId(customerId);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setPaymentStatus(PaymentStatus.PENDING);
        String bookingType = resolveBookingType(request.bookingType);
        order.setBookingType(bookingType);
        applySchedule(order, request, bookingType);
        order.setAddress(resolveAddress(customerId, request.addressId, latitude, longitude));
        order.setTotalAmount(resolvePrice(service));

        OrderItem item = new OrderItem();
        item.setPartnerId(service.getId());
        item.setPartnerName(service.getName());
        item.setPrice(order.getTotalAmount());
        item.setQuantity(1);
        item.setOrder(order);
        order.setItems(new ArrayList<>(List.of(item)));

        return toResponse(orderRepository.save(order), service);
    }

    @Transactional(readOnly = true)
    public BookingStatusResponse details(Long customerId, Long bookingId) {
        Order order = orderRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        if (!order.getUserId().equals(customerId)) {
            throw new UnauthorizedException("You are not allowed to access this booking");
        }
        AdminServiceEntity service = firstService(order).orElse(null);
        return toResponse(order, service);
    }

    @Transactional
    public BookingStatusResponse reschedule(Long customerId, Long bookingId, BookingRescheduleRequest request) {
        Order order = findOwnedBooking(customerId, bookingId);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BookingValidationException("BOOKING_CANCELLED", "Cancelled booking cannot be rescheduled");
        }
        order.setBookingType("SCHEDULED");
        order.setScheduledAt(request.date.atZoneSameInstant(INDIA_ZONE).toLocalDateTime());
        order.setSlaMinutes(null);
        order.setPromisedStartTime(null);
        BookingStatusResponse response = toResponse(orderRepository.save(order), firstService(order).orElse(null));
        response.message = "Booking rescheduled successfully.";
        return response;
    }

    @Transactional
    public BookingStatusResponse cancel(Long customerId, Long bookingId) {
        Order order = findOwnedBooking(customerId, bookingId);
        if (!order.getStatus().canCancel()) {
            throw new BookingValidationException("CANNOT_CANCEL", "Booking cannot be cancelled at this stage");
        }
        order.setStatus(OrderStatus.CANCELLED);
        return toResponse(orderRepository.save(order), firstService(order).orElse(null));
    }

    private Order findOwnedBooking(Long customerId, Long bookingId) {
        Order order = orderRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        if (!order.getUserId().equals(customerId)) {
            throw new UnauthorizedException("You are not allowed to access this booking");
        }
        return order;
    }

    private String resolveBookingType(String bookingType) {
        if (bookingType == null || bookingType.isBlank()) {
            return "INSTANT";
        }
        return bookingType.trim().toUpperCase(Locale.ROOT);
    }

    private void applySchedule(Order order, InstantBookingCreateRequest request, String bookingType) {
        if ("SCHEDULED".equals(bookingType)) {
            LocalDateTime scheduledAt = parseScheduledAt(request.date, request.slot);
            order.setScheduledAt(scheduledAt);
            order.setSlaMinutes(null);
            order.setPromisedStartTime(null);
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        order.setScheduledAt(now);
        order.setSlaMinutes(INSTANT_SLA_MINUTES);
        order.setPromisedStartTime(now.plusMinutes(INSTANT_SLA_MINUTES));
    }

    private LocalDateTime parseScheduledAt(LocalDate date, String slot) {
        if (date == null) {
            throw new BookingValidationException("DATE_REQUIRED", "date is required for scheduled booking");
        }
        String normalizedSlot = slot == null ? "" : slot.trim();
        if (normalizedSlot.isBlank()) {
            throw new BookingValidationException("SLOT_REQUIRED", "slot is required for scheduled booking");
        }
        String start = normalizedSlot.split("-", 2)[0].trim();
        try {
            return LocalDateTime.of(date, LocalTime.parse(start, SLOT_TIME_FORMAT));
        } catch (DateTimeParseException exception) {
            throw new BookingValidationException("INVALID_SLOT", "slot must use HH:mm-HH:mm format");
        }
    }

    private String resolveAddress(Long customerId, String addressId, double latitude, double longitude) {
        Optional<CustomerAddress> savedAddress = parseUuid(addressId)
                .flatMap(id -> addressRepository.findByIdAndCustomerIdAndIsDeletedFalse(id, customerId));
        if (savedAddress.isPresent()) {
            return joinAddress(savedAddress.get());
        }
        ReverseGeocodeResponse reverse = reverseGeocodeService.reverse(latitude, longitude);
        String resolved = firstNonBlank(reverse.formattedAddress, reverse.area, reverse.city, reverse.pincode);
        return resolved == null ? latitude + ", " + longitude : resolved;
    }

    private Optional<UUID> parseUuid(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(raw.trim()));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private String joinAddress(CustomerAddress address) {
        List<String> parts = new ArrayList<>();
        add(parts, address.getFlatNo());
        add(parts, address.getBuildingName());
        add(parts, address.getAddressLine());
        add(parts, address.getSector());
        add(parts, address.getArea());
        add(parts, address.getCity());
        return String.join(", ", parts);
    }

    private void add(List<String> parts, String value) {
        if (value != null && !value.isBlank()) {
            parts.add(value.trim());
        }
    }

    private Double resolvePrice(AdminServiceEntity service) {
        BigDecimal price = service.getDiscountedPrice() != null && service.getDiscountedPrice().doubleValue() > 0
                ? service.getDiscountedPrice()
                : service.getBasePrice();
        if (price == null || price.doubleValue() <= 0) {
            throw new BookingValidationException("INVALID_PRICE", "Service price is missing");
        }
        return price.doubleValue();
    }

    private Optional<AdminServiceEntity> firstService(Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            return Optional.empty();
        }
        Long serviceId = order.getItems().get(0).getPartnerId();
        return serviceId == null ? Optional.empty() : catalogRepository.findById(serviceId);
    }

    private BookingStatusResponse toResponse(Order order, AdminServiceEntity service) {
        BookingStatusResponse response = new BookingStatusResponse();
        response.id = order.getId();
        response.bookingId = order.getId();
        response.bookingType = order.getBookingType() == null ? "INSTANT" : order.getBookingType();
        response.status = customerBookingStatus(order);
        response.assignmentStatus = assignmentStatus(order);
        response.serviceId = service == null ? null : service.getId();
        response.serviceName = service == null ? serviceNameFromItem(order) : service.getName();
        response.createdAt = toOffset(order.getCreatedAt());
        response.promisedStartTime = toOffset(order.getPromisedStartTime());
        response.scheduledAt = toOffset(order.getScheduledAt());
        response.date = order.getScheduledAt() == null ? null : order.getScheduledAt().toLocalDate();
        response.slot = slotFromScheduledAt(order);
        response.slaMinutes = order.getSlaMinutes();
        response.totalAmount = order.getTotalAmount();
        response.fullAddress = order.getAddress();
        response.etaMinutes = etaMinutes(order);
        response.message = message(response.assignmentStatus);
        attachPartner(response, order);
        return response;
    }

    private void attachPartner(BookingStatusResponse response, Order order) {
        if (order.getAssignedPartnerId() == null) {
            return;
        }
        userRepository.findByIdAndDeletedFalse(order.getAssignedPartnerId()).ifPresent(partner -> {
            response.partnerId = partner.getId();
            response.partnerName = partner.getFullName();
            response.partnerPhone = partner.getPhoneNumber();
            response.partnerProfileImage = partner.getProfileImage();
            response.partnerRating = 4.8;
        });
    }

    private String customerBookingStatus(Order order) {
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return "CANCELLED";
        }
        if (order.getStatus() == OrderStatus.INSTANT_FAILED) {
            return "INSTANT_FAILED";
        }
        return "CONFIRMED";
    }

    private String assignmentStatus(Order order) {
        if ("SCHEDULED".equalsIgnoreCase(order.getBookingType())
                && order.getStatus() == OrderStatus.CONFIRMED
                && order.getAssignedPartnerId() == null) {
            return "SCHEDULED";
        }
        return switch (order.getStatus()) {
            case PARTNER_ASSIGNED, PARTNER_ACCEPTED -> "PARTNER_ASSIGNED";
            case PARTNER_REACHED -> "PARTNER_ARRIVED";
            case SERVICE_STARTED -> "IN_PROGRESS";
            case SERVICE_COMPLETED, PAYMENT_COMPLETED, DELIVERED -> "COMPLETED";
            case INSTANT_FAILED -> "INSTANT_FAILED";
            case CANCELLED -> "CANCELLED";
            default -> order.getAssignedPartnerId() == null ? "SEARCHING_PARTNER" : "PARTNER_ASSIGNED";
        };
    }

    private Integer etaMinutes(Order order) {
        if (order.getAssignedPartnerId() != null) {
            return 12;
        }
        return order.getSlaMinutes() == null ? INSTANT_SLA_MINUTES : order.getSlaMinutes();
    }

    private String message(String assignmentStatus) {
        return switch (assignmentStatus) {
            case "PARTNER_ASSIGNED" -> "Partner assigned and on the way.";
            case "PARTNER_ARRIVED" -> "Partner has arrived.";
            case "IN_PROGRESS" -> "Service has started.";
            case "COMPLETED" -> "Service completed.";
            case "INSTANT_FAILED" -> "No partner is available right now.";
            case "CANCELLED" -> "Booking cancelled.";
            case "SCHEDULED" -> "Partner will be assigned before service.";
            default -> "We are matching your request with nearby partners.";
        };
    }

    private String slotFromScheduledAt(Order order) {
        if (!"SCHEDULED".equalsIgnoreCase(order.getBookingType()) || order.getScheduledAt() == null) {
            return null;
        }
        LocalTime start = order.getScheduledAt().toLocalTime();
        return start.format(SLOT_TIME_FORMAT) + "-" + start.plusHours(1).format(SLOT_TIME_FORMAT);
    }

    private String serviceNameFromItem(Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            return null;
        }
        return order.getItems().get(0).getPartnerName();
    }

    private OffsetDateTime toOffset(LocalDateTime value) {
        return value == null ? null : value.atZone(INDIA_ZONE).toOffsetDateTime();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}

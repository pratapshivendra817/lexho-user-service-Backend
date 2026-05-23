package com.lexho.user_service.customer.service;

import com.lexho.user_service.admin.entity.AdminServiceEntity;
import com.lexho.user_service.customer.dto.CustomerAddToCartRequest;
import com.lexho.user_service.customer.dto.CustomerCartResponse;
import com.lexho.user_service.customer.dto.CustomerCartItemResponse;
import com.lexho.user_service.customer.dto.CustomerUpdateCartItemRequest;
import com.lexho.user_service.customer.entity.CustomerCartItemEntity;
import com.lexho.user_service.customer.repository.CustomerCartItemRepository;
import com.lexho.user_service.customer.repository.CustomerCatalogRepository;
import com.lexho.booking.entity.Order;
import com.lexho.booking.repository.OrderRepository;
import com.lexho.common.enums.OrderStatus;
import com.lexho.partner.repository.PartnerRepository;
import com.lexho.user_service.lexho.entity.LexhoAddress;
import com.lexho.user_service.lexho.entity.LexhoUserProfile;
import com.lexho.user_service.lexho.repository.LexhoAddressRepository;
import com.lexho.user_service.lexho.repository.LexhoUserProfileRepository;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CustomerCartService {

    private final CustomerCartItemRepository customerCartItemRepository;
    private final CustomerCatalogRepository customerCatalogRepository;
    private final LexhoAddressRepository lexhoAddressRepository;
    private final LexhoUserProfileRepository lexhoUserProfileRepository;
    private final PartnerRepository partnerRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public CustomerCartItemResponse addToCart(Long userId, CustomerAddToCartRequest request) {
        if (userId == null) {
            throw new BadRequestException("Invalid user context");
        }
        if (request.getQuantity() == null || request.getQuantity() < 1) {
            throw new BadRequestException("quantity must be at least 1");
        }

        AdminServiceEntity service = customerCatalogRepository
                .findById(request.getServiceId())
                .filter(AdminServiceEntity::isActive)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));

        CustomerCartItemEntity item = customerCartItemRepository
                .findByUserIdAndServiceId(userId, service.getId())
                .orElseGet(CustomerCartItemEntity::new);

        if (item.getId() == null) {
            item.setUserId(userId);
            item.setServiceId(service.getId());
            item.setQuantity(request.getQuantity());
        } else {
            int current = item.getQuantity() == null ? 0 : item.getQuantity();
            item.setQuantity(current + request.getQuantity());
        }

        item.setUserId(userId);
        item.setServiceId(service.getId());

        CustomerCartItemEntity saved = customerCartItemRepository.save(item);
        return CustomerCartItemResponse.builder()
                .itemId(saved.getId())
                .serviceId(saved.getServiceId())
                .name(service.getName())
                .imageUrl(toAbsoluteImageUrl(service.getImageUrl()))
                .serviceType(toServiceType(service.getCategoryName()))
                .price(resolveUnitPrice(service))
                .quantity(saved.getQuantity())
                .totalPrice(resolveUnitPrice(service) * saved.getQuantity())
                .build();
    }

    @Transactional(readOnly = true)
    public CustomerCartResponse getCart(Long userId) {
        List<CustomerCartItemEntity> cartItems = customerCartItemRepository.findByUserId(userId);
        List<CustomerCartItemResponse> items = cartItems.stream().map(item -> {
            AdminServiceEntity service = customerCatalogRepository.findById(item.getServiceId()).orElse(null);
            double price = resolveUnitPrice(service);
            int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
            return CustomerCartItemResponse.builder()
                    .itemId(item.getId())
                    .serviceId(item.getServiceId())
                    .name(service == null || service.getName() == null ? "" : service.getName())
                    .imageUrl(toAbsoluteImageUrl(service == null ? null : service.getImageUrl()))
                    .serviceType(toServiceType(service == null ? null : service.getCategoryName()))
                    .price(price)
                    .quantity(quantity)
                    .totalPrice(round2(price * quantity))
                    .build();
        }).toList();

        double itemTotal = round2(items.stream().mapToDouble(CustomerCartItemResponse::getTotalPrice).sum());
        double gstAmount = round2(itemTotal * 0.03d);
        double serviceFee = round2(itemTotal * 0.02d);
        double taxAmount = 0d;
        double grandTotal = round2(itemTotal + gstAmount + serviceFee + taxAmount);
        double totalSaved = round2(calculateTotalSaved(cartItems));

        LexhoUserProfile profile = lexhoUserProfileRepository.findById(userId).orElse(null);
        LexhoAddress addressEntity = lexhoAddressRepository.findByUserId(userId).stream()
                .filter(LexhoAddress::isDefault)
                .findFirst()
                .orElseGet(() -> lexhoAddressRepository.findByUserId(userId).stream().findFirst().orElse(null));

        CustomerCartResponse.Address address = CustomerCartResponse.Address.builder()
                .id(addressEntity == null ? 0L : addressEntity.getId())
                .fullAddress(addressEntity == null ? "" : buildFullAddress(addressEntity))
                .city(addressEntity == null || addressEntity.getCity() == null ? "" : addressEntity.getCity())
                .pincode(addressEntity == null || addressEntity.getPincode() == null ? "" : addressEntity.getPincode())
                .build();

        long cartId = cartItems.stream().map(CustomerCartItemEntity::getId).findFirst().orElse(0L);
        Order latestOrder = orderRepository.findTopByUserIdOrderByCreatedAtDesc(userId).orElse(null);
        String scheduledTime = latestOrder == null || latestOrder.getScheduledAt() == null
                ? null
                : latestOrder.getScheduledAt().atOffset(ZoneOffset.ofHoursMinutes(5, 30)).toInstant().toString();
        String requestStatus = mapRequestStatus(latestOrder == null ? null : latestOrder.getStatus());
        int matchedPartners = estimateMatchedPartners(items);
        String assignmentMode = matchedPartners > 0 ? "AUTO" : "MANUAL";

        return CustomerCartResponse.builder()
                .cartId(cartId)
                .grandTotal(grandTotal)
                .itemTotal(itemTotal)
                .gstAmount(gstAmount)
                .serviceFee(serviceFee)
                .taxAmount(taxAmount)
                .toPay(grandTotal)
                .totalSaved(totalSaved)
                .currency("INR")
                .scheduledTime(scheduledTime)
                .requestStatus(requestStatus)
                .matchedPartners(matchedPartners)
                .assignmentMode(assignmentMode)
                .contactName(profile == null || profile.getName() == null ? "" : profile.getName())
                .contactPhone(profile == null || profile.getMobile() == null ? "" : profile.getMobile())
                .address(address)
                .items(items)
                .build();
    }

    @Transactional
    public CustomerCartResponse updateQuantity(Long userId, CustomerUpdateCartItemRequest request) {
        CustomerCartItemEntity item = customerCartItemRepository.findByIdAndUserId(request.getItemId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        item.setQuantity(request.getQuantity());
        customerCartItemRepository.save(item);
        return getCart(userId);
    }

    @Transactional
    public void removeItem(Long userId, Long itemId) {
        CustomerCartItemEntity item = customerCartItemRepository.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        customerCartItemRepository.delete(item);
    }

    @Transactional
    public void clearCart(Long userId) {
        customerCartItemRepository.deleteByUserId(userId);
    }

    private double resolveUnitPrice(AdminServiceEntity service) {
        if (service == null) {
            return 0d;
        }
        BigDecimal discounted = service.getDiscountedPrice();
        if (discounted != null && discounted.doubleValue() > 0d) {
            return round2(discounted.doubleValue());
        }
        BigDecimal base = service.getBasePrice();
        return base == null ? 0d : round2(base.doubleValue());
    }

    private double calculateTotalSaved(List<CustomerCartItemEntity> cartItems) {
        double saved = 0d;
        for (CustomerCartItemEntity item : cartItems) {
            AdminServiceEntity service = customerCatalogRepository.findById(item.getServiceId()).orElse(null);
            if (service == null || service.getBasePrice() == null || service.getDiscountedPrice() == null) {
                continue;
            }
            double diff = service.getBasePrice().doubleValue() - service.getDiscountedPrice().doubleValue();
            if (diff > 0) {
                int qty = item.getQuantity() == null ? 0 : item.getQuantity();
                saved += diff * qty;
            }
        }
        return saved;
    }

    private String toAbsoluteImageUrl(String raw) {
        if (raw == null || raw.isBlank()) {
            return "https://cdn.lexho.com/defaults/service.png";
        }
        if (raw.startsWith("http://") || raw.startsWith("https://")) {
            return raw.replace("http://", "https://");
        }
        String normalized = raw.startsWith("/") ? raw.substring(1) : raw;
        return "https://cdn.lexho.com/" + normalized;
    }

    private String toServiceType(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            return "GENERAL";
        }
        return categoryName.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    private String buildFullAddress(LexhoAddress address) {
        return (address.getHouseNo() == null ? "" : address.getHouseNo())
                + ", " + (address.getArea() == null ? "" : address.getArea())
                + ", " + (address.getCity() == null ? "" : address.getCity());
    }

    private double round2(double value) {
        return Math.round(value * 100.0d) / 100.0d;
    }

    private String mapRequestStatus(OrderStatus status) {
        if (status == null) {
            return "IDLE";
        }
        return switch (status) {
            case CANCELLED -> "REJECTED";
            case INSTANT_FAILED -> "REJECTED";
            case DELIVERED, PAYMENT_COMPLETED, SERVICE_COMPLETED -> "ACCEPTED";
            case SEARCHING_PARTNER, SCHEDULED, BOOKING_CREATED, PARTNER_SEARCH, PARTNER_ASSIGNED, PARTNER_ACCEPTED, PARTNER_REACHED, SERVICE_STARTED,
                    CREATED, CONFIRMED, PREPARING, OUT_FOR_DELIVERY -> "PENDING";
        };
    }

    private int estimateMatchedPartners(List<CustomerCartItemResponse> items) {
        Set<Long> categoryIds = items.stream()
                .map(CustomerCartItemResponse::getServiceId)
                .map(id -> customerCatalogRepository.findById(id).orElse(null))
                .filter(service -> service != null && service.getCategory() != null)
                .map(service -> service.getCategory().getId())
                .collect(java.util.stream.Collectors.toSet());
        if (categoryIds.isEmpty()) {
            return 0;
        }
        return (int) partnerRepository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getAvailable()))
                .filter(p -> p.getCategoryId() != null && categoryIds.contains(p.getCategoryId()))
                .count();
    }
}

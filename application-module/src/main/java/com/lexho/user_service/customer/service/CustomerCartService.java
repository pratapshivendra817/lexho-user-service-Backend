package com.lexho.user_service.customer.service;

import com.lexho.user_service.admin.entity.AdminServiceEntity;
import com.lexho.user_service.addon.dto.CartAddonResponse;
import com.lexho.user_service.addon.entity.AddonEntity;
import com.lexho.user_service.addon.entity.CartItemAddonEntity;
import com.lexho.user_service.addon.repository.AddonRepository;
import com.lexho.user_service.addon.repository.CartItemAddonRepository;
import com.lexho.user_service.addon.service.AddonManagementService;
import com.lexho.user_service.cartconfig.dto.CartConfigResponse;
import com.lexho.user_service.cartconfig.entity.CartConfigEntity;
import com.lexho.user_service.cartconfig.service.CartConfigService;
import com.lexho.user_service.cookfamilysize.entity.CookFamilySizeEntity;
import com.lexho.user_service.cookfamilysize.service.CookFamilySizeService;
import com.lexho.user_service.customer.dto.CustomerAddToCartRequest;
import com.lexho.user_service.customer.dto.CustomerCartResponse;
import com.lexho.user_service.customer.dto.CustomerCartItemResponse;
import com.lexho.user_service.customer.dto.CookFamilySizeResponse;
import com.lexho.user_service.customer.dto.CustomerUpdateCartItemRequest;
import com.lexho.user_service.customer.entity.CustomerCartItemEntity;
import com.lexho.user_service.customer.repository.CustomerCartItemRepository;
import com.lexho.user_service.customer.repository.CustomerCatalogRepository;
import com.lexho.booking.entity.Order;
import com.lexho.booking.repository.OrderRepository;
import com.lexho.common.enums.OrderStatus;
import com.lexho.provider.repository.PartnerRepository;
import com.lexho.user_service.lexho.entity.LexhoAddress;
import com.lexho.user_service.lexho.entity.LexhoUserProfile;
import com.lexho.user_service.lexho.repository.LexhoAddressRepository;
import com.lexho.user_service.lexho.repository.LexhoUserProfileRepository;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.CodedValidationException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.subscription.enums.FamilySize;
import com.lexho.user_service.subscription.service.SubscriptionPricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerCartService {

    private final CustomerCartItemRepository customerCartItemRepository;
    private final CustomerCatalogRepository customerCatalogRepository;
    private final LexhoAddressRepository lexhoAddressRepository;
    private final LexhoUserProfileRepository lexhoUserProfileRepository;
    private final PartnerRepository partnerRepository;
    private final OrderRepository orderRepository;
    private final AddonManagementService addonManagementService;
    private final CartItemAddonRepository cartItemAddonRepository;
    private final AddonRepository addonRepository;
    private final CartConfigService cartConfigService;
    private final SubscriptionPricingService subscriptionPricingService;
    private final CookFamilySizeService cookFamilySizeService;
    @Value("${app.media.public-base-url:https://cdn.lexho.com}")
    private String mediaPublicBaseUrl;

    public CartConfigResponse config() {
        return cartConfigService.current();
    }

    public List<CookFamilySizeResponse> cookFamilySizes() {
        return cookFamilySizeService.customerList();
    }

    @Transactional
    public CustomerCartItemResponse addToCart(Long userId, CustomerAddToCartRequest request) {
        if (userId == null) {
            throw new BadRequestException("Invalid user context");
        }
        if (request.getQuantity() == null || request.getQuantity() < 1) {
            throw new CodedValidationException("QUANTITY_EXCEEDED", "quantity must be at least 1");
        }
        CartConfigEntity config = cartConfigService.currentEntity();
        int maxQuantity = config.getMaxServiceQuantity();
        if (request.getQuantity() < config.getMinServiceQuantity()) {
            throw new CodedValidationException("QUANTITY_EXCEEDED",
                    "quantity must be at least " + config.getMinServiceQuantity());
        }
        if (request.getQuantity() > maxQuantity) {
            throw new CodedValidationException("QUANTITY_EXCEEDED",
                    "quantity must not exceed " + maxQuantity);
        }

        AdminServiceEntity service = customerCatalogRepository
                .findById(request.getServiceId())
                .filter(AdminServiceEntity::isActive)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new CodedValidationException("SERVICE_UNAVAILABLE", "Service is unavailable"));

        AddonManagementService.ValidatedAddonSelection selection =
                addonManagementService.validateSelections(service, request.getAddons());
        FamilySelection familySelection = normalizeFamilySelection(request.getFamilySizeId(), request.getFamilySize());
        String addonSignature = cartSignature(selection, familySelection);
        CustomerCartItemEntity item = customerCartItemRepository
                .findByUserIdAndServiceIdAndAddonSignature(userId, service.getId(), addonSignature)
                .orElseGet(CustomerCartItemEntity::new);

        if (item.getId() == null) {
            if (customerCartItemRepository.countByUserId(userId) >= config.getMaxCartItems()) {
                throw new CodedValidationException("CART_LIMIT_EXCEEDED",
                        "Cart can contain at most " + config.getMaxCartItems() + " items");
            }
            item.setUserId(userId);
            item.setServiceId(service.getId());
            item.setAddonSignature(addonSignature);
            item.setQuantity(request.getQuantity());
        } else {
            int current = item.getQuantity() == null ? 0 : item.getQuantity();
            int mergedQuantity = current + request.getQuantity();
            if (mergedQuantity > maxQuantity) {
                throw new CodedValidationException("QUANTITY_EXCEEDED",
                        "quantity must not exceed " + maxQuantity);
            }
            item.setQuantity(mergedQuantity);
        }

        item.setUserId(userId);
        item.setServiceId(service.getId());
        item.setAddonSignature(addonSignature);
        applyFamilySelection(item, familySelection);

        CustomerCartItemEntity saved = customerCartItemRepository.save(item);
        saveCartAddons(userId, service, saved.getId(), selection);
        List<CartAddonResponse> addons = buildCartAddonResponses(saved.getId());
        double unitPrice = resolveUnitPrice(service, saved);
        double addonTotal = addons.stream().mapToDouble(CartAddonResponse::getTotalPrice).sum();
        return CustomerCartItemResponse.builder()
                .itemId(saved.getId())
                .serviceId(saved.getServiceId())
                .name(service.getName())
                .imageUrl(toAbsoluteImageUrl(service.getImageUrl()))
                .serviceType(toServiceType(service.getCategoryName()))
                .familySizeId(saved.getFamilySizeId())
                .familySize(saved.getFamilySize())
                .familySizeLabel(saved.getFamilySizeLabel())
                .price(unitPrice)
                .quantity(saved.getQuantity())
                .addons(addons)
                .totalPrice(round2((unitPrice * saved.getQuantity()) + addonTotal))
                .build();
    }

    @Transactional(readOnly = true)
    public CustomerCartResponse getCart(Long userId) {
        List<CustomerCartItemEntity> cartItems = customerCartItemRepository.findByUserId(userId);
        List<CustomerCartItemResponse> items = cartItems.stream().map(item -> {
            AdminServiceEntity service = customerCatalogRepository.findById(item.getServiceId()).orElse(null);
            double price = resolveUnitPrice(service, item);
            int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
            List<CartAddonResponse> addons = buildCartAddonResponses(item.getId());
            double addonTotal = addons.stream().mapToDouble(CartAddonResponse::getTotalPrice).sum();
            return CustomerCartItemResponse.builder()
                    .itemId(item.getId())
                    .serviceId(item.getServiceId())
                    .name(service == null || service.getName() == null ? "" : service.getName())
                    .imageUrl(toAbsoluteImageUrl(service == null ? null : service.getImageUrl()))
                    .serviceType(toServiceType(service == null ? null : service.getCategoryName()))
                    .familySizeId(item.getFamilySizeId())
                    .familySize(item.getFamilySize())
                    .familySizeLabel(item.getFamilySizeLabel())
                    .price(price)
                    .quantity(quantity)
                    .addons(addons)
                    .totalPrice(round2((price * quantity) + addonTotal))
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
        CartConfigEntity config = cartConfigService.currentEntity();
        Integer requestedQuantity = request.getQuantity();
        if (requestedQuantity != null && requestedQuantity < config.getMinServiceQuantity()) {
            throw new CodedValidationException("QUANTITY_EXCEEDED",
                    "quantity must be at least " + config.getMinServiceQuantity());
        }
        if (requestedQuantity != null && requestedQuantity > config.getMaxServiceQuantity()) {
            throw new CodedValidationException("QUANTITY_EXCEEDED",
                    "quantity must not exceed " + config.getMaxServiceQuantity());
        }
        if (requestedQuantity != null) {
            item.setQuantity(requestedQuantity);
        }
        FamilySelection familySelection = normalizeFamilySelection(request.getFamilySizeId(), request.getFamilySize());
        if (familySelection.hasValue()) {
            String newSignature = cartSignature(item.getId(), familySelection);
            CustomerCartItemEntity duplicate = customerCartItemRepository
                    .findByUserIdAndServiceIdAndAddonSignature(userId, item.getServiceId(), newSignature)
                    .filter(existing -> !existing.getId().equals(item.getId()))
                    .orElse(null);
            if (duplicate != null) {
                int mergedQuantity = safeQuantity(duplicate) + safeQuantity(item);
                if (mergedQuantity > config.getMaxServiceQuantity()) {
                    throw new CodedValidationException("QUANTITY_EXCEEDED",
                            "quantity must not exceed " + config.getMaxServiceQuantity());
                }
                duplicate.setQuantity(mergedQuantity);
                cartItemAddonRepository.deleteByCartItemId(item.getId());
                customerCartItemRepository.delete(item);
                customerCartItemRepository.save(duplicate);
                return getCart(userId);
            }
            item.setAddonSignature(newSignature);
            applyFamilySelection(item, familySelection);
        }
        customerCartItemRepository.save(item);
        return getCart(userId);
    }

    @Transactional
    public void removeItem(Long userId, Long itemId) {
        CustomerCartItemEntity item = customerCartItemRepository.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        cartItemAddonRepository.deleteByCartItemId(item.getId());
        customerCartItemRepository.delete(item);
    }

    @Transactional
    public void clearCart(Long userId) {
        cartItemAddonRepository.deleteByUserId(userId);
        customerCartItemRepository.deleteByUserId(userId);
    }

    private void saveCartAddons(Long userId, AdminServiceEntity service, Long cartItemId,
                                AddonManagementService.ValidatedAddonSelection selection) {
        cartItemAddonRepository.deleteByCartItemId(cartItemId);
        List<CartItemAddonEntity> rows = selection.getAddons().stream().map(item -> {
            CartItemAddonEntity row = new CartItemAddonEntity();
            row.setCartItemId(cartItemId);
            row.setUserId(userId);
            row.setServiceId(service.getId());
            row.setAddonId(item.getAddon().getId());
            row.setQuantity(item.getQuantity());
            return row;
        }).toList();
        cartItemAddonRepository.saveAll(rows);
    }

    private String cartSignature(AddonManagementService.ValidatedAddonSelection selection,
                                 FamilySelection familySelection) {
        return familySignature(familySelection) + "::" + addonSignature(selection);
    }

    private String cartSignature(Long cartItemId, FamilySelection familySelection) {
        return familySignature(familySelection) + "::" + addonSignature(cartItemAddonRepository.findByCartItemId(cartItemId));
    }

    private String addonSignature(AddonManagementService.ValidatedAddonSelection selection) {
        if (selection == null || selection.getAddons() == null || selection.getAddons().isEmpty()) {
            return "NONE";
        }
        return selection.getAddons().stream()
                .sorted(Comparator.comparing(item -> item.getAddon().getId()))
                .map(item -> item.getAddon().getId() + ":" + item.getQuantity())
                .collect(Collectors.joining("|"));
    }

    private String addonSignature(List<CartItemAddonEntity> rows) {
        if (rows == null || rows.isEmpty()) {
            return "NONE";
        }
        return rows.stream()
                .sorted(Comparator.comparing(CartItemAddonEntity::getAddonId))
                .map(item -> item.getAddonId() + ":" + item.getQuantity())
                .collect(Collectors.joining("|"));
    }

    private String familySignature(FamilySelection familySelection) {
        if (familySelection == null || !familySelection.hasValue()) {
            return "FAMILY:NONE";
        }
        return "FAMILY:" + familySelection.familySize();
    }

    private FamilySelection normalizeFamilySelection(Long familySizeId, String familySize) {
        if ((familySize == null || familySize.isBlank()) && familySizeId == null) {
            return FamilySelection.empty();
        }
        CookFamilySizeEntity entity = cookFamilySizeService.requireActive(familySizeId, familySize);
        return new FamilySelection(entity.getId(), entity.getFamilySize(), entity.getFamilySizeLabel());
    }

    private void applyFamilySelection(CustomerCartItemEntity item, FamilySelection familySelection) {
        if (familySelection == null || !familySelection.hasValue()) {
            item.setFamilySizeId(null);
            item.setFamilySize(null);
            item.setFamilySizeLabel(null);
            return;
        }
        item.setFamilySizeId(familySelection.familySizeId());
        item.setFamilySize(familySelection.familySize());
        item.setFamilySizeLabel(familySelection.familySizeLabel());
    }

    private FamilySize parseFamilySize(String familySize) {
        try {
            return FamilySize.valueOf(familySize.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new CodedValidationException("SERVICE_UNAVAILABLE", "Invalid family size");
        }
    }

    private List<CartAddonResponse> buildCartAddonResponses(Long cartItemId) {
        List<CartItemAddonEntity> rows = cartItemAddonRepository.findByCartItemId(cartItemId);
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, AddonEntity> addons = addonRepository.findByIdIn(
                rows.stream().map(CartItemAddonEntity::getAddonId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(AddonEntity::getId, Function.identity()));
        return rows.stream().map(row -> {
            AddonEntity addon = addons.get(row.getAddonId());
            CartAddonResponse response = new CartAddonResponse();
            response.setAddonId(row.getAddonId());
            response.setAddonName(addon == null ? "" : addon.getAddonName());
            double price = addon == null || addon.getPrice() == null ? 0d : addon.getPrice().doubleValue();
            int quantity = row.getQuantity() == null ? 1 : row.getQuantity();
            response.setPrice(round2(price));
            response.setQuantity(quantity);
            response.setTotalPrice(round2(price * quantity));
            response.setRequired(addon != null && addon.isRequired());
            response.setQuantityAllowed(addon != null && addon.isQuantityAllowed());
            return response;
        }).toList();
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

    private double resolveUnitPrice(AdminServiceEntity service, CustomerCartItemEntity item) {
        if (service != null && item != null && item.getFamilySize() != null && !item.getFamilySize().isBlank()) {
            FamilySize familySize = parseFamilySize(item.getFamilySize());
            BigDecimal price = subscriptionPricingService.resolveCookPricePerVisit(service.getId(), familySize);
            return round2(price.doubleValue());
        }
        return resolveUnitPrice(service);
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
        String base = sanitizeBaseUrl(mediaPublicBaseUrl);
        if (raw == null || raw.isBlank()) {
            return base + "/defaults/service.png";
        }
        String trimmed = raw.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            if (lower.contains("localhost") || lower.contains("127.0.0.1") || lower.contains("0.0.0.0")) {
                String path = extractPath(trimmed);
                return path == null ? base + "/defaults/service.png" : base + path;
            }
            return lower.startsWith("http://") ? "https://" + trimmed.substring(7) : trimmed;
        }
        String normalized = trimmed.startsWith("/") ? trimmed : "/" + trimmed;
        return base + normalized;
    }

    private String sanitizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://cdn.lexho.com";
        }
        String trimmed = baseUrl.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    private String extractPath(String url) {
        try {
            java.net.URI uri = java.net.URI.create(url);
            String path = uri.getPath();
            if (path == null || path.isBlank()) {
                return null;
            }
            return path.startsWith("/") ? path : "/" + path;
        } catch (IllegalArgumentException ex) {
            return null;
        }
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

    private int safeQuantity(CustomerCartItemEntity item) {
        return item == null || item.getQuantity() == null ? 0 : item.getQuantity();
    }

    private String mapRequestStatus(OrderStatus status) {
        if (status == null) {
            return "IDLE";
        }
        return switch (status) {
            case CANCELLED -> "REJECTED";
            case INSTANT_FAILED -> "REJECTED";
            case DELIVERED, PAYMENT_COMPLETED, SERVICE_COMPLETED -> "ACCEPTED";
            case SEARCHING_PARTNER, SCHEDULED, BOOKING_CREATED, PARTNER_SEARCH, PARTNER_ASSIGNED, PARTNER_ACCEPTED,
                    ARRIVED, PARTNER_REACHED, SERVICE_STARTED, SERVICE_IN_PROGRESS,
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

    private record FamilySelection(Long familySizeId, String familySize, String familySizeLabel) {
        static FamilySelection empty() {
            return new FamilySelection(null, null, null);
        }

        boolean hasValue() {
            return familySize != null && !familySize.isBlank();
        }
    }
}

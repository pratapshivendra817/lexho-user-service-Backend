package com.lexho.user_service.lexhopartner.service;

import com.lexho.booking.entity.Order;
import com.lexho.booking.entity.OrderItem;
import com.lexho.user_service.admin.entity.AdminCategoryEntity;
import com.lexho.user_service.admin.entity.AdminServiceEntity;
import com.lexho.user_service.admin.repository.AdminServiceEntityRepository;
import com.lexho.user_service.lexhopartner.config.PartnerSettlementProperties;
import com.lexho.user_service.lexhopartner.entity.PartnerOrder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Service
public class CompletionSettingsService {

    private final AdminServiceEntityRepository serviceRepository;
    private final PartnerSettlementProperties settlementProperties;

    public CompletionSettingsService(AdminServiceEntityRepository serviceRepository,
                                     PartnerSettlementProperties settlementProperties) {
        this.serviceRepository = serviceRepository;
        this.settlementProperties = settlementProperties;
    }

    public BigDecimal commissionPercentFor(PartnerOrder order) {
        return findService(order)
                .map(this::commissionPercent)
                .orElse(settlementProperties.getCommissionPercent())
                .setScale(2, RoundingMode.HALF_UP);
    }

    public boolean reviewEligibleFor(PartnerOrder order) {
        return findService(order).map(this::reviewEnabled).orElse(true);
    }

    public boolean reviewEligibleFor(Order order) {
        return findService(order).map(this::reviewEnabled).orElse(true);
    }

    private Optional<AdminServiceEntity> findService(PartnerOrder order) {
        if (order == null || order.getServiceType() == null || order.getServiceType().isBlank()) {
            return Optional.empty();
        }
        String serviceType = order.getServiceType().trim();
        return serviceRepository.findFirstByDeletedFalseAndActiveTrueAndNameIgnoreCase(serviceType)
                .or(() -> serviceRepository.findFirstByDeletedFalseAndActiveTrueAndServiceTypeIgnoreCase(serviceType));
    }

    private Optional<AdminServiceEntity> findService(Order order) {
        if (order == null || order.getItems() == null || order.getItems().isEmpty()) {
            return Optional.empty();
        }
        Long serviceId = order.getItems().stream()
                .filter(item -> item != null)
                .map(OrderItem::getPartnerId)
                .filter(id -> id != null)
                .findFirst()
                .orElse(null);
        if (serviceId == null) {
            return Optional.empty();
        }
        return serviceRepository.findByIdAndDeletedFalse(serviceId);
    }

    private BigDecimal commissionPercent(AdminServiceEntity service) {
        if (service.getCommissionPercent() != null) {
            return service.getCommissionPercent();
        }
        AdminCategoryEntity category = service.getCategory();
        if (category != null && category.getCommissionPercent() != null) {
            return category.getCommissionPercent();
        }
        return settlementProperties.getCommissionPercent();
    }

    private boolean reviewEnabled(AdminServiceEntity service) {
        AdminCategoryEntity category = service.getCategory();
        return service.isReviewEnabled() && (category == null || category.isReviewEnabled());
    }
}

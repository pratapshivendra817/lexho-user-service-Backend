package com.lexho.user_service.lexhopartner.service;

import com.lexho.booking.entity.Order;
import com.lexho.booking.entity.OrderItem;
import com.lexho.common.enums.PaymentStatus;
import com.lexho.user_service.admin.entity.AdminCategoryEntity;
import com.lexho.user_service.admin.entity.AdminServiceEntity;
import com.lexho.user_service.admin.repository.AdminServiceEntityRepository;
import com.lexho.user_service.lexhopartner.config.PartnerSettlementProperties;
import com.lexho.user_service.lexhopartner.entity.OrderStatus;
import com.lexho.user_service.lexhopartner.entity.PartnerOrder;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CompletionSettingsServiceTest {

    private final AdminServiceEntityRepository serviceRepository = mock(AdminServiceEntityRepository.class);
    private final PartnerSettlementProperties properties = new PartnerSettlementProperties();
    private final CompletionSettingsService service = new CompletionSettingsService(serviceRepository, properties);

    @Test
    void commissionPercentUsesServiceThenCategoryThenConfiguredFallback() {
        properties.setCommissionPercent(new BigDecimal("12.50"));

        AdminCategoryEntity category = new AdminCategoryEntity();
        category.setCommissionPercent(new BigDecimal("8.00"));
        AdminServiceEntity adminService = new AdminServiceEntity();
        adminService.setName("Cleaning");
        adminService.setCategory(category);
        adminService.setCommissionPercent(new BigDecimal("6.25"));

        PartnerOrder order = partnerOrder("Cleaning");
        when(serviceRepository.findFirstByDeletedFalseAndActiveTrueAndNameIgnoreCase("Cleaning"))
                .thenReturn(Optional.of(adminService));

        assertThat(service.commissionPercentFor(order)).isEqualByComparingTo("6.25");

        adminService.setCommissionPercent(null);
        assertThat(service.commissionPercentFor(order)).isEqualByComparingTo("8.00");

        category.setCommissionPercent(null);
        assertThat(service.commissionPercentFor(order)).isEqualByComparingTo("12.50");
    }

    @Test
    void reviewEligibilityRequiresServiceAndCategoryToAllowReviews() {
        AdminCategoryEntity category = new AdminCategoryEntity();
        AdminServiceEntity adminService = new AdminServiceEntity();
        adminService.setName("Cleaning");
        adminService.setCategory(category);
        PartnerOrder order = partnerOrder("Cleaning");
        when(serviceRepository.findFirstByDeletedFalseAndActiveTrueAndNameIgnoreCase("Cleaning"))
                .thenReturn(Optional.of(adminService));

        adminService.setReviewEnabled(true);
        category.setReviewEnabled(true);
        assertThat(service.reviewEligibleFor(order)).isTrue();

        category.setReviewEnabled(false);
        assertThat(service.reviewEligibleFor(order)).isFalse();

        category.setReviewEnabled(true);
        adminService.setReviewEnabled(false);
        assertThat(service.reviewEligibleFor(order)).isFalse();
    }

    @Test
    void orderReviewEligibilitySkipsNullOrderItems() {
        Order order = new Order();
        java.util.List<OrderItem> items = new java.util.ArrayList<>();
        items.add(null);
        OrderItem item = new OrderItem();
        item.setPartnerId(5L);
        items.add(item);
        order.setItems(items);

        AdminServiceEntity adminService = new AdminServiceEntity();
        adminService.setReviewEnabled(true);
        when(serviceRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(adminService));

        assertThat(service.reviewEligibleFor(order)).isTrue();
    }

    private PartnerOrder partnerOrder(String serviceType) {
        PartnerOrder order = new PartnerOrder();
        order.setStatus(OrderStatus.COMPLETED);
        order.setPaymentStatus(PaymentStatus.SUCCESS);
        order.setServiceType(serviceType);
        return order;
    }
}

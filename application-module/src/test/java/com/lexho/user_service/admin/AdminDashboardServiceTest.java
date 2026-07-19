package com.lexho.user_service.admin;

import com.lexho.booking.repository.OrderRepository;
import com.lexho.common.enums.OrderStatus;
import com.lexho.provider.repository.PartnerRepository;
import com.lexho.user.repository.UserRepository;
import com.lexho.user_service.admin.dto.AdminServiceSummaryResponse;
import com.lexho.user_service.admin.dto.DataResponse;
import com.lexho.user_service.admin.repository.AdminServiceEntityRepository;
import com.lexho.user_service.admin.service.AdminCatalogService;
import com.lexho.user_service.admin.service.AdminDashboardService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminDashboardServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PartnerRepository partnerRepository = mock(PartnerRepository.class);
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final AdminServiceEntityRepository adminServiceEntityRepository = mock(AdminServiceEntityRepository.class);
    private final AdminCatalogService adminCatalogService = mock(AdminCatalogService.class);
    private final AdminDashboardService service = new AdminDashboardService(
            userRepository, partnerRepository, orderRepository, adminServiceEntityRepository, adminCatalogService);

    @Test
    void dashboardStatsShouldAggregateRepositoryCounts() {
        when(userRepository.count()).thenReturn(10L);
        when(partnerRepository.count()).thenReturn(3L);
        when(orderRepository.count()).thenReturn(21L);
        when(orderRepository.sumCompletedRevenue()).thenReturn(1500.50);
        when(orderRepository.countByStatus(OrderStatus.DELIVERED)).thenReturn(8L);

        Map<String, Object> stats = service.dashboardStats();

        assertEquals(10L, stats.get("totalUsers"));
        assertEquals(3L, stats.get("totalProviders"));
        assertEquals(21L, stats.get("totalBookings"));
        assertEquals(1500.50, stats.get("totalCompletedRevenue"));
        assertEquals(8L, stats.get("completedBookings"));
    }

    @Test
    void monthlyRevenueAndDistributionShouldMapProjectionRows() {
        List<Object[]> monthlyRevenueRows = List.<Object[]>of(new Object[] {"2026-06", 1200.0});
        List<Object[]> distributionRows = List.<Object[]>of(new Object[] {"Cleaning", 7L});
        when(orderRepository.monthlyRevenue(org.mockito.ArgumentMatchers.any()))
                .thenReturn(monthlyRevenueRows);
        when(adminServiceEntityRepository.distributionByCategory())
                .thenReturn(distributionRows);

        assertEquals("2026-06", service.monthlyRevenue().get(0).get("month"));
        assertEquals(1200.0, service.monthlyRevenue().get(0).get("amount"));
        assertEquals("Cleaning", service.serviceDistribution().get(0).get("name"));
        assertEquals(7L, service.serviceDistribution().get(0).get("value"));
    }

    @Test
    void serviceSummaryShouldDelegateToCatalogService() {
        DataResponse<List<AdminServiceSummaryResponse>> expected = new DataResponse<>(List.of());
        when(adminCatalogService.serviceSummary()).thenReturn(expected);

        assertEquals(expected, service.serviceSummary());
        verify(adminCatalogService).serviceSummary();
    }
}

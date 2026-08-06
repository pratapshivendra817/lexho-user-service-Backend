package com.lexho.user_service.admin.service;

import com.lexho.user_service.admin.dto.AdminServiceRatingAnalyticsResponse;
import com.lexho.user_service.admin.dto.AdminServiceResponse;
import com.lexho.user_service.admin.dto.AdminServiceSummaryResponse;
import com.lexho.user_service.admin.dto.DataResponse;
import com.lexho.booking.repository.OrderRepository;
import com.lexho.common.enums.OrderStatus;
import com.lexho.payment.entity.PaymentTransactionStatus;
import com.lexho.payment.repository.PaymentTransactionRepository;
import com.lexho.provider.repository.PartnerRepository;
import com.lexho.user.repository.UserRepository;
import com.lexho.user_service.admin.repository.AdminServiceEntityRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final PartnerRepository partnerRepository;
    private final OrderRepository orderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final AdminServiceEntityRepository adminServiceEntityRepository;
    private final AdminCatalogService adminCatalogService;

    public AdminDashboardService(UserRepository userRepository,
                                 PartnerRepository partnerRepository,
                                 OrderRepository orderRepository,
                                 PaymentTransactionRepository paymentTransactionRepository,
                                 AdminServiceEntityRepository adminServiceEntityRepository,
                                 AdminCatalogService adminCatalogService) {
        this.userRepository = userRepository;
        this.partnerRepository = partnerRepository;
        this.orderRepository = orderRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.adminServiceEntityRepository = adminServiceEntityRepository;
        this.adminCatalogService = adminCatalogService;
    }

    public Map<String, Object> dashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalProviders", partnerRepository.count());
        stats.put("totalBookings", orderRepository.count());
        stats.put("totalCompletedRevenue", orderRepository.sumCompletedRevenue());
        stats.put("completedBookings", orderRepository.countByStatus(OrderStatus.DELIVERED));
        stats.put("ordersCreated", orderRepository.count());
        stats.put("ordersCompleted", orderRepository.countByStatus(OrderStatus.DELIVERED));
        stats.put("paymentsSuccess", paymentTransactionRepository.countByPaymentStatus(PaymentTransactionStatus.PAID));
        stats.put("paymentsFailed", paymentTransactionRepository.countByPaymentStatus(PaymentTransactionStatus.FAILED));
        stats.put("partnerOnline", partnerRepository.countByAvailableTrue());
        stats.put("totalServices", adminServiceEntityRepository.countByDeletedFalse());
        stats.put("activeServices", adminServiceEntityRepository.countByDeletedFalseAndActiveTrue());
        stats.put("inactiveServices", adminServiceEntityRepository.countByDeletedFalseAndActiveFalse());
        BigDecimal revenue = paymentTransactionRepository.sumPaidAmount();
        stats.put("revenue", revenue == null ? BigDecimal.ZERO : revenue);
        stats.put("totalRevenue", revenue == null ? BigDecimal.ZERO : revenue);
        return stats;
    }

    public List<Map<String, Object>> monthlyRevenue() {
        LocalDateTime from = LocalDateTime.now().minusMonths(12);
        return orderRepository.monthlyRevenue(from)
                .stream()
                .map(row -> {
                    Map<String, Object> point = new HashMap<>();
                    point.put("month", row[0]);
                    point.put("amount", row[1]);
                    return point;
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> serviceDistribution() {
        return adminServiceEntityRepository.distributionByCategory()
                .stream()
                .map(row -> {
                    Map<String, Object> part = new HashMap<>();
                    part.put("name", row[0]);
                    part.put("value", row[1]);
                    return part;
                })
                .collect(Collectors.toList());
    }

    public DataResponse<List<AdminServiceSummaryResponse>> serviceSummary() {
        return adminCatalogService.serviceSummary();
    }

    public DataResponse<List<AdminServiceResponse>> topServices(int limit) {
        return adminCatalogService.topServices(limit);
    }

    public DataResponse<List<AdminServiceRatingAnalyticsResponse>> serviceAnalytics() {
        return adminCatalogService.serviceAnalytics();
    }
}

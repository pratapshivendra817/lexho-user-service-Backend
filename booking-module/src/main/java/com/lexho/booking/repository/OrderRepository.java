package com.lexho.booking.repository;

import com.lexho.booking.entity.Order;
import com.lexho.common.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserId(Long userId);
    //List<Order> findByUserId(Long userId);

    @Query("""
            SELECT o FROM Order o
            WHERE (:status IS NULL OR o.status = :status)
              AND (:search IS NULL OR str(o.id) LIKE CONCAT('%', :search, '%')
                   OR str(o.userId) LIKE CONCAT('%', :search, '%'))
            """)
    Page<Order> searchOrders(@Param("search") String search,
                             @Param("status") OrderStatus status,
                             Pageable pageable);

    long countByStatus(OrderStatus status);

    @Query("""
            SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o
            WHERE o.paymentStatus = com.lexho.common.enums.PaymentStatus.SUCCESS
            """)
    Double sumCompletedRevenue();

    @Query(value = """
            SELECT DATE_FORMAT(created_at, '%Y-%m') AS month, COALESCE(SUM(total_amount), 0) AS amount
            FROM orders
            WHERE payment_status = 'SUCCESS'
              AND created_at >= :fromDate
            GROUP BY DATE_FORMAT(created_at, '%Y-%m')
            ORDER BY month
            """, nativeQuery = true)
    List<Object[]> monthlyRevenue(@Param("fromDate") LocalDateTime fromDate);
}

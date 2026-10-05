package com.lexho.booking.repository;

import com.lexho.booking.entity.Order;
import com.lexho.common.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.Collection;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserId(Long userId);
    long countByUserIdAndStatusIn(Long userId, Set<OrderStatus> statuses);
    long countByAssignedPartnerIdAndStatusIn(Long assignedPartnerId, Set<OrderStatus> statuses);
    boolean existsByAssignedPartnerIdAndScheduledAtGreaterThanEqualAndScheduledAtLessThanAndStatusInAndIdNot(
            Long assignedPartnerId,
            LocalDateTime scheduledAtFrom,
            LocalDateTime scheduledAtTo,
            Set<OrderStatus> statuses,
            Long excludedOrderId
    );
    boolean existsByUserIdAndAssignedPartnerIdAndStatusIn(Long userId, Long assignedPartnerId, Set<OrderStatus> statuses);

    @Query("""
            SELECT COUNT(o) FROM Order o
            WHERE o.userId = :userId
              AND o.paymentStatus = com.lexho.common.enums.PaymentStatus.SUCCESS
            """)
    long countSuccessfulPaidBookingsByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT COUNT(o) FROM Order o
            WHERE o.userId = :userId
              AND o.paymentStatus = com.lexho.common.enums.PaymentStatus.SUCCESS
              AND (:excludedBookingId IS NULL OR o.id <> :excludedBookingId)
            """)
    long countSuccessfulPaidBookingsByUserIdExcludingBooking(@Param("userId") Long userId,
                                                             @Param("excludedBookingId") Long excludedBookingId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    java.util.Optional<Order> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select o from Order o
            where o.bookingType = :bookingType
              and o.status = :status
              and o.assignedPartnerId is null
              and o.createdAt < :createdBefore
            """)
    List<Order> findStuckInstantSearchOrders(@Param("bookingType") String bookingType,
                                             @Param("status") OrderStatus status,
                                             @Param("createdBefore") LocalDateTime createdBefore);

    @Query("""
            select o from Order o
            where o.bookingType = :bookingType
              and o.paymentStatus = com.lexho.common.enums.PaymentStatus.SUCCESS
              and o.status in :statuses
              and o.assignedPartnerId is null
              and o.dispatchStartedAt is null
              and o.unavailableReason is null
              and o.scheduledAt is not null
              and o.scheduledAt <= :scheduledBefore
            order by o.scheduledAt asc, o.id asc
            """)
    List<Order> findPaidScheduledBookingsAwaitingDispatch(@Param("bookingType") String bookingType,
                                                           @Param("statuses") Collection<OrderStatus> statuses,
                                                           @Param("scheduledBefore") LocalDateTime scheduledBefore,
                                                           Pageable pageable);
    java.util.Optional<Order> findTopByUserIdOrderByCreatedAtDesc(Long userId);
    long countByScheduledAtGreaterThanEqualAndScheduledAtLessThanAndStatusIn(
            LocalDateTime from,
            LocalDateTime to,
            Set<OrderStatus> statuses
    );
    //List<Order> findByUserId(Long userId);

    default Page<Order> searchOrders(String search, OrderStatus status, Pageable pageable) {
        return status == null
                ? searchOrdersWithoutStatus(search, pageable)
                : searchOrdersByStatus(search, status, pageable);
    }

    @Query("""
            SELECT o FROM Order o
            WHERE (:search IS NULL OR str(o.id) LIKE CONCAT('%', :search, '%')
                   OR str(o.userId) LIKE CONCAT('%', :search, '%'))
            """)
    Page<Order> searchOrdersWithoutStatus(@Param("search") String search,
                                          Pageable pageable);

    @Query("""
            SELECT o FROM Order o
            WHERE o.status = :status
              AND (:search IS NULL OR str(o.id) LIKE CONCAT('%', :search, '%')
                   OR str(o.userId) LIKE CONCAT('%', :search, '%'))
            """)
    Page<Order> searchOrdersByStatus(@Param("search") String search,
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

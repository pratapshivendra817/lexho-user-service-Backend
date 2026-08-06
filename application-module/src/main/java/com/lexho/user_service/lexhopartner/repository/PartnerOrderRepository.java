package com.lexho.user_service.lexhopartner.repository;

import com.lexho.user_service.lexhopartner.entity.OrderStatus;
import com.lexho.user_service.lexhopartner.entity.PartnerOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;

public interface PartnerOrderRepository extends JpaRepository<PartnerOrder, Long> {
    Page<PartnerOrder> findByStatusInAndAssignedPartnerIdIsNull(Iterable<OrderStatus> statuses, Pageable pageable);
    Page<PartnerOrder> findByAssignedPartnerId(Long partnerId, Pageable pageable);
    long countByAssignedPartnerIdAndStatus(Long partnerId, OrderStatus status);
    long countByAssignedPartnerId(Long partnerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from PartnerOrder o where o.id = :id")
    Optional<PartnerOrder> findByIdForUpdate(@Param("id") Long id);

    @Query("select coalesce(sum(o.amount), 0) from PartnerOrder o where o.assignedPartnerId = :partnerId and o.status = :status "
            + "and o.updatedAt between :start and :end")
    Double sumAmountByPartnerAndStatusAndUpdatedAtBetween(@Param("partnerId") Long partnerId,
                                                          @Param("status") OrderStatus status,
                                                          @Param("start") LocalDateTime start,
                                                          @Param("end") LocalDateTime end);

    @Query("select coalesce(sum(o.amount), 0) from PartnerOrder o where o.assignedPartnerId = :partnerId and o.status = :status")
    Double sumAmountByPartnerAndStatus(@Param("partnerId") Long partnerId,
                                       @Param("status") OrderStatus status);

    @Query("select count(o) from PartnerOrder o where o.assignedPartnerId = :partnerId and o.status = :status and o.updatedAt between :start and :end")
    Long countByPartnerAndStatusAndUpdatedAtBetween(@Param("partnerId") Long partnerId,
                                                    @Param("status") OrderStatus status,
                                                    @Param("start") LocalDateTime start,
                                                    @Param("end") LocalDateTime end);

    @Query("select count(o) from PartnerOrder o where o.assignedPartnerId = :partnerId and o.status = :status")
    Long countByPartnerAndStatus(@Param("partnerId") Long partnerId,
                                 @Param("status") OrderStatus status);

    Page<PartnerOrder> findByAssignedPartnerIdAndStatusAndUpdatedAtBetween(Long partnerId,
                                                                            OrderStatus status,
                                                                            LocalDateTime start,
                                                                            LocalDateTime end,
                                                                            Pageable pageable);
}

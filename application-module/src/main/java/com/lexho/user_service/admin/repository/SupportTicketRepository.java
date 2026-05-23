package com.lexho.user_service.admin.repository;

import com.lexho.user_service.admin.entity.SupportTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    @Query("""
            SELECT t FROM SupportTicket t
            WHERE (:search IS NULL OR LOWER(t.subject) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(t.message) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:status IS NULL OR LOWER(t.status) = LOWER(:status))
              AND (:categoryId IS NULL OR LOWER(t.categoryId) = LOWER(:categoryId))
              AND (:priority IS NULL OR LOWER(t.priority) = LOWER(:priority))
            """)
    Page<SupportTicket> search(@Param("search") String search,
                               @Param("status") String status,
                               @Param("categoryId") String categoryId,
                               @Param("priority") String priority,
                               Pageable pageable);

    List<SupportTicket> findByUserIdOrderByCreatedAtDesc(Long userId);

    Page<SupportTicket> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<SupportTicket> findByUserIdAndStatusIgnoreCaseOrderByCreatedAtDesc(Long userId, String status, Pageable pageable);

    Optional<SupportTicket> findByUserIdAndId(Long userId, Long id);
}

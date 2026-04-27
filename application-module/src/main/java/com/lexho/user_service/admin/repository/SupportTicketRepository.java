package com.lexho.user_service.admin.repository;

import com.lexho.user_service.admin.entity.SupportTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    @Query("""
            SELECT t FROM SupportTicket t
            WHERE (:search IS NULL OR LOWER(t.subject) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(t.message) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:status IS NULL OR LOWER(t.status) = LOWER(:status))
            """)
    Page<SupportTicket> search(@Param("search") String search,
                               @Param("status") String status,
                               Pageable pageable);
}

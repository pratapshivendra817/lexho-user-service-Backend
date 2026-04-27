package com.lexho.user_service.admin.repository;

import com.lexho.user_service.admin.entity.SupportTicketReply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupportTicketReplyRepository extends JpaRepository<SupportTicketReply, Long> {
    List<SupportTicketReply> findByTicket_IdOrderByCreatedAtAsc(Long ticketId);
}

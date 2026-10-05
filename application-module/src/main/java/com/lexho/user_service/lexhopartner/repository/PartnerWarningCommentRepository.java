package com.lexho.user_service.lexhopartner.repository;

import com.lexho.user_service.lexhopartner.entity.PartnerWarningComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PartnerWarningCommentRepository extends JpaRepository<PartnerWarningComment, Long> {
    List<PartnerWarningComment> findByWarning_IdOrderByCreatedAtAsc(Long warningId);
    long countByWarning_Id(Long warningId);
}

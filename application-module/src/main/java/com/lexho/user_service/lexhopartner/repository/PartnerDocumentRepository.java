package com.lexho.user_service.lexhopartner.repository;

import com.lexho.user_service.lexhopartner.entity.PartnerDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PartnerDocumentRepository extends JpaRepository<PartnerDocument, Long> {
    List<PartnerDocument> findByPartnerId(Long partnerId);
}

package com.lexho.partner.repository;

import com.lexho.partner.entity.ProviderDocument;
import com.lexho.partner.enums.ProviderDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProviderDocumentRepository extends JpaRepository<ProviderDocument, Long> {

    List<ProviderDocument> findByProviderIdOrderByUploadedAtDesc(Long providerId);

    Optional<ProviderDocument> findByIdAndProviderId(Long id, Long providerId);

    boolean existsByProviderIdAndDocumentType(Long providerId, ProviderDocumentType documentType);
}

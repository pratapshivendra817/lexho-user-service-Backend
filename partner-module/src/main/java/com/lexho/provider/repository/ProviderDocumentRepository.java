package com.lexho.provider.repository;

import com.lexho.provider.entity.ProviderDocument;
import com.lexho.provider.enums.ProviderDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProviderDocumentRepository extends JpaRepository<ProviderDocument, Long> {

    List<ProviderDocument> findByProviderIdOrderByUploadedAtDesc(Long providerId);

    Optional<ProviderDocument> findByIdAndProviderId(Long id, Long providerId);

    boolean existsByProviderIdAndDocumentType(Long providerId, ProviderDocumentType documentType);
}

package com.lexho.user_service.legal.service;

import com.lexho.user_service.legal.dto.LegalAcceptRequest;
import com.lexho.user_service.legal.dto.LegalAcceptanceResponse;
import com.lexho.user_service.legal.entity.LegalAcceptance;
import com.lexho.user_service.legal.repository.LegalAcceptanceRepository;
import com.lexho.user_service.legalcontent.dto.LegalDocumentResponse;
import com.lexho.user_service.legalcontent.service.LegalContentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class LegalAcceptanceService {
    private final LegalAcceptanceRepository repository;
    private final LegalContentService legalContentService;

    public LegalAcceptanceService(LegalAcceptanceRepository repository, LegalContentService legalContentService) {
        this.repository = repository;
        this.legalContentService = legalContentService;
    }

    @Transactional
    public LegalAcceptanceResponse accept(Long userId, String userType, LegalAcceptRequest request) {
        String type = normalizeType(request.documentType());
        int version = request.documentVersion() == null
                ? legalContentService.getPublishedByType(type).version()
                : request.documentVersion();
        return repository.findByUserIdAndUserTypeAndDocumentTypeAndDocumentVersion(userId, userType, type, version)
                .map(this::toResponse)
                .orElseGet(() -> {
                    LegalAcceptance acceptance = new LegalAcceptance();
                    acceptance.setUserId(userId);
                    acceptance.setUserType(userType);
                    acceptance.setDocumentType(type);
                    acceptance.setDocumentVersion(version);
                    return toResponse(repository.save(acceptance));
                });
    }

    @Transactional(readOnly = true)
    public List<LegalAcceptanceResponse> history(Long userId, String userType) {
        return repository.findByUserIdAndUserTypeOrderByAcceptedAtDesc(userId, userType)
                .stream().map(this::toResponse).toList();
    }

    private LegalAcceptanceResponse toResponse(LegalAcceptance acceptance) {
        return new LegalAcceptanceResponse(acceptance.getId(), acceptance.getUserId(), acceptance.getUserType(),
                acceptance.getDocumentType(), acceptance.getDocumentVersion(), acceptance.getAcceptedAt());
    }

    private String normalizeType(String value) {
        return value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
    }
}

package com.lexho.user_service.admin.service;

import com.lexho.user_service.admin.dto.AdminProfileContentRequest;
import com.lexho.user_service.admin.dto.AdminProfileContentResponse;
import com.lexho.user_service.admin.entity.AdminProfileContentEntity;
import com.lexho.user_service.admin.repository.AdminProfileContentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminProfileContentService {

    private static final long PROFILE_CONTENT_ID = 1L;
    private final AdminProfileContentRepository adminProfileContentRepository;

    public AdminProfileContentService(AdminProfileContentRepository adminProfileContentRepository) {
        this.adminProfileContentRepository = adminProfileContentRepository;
    }

    @Transactional(readOnly = true)
    public AdminProfileContentResponse getProfileContent() {
        return toResponse(adminProfileContentRepository.findById(PROFILE_CONTENT_ID).orElseGet(this::defaultEntity));
    }

    @Transactional
    public AdminProfileContentResponse updateProfileContent(AdminProfileContentRequest request) {
        AdminProfileContentEntity entity = adminProfileContentRepository.findById(PROFILE_CONTENT_ID).orElseGet(this::defaultEntity);
        entity.setAboutUs(request.getAboutUs().trim());
        entity.setTermsOfService(request.getTermsOfService().trim());
        entity.setPrivacyPolicy(request.getPrivacyPolicy().trim());
        return toResponse(adminProfileContentRepository.save(entity));
    }

    private AdminProfileContentEntity defaultEntity() {
        AdminProfileContentEntity entity = new AdminProfileContentEntity();
        entity.setId(PROFILE_CONTENT_ID);
        entity.setAboutUs("");
        entity.setTermsOfService("");
        entity.setPrivacyPolicy("");
        return entity;
    }

    private AdminProfileContentResponse toResponse(AdminProfileContentEntity entity) {
        AdminProfileContentResponse response = new AdminProfileContentResponse();
        response.setAboutUs(entity.getAboutUs());
        response.setTermsOfService(entity.getTermsOfService());
        response.setPrivacyPolicy(entity.getPrivacyPolicy());
        return response;
    }
}

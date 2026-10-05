package com.lexho.user_service.admin.service;

import com.lexho.user_service.admin.dto.AdminSupportFaqCategoryUpsertRequest;
import com.lexho.user_service.admin.dto.AdminSupportFaqUpsertRequest;
import com.lexho.user_service.admin.dto.DataResponse;
import com.lexho.user_service.admin.entity.SupportFaq;
import com.lexho.user_service.admin.entity.SupportFaqCategory;
import com.lexho.user_service.admin.repository.SupportFaqCategoryRepository;
import com.lexho.user_service.admin.repository.SupportFaqRepository;
import com.lexho.user_service.audit.service.AuditLogService;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AdminSupportFaqService {

    private final SupportFaqCategoryRepository categoryRepository;
    private final SupportFaqRepository faqRepository;
    private final AuditLogService auditLogService;

    public AdminSupportFaqService(SupportFaqCategoryRepository categoryRepository,
                                  SupportFaqRepository faqRepository) {
        this(categoryRepository, faqRepository, Optional.empty());
    }

    @Autowired
    public AdminSupportFaqService(SupportFaqCategoryRepository categoryRepository,
                                  SupportFaqRepository faqRepository,
                                  Optional<AuditLogService> auditLogService) {
        this.categoryRepository = categoryRepository;
        this.faqRepository = faqRepository;
        this.auditLogService = auditLogService.orElse(null);
    }

    public DataResponse<List<Map<String, Object>>> listCategories() {
        List<Map<String, Object>> items = categoryRepository.findAll().stream()
                .map(this::mapCategory)
                .collect(Collectors.toList());
        return new DataResponse<>(items);
    }

    @Transactional
    public DataResponse<Map<String, Object>> createCategory(AdminSupportFaqCategoryUpsertRequest request) {
        String key = normalizeKey(request.getId());
        if (categoryRepository.existsByCategoryKey(key)) {
            throw new BadRequestException("FAQ category id already exists");
        }
        SupportFaqCategory category = new SupportFaqCategory();
        category.setCategoryKey(key);
        applyCategory(category, request);
        return new DataResponse<>(mapCategory(categoryRepository.save(category)));
    }

    @Transactional
    public DataResponse<Map<String, Object>> updateCategory(String categoryId, AdminSupportFaqCategoryUpsertRequest request) {
        SupportFaqCategory category = findCategory(categoryId);
        applyCategory(category, request);
        return new DataResponse<>(mapCategory(categoryRepository.save(category)));
    }

    @Transactional
    public DataResponse<Map<String, Object>> deleteCategory(String categoryId) {
        SupportFaqCategory category = findCategory(categoryId);
        categoryRepository.delete(category);
        return new DataResponse<>(Map.of("deleted", true));
    }

    @Transactional(readOnly = true)
    public DataResponse<List<Map<String, Object>>> listFaqs(String categoryId) {
        SupportFaqCategory category = findCategory(categoryId);
        List<Map<String, Object>> items = faqRepository.findByCategoryOrderByDisplayOrderAscIdAsc(category).stream()
                .map(this::mapFaq)
                .collect(Collectors.toList());
        return new DataResponse<>(items);
    }

    @Transactional
    public DataResponse<Map<String, Object>> createFaq(String categoryId, AdminSupportFaqUpsertRequest request) {
        SupportFaqCategory category = findCategory(categoryId);
        String key = normalizeKey(request.getId());
        if (faqRepository.existsByFaqKey(key)) {
            throw new BadRequestException("FAQ id already exists");
        }
        SupportFaq faq = new SupportFaq();
        faq.setFaqKey(key);
        faq.setCategory(category);
        applyFaq(faq, request);
        SupportFaq saved = faqRepository.save(faq);
        audit("FAQ_CREATED", saved);
        return new DataResponse<>(mapFaq(saved));
    }

    @Transactional
    public DataResponse<Map<String, Object>> updateFaq(String categoryId, String faqId, AdminSupportFaqUpsertRequest request) {
        SupportFaqCategory category = findCategory(categoryId);
        SupportFaq faq = findFaq(faqId);
        faq.setCategory(category);
        applyFaq(faq, request);
        SupportFaq saved = faqRepository.save(faq);
        audit("FAQ_UPDATED", saved);
        return new DataResponse<>(mapFaq(saved));
    }

    @Transactional
    public DataResponse<Map<String, Object>> deleteFaq(String categoryId, String faqId) {
        findCategory(categoryId);
        SupportFaq faq = findFaq(faqId);
        faqRepository.delete(faq);
        audit("FAQ_DELETED", faq);
        return new DataResponse<>(Map.of("deleted", true));
    }

    private void applyCategory(SupportFaqCategory category, AdminSupportFaqCategoryUpsertRequest request) {
        category.setLabel(request.getLabel().trim());
        category.setDisplayOrder(request.getDisplayOrder());
        category.setActive(Boolean.TRUE.equals(request.getActive()));
    }

    private void applyFaq(SupportFaq faq, AdminSupportFaqUpsertRequest request) {
        faq.setQuestion(request.getQuestion().trim());
        faq.setAnswer(request.getAnswer().trim());
        faq.setDisplayOrder(request.getDisplayOrder());
        faq.setActive(Boolean.TRUE.equals(request.getActive()));
        faq.setUserType(normalizeUserType(request.getUserType()));
    }

    private SupportFaqCategory findCategory(String categoryId) {
        return categoryRepository.findByCategoryKey(normalizeKey(categoryId))
                .orElseThrow(() -> new ResourceNotFoundException("FAQ category not found"));
    }

    private SupportFaq findFaq(String faqId) {
        return faqRepository.findByFaqKey(normalizeKey(faqId))
                .orElseThrow(() -> new ResourceNotFoundException("FAQ not found"));
    }

    private String normalizeKey(String key) {
        if (key == null || key.isBlank()) {
            throw new BadRequestException("id is required");
        }
        return key.trim().toLowerCase();
    }

    private Map<String, Object> mapCategory(SupportFaqCategory category) {
        return Map.of(
                "id", category.getCategoryKey(),
                "label", category.getLabel(),
                "displayOrder", category.getDisplayOrder(),
                "active", category.isActive()
        );
    }

    private Map<String, Object> mapFaq(SupportFaq faq) {
        return Map.of(
                "id", faq.getFaqKey(),
                "question", faq.getQuestion(),
                "answer", faq.getAnswer(),
                "displayOrder", faq.getDisplayOrder(),
                "active", faq.isActive(),
                "userType", faq.getUserType(),
                "categoryId", faq.getCategory().getCategoryKey()
        );
    }

    private String normalizeUserType(String userType) {
        if (userType == null || userType.isBlank()) {
            return "GENERAL";
        }
        String normalized = userType.trim().toUpperCase();
        if (!List.of("CUSTOMER", "PARTNER", "GENERAL").contains(normalized)) {
            throw new BadRequestException("Unsupported FAQ userType");
        }
        return normalized;
    }

    private void audit(String action, SupportFaq faq) {
        if (auditLogService != null) {
            auditLogService.log(null, "ADMIN", action, "FAQ", faq.getFaqKey(), null, faq.getQuestion());
        }
    }
}

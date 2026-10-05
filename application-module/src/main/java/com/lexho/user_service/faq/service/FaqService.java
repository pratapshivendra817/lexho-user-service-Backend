package com.lexho.user_service.faq.service;

import com.lexho.user_service.admin.entity.SupportFaq;
import com.lexho.user_service.admin.repository.SupportFaqRepository;
import com.lexho.user_service.faq.dto.FaqResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class FaqService {
    private final SupportFaqRepository repository;

    public FaqService(SupportFaqRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<FaqResponse> publicFaqs(String userType) {
        List<SupportFaq> faqs;
        if (userType == null || userType.isBlank()) {
            faqs = repository.findByActiveTrueOrderByDisplayOrderAscIdAsc();
        } else {
            String normalized = userType.trim().toUpperCase(Locale.ROOT);
            faqs = repository.findByUserTypeInAndActiveTrueOrderByDisplayOrderAscIdAsc(
                    List.of("GENERAL", normalized));
        }
        return faqs.stream().map(this::toResponse).toList();
    }

    private FaqResponse toResponse(SupportFaq faq) {
        return new FaqResponse(
                faq.getId(),
                faq.getCategory().getCategoryKey(),
                faq.getQuestion(),
                faq.getAnswer(),
                faq.getUserType(),
                faq.getDisplayOrder(),
                faq.isActive()
        );
    }
}

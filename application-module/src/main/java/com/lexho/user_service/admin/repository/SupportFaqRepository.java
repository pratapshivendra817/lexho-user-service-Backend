package com.lexho.user_service.admin.repository;

import com.lexho.user_service.admin.entity.SupportFaq;
import com.lexho.user_service.admin.entity.SupportFaqCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupportFaqRepository extends JpaRepository<SupportFaq, Long> {
    Optional<SupportFaq> findByFaqKey(String faqKey);

    boolean existsByFaqKey(String faqKey);

    List<SupportFaq> findByCategoryAndActiveTrueOrderByDisplayOrderAscIdAsc(SupportFaqCategory category);

    List<SupportFaq> findByCategoryOrderByDisplayOrderAscIdAsc(SupportFaqCategory category);
}


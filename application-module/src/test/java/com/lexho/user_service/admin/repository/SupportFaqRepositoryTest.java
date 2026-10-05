package com.lexho.user_service.admin.repository;

import com.lexho.admin.entity.Admin;
import com.lexho.testconfig.LexhoMySqlDataJpaTest;
import com.lexho.user_service.admin.entity.SupportFaq;
import com.lexho.user_service.admin.entity.AdminOtp;
import com.lexho.user_service.admin.entity.SupportFaqCategory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@LexhoMySqlDataJpaTest
@ContextConfiguration(classes = SupportFaqRepositoryTest.JpaConfig.class)
class SupportFaqRepositoryTest {

    @Autowired
    private SupportFaqRepository faqRepository;

    @Autowired
    private SupportFaqCategoryRepository categoryRepository;

    @Test
    void categoryAndFaqQueriesRespectOrderingActiveAndUniqueKeys() {
        SupportFaqCategory billing = categoryRepository.save(category("billing", "Billing", 2, true));
        SupportFaqCategory general = categoryRepository.save(category("general", "General", 1, true));
        categoryRepository.save(category("inactive", "Inactive", 3, false));

        SupportFaq faq2 = faqRepository.save(faq("billing-refund", billing, "Refund ETA?", 2, true));
        SupportFaq faq1 = faqRepository.save(faq("billing-invoice", billing, "Where is my invoice?", 1, true));
        faqRepository.save(faq("billing-disabled", billing, "Old billing FAQ", 3, false));
        faqRepository.save(faq("general-help", general, "Need help?", 1, true));

        assertThat(categoryRepository.findByCategoryKey("billing")).contains(billing);
        assertThat(categoryRepository.existsByCategoryKey("general")).isTrue();
        assertThat(categoryRepository.findByActiveTrueOrderByDisplayOrderAscIdAsc())
                .extracting(SupportFaqCategory::getCategoryKey)
                .containsExactly("general", "billing");

        assertThat(faqRepository.findByFaqKey("billing-invoice")).contains(faq1);
        assertThat(faqRepository.existsByFaqKey("billing-refund")).isTrue();
        assertThat(faqRepository.findByCategoryAndActiveTrueOrderByDisplayOrderAscIdAsc(billing))
                .extracting(SupportFaq::getFaqKey)
                .containsExactly("billing-invoice", "billing-refund");
        assertThat(faqRepository.findByCategoryOrderByDisplayOrderAscIdAsc(billing))
                .extracting(SupportFaq::getFaqKey)
                .containsExactly("billing-invoice", "billing-refund", "billing-disabled");

        assertThatThrownBy(() -> faqRepository.saveAndFlush(faq("billing-invoice", general, "Duplicate key", 9, true)))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(faq2.getCreatedAt()).isNotNull();
    }

    private SupportFaqCategory category(String key, String label, int order, boolean active) {
        SupportFaqCategory entity = new SupportFaqCategory();
        entity.setCategoryKey(key);
        entity.setLabel(label);
        entity.setDisplayOrder(order);
        entity.setActive(active);
        return entity;
    }

    private SupportFaq faq(String key, SupportFaqCategory category, String question, int order, boolean active) {
        SupportFaq entity = new SupportFaq();
        entity.setFaqKey(key);
        entity.setCategory(category);
        entity.setQuestion(question);
        entity.setAnswer("Answer for " + question);
        entity.setDisplayOrder(order);
        entity.setActive(active);
        return entity;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {SupportFaq.class, SupportFaqCategory.class, AdminOtp.class, Admin.class})
    @EnableJpaRepositories(
            basePackageClasses = {SupportFaqRepository.class, SupportFaqCategoryRepository.class},
            includeFilters = {
                    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SupportFaqRepository.class),
                    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SupportFaqCategoryRepository.class)
            }
    )
    static class JpaConfig {
    }
}


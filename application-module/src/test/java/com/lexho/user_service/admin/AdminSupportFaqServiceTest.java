package com.lexho.user_service.admin;

import com.lexho.user_service.admin.dto.AdminSupportFaqCategoryUpsertRequest;
import com.lexho.user_service.admin.dto.AdminSupportFaqUpsertRequest;
import com.lexho.user_service.admin.entity.SupportFaq;
import com.lexho.user_service.admin.entity.SupportFaqCategory;
import com.lexho.user_service.admin.repository.SupportFaqCategoryRepository;
import com.lexho.user_service.admin.repository.SupportFaqRepository;
import com.lexho.user_service.admin.service.AdminSupportFaqService;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminSupportFaqServiceTest {
    @Mock SupportFaqCategoryRepository categoryRepository;
    @Mock SupportFaqRepository faqRepository;
    private AdminSupportFaqService service;

    @BeforeEach void setUp() { service = new AdminSupportFaqService(categoryRepository, faqRepository); }

    @Test
    void categoryCrudNormalizesKeysAndRejectsDuplicates() {
        SupportFaqCategory category = category("general");
        when(categoryRepository.findAll()).thenReturn(List.of(category));
        assertEquals("general", service.listCategories().getData().get(0).get("id"));

        when(categoryRepository.existsByCategoryKey("general")).thenReturn(true);
        assertThrows(BadRequestException.class, () -> service.createCategory(categoryRequest(" General ")));
        assertThrows(BadRequestException.class, () -> service.createCategory(categoryRequest(" ")));

        when(categoryRepository.existsByCategoryKey("billing")).thenReturn(false);
        when(categoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        assertEquals("billing", service.createCategory(categoryRequest(" Billing ")).getData().get("id"));

        when(categoryRepository.findByCategoryKey("general")).thenReturn(Optional.of(category));
        assertEquals("Label", service.updateCategory(" GENERAL ", categoryRequest("ignored")).getData().get("label"));
        assertEquals(true, service.deleteCategory("general").getData().get("deleted"));
        verify(categoryRepository).delete(category);
    }

    @Test
    void faqCrudValidatesCategoryAndFaqKeys() {
        SupportFaqCategory category = category("general");
        when(categoryRepository.findByCategoryKey("general")).thenReturn(Optional.of(category));
        when(faqRepository.existsByFaqKey("q1")).thenReturn(true);
        assertThrows(BadRequestException.class, () -> service.createFaq("general", faqRequest(" Q1 ")));

        when(faqRepository.existsByFaqKey("q2")).thenReturn(false);
        when(faqRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        assertEquals("Question", service.createFaq("general", faqRequest(" Q2 ")).getData().get("question"));

        SupportFaq faq = faq("q1", category);
        when(faqRepository.findByFaqKey("q1")).thenReturn(Optional.of(faq));
        assertEquals("general", service.updateFaq("general", "q1", faqRequest("ignored")).getData().get("categoryId"));
        assertEquals(true, service.deleteFaq("general", "q1").getData().get("deleted"));
        verify(faqRepository).delete(faq);
    }

    @Test
    void listingAndMissingEntitiesAreHandled() {
        SupportFaqCategory category = category("general");
        SupportFaq faq = faq("q1", category);
        when(categoryRepository.findByCategoryKey("general")).thenReturn(Optional.of(category));
        when(faqRepository.findByCategoryOrderByDisplayOrderAscIdAsc(category)).thenReturn(List.of(faq));
        assertEquals(1, service.listFaqs("general").getData().size());

        when(categoryRepository.findByCategoryKey("missing")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.listFaqs("missing"));
        when(faqRepository.findByFaqKey("missing")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.deleteFaq("general", "missing"));
    }

    private AdminSupportFaqCategoryUpsertRequest categoryRequest(String id) {
        AdminSupportFaqCategoryUpsertRequest r = new AdminSupportFaqCategoryUpsertRequest();
        r.setId(id); r.setLabel(" Label "); r.setDisplayOrder(1); r.setActive(true); return r;
    }

    private AdminSupportFaqUpsertRequest faqRequest(String id) {
        AdminSupportFaqUpsertRequest r = new AdminSupportFaqUpsertRequest();
        r.setId(id); r.setQuestion(" Question "); r.setAnswer(" Answer "); r.setDisplayOrder(1); r.setActive(true); return r;
    }

    private SupportFaqCategory category(String key) {
        SupportFaqCategory c = new SupportFaqCategory(); c.setCategoryKey(key); c.setLabel("General");
        c.setDisplayOrder(1); c.setActive(true); return c;
    }

    private SupportFaq faq(String key, SupportFaqCategory category) {
        SupportFaq f = new SupportFaq(); f.setFaqKey(key); f.setCategory(category); f.setQuestion("Q");
        f.setAnswer("A"); f.setDisplayOrder(1); f.setActive(true); return f;
    }
}

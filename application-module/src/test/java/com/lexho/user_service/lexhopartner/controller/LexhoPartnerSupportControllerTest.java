package com.lexho.user_service.lexhopartner.controller;

import com.lexho.user_service.admin.entity.SupportFaq;
import com.lexho.user_service.admin.entity.SupportFaqCategory;
import com.lexho.user_service.admin.repository.SupportFaqCategoryRepository;
import com.lexho.user_service.admin.repository.SupportFaqRepository;
import com.lexho.user_service.lexhopartner.service.PartnerSupportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LexhoPartnerSupportControllerTest {

    private PartnerSupportService partnerSupportService;
    private SupportFaqCategoryRepository supportFaqCategoryRepository;
    private SupportFaqRepository supportFaqRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        partnerSupportService = mock(PartnerSupportService.class);
        supportFaqCategoryRepository = mock(SupportFaqCategoryRepository.class);
        supportFaqRepository = mock(SupportFaqRepository.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new LexhoPartnerSupportController(
                partnerSupportService,
                supportFaqCategoryRepository,
                supportFaqRepository
        )).build();
    }

    @Test
    void faqsShouldReturnActiveFaqGroups() throws Exception {
        SupportFaqCategory category = new SupportFaqCategory();
        category.setCategoryKey("partner-payments");
        category.setLabel("Partner Payments");
        category.setDisplayOrder(1);
        SupportFaq faq = new SupportFaq();
        faq.setFaqKey("payout-delay");
        faq.setQuestion("When will payout arrive?");
        faq.setAnswer("According to settlement cycle.");
        faq.setDisplayOrder(1);
        when(supportFaqCategoryRepository.findByActiveTrueOrderByDisplayOrderAscIdAsc()).thenReturn(List.of(category));
        when(supportFaqRepository.findByCategoryAndActiveTrueOrderByDisplayOrderAscIdAsc(category)).thenReturn(List.of(faq));

        mockMvc.perform(get("/api/partner/support/faqs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("partner-payments"))
                .andExpect(jsonPath("$.data[0].faqs[0].id").value("payout-delay"));
    }

    @Test
    void streamShouldDelegateToPartnerSupportService() throws Exception {
        when(partnerSupportService.stream(7L, "TKT-000005")).thenReturn(new SseEmitter());

        mockMvc.perform(get("/api/partner/support/tickets/TKT-000005/stream").requestAttr("userId", 7L))
                .andExpect(status().isOk());
    }
}

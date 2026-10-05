package com.lexho.user_service.faq.service;

import com.lexho.user_service.admin.entity.SupportFaq;
import com.lexho.user_service.admin.entity.SupportFaqCategory;
import com.lexho.user_service.admin.repository.SupportFaqRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FaqServiceTest {

    @Test
    void returnsGeneralAndRequestedUserTypeFaqs() {
        SupportFaqRepository repository = mock(SupportFaqRepository.class);
        FaqService service = new FaqService(repository);
        SupportFaq faq = faq("GENERAL");
        when(repository.findByUserTypeInAndActiveTrueOrderByDisplayOrderAscIdAsc(List.of("GENERAL", "PARTNER")))
                .thenReturn(List.of(faq));

        var response = service.publicFaqs("partner");

        assertThat(response).hasSize(1);
        assertThat(response.get(0).userType()).isEqualTo("GENERAL");
        verify(repository).findByUserTypeInAndActiveTrueOrderByDisplayOrderAscIdAsc(List.of("GENERAL", "PARTNER"));
    }

    @Test
    void returnsAllActiveFaqsWhenUserTypeIsMissing() {
        SupportFaqRepository repository = mock(SupportFaqRepository.class);
        FaqService service = new FaqService(repository);
        when(repository.findByActiveTrueOrderByDisplayOrderAscIdAsc()).thenReturn(List.of(faq("CUSTOMER")));

        var response = service.publicFaqs(null);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).userType()).isEqualTo("CUSTOMER");
        verify(repository).findByActiveTrueOrderByDisplayOrderAscIdAsc();
    }

    private SupportFaq faq(String userType) {
        SupportFaqCategory category = new SupportFaqCategory();
        category.setCategoryKey("general");
        SupportFaq faq = new SupportFaq();
        faq.setCategory(category);
        faq.setQuestion("Question?");
        faq.setAnswer("Answer");
        faq.setDisplayOrder(1);
        faq.setActive(true);
        faq.setUserType(userType);
        return faq;
    }
}

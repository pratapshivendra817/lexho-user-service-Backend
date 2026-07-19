package com.lexho.user_service.admin.service;

import com.lexho.user_service.admin.dto.AdminProfileContentRequest;
import com.lexho.user_service.admin.entity.AdminProfileContentEntity;
import com.lexho.user_service.admin.repository.AdminProfileContentRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminProfileContentServiceTest {

    private final AdminProfileContentRepository repository = mock(AdminProfileContentRepository.class);
    private final AdminProfileContentService service = new AdminProfileContentService(repository);

    @Test
    void returnsDefaultsWhenContentDoesNotExist() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        var response = service.getProfileContent();

        assertThat(response.getAboutUs()).isEmpty();
        assertThat(response.getTermsOfService()).isEmpty();
        assertThat(response.getPrivacyPolicy()).isEmpty();
    }

    @Test
    void updatesAndTrimsProfileContent() {
        AdminProfileContentRequest request = new AdminProfileContentRequest();
        request.setAboutUs(" about ");
        request.setTermsOfService(" terms ");
        request.setPrivacyPolicy(" privacy ");
        when(repository.findById(1L)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.updateProfileContent(request);

        assertThat(response.getAboutUs()).isEqualTo("about");
        assertThat(response.getTermsOfService()).isEqualTo("terms");
        assertThat(response.getPrivacyPolicy()).isEqualTo("privacy");
    }
}

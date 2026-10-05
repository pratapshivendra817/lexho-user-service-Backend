package com.lexho.user_service.notification.service;

import com.lexho.notification.enums.NotificationType;
import com.lexho.user_service.notification.dto.NotificationTemplateRequest;
import com.lexho.user_service.notification.entity.NotificationTemplateEntity;
import com.lexho.user_service.notification.repository.NotificationTemplateRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificationTemplateServiceTest {

    private final NotificationTemplateRepository repository = mock(NotificationTemplateRepository.class);
    private final NotificationTemplateService service = new NotificationTemplateService(repository);

    @Test
    void renderUsesActiveAdminTemplateAndAppliesVariables() {
        NotificationTemplateEntity template = new NotificationTemplateEntity();
        template.setTemplateName("SERVICE_COMPLETED_PARTNER");
        template.setLanguage("en");
        template.setNotificationType(NotificationType.SERVICE_COMPLETED);
        template.setTitleTemplate("Done #{{orderId}}");
        template.setMessageTemplate("Earning INR {{earningAmount}}");

        when(repository.findByTemplateNameAndLanguageIgnoreCaseAndActiveTrue("SERVICE_COMPLETED_PARTNER", "en"))
                .thenReturn(Optional.of(template));

        var rendered = service.render(
                "service_completed_partner",
                NotificationType.SYSTEM,
                "Fallback",
                "Fallback {{orderId}}",
                java.util.Map.of("orderId", "15", "earningAmount", "212.50")
        );

        assertThat(rendered.notificationType()).isEqualTo(NotificationType.SERVICE_COMPLETED);
        assertThat(rendered.title()).isEqualTo("Done #15");
        assertThat(rendered.message()).isEqualTo("Earning INR 212.50");
    }

    @Test
    void upsertNormalizesTemplateNameAndLanguage() {
        when(repository.findByTemplateNameAndLanguageIgnoreCase("SERVICE_COMPLETED_CUSTOMER", "hi"))
                .thenReturn(Optional.empty());
        when(repository.save(any(NotificationTemplateEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationTemplateRequest request = new NotificationTemplateRequest();
        request.setTemplateName(" service_completed_customer ");
        request.setLanguage(" HI ");
        request.setNotificationType(NotificationType.SERVICE_COMPLETED);
        request.setTitleTemplate(" Ho gaya ");
        request.setMessageTemplate("Order {{orderId}} complete");

        var saved = service.upsert(request);

        assertThat(saved.templateName()).isEqualTo("SERVICE_COMPLETED_CUSTOMER");
        assertThat(saved.language()).isEqualTo("hi");
        assertThat(saved.titleTemplate()).isEqualTo("Ho gaya");
    }
}

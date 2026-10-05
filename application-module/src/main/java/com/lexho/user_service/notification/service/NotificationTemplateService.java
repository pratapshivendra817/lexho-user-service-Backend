package com.lexho.user_service.notification.service;

import com.lexho.notification.enums.NotificationType;
import com.lexho.user_service.notification.dto.NotificationTemplateRequest;
import com.lexho.user_service.notification.dto.NotificationTemplateResponse;
import com.lexho.user_service.notification.entity.NotificationTemplateEntity;
import com.lexho.user_service.notification.repository.NotificationTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class NotificationTemplateService {

    private final NotificationTemplateRepository repository;

    public NotificationTemplateService(NotificationTemplateRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public RenderedTemplate render(String templateName,
                                   NotificationType fallbackType,
                                   String fallbackTitle,
                                   String fallbackMessage,
                                   Map<String, String> variables) {
        NotificationTemplateEntity template = repository
                .findByTemplateNameAndLanguageIgnoreCaseAndActiveTrue(normalizeTemplateName(templateName), "en")
                .or(() -> repository.findByTemplateNameAndActiveTrue(normalizeTemplateName(templateName)))
                .orElse(null);
        if (template == null) {
            return new RenderedTemplate(fallbackType, fallbackTitle, applyVariables(fallbackMessage, variables));
        }
        return new RenderedTemplate(
                template.getNotificationType(),
                applyVariables(template.getTitleTemplate(), variables),
                applyVariables(template.getMessageTemplate(), variables)
        );
    }

    @Transactional(readOnly = true)
    public List<NotificationTemplateResponse> list() {
        return repository.findAllByOrderByTemplateNameAscLanguageAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public NotificationTemplateResponse upsert(NotificationTemplateRequest request) {
        String templateName = normalizeTemplateName(request.getTemplateName());
        String language = normalizeLanguage(request.getLanguage());
        NotificationTemplateEntity template = repository
                .findByTemplateNameAndLanguageIgnoreCase(templateName, language)
                .orElseGet(NotificationTemplateEntity::new);
        template.setTemplateName(templateName);
        template.setLanguage(language);
        template.setNotificationType(request.getNotificationType());
        template.setTitleTemplate(request.getTitleTemplate().trim());
        template.setMessageTemplate(request.getMessageTemplate().trim());
        template.setActive(request.getActive() == null || request.getActive());
        return toResponse(repository.save(template));
    }

    private NotificationTemplateResponse toResponse(NotificationTemplateEntity template) {
        return new NotificationTemplateResponse(
                template.getId(),
                template.getTemplateName(),
                template.getNotificationType(),
                template.getTitleTemplate(),
                template.getMessageTemplate(),
                template.getLanguage(),
                template.isActive(),
                template.getCreatedAt()
        );
    }

    private String applyVariables(String template, Map<String, String> variables) {
        String rendered = template;
        if (variables == null || variables.isEmpty()) {
            return rendered;
        }
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue() == null ? "" : entry.getValue());
        }
        return rendered;
    }

    private String normalizeTemplateName(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeLanguage(String value) {
        return value == null || value.isBlank() ? "en" : value.trim().toLowerCase(Locale.ROOT);
    }

    public record RenderedTemplate(NotificationType notificationType, String title, String message) {
    }
}

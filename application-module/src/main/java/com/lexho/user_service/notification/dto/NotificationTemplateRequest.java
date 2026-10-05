package com.lexho.user_service.notification.dto;

import com.lexho.notification.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class NotificationTemplateRequest {

    @NotBlank(message = "templateName is required")
    @Size(max = 120, message = "templateName must be at most 120 characters")
    private String templateName;

    @NotNull(message = "notificationType is required")
    private NotificationType notificationType;

    @NotBlank(message = "titleTemplate is required")
    @Size(max = 255, message = "titleTemplate must be at most 255 characters")
    private String titleTemplate;

    @NotBlank(message = "messageTemplate is required")
    @Size(max = 2000, message = "messageTemplate must be at most 2000 characters")
    private String messageTemplate;

    @Size(max = 20, message = "language must be at most 20 characters")
    private String language;

    private Boolean active;

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(NotificationType notificationType) {
        this.notificationType = notificationType;
    }

    public String getTitleTemplate() {
        return titleTemplate;
    }

    public void setTitleTemplate(String titleTemplate) {
        this.titleTemplate = titleTemplate;
    }

    public String getMessageTemplate() {
        return messageTemplate;
    }

    public void setMessageTemplate(String messageTemplate) {
        this.messageTemplate = messageTemplate;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}

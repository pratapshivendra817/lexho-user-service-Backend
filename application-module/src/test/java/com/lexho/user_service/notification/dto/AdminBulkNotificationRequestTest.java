package com.lexho.user_service.notification.dto;

import com.lexho.notification.enums.ChannelType;
import com.lexho.notification.enums.NotificationType;
import com.lexho.notification.enums.NotificationUserType;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminBulkNotificationRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validationRejectsMissingRequiredFields() {
        AdminBulkNotificationRequest request = new AdminBulkNotificationRequest();

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("title", "message", "notificationType", "channels", "targetUserType");
    }

    @Test
    void legacySettersNormalizeValuesAndAvoidDuplicateChannels() {
        AdminBulkNotificationRequest request = new AdminBulkNotificationRequest();

        request.setChannelType(ChannelType.IN_APP);
        request.setChannelType(ChannelType.IN_APP);
        request.setUserId(42L);
        request.setUserId(42L);
        request.setPartnerId(84L);
        request.setLegacyUserType("customers");
        request.setTargetUserTypeFromString("partners");
        request.setNotificationTypeFromString("payment success");
        request.setLegacyType("offers");

        assertThat(request.getChannels()).containsExactly(ChannelType.IN_APP);
        assertThat(request.getUserIds()).containsExactly(42L, 84L);
        assertThat(request.getTargetUserType()).isEqualTo(NotificationUserType.PARTNER);
        assertThat(request.getNotificationType()).isEqualTo(NotificationType.PROMOTIONAL_OFFER);
    }

    @Test
    void legacyPartnerPushPayloadMapsToPartnerSystemNotificationWithInAppAndPushChannels() {
        AdminBulkNotificationRequest request = new AdminBulkNotificationRequest();

        request.setTitle("Onboarding approved");
        request.setMessage("Your onboarding has been approved.");
        request.setAppType("PARTNER");
        request.setPartnerId(15L);
        request.setLegacyType("PUSH");
        request.setNotificationTypeFromString("ONBOARDING");
        request.setReferenceType("ONBOARDING");
        request.setRedirectType("ONBOARDING");
        request.setRedirectValue("15");

        assertThat(validator.validate(request)).isEmpty();
        assertThat(request.getTargetUserType()).isEqualTo(NotificationUserType.PARTNER);
        assertThat(request.getNotificationType()).isEqualTo(NotificationType.SYSTEM);
        assertThat(request.getChannels()).containsExactly(ChannelType.IN_APP, ChannelType.PUSH);
        assertThat(request.getUserIds()).containsExactly(15L);
    }

    @Test
    void blankLegacyValuesPreserveExistingEnumsAndUnknownValuesFailFast() {
        AdminBulkNotificationRequest request = new AdminBulkNotificationRequest();
        request.setTargetUserType(NotificationUserType.ADMIN);
        request.setNotificationType(NotificationType.REMINDER);

        request.setLegacyUserType(" ");
        request.setNotificationTypeFromString(" ");

        assertThat(request.getTargetUserType()).isEqualTo(NotificationUserType.ADMIN);
        assertThat(request.getNotificationType()).isEqualTo(NotificationType.REMINDER);
        assertThatThrownBy(() -> request.setLegacyType("not_real"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

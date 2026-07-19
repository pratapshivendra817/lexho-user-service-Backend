package com.lexho.user_service.notification.service;

import com.lexho.notification.entity.Notification;
import com.lexho.user_service.notification.config.NotificationChannelProperties;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SmsNotificationServiceTest {

    private final NotificationChannelProperties properties = new NotificationChannelProperties();
    private final NotificationRecipientResolver resolver = mock(NotificationRecipientResolver.class);
    private final SmsNotificationService service = new SmsNotificationService(properties, resolver);
    private final Notification notification = new Notification();

    @Test
    void validatesChannelGatewayAndRecipient() {
        assertThatThrownBy(() -> service.send(notification)).hasMessageContaining("disabled");

        properties.getSms().setEnabled(true);
        assertThatThrownBy(() -> service.send(notification)).hasMessageContaining("gateway");

        properties.getSms().setGatewayUrl("https://sms.test/send");
        when(resolver.resolve(notification)).thenReturn(new NotificationRecipientResolver.RecipientContact(" ", null, null));
        assertThatThrownBy(() -> service.send(notification)).hasMessageContaining("mobile");
    }

    @Test
    void postsSmsWithBearerAuthentication() {
        properties.getSms().setEnabled(true);
        properties.getSms().setGatewayUrl("https://sms.test/send");
        properties.getSms().setAuthToken(" secret ");
        properties.getSms().setSenderId("LEXHO");
        when(resolver.resolve(notification))
                .thenReturn(new NotificationRecipientResolver.RecipientContact(" 9999999999 ", null, null));
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("https://sms.test/send"))
                .andExpect(header("Authorization", "Bearer secret"))
                .andRespond(withSuccess());

        service.send(notification);

        server.verify();
    }
}

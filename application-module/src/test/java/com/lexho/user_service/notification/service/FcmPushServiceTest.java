package com.lexho.user_service.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FcmPushServiceTest {

    @Test
    void rejectsMissingFirebaseAndReturnsSuccessfulSendCount() throws Exception {
        @SuppressWarnings("unchecked")
        ObjectProvider<FirebaseMessaging> missing = mock(ObjectProvider.class);
        when(missing.getIfAvailable()).thenReturn(null);
        assertThatThrownBy(() -> new FcmPushService(missing).sendToTokens(List.of("a"), "title", "body", null))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("not initialized");

        FirebaseMessaging messaging = mock(FirebaseMessaging.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<FirebaseMessaging> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(messaging);
        when(messaging.send(any())).thenReturn("message-id");

        assertThat(new FcmPushService(provider).sendToTokens(
                List.of("a", "b"), "title", "body", Map.of("valid", "value"))).isEqualTo(2);
    }

    @Test
    void continuesPartialFailureButThrowsWhenEveryTokenFails() throws Exception {
        FirebaseMessaging messaging = mock(FirebaseMessaging.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<FirebaseMessaging> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(messaging);
        when(messaging.send(any())).thenThrow(new IllegalArgumentException("bad token")).thenReturn("ok");
        FcmPushService service = new FcmPushService(provider);

        assertThat(service.sendToTokens(List.of("bad", "good"), "title", "body",
                java.util.Collections.singletonMap(null, "ignored"))).isEqualTo(1);

        when(messaging.send(any())).thenThrow(new IllegalStateException("down"));
        assertThatThrownBy(() -> service.sendToTokens(List.of("bad"), "title", "body", Map.of()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("all tokens");
    }
}

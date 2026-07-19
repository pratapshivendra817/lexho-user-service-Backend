package com.lexho.user_service.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class FcmPushService {

    private static final Logger log = LoggerFactory.getLogger(FcmPushService.class);

    private final FirebaseMessaging firebaseMessaging;

    public FcmPushService(ObjectProvider<FirebaseMessaging> firebaseMessagingProvider) {
        this.firebaseMessaging = firebaseMessagingProvider.getIfAvailable();
    }

    public int sendToTokens(List<String> tokens, String title, String body, Map<String, String> data) {
        if (firebaseMessaging == null) {
            throw new IllegalStateException("FCM messaging not initialized. Check app.push.fcm configuration.");
        }
        int sent = 0;
        List<String> failures = new ArrayList<>();
        for (String token : tokens) {
            try {
                Message message = Message.builder()
                        .setToken(token)
                        .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                        .putAllData(safeData(data))
                        .build();
                firebaseMessaging.send(message);
                sent++;
            } catch (FirebaseMessagingException ex) {
                String failure = "code=" + ex.getMessagingErrorCode() + ", message=" + ex.getMessage();
                failures.add(failure);
                log.warn("FCM send failed for token. {}", failure);
            } catch (Exception ex) {
                String failure = "runtime=" + ex.getClass().getSimpleName() + ", message=" + ex.getMessage();
                failures.add(failure);
                log.warn("FCM send failed for token due to runtime error: {}", failure);
            }
        }
        if (sent <= 0 && !failures.isEmpty()) {
            throw new IllegalStateException("FCM dispatch failed for all tokens. " + String.join(" | ", failures));
        }
        return sent;
    }

    private Map<String, String> safeData(Map<String, String> input) {
        Map<String, String> safe = new HashMap<>();
        if (input == null) {
            return safe;
        }
        input.forEach((key, value) -> {
            if (key != null && !key.isBlank() && value != null) {
                safe.put(key, value);
            }
        });
        return safe;
    }
}

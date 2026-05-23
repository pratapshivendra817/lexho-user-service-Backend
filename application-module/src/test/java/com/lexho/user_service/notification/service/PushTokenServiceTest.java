package com.lexho.user_service.notification.service;

import com.lexho.user_service.notification.dto.PushTokenUpsertRequest;
import com.lexho.user_service.notification.entity.PushDeviceTokenEntity;
import com.lexho.user_service.notification.repository.PushDeviceTokenRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PushTokenServiceTest {

    @Test
    void upsertShouldPreferFcmTokenAndMapByToken() {
        PushDeviceTokenRepository repository = mock(PushDeviceTokenRepository.class);
        PushTokenService service = new PushTokenService(repository);

        PushDeviceTokenEntity existing = new PushDeviceTokenEntity();
        existing.setUserId(9L);
        existing.setRole("CUSTOMER");
        existing.setDeviceId("old-device");
        existing.setToken("same-token");

        when(repository.findByToken("same-token")).thenReturn(Optional.of(existing));

        PushTokenUpsertRequest request = new PushTokenUpsertRequest();
        request.deviceId = "new-device";
        request.token = "legacy-token";
        request.fcmToken = "same-token";
        request.platform = "ANDROID";
        request.appType = "CUSTOMER";

        service.upsert(21L, "CUSTOMER", request);

        verify(repository, never()).findByUserIdAndRoleAndDeviceId(any(), any(), any());
        verify(repository).save(existing);
    }
}

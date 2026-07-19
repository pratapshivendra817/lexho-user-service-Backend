package com.lexho.user_service.lexhopartner.service;

import com.lexho.user_service.lexhopartner.dto.PartnerDeviceTokenRequest;
import com.lexho.user_service.lexhopartner.entity.PartnerDeviceToken;
import com.lexho.user_service.lexhopartner.repository.PartnerDeviceTokenRepository;
import com.lexho.user_service.notification.dto.PushTokenUpsertRequest;
import com.lexho.user_service.notification.service.PushTokenService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PartnerDeviceTokenServiceTest {

    private final PartnerDeviceTokenRepository repository = mock(PartnerDeviceTokenRepository.class);
    private final PushTokenService pushTokenService = mock(PushTokenService.class);
    private final PartnerDeviceTokenService service = new PartnerDeviceTokenService(repository, pushTokenService);

    @Test
    void registersNewOrExistingTokenAndNormalizesDeviceType() {
        PartnerDeviceToken saved = new PartnerDeviceToken();
        saved.setId(8L);
        saved.setDeviceType("ANDROID");
        saved.setActive(true);
        when(repository.findByPartnerIdAndToken(2L, " token ")).thenReturn(Optional.empty());
        when(repository.save(any())).thenReturn(saved);

        var result = service.register(2L, new PartnerDeviceTokenRequest(" token ", " android "));

        assertThat(result).containsEntry("id", 8L).containsEntry("deviceType", "ANDROID").containsEntry("active", true);
        ArgumentCaptor<PushTokenUpsertRequest> captor = ArgumentCaptor.forClass(PushTokenUpsertRequest.class);
        verify(pushTokenService).upsert(eq(2L), eq("PARTNER"), captor.capture());
        assertThat(captor.getValue().token).isEqualTo(" token ");
        assertThat(captor.getValue().platform).isEqualTo("ANDROID");
        assertThat(captor.getValue().appType).isEqualTo("PARTNER");
    }

    @Test
    void deleteReturnsFalseWhenMissingAndDeactivatesWhenPresent() {
        PartnerDeviceTokenRequest request = new PartnerDeviceTokenRequest("token", "ios");
        when(repository.findByPartnerIdAndToken(2L, "token")).thenReturn(Optional.empty());
        assertThat(service.delete(2L, request)).containsEntry("deleted", false);

        PartnerDeviceToken token = new PartnerDeviceToken();
        token.setActive(true);
        when(repository.findByPartnerIdAndToken(3L, "token")).thenReturn(Optional.of(token));

        assertThat(service.delete(3L, request)).containsEntry("deleted", true);
        assertThat(token.isActive()).isFalse();
        verify(repository).save(token);
        verify(pushTokenService).deactivate(eq(3L), eq("PARTNER"), any());
    }
}

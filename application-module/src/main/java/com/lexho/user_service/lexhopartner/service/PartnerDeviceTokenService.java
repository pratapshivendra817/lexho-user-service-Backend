package com.lexho.user_service.lexhopartner.service;

import com.lexho.user_service.lexhopartner.dto.PartnerDeviceTokenRequest;
import com.lexho.user_service.lexhopartner.entity.PartnerDeviceToken;
import com.lexho.user_service.lexhopartner.repository.PartnerDeviceTokenRepository;
import com.lexho.user_service.notification.dto.PushTokenUpsertRequest;
import com.lexho.user_service.notification.service.PushTokenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class PartnerDeviceTokenService {

    private final PartnerDeviceTokenRepository repository;
    private final PushTokenService pushTokenService;

    public PartnerDeviceTokenService(PartnerDeviceTokenRepository repository,
                                     PushTokenService pushTokenService) {
        this.repository = repository;
        this.pushTokenService = pushTokenService;
    }

    @Transactional
    public Map<String, Object> register(Long partnerId, PartnerDeviceTokenRequest request) {
        PartnerDeviceToken token = repository.findByPartnerIdAndToken(partnerId, request.token())
                .orElseGet(PartnerDeviceToken::new);
        token.setPartnerId(partnerId);
        token.setToken(request.token().trim());
        token.setDeviceType(request.deviceType().trim().toUpperCase());
        token.setActive(true);
        PartnerDeviceToken saved = repository.save(token);
        PushTokenUpsertRequest pushRequest = new PushTokenUpsertRequest();
        pushRequest.deviceId = deviceIdFor(request.token());
        pushRequest.token = request.token();
        pushRequest.platform = saved.getDeviceType();
        pushRequest.appType = "PARTNER";
        pushTokenService.upsert(partnerId, "PARTNER", pushRequest);
        return Map.of(
                "id", saved.getId(),
                "partnerId", partnerId,
                "deviceType", saved.getDeviceType(),
                "active", saved.isActive()
        );
    }

    @Transactional
    public Map<String, Object> delete(Long partnerId, PartnerDeviceTokenRequest request) {
        PartnerDeviceToken token = repository.findByPartnerIdAndToken(partnerId, request.token())
                .orElse(null);
        if (token == null) {
            return Map.of("partnerId", partnerId, "deleted", false);
        }
        token.setActive(false);
        repository.save(token);
        pushTokenService.deactivate(partnerId, "PARTNER", deviceIdFor(request.token()));
        return Map.of("partnerId", partnerId, "deleted", true);
    }

    private String deviceIdFor(String token) {
        String trimmed = token == null ? "" : token.trim();
        return "partner-token-" + Integer.toUnsignedString(trimmed.hashCode());
    }
}

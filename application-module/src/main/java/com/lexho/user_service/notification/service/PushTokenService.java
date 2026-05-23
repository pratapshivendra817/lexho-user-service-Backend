package com.lexho.user_service.notification.service;

import com.lexho.user_service.notification.dto.PushTokenUpsertRequest;
import com.lexho.user_service.notification.entity.PushDeviceTokenEntity;
import com.lexho.user_service.notification.repository.PushDeviceTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PushTokenService {

    private final PushDeviceTokenRepository pushDeviceTokenRepository;

    public PushTokenService(PushDeviceTokenRepository pushDeviceTokenRepository) {
        this.pushDeviceTokenRepository = pushDeviceTokenRepository;
    }

    @Transactional
    public void upsert(Long userId, String role, PushTokenUpsertRequest request) {
        String resolvedToken = request.resolvedToken();
        PushDeviceTokenEntity token = pushDeviceTokenRepository
                .findByToken(resolvedToken)
                .orElseGet(() -> pushDeviceTokenRepository
                        .findByUserIdAndRoleAndDeviceId(userId, role, request.deviceId.trim())
                        .orElseGet(PushDeviceTokenEntity::new));
        if (token.getId() != null
                && (!userId.equals(token.getUserId()) || !role.equals(token.getRole()))) {
            token.setUserId(userId);
            token.setRole(role);
        }
        token.setUserId(userId);
        token.setRole(role);
        token.setDeviceId(request.deviceId.trim());
        token.setToken(resolvedToken);
        token.setPlatform(request.platform == null ? null : request.platform.trim());
        token.setAppType(request.appType == null ? null : request.appType.trim());
        token.setActive(true);
        pushDeviceTokenRepository.save(token);
    }

    @Transactional
    public void deactivate(Long userId, String role, String deviceId) {
        pushDeviceTokenRepository.findByUserIdAndRoleAndDeviceId(userId, role, deviceId.trim())
                .ifPresent(token -> {
                    token.setActive(false);
                    pushDeviceTokenRepository.save(token);
                });
    }

    @Transactional(readOnly = true)
    public List<String> activeTokens(Long userId, String role) {
        return pushDeviceTokenRepository.findByUserIdAndRoleAndActiveTrue(userId, role).stream()
                .map(PushDeviceTokenEntity::getToken)
                .distinct()
                .toList();
    }
}

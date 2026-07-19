package com.lexho.user_service.dispatch.service;

import com.lexho.user_service.dispatch.repository.CurrentPartnerLocationRepository;
import org.springframework.stereotype.Component;

@Component
public class CurrentPartnerLocationRepositoryAdapterImpl implements GeoBatchingService.CurrentPartnerLocationRepositoryAdapter {
    private final CurrentPartnerLocationRepository repository;

    public CurrentPartnerLocationRepositoryAdapterImpl(CurrentPartnerLocationRepository repository) {
        this.repository = repository;
    }

    @Override
    public void refreshGeoPoint(Long partnerId) {
        repository.refreshGeoPoint(partnerId);
    }
}

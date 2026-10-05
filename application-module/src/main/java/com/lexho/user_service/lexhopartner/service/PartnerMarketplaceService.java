package com.lexho.user_service.lexhopartner.service;

import com.lexho.provider.entity.Partner;
import com.lexho.provider.repository.PartnerRepository;
import com.lexho.user_service.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Compatibility boundary for marketplace-owned fields while partners remains canonical.
 */
@Service
public class PartnerMarketplaceService {

    private final PartnerRepository repository;

    public PartnerMarketplaceService(PartnerRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Partner require(Long partnerId) {
        return repository.findById(partnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Partner not found"));
    }

    @Transactional(readOnly = true)
    public Optional<Partner> find(Long partnerId) {
        return repository.findById(partnerId);
    }

    @Transactional(readOnly = true)
    public List<Partner> findAllByIds(Iterable<Long> ids) {
        return repository.findAllById(ids);
    }

    @Transactional
    public Partner save(Partner partner) {
        return repository.save(partner);
    }

    @Transactional
    public void updateRating(Long partnerId, double rating, long totalReviews) {
        find(partnerId).ifPresent(partner -> {
            partner.setRating(rating);
            partner.setTotalReviews(totalReviews);
            repository.save(partner);
        });
    }
}

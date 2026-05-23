package com.lexho.user_service.customer.banner.service;

import com.lexho.user_service.customer.banner.dto.CustomerBannerResponse;
import com.lexho.user_service.customer.banner.entity.CustomerBannerEntity;
import com.lexho.user_service.customer.banner.repository.CustomerBannerRepository;
import com.lexho.user_service.util.MediaUrlResolver;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("customerListingBannerService")
@RequiredArgsConstructor
public class CustomerBannerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerBannerService.class);
    private static final int MAX_BANNERS = 10;

    private final CustomerBannerRepository customerBannerRepository;
    private final MediaUrlResolver mediaUrlResolver;

    @Cacheable(value = "customerBanners", key = "'banners_' + (#categoryId == null ? 'global' : #categoryId)")
    public List<CustomerBannerResponse> getBanners(Long categoryId) {
        log.info("Fetching customer banners for categoryId={}", categoryId);
        PageRequest limit = PageRequest.of(0, MAX_BANNERS);
        List<CustomerBannerEntity> banners = categoryId == null
                ? customerBannerRepository.findActiveGlobal(limit)
                : customerBannerRepository.findActiveByCategoryOrGlobal(categoryId, limit);

        return banners.stream().map(this::toDto).toList();
    }

    private CustomerBannerResponse toDto(CustomerBannerEntity entity) {
        String resolvedImageUrl = mediaUrlResolver.toPublicUrl(entity.getImageUrl());
        return CustomerBannerResponse.builder()
                .id(entity.getId())
                .imageUrl(resolvedImageUrl == null ? "" : resolvedImageUrl)
                .redirectType(entity.getRedirectType())
                .redirectId(entity.getRedirectId())
                .redirectUrl(entity.getRedirectUrl())
                .build();
    }
}

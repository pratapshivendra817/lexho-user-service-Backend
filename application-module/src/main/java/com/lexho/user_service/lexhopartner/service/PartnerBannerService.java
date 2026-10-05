package com.lexho.user_service.lexhopartner.service;

import com.lexho.user_service.homebanner.repository.HomeBannerRepository;
import com.lexho.user_service.homebanner.entity.HomeBannerAudience;
import com.lexho.user_service.lexhopartner.dto.PartnerBannerPageResponse;
import com.lexho.user_service.lexhopartner.dto.PartnerBannerResponse;
import com.lexho.user_service.localization.dynamic.DynamicContentTranslationService;
import com.lexho.user_service.util.MediaUrlResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PartnerBannerService {

    private static final int MAX_BANNERS = 5;

    private final HomeBannerRepository homeBannerRepository;
    private final MediaUrlResolver mediaUrlResolver;
    private final DynamicContentTranslationService dynamicTranslationService;

    public PartnerBannerService(HomeBannerRepository homeBannerRepository, MediaUrlResolver mediaUrlResolver) {
        this(homeBannerRepository, mediaUrlResolver, Optional.empty());
    }

    public PartnerBannerService(HomeBannerRepository homeBannerRepository,
                                MediaUrlResolver mediaUrlResolver,
                                DynamicContentTranslationService dynamicTranslationService) {
        this(homeBannerRepository, mediaUrlResolver, Optional.ofNullable(dynamicTranslationService));
    }

    @Autowired
    public PartnerBannerService(HomeBannerRepository homeBannerRepository,
                                MediaUrlResolver mediaUrlResolver,
                                Optional<DynamicContentTranslationService> dynamicTranslationService) {
        this.homeBannerRepository = homeBannerRepository;
        this.mediaUrlResolver = mediaUrlResolver;
        this.dynamicTranslationService = optionalValue(dynamicTranslationService);
    }

    private static <T> T optionalValue(Optional<T> optional) {
        return optional == null ? null : optional.orElse(null);
    }

    @Transactional(readOnly = true)
    public PartnerBannerPageResponse partnerBanners(Long partnerId, int page, int size) {
        int safePage = Math.max(0, page);
        int requested = size <= 0 ? MAX_BANNERS : size;
        int cappedSize = Math.min(requested, MAX_BANNERS);
        List<PartnerBannerResponse> content = homeBannerRepository.findTopActiveByAudienceIn(
                        List.of(HomeBannerAudience.PARTNER, HomeBannerAudience.BOTH),
                        PageRequest.of(safePage, cappedSize))
                .stream()
                .map(b -> new PartnerBannerResponse(
                        b.getId(),
                        translate(partnerId, b.getId(), "title", b.getTitle()),
                        translate(partnerId, b.getId(), "subtitle", b.getSubtitle()),
                        mediaUrlResolver.toPublicUrl(b.getImageUrl())
                ))
                .toList();
        return new PartnerBannerPageResponse(content);
    }

    private String translate(Long partnerId, Long bannerId, String fieldName, String englishText) {
        if (dynamicTranslationService == null || englishText == null || englishText.isBlank()) {
            return englishText;
        }
        return dynamicTranslationService.translateForPartner(
                partnerId, null, "home_banner", bannerId, fieldName, englishText);
    }
}

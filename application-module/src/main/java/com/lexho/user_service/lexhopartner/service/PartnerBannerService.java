package com.lexho.user_service.lexhopartner.service;

import com.lexho.user_service.homebanner.repository.HomeBannerRepository;
import com.lexho.user_service.homebanner.entity.HomeBannerAudience;
import com.lexho.user_service.lexhopartner.dto.PartnerBannerPageResponse;
import com.lexho.user_service.lexhopartner.dto.PartnerBannerResponse;
import com.lexho.user_service.util.MediaUrlResolver;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PartnerBannerService {

    private static final int MAX_BANNERS = 5;

    private final HomeBannerRepository homeBannerRepository;
    private final MediaUrlResolver mediaUrlResolver;

    public PartnerBannerService(HomeBannerRepository homeBannerRepository, MediaUrlResolver mediaUrlResolver) {
        this.homeBannerRepository = homeBannerRepository;
        this.mediaUrlResolver = mediaUrlResolver;
    }

    @Transactional(readOnly = true)
    public PartnerBannerPageResponse partnerBanners(int page, int size) {
        int safePage = Math.max(0, page);
        int requested = size <= 0 ? MAX_BANNERS : size;
        int cappedSize = Math.min(requested, MAX_BANNERS);
        List<PartnerBannerResponse> content = homeBannerRepository.findTopActiveByAudienceIn(
                        List.of(HomeBannerAudience.PARTNER, HomeBannerAudience.BOTH),
                        PageRequest.of(safePage, cappedSize))
                .stream()
                .map(b -> new PartnerBannerResponse(
                        b.getId(),
                        b.getTitle(),
                        b.getSubtitle(),
                        mediaUrlResolver.toPublicUrl(b.getImageUrl())
                ))
                .toList();
        return new PartnerBannerPageResponse(content);
    }
}

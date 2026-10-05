package com.lexho.user_service.homebanner.service;

import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.homebanner.dto.HomeBannerReorderItemRequest;
import com.lexho.user_service.homebanner.dto.HomeBannerReorderRequest;
import com.lexho.user_service.homebanner.dto.HomeBannerResponse;
import com.lexho.user_service.homebanner.dto.HomeBannerUpsertRequest;
import com.lexho.user_service.homebanner.entity.HomeBannerAudience;
import com.lexho.user_service.homebanner.entity.HomeBanner;
import com.lexho.user_service.homebanner.entity.HomeBannerRedirectType;
import com.lexho.user_service.homebanner.repository.HomeBannerRepository;
import com.lexho.user_service.util.MediaUrlResolver;
import com.lexho.user_service.util.StringSanitizers;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class HomeBannerService {

    private static final int CUSTOMER_BANNER_LIMIT = 5;
    private static final String CACHE_NAME = "homeBanners";

    private final HomeBannerRepository homeBannerRepository;
    private final MediaUrlResolver mediaUrlResolver;

    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_NAME, key = "'top5_active_customer'")
    public List<HomeBannerResponse> getCustomerBanners() {
        return homeBannerRepository.findTopActiveByAudienceIn(
                        List.of(HomeBannerAudience.CUSTOMER, HomeBannerAudience.BOTH),
                        PageRequest.of(0, CUSTOMER_BANNER_LIMIT))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HomeBannerResponse> getAdminBanners(HomeBannerAudience audience) {
        return adminBanners(audience);
    }

    private List<HomeBannerResponse> adminBanners(HomeBannerAudience audience) {
        List<HomeBanner> banners = audience == null
                ? homeBannerRepository.findAllByOrderByPriorityAscIdAsc()
                : homeBannerRepository.findByAudienceOrderByPriorityAscIdAsc(audience);
        return banners
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public HomeBannerResponse create(HomeBannerUpsertRequest request) {
        validateRedirect(request.getRedirectType(), request.getRedirectId());
        HomeBanner banner = new HomeBanner();
        apply(banner, request);
        return toResponse(homeBannerRepository.save(banner));
    }

    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public HomeBannerResponse update(Long id, HomeBannerUpsertRequest request) {
        validateRedirect(request.getRedirectType(), request.getRedirectId());
        HomeBanner banner = findById(id);
        apply(banner, request);
        return toResponse(homeBannerRepository.save(banner));
    }

    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void delete(Long id) {
        HomeBanner banner = findById(id);
        homeBannerRepository.delete(banner);
    }

    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public List<HomeBannerResponse> reorder(HomeBannerReorderRequest request) {
        Set<Long> seenIds = new HashSet<>();
        for (HomeBannerReorderItemRequest item : request.getItems()) {
            if (!seenIds.add(item.getId())) {
                throw new BadRequestException("Duplicate banner id in reorder payload: " + item.getId());
            }
            HomeBanner banner = findById(item.getId());
            banner.setPriority(item.getPriority());
        }
        return adminBanners(null);
    }

    private HomeBanner findById(Long id) {
        return homeBannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Home banner not found: " + id));
    }

    private void apply(HomeBanner banner, HomeBannerUpsertRequest request) {
        banner.setTitle(request.getTitle().trim());
        banner.setSubtitle(normalize(request.getSubtitle()));
        banner.setImageUrl(mediaUrlResolver.toStorablePath(request.getImageUrl()));
        banner.setRedirectType(request.getRedirectType());
        banner.setRedirectId(request.getRedirectId());
        banner.setPriority(request.getPriority());
        banner.setActive(Boolean.TRUE.equals(request.getActive()));
        banner.setBannerType(normalize(request.getBannerType()));
        banner.setAudience(request.getAudience());
    }

    private HomeBannerResponse toResponse(HomeBanner banner) {
        return HomeBannerResponse.builder()
                .id(banner.getId())
                .title(banner.getTitle())
                .subtitle(banner.getSubtitle())
                .imageUrl(mediaUrlResolver.toPublicUrl(banner.getImageUrl()))
                .redirectType(banner.getRedirectType())
                .redirectId(banner.getRedirectId())
                .priority(banner.getPriority())
                .active(banner.isActive())
                .bannerType(banner.getBannerType())
                .audience(banner.getAudience())
                .createdAt(banner.getCreatedAt())
                .updatedAt(banner.getUpdatedAt())
                .build();
    }

    private void validateRedirect(HomeBannerRedirectType redirectType, Long redirectId) {
        if (redirectType == HomeBannerRedirectType.EXTERNAL || redirectType == HomeBannerRedirectType.NONE) {
            return;
        }
        if (redirectId == null) {
            throw new BadRequestException("redirect_id is required for redirect_type " + redirectType);
        }
    }

    private String normalize(String value) {
        return StringSanitizers.trimToNull(value);
    }
}

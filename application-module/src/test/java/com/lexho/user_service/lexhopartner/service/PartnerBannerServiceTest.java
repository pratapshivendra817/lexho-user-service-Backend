package com.lexho.user_service.lexhopartner.service;

import com.lexho.user_service.homebanner.entity.HomeBanner;
import com.lexho.user_service.homebanner.repository.HomeBannerRepository;
import com.lexho.user_service.localization.dynamic.DynamicContentTranslationService;
import com.lexho.user_service.util.MediaUrlResolver;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PartnerBannerServiceTest {

    @Test
    void clampsPagingMapsActivePartnerBannersAndResolvesMediaUrl() {
        HomeBannerRepository repository = mock(HomeBannerRepository.class);
        MediaUrlResolver resolver = mock(MediaUrlResolver.class);
        DynamicContentTranslationService translationService = mock(DynamicContentTranslationService.class);
        HomeBanner banner = new HomeBanner();
        banner.setId(1L);
        banner.setTitle("Work");
        banner.setSubtitle("Today");
        banner.setImageUrl("banner.jpg");
        when(repository.findTopActiveByAudienceIn(anyList(), argThat((Pageable p) ->
                p.getPageNumber() == 0 && p.getPageSize() == 5))).thenReturn(List.of(banner));
        when(resolver.toPublicUrl("banner.jpg")).thenReturn("https://cdn/banner.jpg");
        when(translationService.translateForPartner(10L, null, "home_banner", 1L, "title", "Work"))
                .thenReturn("Kaam");
        when(translationService.translateForPartner(10L, null, "home_banner", 1L, "subtitle", "Today"))
                .thenReturn("Aaj");

        var response = new PartnerBannerService(repository, resolver, translationService).partnerBanners(10L, -1, 100);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).title()).isEqualTo("Kaam");
        assertThat(response.content().get(0).subtitle()).isEqualTo("Aaj");
        assertThat(response.content().get(0).bannerImageUrl()).isEqualTo("https://cdn/banner.jpg");
    }
}

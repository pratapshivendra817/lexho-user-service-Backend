package com.lexho.user_service.homebanner.service;

import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.homebanner.dto.HomeBannerReorderItemRequest;
import com.lexho.user_service.homebanner.dto.HomeBannerReorderRequest;
import com.lexho.user_service.homebanner.dto.HomeBannerUpsertRequest;
import com.lexho.user_service.homebanner.entity.HomeBanner;
import com.lexho.user_service.homebanner.entity.HomeBannerAudience;
import com.lexho.user_service.homebanner.entity.HomeBannerRedirectType;
import com.lexho.user_service.homebanner.repository.HomeBannerRepository;
import com.lexho.user_service.util.MediaUrlResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HomeBannerServiceTest {
    @Mock HomeBannerRepository repository;
    private HomeBannerService service;

    @BeforeEach
    void setUp() {
        service = new HomeBannerService(repository, new MediaUrlResolver());
    }

    @Test
    void customerAndAdminListsMapStoredPaths() {
        HomeBanner banner = banner(1L);
        when(repository.findTopActiveByAudienceIn(anyList(), any())).thenReturn(List.of(banner));
        assertTrue(service.getCustomerBanners().get(0).getImageUrl().endsWith("/images/banner.png"));

        when(repository.findAllByOrderByPriorityAscIdAsc()).thenReturn(List.of(banner));
        assertEquals(1, service.getAdminBanners(null).size());
        when(repository.findByAudienceOrderByPriorityAscIdAsc(HomeBannerAudience.CUSTOMER)).thenReturn(List.of(banner));
        assertEquals(1, service.getAdminBanners(HomeBannerAudience.CUSTOMER).size());
    }

    @Test
    void createValidatesRedirectAndNormalizesFields() {
        HomeBannerUpsertRequest invalid = request(HomeBannerRedirectType.SERVICE, null);
        assertThrows(BadRequestException.class, () -> service.create(invalid));

        when(repository.save(any())).thenAnswer(i -> {
            HomeBanner banner = i.getArgument(0); banner.setId(8L); return banner;
        });
        var response = service.create(request(HomeBannerRedirectType.EXTERNAL, null));
        assertEquals(8L, response.getId());
        assertEquals("Title", response.getTitle());
        assertNull(response.getSubtitle());
        assertTrue(response.getImageUrl().endsWith("/uploads/banner.png"));
    }

    @Test
    void updateDeleteAndMissingBannerPaths() {
        HomeBanner banner = banner(2L);
        when(repository.findById(2L)).thenReturn(Optional.of(banner));
        when(repository.save(banner)).thenReturn(banner);
        assertEquals(4, service.update(2L, request(HomeBannerRedirectType.NONE, null)).getPriority());
        service.delete(2L);
        verify(repository).delete(banner);

        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.delete(99L));
    }

    @Test
    void reorderUpdatesPrioritiesAndRejectsDuplicateIds() {
        HomeBanner first = banner(1L); HomeBanner second = banner(2L);
        when(repository.findById(1L)).thenReturn(Optional.of(first));
        when(repository.findById(2L)).thenReturn(Optional.of(second));
        when(repository.findAllByOrderByPriorityAscIdAsc()).thenReturn(List.of(first, second));
        var request = reorder(item(1L, 20), item(2L, 10));
        assertEquals(2, service.reorder(request).size());
        assertEquals(20, first.getPriority());

        HomeBannerReorderRequest duplicate = reorder(item(1L, 1), item(1L, 2));
        assertThrows(BadRequestException.class, () -> service.reorder(duplicate));
    }

    private HomeBannerUpsertRequest request(HomeBannerRedirectType type, Long redirectId) {
        HomeBannerUpsertRequest request = new HomeBannerUpsertRequest();
        request.setTitle(" Title "); request.setSubtitle(" ");
        request.setImageUrl("https://cdn.test/uploads/banner.png"); request.setRedirectType(type);
        request.setRedirectId(redirectId); request.setPriority(4); request.setActive(true);
        request.setBannerType(" promo "); request.setAudience(HomeBannerAudience.CUSTOMER);
        return request;
    }

    private HomeBanner banner(Long id) {
        HomeBanner banner = new HomeBanner();
        banner.setId(id); banner.setTitle("Banner"); banner.setImageUrl("/images/banner.png");
        banner.setRedirectType(HomeBannerRedirectType.NONE); banner.setPriority(1);
        banner.setActive(true); banner.setAudience(HomeBannerAudience.CUSTOMER);
        return banner;
    }

    private HomeBannerReorderItemRequest item(Long id, int priority) {
        HomeBannerReorderItemRequest item = new HomeBannerReorderItemRequest();
        item.setId(id); item.setPriority(priority); return item;
    }

    private HomeBannerReorderRequest reorder(HomeBannerReorderItemRequest... items) {
        HomeBannerReorderRequest request = new HomeBannerReorderRequest();
        request.setItems(List.of(items)); return request;
    }
}

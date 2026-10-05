package com.lexho.user_service.socialmedia;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.user_service.admin.service.AdminAuditService;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.security.SecurityUtils;
import com.lexho.user_service.socialmedia.dto.SocialMediaLinkRequest;
import com.lexho.user_service.socialmedia.dto.SocialMediaReorderRequest;
import com.lexho.user_service.socialmedia.dto.SocialMediaStatusRequest;
import com.lexho.user_service.socialmedia.entity.SocialMediaLink;
import com.lexho.user_service.socialmedia.enums.SocialMediaPlatform;
import com.lexho.user_service.socialmedia.mapper.SocialMediaLinkMapper;
import com.lexho.user_service.socialmedia.repository.SocialMediaLinkRepository;
import com.lexho.user_service.socialmedia.service.SocialMediaManagementServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SocialMediaManagementServiceTest {
    @Mock
    private SocialMediaLinkRepository repository;
    @Mock
    private AdminAuditService adminAuditService;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    private SocialMediaManagementServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(42L));
        service = new SocialMediaManagementServiceImpl(
                repository,
                new SocialMediaLinkMapper(),
                adminAuditService,
                securityUtils,
                new ObjectMapper(),
                redisTemplateProvider
        );
    }

    @Test
    void createShouldPersistTrimmedFieldsAndAudit() {
        SocialMediaLinkRequest request = request(SocialMediaPlatform.INSTAGRAM, " https://instagram.com/lexho ");
        when(repository.existsByPlatform(SocialMediaPlatform.INSTAGRAM)).thenReturn(false);
        when(repository.save(any(SocialMediaLink.class))).thenAnswer(invocation -> {
            SocialMediaLink link = invocation.getArgument(0);
            ReflectionTestUtils.setField(link, "id", 7L);
            link.onCreate();
            return link;
        });

        var response = service.create(request);

        assertEquals(7L, response.getId());
        assertEquals("https://instagram.com/lexho", response.getUrl());
        assertEquals(42L, response.getCreatedBy());
        verify(adminAuditService).log(42L, "SOCIAL_MEDIA_CREATE", "social_media_links", 7L, "platform=INSTAGRAM");
    }

    @Test
    void createShouldRejectDuplicatePlatformAndInvalidUrl() {
        SocialMediaLinkRequest request = request(SocialMediaPlatform.FACEBOOK, "https://facebook.com/lexho");
        when(repository.existsByPlatform(SocialMediaPlatform.FACEBOOK)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> service.create(request));

        request.setUrl("ftp://facebook.com/lexho");
        assertThrows(BadRequestException.class, () -> service.create(request));
        verify(repository, never()).save(any());
    }

    @Test
    void getUpdateStatusDeleteAndListShouldUseRepositoryAndAudit() {
        SocialMediaLink link = link(3L, SocialMediaPlatform.YOUTUBE, true, 2);
        when(repository.findById(3L)).thenReturn(Optional.of(link));
        when(repository.existsByPlatformAndIdNot(SocialMediaPlatform.YOUTUBE, 3L)).thenReturn(false);
        when(repository.save(link)).thenReturn(link);
        when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(link)));

        assertEquals(1, service.getAdminLinks(0, 1000, null, null).getData().size());
        assertEquals(SocialMediaPlatform.YOUTUBE, service.getById(3L).getPlatform());

        var update = request(SocialMediaPlatform.YOUTUBE, "https://youtube.com/@lexho");
        update.setIsActive(false);
        assertFalse(service.update(3L, update).isActive());

        SocialMediaStatusRequest statusRequest = new SocialMediaStatusRequest();
        statusRequest.setIsActive(true);
        assertTrue(service.updateStatus(3L, statusRequest).isActive());

        service.delete(3L);

        verify(adminAuditService).log(42L, "SOCIAL_MEDIA_UPDATE", "social_media_links", 3L,
                "platform=YOUTUBE,active=false");
        verify(adminAuditService).log(42L, "SOCIAL_MEDIA_STATUS_UPDATE", "social_media_links", 3L,
                "active=true");
        verify(adminAuditService).log(42L, "SOCIAL_MEDIA_DELETE", "social_media_links", 3L,
                "platform=YOUTUBE");
    }

    @Test
    void updateShouldRejectMissingAndDuplicatePlatform() {
        SocialMediaLinkRequest request = request(SocialMediaPlatform.X, "https://x.com/lexho");
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.update(99L, request));

        when(repository.findById(2L)).thenReturn(Optional.of(link(2L, SocialMediaPlatform.X, true, 1)));
        when(repository.existsByPlatformAndIdNot(SocialMediaPlatform.X, 2L)).thenReturn(true);
        assertThrows(BadRequestException.class, () -> service.update(2L, request));
    }

    @Test
    void reorderShouldRejectDuplicatesAndPersistOrdering() {
        SocialMediaLink first = link(1L, SocialMediaPlatform.INSTAGRAM, true, 1);
        SocialMediaLink second = link(2L, SocialMediaPlatform.FACEBOOK, true, 2);
        when(repository.findAllById(any(Iterable.class))).thenReturn(List.of(first, second));
        when(repository.findAll(any(org.springframework.data.domain.Sort.class))).thenReturn(List.of(second, first));

        var firstRow = reorderRow(1L, 2);
        var secondRow = reorderRow(2L, 1);
        var result = service.reorder(List.of(firstRow, secondRow));

        assertEquals(2, first.getDisplayOrder());
        assertEquals(1, second.getDisplayOrder());
        assertEquals(2, result.size());
        verify(adminAuditService).log(42L, "SOCIAL_MEDIA_REORDER", "social_media_links", null, "count=2");

        assertThrows(BadRequestException.class, () -> service.reorder(List.of(reorderRow(1L, 1), reorderRow(1L, 2))));
        assertThrows(BadRequestException.class, () -> service.reorder(List.of(reorderRow(1L, 1), reorderRow(2L, 1))));
    }

    @Test
    void publicApiShouldReturnOnlyActiveOrderedFields() {
        SocialMediaLink active = link(4L, SocialMediaPlatform.LINKEDIN, true, 1);
        when(repository.findByActiveTrueOrderByDisplayOrderAscIdAsc()).thenReturn(List.of(active));

        var response = service.getActivePublicLinks();

        assertEquals(1, response.size());
        assertEquals(SocialMediaPlatform.LINKEDIN, response.get(0).getPlatform());
        assertEquals(1, response.get(0).getDisplayOrder());
    }

    private SocialMediaLinkRequest request(SocialMediaPlatform platform, String url) {
        SocialMediaLinkRequest request = new SocialMediaLinkRequest();
        request.setPlatform(platform);
        request.setUrl(url);
        request.setLabel(" Lexho ");
        request.setIcon(" icon ");
        request.setDisplayOrder(1);
        request.setIsActive(true);
        return request;
    }

    private SocialMediaReorderRequest reorderRow(Long id, int displayOrder) {
        SocialMediaReorderRequest request = new SocialMediaReorderRequest();
        request.setId(id);
        request.setDisplayOrder(displayOrder);
        return request;
    }

    private SocialMediaLink link(Long id, SocialMediaPlatform platform, boolean active, int displayOrder) {
        SocialMediaLink link = new SocialMediaLink();
        ReflectionTestUtils.setField(link, "id", id);
        link.setPlatform(platform);
        link.setUrl("https://example.com/" + platform.name().toLowerCase());
        link.setLabel(platform.name());
        link.setIcon(platform.name().toLowerCase());
        link.setActive(active);
        link.setDisplayOrder(displayOrder);
        ReflectionTestUtils.setField(link, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(link, "updatedAt", LocalDateTime.now());
        return link;
    }
}

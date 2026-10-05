package com.lexho.user_service.socialmedia.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.user_service.admin.service.AdminAuditService;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.security.SecurityUtils;
import com.lexho.user_service.socialmedia.dto.PublicSocialMediaLinkResponse;
import com.lexho.user_service.socialmedia.dto.SocialMediaLinkRequest;
import com.lexho.user_service.socialmedia.dto.SocialMediaLinkResponse;
import com.lexho.user_service.socialmedia.dto.SocialMediaPageResponse;
import com.lexho.user_service.socialmedia.dto.SocialMediaReorderRequest;
import com.lexho.user_service.socialmedia.dto.SocialMediaStatusRequest;
import com.lexho.user_service.socialmedia.entity.SocialMediaLink;
import com.lexho.user_service.socialmedia.enums.SocialMediaPlatform;
import com.lexho.user_service.socialmedia.mapper.SocialMediaLinkMapper;
import com.lexho.user_service.socialmedia.repository.SocialMediaLinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SocialMediaManagementServiceImpl implements SocialMediaManagementService {
    private static final String CACHE_KEY = "lexho:social-media:active";
    private static final String ENTITY_NAME = "social_media_links";
    private static final String PLATFORM_AUDIT_PREFIX = "platform=";

    private final SocialMediaLinkRepository repository;
    private final SocialMediaLinkMapper mapper;
    private final AdminAuditService adminAuditService;
    private final SecurityUtils securityUtils;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    @Override
    @Transactional(readOnly = true)
    public SocialMediaPageResponse getAdminLinks(int page, int size, Boolean active, SocialMediaPlatform platform) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageable = PageRequest.of(safePage, safeSize, socialMediaSort());
        Page<SocialMediaLink> result;
        if (platform != null && active != null) {
            result = repository.findByPlatformAndActive(platform, active, pageable);
        } else if (platform != null) {
            result = repository.findByPlatform(platform, pageable);
        } else if (active != null) {
            result = repository.findByActive(active, pageable);
        } else {
            result = repository.findAll(pageable);
        }
        return new SocialMediaPageResponse(
                result.getContent().stream().map(mapper::toAdminResponse).toList(),
                result.getTotalElements(),
                result.getTotalPages(),
                safePage,
                safeSize
        );
    }

    @Override
    @Transactional(readOnly = true)
    public SocialMediaLinkResponse getById(Long id) {
        return mapper.toAdminResponse(findLink(id));
    }

    @Override
    @Transactional
    public SocialMediaLinkResponse create(SocialMediaLinkRequest request) {
        validateRequest(request);
        if (repository.existsByPlatform(request.getPlatform())) {
            throw new BadRequestException("Social media platform already exists: " + request.getPlatform());
        }
        Long adminId = currentAdminId();
        SocialMediaLink entity = new SocialMediaLink();
        mapper.applyRequest(entity, request);
        entity.setCreatedBy(adminId);
        entity.setUpdatedBy(adminId);
        SocialMediaLink saved = repository.save(entity);
        evictPublicCache();
        logAudit("SOCIAL_MEDIA_CREATE", saved.getId(), PLATFORM_AUDIT_PREFIX + saved.getPlatform());
        return mapper.toAdminResponse(saved);
    }

    @Override
    @Transactional
    public SocialMediaLinkResponse update(Long id, SocialMediaLinkRequest request) {
        validateRequest(request);
        SocialMediaLink entity = findLink(id);
        if (repository.existsByPlatformAndIdNot(request.getPlatform(), id)) {
            throw new BadRequestException("Social media platform already exists: " + request.getPlatform());
        }
        mapper.applyRequest(entity, request);
        entity.setUpdatedBy(currentAdminId());
        SocialMediaLink saved = repository.save(entity);
        evictPublicCache();
        logAudit("SOCIAL_MEDIA_UPDATE", saved.getId(),
                PLATFORM_AUDIT_PREFIX + saved.getPlatform() + ",active=" + saved.isActive());
        return mapper.toAdminResponse(saved);
    }

    @Override
    @Transactional
    public SocialMediaLinkResponse updateStatus(Long id, SocialMediaStatusRequest request) {
        SocialMediaLink entity = findLink(id);
        entity.setActive(Boolean.TRUE.equals(request.getIsActive()));
        entity.setUpdatedBy(currentAdminId());
        SocialMediaLink saved = repository.save(entity);
        evictPublicCache();
        logAudit("SOCIAL_MEDIA_STATUS_UPDATE", saved.getId(), "active=" + saved.isActive());
        return mapper.toAdminResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        SocialMediaLink entity = findLink(id);
        repository.delete(entity);
        evictPublicCache();
        logAudit("SOCIAL_MEDIA_DELETE", id, PLATFORM_AUDIT_PREFIX + entity.getPlatform());
    }

    @Override
    @Transactional
    public List<SocialMediaLinkResponse> reorder(List<SocialMediaReorderRequest> request) {
        if (request == null || request.isEmpty()) {
            throw new BadRequestException("At least one reorder row is required");
        }
        Set<Long> ids = new HashSet<>();
        Set<Integer> orders = new HashSet<>();
        for (SocialMediaReorderRequest row : request) {
            if (!ids.add(row.getId())) {
                throw new BadRequestException("Duplicate social media id in reorder request: " + row.getId());
            }
            if (row.getDisplayOrder() == null || row.getDisplayOrder() < 1) {
                throw new BadRequestException("displayOrder must be greater than 0");
            }
            if (!orders.add(row.getDisplayOrder())) {
                throw new BadRequestException("Duplicate displayOrder in reorder request: " + row.getDisplayOrder());
            }
        }
        Map<Long, SocialMediaLink> links = repository.findAllById(ids).stream()
                .collect(Collectors.toMap(SocialMediaLink::getId, Function.identity()));
        Long adminId = currentAdminId();
        for (SocialMediaReorderRequest row : request) {
            SocialMediaLink link = links.get(row.getId());
            if (link == null) {
                throw new ResourceNotFoundException("Social media link not found: " + row.getId());
            }
            link.setDisplayOrder(row.getDisplayOrder());
            link.setUpdatedBy(adminId);
        }
        repository.saveAll(links.values());
        evictPublicCache();
        logAudit("SOCIAL_MEDIA_REORDER", null, "count=" + request.size());
        return repository.findAll(socialMediaSort()).stream().map(mapper::toAdminResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicSocialMediaLinkResponse> getActivePublicLinks() {
        List<PublicSocialMediaLinkResponse> cached = readCachedPublicLinks();
        if (cached != null) {
            return cached;
        }
        List<PublicSocialMediaLinkResponse> links = repository.findByActiveTrueOrderByDisplayOrderAscIdAsc().stream()
                .map(mapper::toPublicResponse)
                .toList();
        writeCachedPublicLinks(links);
        return links;
    }

    private void validateRequest(SocialMediaLinkRequest request) {
        validateHttpUrl(request.getUrl());
        if (request.getLabel() != null) {
            request.setLabel(request.getLabel().trim());
        }
        if (request.getIcon() != null) {
            request.setIcon(request.getIcon().trim());
        }
        if (request.getUrl() != null) {
            request.setUrl(request.getUrl().trim());
        }
    }

    private void validateHttpUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new BadRequestException("url is required");
        }
        try {
            URI uri = URI.create(url.trim());
            String scheme = uri.getScheme();
            if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    || uri.getHost() == null
                    || uri.getHost().isBlank()) {
                throw new BadRequestException("url must be a valid HTTP/HTTPS URL");
            }
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("url must be a valid HTTP/HTTPS URL");
        }
    }

    private SocialMediaLink findLink(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Social media link not found: " + id));
    }

    private Long currentAdminId() {
        return securityUtils.getCurrentUserId().orElse(0L);
    }

    private void logAudit(String action, Long entityId, String metaData) {
        adminAuditService.log(currentAdminId(), action, ENTITY_NAME, entityId, metaData);
    }

    private void evictPublicCache() {
        try {
            StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
            if (redisTemplate != null) {
                redisTemplate.delete(CACHE_KEY);
            }
        } catch (Exception ex) {
            log.warn("Redis unavailable while evicting social media cache");
        }
    }

    private List<PublicSocialMediaLinkResponse> readCachedPublicLinks() {
        try {
            StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
            if (redisTemplate == null) {
                return null;
            }
            String payload = redisTemplate.opsForValue().get(CACHE_KEY);
            if (payload == null || payload.isBlank()) {
                return null;
            }
            return objectMapper.readValue(payload, new TypeReference<>() {
            });
        } catch (Exception ex) {
            log.warn("Redis unavailable while reading social media cache");
            return null;
        }
    }

    private void writeCachedPublicLinks(List<PublicSocialMediaLinkResponse> links) {
        try {
            StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
            if (redisTemplate != null) {
                redisTemplate.opsForValue().set(CACHE_KEY, objectMapper.writeValueAsString(links), 10, TimeUnit.MINUTES);
            }
        } catch (Exception ex) {
            log.warn("Redis unavailable while writing social media cache");
        }
    }

    private Sort socialMediaSort() {
        return Sort.by(Sort.Order.asc("displayOrder"), Sort.Order.asc("id"));
    }
}

package com.lexho.user_service.customer.banner.service;

import com.lexho.user_service.admin.dto.AdminMessageResponse;
import com.lexho.user_service.admin.dto.DataResponse;
import com.lexho.user_service.admin.dto.PagedDataResponse;
import com.lexho.user_service.customer.banner.dto.AdminCustomerBannerResponse;
import com.lexho.user_service.customer.banner.dto.AdminCustomerBannerUpsertRequest;
import com.lexho.user_service.customer.banner.entity.BannerRedirectType;
import com.lexho.user_service.customer.banner.entity.CustomerBannerEntity;
import com.lexho.user_service.customer.banner.repository.CustomerBannerRepository;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.util.MediaUrlResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminCustomerBannerService {

    private final CustomerBannerRepository customerBannerRepository;
    private final MediaUrlResolver mediaUrlResolver;

    @Transactional
    @CacheEvict(value = "customerBanners", allEntries = true)
    public DataResponse<AdminCustomerBannerResponse> create(AdminCustomerBannerUpsertRequest request) {
        validateRequest(request);
        CustomerBannerEntity entity = new CustomerBannerEntity();
        apply(entity, request);
        return new DataResponse<>(toResponse(customerBannerRepository.save(entity)));
    }

    @Transactional
    @CacheEvict(value = "customerBanners", allEntries = true)
    public DataResponse<AdminCustomerBannerResponse> update(Long id, AdminCustomerBannerUpsertRequest request) {
        validateRequest(request);
        CustomerBannerEntity entity = getById(id);
        apply(entity, request);
        return new DataResponse<>(toResponse(customerBannerRepository.save(entity)));
    }

    @Transactional(readOnly = true)
    public DataResponse<AdminCustomerBannerResponse> byId(Long id) {
        return new DataResponse<>(toResponse(getById(id)));
    }

    @Transactional(readOnly = true)
    public PagedDataResponse<AdminCustomerBannerResponse> list(int page, int limit, Long categoryId, Boolean active) {
        int safePage = Math.max(page, 1) - 1;
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        Page<CustomerBannerEntity> result = customerBannerRepository.searchForAdmin(
                categoryId,
                active,
                PageRequest.of(safePage, safeLimit, Sort.by(Sort.Direction.ASC, "priority").and(Sort.by(Sort.Direction.DESC, "id")))
        );

        return new PagedDataResponse<>(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getTotalElements()
        );
    }

    @Transactional
    @CacheEvict(value = "customerBanners", allEntries = true)
    public DataResponse<AdminCustomerBannerResponse> updateStatus(Long id, boolean isActive) {
        CustomerBannerEntity entity = getById(id);
        entity.setActive(isActive);
        return new DataResponse<>(toResponse(customerBannerRepository.save(entity)));
    }

    @Transactional
    @CacheEvict(value = "customerBanners", allEntries = true)
    public AdminMessageResponse delete(Long id) {
        CustomerBannerEntity entity = getById(id);
        customerBannerRepository.delete(entity);
        return new AdminMessageResponse("Banner deleted successfully");
    }

    private void apply(CustomerBannerEntity entity, AdminCustomerBannerUpsertRequest request) {
        entity.setCategoryId(request.getCategoryId());
        entity.setTitle(request.getTitle().trim());
        String imageUrl = normalize(request.getImageUrl());
        if (imageUrl == null) {
            imageUrl = normalize(request.getBannerImageUrl());
        }
        entity.setImageUrl(mediaUrlResolver.toStorablePath(imageUrl));
        entity.setRedirectType(request.getRedirectType());
        entity.setRedirectId(resolveRedirectId(request));
        entity.setRedirectUrl(normalize(request.getRedirectUrl()));
        entity.setActive(Boolean.TRUE.equals(request.getActive()));
        entity.setPriority(request.getPriority());
    }

    private AdminCustomerBannerResponse toResponse(CustomerBannerEntity entity) {
        return AdminCustomerBannerResponse.builder()
                .id(entity.getId())
                .categoryId(entity.getCategoryId())
                .title(entity.getTitle())
                .imageUrl(mediaUrlResolver.toPublicUrl(entity.getImageUrl()))
                .redirectType(entity.getRedirectType())
                .redirectId(entity.getRedirectId())
                .redirectUrl(entity.getRedirectUrl())
                .active(entity.isActive())
                .priority(entity.getPriority())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private CustomerBannerEntity getById(Long id) {
        return customerBannerRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Banner not found"));
    }

    private void validateRequest(AdminCustomerBannerUpsertRequest request) {
        if (normalize(request.getImageUrl()) == null && normalize(request.getBannerImageUrl()) == null) {
            throw new BadRequestException("imageUrl is required");
        }
        if (request.getRedirectType() == BannerRedirectType.EXTERNAL) {
            if (normalize(request.getRedirectUrl()) == null) {
                throw new BadRequestException("redirectUrl is required for EXTERNAL redirectType");
            }
            return;
        }
        if (resolveRedirectId(request) == null) {
            throw new BadRequestException("redirectId is required for non-EXTERNAL redirectType");
        }
    }

    private Long resolveRedirectId(AdminCustomerBannerUpsertRequest request) {
        if (request.getRedirectId() != null) {
            return request.getRedirectId();
        }
        String redirectValue = normalize(request.getRedirectValue());
        if (redirectValue == null) {
            return null;
        }
        try {
            return Long.parseLong(redirectValue);
        } catch (NumberFormatException ex) {
            throw new BadRequestException("redirectValue must be numeric for non-EXTERNAL redirectType");
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

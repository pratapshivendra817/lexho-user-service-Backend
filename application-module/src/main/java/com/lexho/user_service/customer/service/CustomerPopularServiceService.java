package com.lexho.user_service.customer.service;

import com.lexho.user_service.admin.entity.AdminCategoryEntity;
import com.lexho.user_service.admin.repository.AdminCategoryRepository;
import com.lexho.user_service.customer.dto.CustomerPopularServiceResponse;
import com.lexho.user_service.customer.repository.CustomerPopularServiceRepository;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.util.MediaUrlResolver;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomerPopularServiceService {

    private static final Logger log = LoggerFactory.getLogger(CustomerPopularServiceService.class);

    private final CustomerPopularServiceRepository customerPopularServiceRepository;
    private final AdminCategoryRepository adminCategoryRepository;
    private final MediaUrlResolver mediaUrlResolver;

    @Transactional(readOnly = true)
    @Cacheable(value = "customerPopularServices", key = "'popular_services_' + #categoryId + '_' + #page + '_' + #size")
    public List<CustomerPopularServiceResponse> getPopularServices(Long categoryId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        if (categoryId == null) {
            throw new BadRequestException("categoryId is required");
        }

        log.info("Fetching popular services: categoryId={}, page={}, size={}", categoryId, safePage, safeSize);
        List<CustomerPopularServiceResponse> response = customerPopularServiceRepository
                .findActivePopularServicesByCategory(categoryId, pageable)
                .stream()
                .map(row -> CustomerPopularServiceResponse.builder()
                        .id(row.getId())
                        .name(row.getName())
                        .imageUrl(mediaUrlResolver.toPublicUrl(row.getImageUrl()))
                        .price(row.getPrice())
                        .rating(row.getRating() == null ? 0.0 : row.getRating())
                        .categoryId(row.getCategoryId())
                        .build())
                .toList();

        log.info("Fetched {} popular services for categoryId={}, page={}, size={}",
                response.size(), categoryId, safePage, safeSize);
        return response;
    }

    @Transactional(readOnly = true)
    public List<CustomerPopularServiceResponse> getPopularServices(Long categoryId) {
        return getPopularServices(categoryId, 0, 20);
    }

    @Transactional(readOnly = true)
    public List<CustomerPopularServiceResponse> getPopularCookServices() {
        AdminCategoryEntity cookCategory = resolveCategory("cook");

        if (!cookCategory.isActive()) {
            throw new BadRequestException("Cook category is inactive");
        }

        return getPopularServices(cookCategory.getId());
    }

    @Transactional(readOnly = true)
    public List<CustomerPopularServiceResponse> getPopularCleaningServices() {
        AdminCategoryEntity cleaningCategory = resolveCategory("cleaning");

        if (!cleaningCategory.isActive()) {
            throw new BadRequestException("Cleaning category is inactive");
        }

        return getPopularServices(cleaningCategory.getId());
    }

    @Transactional(readOnly = true)
    public List<CustomerPopularServiceResponse> getPopularServicesByCategoryKey(String categoryKey) {
        AdminCategoryEntity category = resolveCategory(categoryKey);
        if (!category.isActive()) {
            throw new BadRequestException("Category is inactive");
        }
        return getPopularServices(category.getId());
    }

    private AdminCategoryEntity resolveCategory(String categoryKey) {
        if (categoryKey == null || categoryKey.trim().isEmpty()) {
            throw new BadRequestException("category is required");
        }

        String normalized = categoryKey.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("cook")) {
            return resolveByAliases(
                    List.of("Cook at home", "Cook", "Home Cook"),
                    List.of("cook"),
                    "Cook category not found"
            );
        }
        if (normalized.equals("cleaning")) {
            return resolveByAliases(
                    List.of("Cleaning", "Cleaning at home", "Home Cleaning"),
                    List.of("cleaning", "clean"),
                    "Cleaning category not found"
            );
        }

        String titleCase = toTitleCase(categoryKey.replace('-', ' '));
        return adminCategoryRepository.findByNameIgnoreCase(titleCase).filter(category -> !category.isDeleted())
                .or(() -> adminCategoryRepository.findFirstByNameContainingIgnoreCaseAndDeletedFalse(titleCase))
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryKey));
    }

    private AdminCategoryEntity resolveByAliases(List<String> exactAliases,
                                                 List<String> containsKeywords,
                                                 String notFoundMessage) {
        for (String alias : exactAliases) {
            Optional<AdminCategoryEntity> category = adminCategoryRepository.findByNameIgnoreCase(alias)
                    .filter(c -> !c.isDeleted());
            if (category.isPresent()) {
                return category.get();
            }
        }
        for (String keyword : containsKeywords) {
            Optional<AdminCategoryEntity> category = adminCategoryRepository
                    .findFirstByNameContainingIgnoreCaseAndDeletedFalse(keyword);
            if (category.isPresent()) {
                return category.get();
            }
        }
        throw new ResourceNotFoundException(notFoundMessage);
    }

    private String toTitleCase(String input) {
        String[] parts = input.trim().toLowerCase(Locale.ROOT).split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                out.append(part.substring(1));
            }
        }
        return out.toString();
    }
}

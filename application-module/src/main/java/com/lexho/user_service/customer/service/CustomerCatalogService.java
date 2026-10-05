package com.lexho.user_service.customer.service;

import com.lexho.user_service.admin.entity.AdminCategoryEntity;
import com.lexho.user_service.admin.entity.AdminServiceEntity;
import com.lexho.user_service.admin.repository.AdminCategoryRepository;
import com.lexho.user_service.customer.dto.CustomerServiceCardResponse;
import com.lexho.user_service.customer.repository.CustomerCatalogRepository;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.util.MediaUrlResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerCatalogService {

    private final CustomerCatalogRepository customerCatalogRepository;
    private final AdminCategoryRepository adminCategoryRepository;
    private final MediaUrlResolver mediaUrlResolver;
    private final CustomerServiceBookingModeMapper bookingModeMapper;

    public List<CustomerServiceCardResponse> getServicesByCategory(Long categoryId, int page, int size) {
        if (categoryId == null) {
            throw new BadRequestException("categoryId is required");
        }

        AdminCategoryEntity category = adminCategoryRepository.findByIdAndDeletedFalse(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        if (!category.isActive()) {
            throw new BadRequestException("Category is inactive");
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        return customerCatalogRepository
                .findByCategory_IdAndActiveTrueAndDeletedFalseOrderByNameAscIdAsc(categoryId, pageable)
                .stream()
                .map(this::toCardResponse)
                .toList();
    }

    private CustomerServiceCardResponse toCardResponse(AdminServiceEntity service) {
        String imageUrl = mediaUrlResolver.toPublicUrl(service.getImageUrl());
        List<String> bookingModes = bookingModeMapper.bookingModes(service);
        java.math.BigDecimal effectivePrice = effectivePrice(service);
        return CustomerServiceCardResponse.builder()
                .id(service.getId())
                .serviceId(service.getId())
                .name(service.getName())
                .serviceName(service.getName())
                .description(service.getDescription())
                .imageUrl(imageUrl == null ? "" : imageUrl)
                .price(effectivePrice)
                .discountedPrice(effectivePrice)
                .rating(service.getAvgRating())
                .totalReviews(service.getTotalReviews())
                .isNew(service.isNew())
                .bookingModes(bookingModes)
                .hourlyRate(bookingModeMapper.hourlyRate(service, bookingModes))
                .minHours(bookingModeMapper.minHours(bookingModes))
                .maxHours(bookingModeMapper.maxHours(bookingModes))
                .hourStep(bookingModeMapper.hourStep(bookingModes))
                .build();
    }

    private java.math.BigDecimal effectivePrice(AdminServiceEntity service) {
        if (service.getDiscountedPrice() != null && service.getDiscountedPrice().signum() > 0) {
            return service.getDiscountedPrice();
        }
        return service.getBasePrice();
    }
}

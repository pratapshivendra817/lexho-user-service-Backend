package com.lexho.user_service.customer.service;

import com.lexho.user_service.admin.entity.AdminCategoryEntity;
import com.lexho.user_service.admin.repository.AdminCategoryRepository;
import com.lexho.user_service.customer.repository.CustomerPopularServiceRepository;
import com.lexho.user_service.customer.repository.CustomerPopularServiceView;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.util.MediaUrlResolver;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomerPopularServiceServiceAdditionalTest {

    private final CustomerPopularServiceRepository popularRepository = mock(CustomerPopularServiceRepository.class);
    private final AdminCategoryRepository categoryRepository = mock(AdminCategoryRepository.class);
    private final MediaUrlResolver mediaUrlResolver = mock(MediaUrlResolver.class);
    private final CustomerPopularServiceService service =
            new CustomerPopularServiceService(popularRepository, categoryRepository, mediaUrlResolver);

    @Test
    void getPopularServicesShouldValidateCategoryIdAndClampPageSize() {
        CustomerPopularServiceView view = mock(CustomerPopularServiceView.class);
        when(view.getId()).thenReturn(1L);
        when(view.getName()).thenReturn("Deep Cleaning");
        when(view.getImageUrl()).thenReturn("/img.png");
        when(view.getPrice()).thenReturn(new BigDecimal("499.00"));
        when(view.getRating()).thenReturn(null);
        when(view.getCategoryId()).thenReturn(2L);
        when(mediaUrlResolver.toPublicUrl("/img.png")).thenReturn("/public/img.png");
        when(popularRepository.findActivePopularServicesByCategory(org.mockito.ArgumentMatchers.eq(2L), any(Pageable.class)))
                .thenReturn(List.of(view));

        var response = service.getPopularServices(2L, -1, 500);

        assertEquals(1, response.size());
        assertEquals("Deep Cleaning", response.get(0).getName());
        assertEquals(0.0, response.get(0).getRating());
        assertEquals("/public/img.png", response.get(0).getImageUrl());
    }

    @Test
    void getPopularServicesShouldRejectMissingCategoryId() {
        assertThrows(BadRequestException.class, () -> service.getPopularServices(null, 0, 20));
    }

    @Test
    void getPopularCookServicesShouldResolveAliasAndRejectInactiveCategory() {
        AdminCategoryEntity inactive = category(9L, false);
        when(categoryRepository.findByNameIgnoreCase("Cook at home")).thenReturn(Optional.of(inactive));

        assertThrows(BadRequestException.class, service::getPopularCookServices);
    }

    @Test
    void getPopularServicesByCategoryKeyShouldTitleCaseAndFallbackToContainsLookup() {
        AdminCategoryEntity category = category(12L, true);
        when(categoryRepository.findByNameIgnoreCase("Home Cleaning")).thenReturn(Optional.empty());
        when(categoryRepository.findFirstByNameContainingIgnoreCaseAndDeletedFalse("Home Cleaning"))
                .thenReturn(Optional.of(category));
        when(popularRepository.findActivePopularServicesByCategory(org.mockito.ArgumentMatchers.eq(12L), any(Pageable.class)))
                .thenReturn(List.of());

        assertEquals(List.of(), service.getPopularServicesByCategoryKey("home-cleaning"));
    }

    @Test
    void getPopularServicesByCategoryKeyShouldRejectBlankAndUnknownKey() {
        when(categoryRepository.findByNameIgnoreCase("Unknown")).thenReturn(Optional.empty());
        when(categoryRepository.findFirstByNameContainingIgnoreCaseAndDeletedFalse("Unknown"))
                .thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> service.getPopularServicesByCategoryKey(" "));
        assertThrows(ResourceNotFoundException.class, () -> service.getPopularServicesByCategoryKey("unknown"));
    }

    private AdminCategoryEntity category(Long id, boolean active) {
        AdminCategoryEntity category = new AdminCategoryEntity();
        ReflectionTestUtils.setField(category, "id", id);
        category.setName("Category " + id);
        category.setActive(active);
        category.setDeleted(false);
        return category;
    }
}

package com.lexho.user_service.customer.service;

import com.lexho.user_service.admin.entity.AdminCategoryEntity;
import com.lexho.user_service.admin.entity.AdminServiceEntity;
import com.lexho.user_service.admin.repository.AdminCategoryRepository;
import com.lexho.user_service.customer.dto.CustomerServiceCardResponse;
import com.lexho.user_service.customer.repository.CustomerCatalogRepository;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.util.MediaUrlResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerCatalogServiceTest {

    @Mock
    private CustomerCatalogRepository customerCatalogRepository;
    @Mock
    private AdminCategoryRepository adminCategoryRepository;
    @Mock
    private MediaUrlResolver mediaUrlResolver;
    @Mock
    private CustomerServiceBookingModeMapper bookingModeMapper;

    @InjectMocks
    private CustomerCatalogService customerCatalogService;

    @Test
    void shouldReturnServicesByCategory() {
        AdminCategoryEntity category = new AdminCategoryEntity();
        category.setActive(true);

        AdminServiceEntity service = new AdminServiceEntity();
        service.setName("Bathroom Cleaning");
        service.setBasePrice(new BigDecimal("150.00"));
        service.setDiscountedPrice(new BigDecimal("49.00"));
        service.setAvgRating(4.9);
        service.setTotalReviews(27000L);
        service.setNew(true);

        when(adminCategoryRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(category));
        when(customerCatalogRepository.findByCategory_IdAndActiveTrueAndDeletedFalse(
                ArgumentMatchers.eq(1L), ArgumentMatchers.any())).thenReturn(new PageImpl<>(List.of(service)));
        when(bookingModeMapper.bookingModes(service)).thenReturn(List.of("FIXED"));

        List<CustomerServiceCardResponse> result = customerCatalogService.getServicesByCategory(1L, 0, 20);

        assertEquals(1, result.size());
        assertEquals("Bathroom Cleaning", result.get(0).getName());
        assertEquals(new BigDecimal("49.00"), result.get(0).getPrice());
        assertEquals(new BigDecimal("49.00"), result.get(0).getDiscountedPrice());
        assertTrue(result.get(0).isNew());
    }

    @Test
    void shouldThrowWhenCategoryNotFound() {
        when(adminCategoryRepository.findByIdAndDeletedFalse(999L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> customerCatalogService.getServicesByCategory(999L, 0, 20));
    }

    @Test
    void shouldThrowWhenCategoryInactive() {
        AdminCategoryEntity category = new AdminCategoryEntity();
        category.setActive(false);
        when(adminCategoryRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(category));
        assertThrows(BadRequestException.class, () -> customerCatalogService.getServicesByCategory(1L, 0, 20));
    }
}

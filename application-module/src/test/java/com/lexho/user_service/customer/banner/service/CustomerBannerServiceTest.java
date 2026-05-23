package com.lexho.user_service.customer.banner.service;

import com.lexho.user_service.customer.banner.dto.CustomerBannerResponse;
import com.lexho.user_service.customer.banner.entity.BannerRedirectType;
import com.lexho.user_service.customer.banner.entity.CustomerBannerEntity;
import com.lexho.user_service.customer.banner.repository.CustomerBannerRepository;
import com.lexho.user_service.util.MediaUrlResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerBannerServiceTest {

    @Mock
    private CustomerBannerRepository customerBannerRepository;
    @Mock
    private MediaUrlResolver mediaUrlResolver;

    @InjectMocks
    private CustomerBannerService customerBannerService;

    @Test
    void shouldReturnCategoryAndGlobalBannersSortedFromRepository() {
        CustomerBannerEntity banner = new CustomerBannerEntity();
        banner.setId(1L);
        banner.setImageUrl("/images/banners/a.jpg");
        banner.setRedirectType(BannerRedirectType.SERVICE);
        banner.setRedirectId(12L);
        banner.setRedirectUrl(null);

        when(customerBannerRepository.findActiveByCategoryOrGlobal(
                ArgumentMatchers.eq(2L), ArgumentMatchers.any())).thenReturn(List.of(banner));
        when(mediaUrlResolver.toPublicUrl("/images/banners/a.jpg"))
                .thenReturn("http://localhost:8082/images/banners/a.jpg");

        List<CustomerBannerResponse> result = customerBannerService.getBanners(2L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals("http://localhost:8082/images/banners/a.jpg", result.get(0).getImageUrl());
        assertEquals(BannerRedirectType.SERVICE, result.get(0).getRedirectType());
        verify(customerBannerRepository).findActiveByCategoryOrGlobal(ArgumentMatchers.eq(2L), ArgumentMatchers.any());
    }

    @Test
    void shouldReturnOnlyGlobalWhenCategoryMissing() {
        when(customerBannerRepository.findActiveGlobal(ArgumentMatchers.any())).thenReturn(List.of());
        List<CustomerBannerResponse> result = customerBannerService.getBanners(null);
        assertEquals(0, result.size());
        verify(customerBannerRepository).findActiveGlobal(ArgumentMatchers.any());
    }
}

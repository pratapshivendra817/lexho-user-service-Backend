package com.lexho.user_service.customer.banner.service;

import com.lexho.user_service.customer.banner.dto.AdminCustomerBannerUpsertRequest;
import com.lexho.user_service.customer.banner.entity.BannerRedirectType;
import com.lexho.user_service.customer.banner.entity.CustomerBannerEntity;
import com.lexho.user_service.customer.banner.repository.CustomerBannerRepository;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.util.MediaUrlResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCustomerBannerServiceTest {

    @Mock
    private CustomerBannerRepository customerBannerRepository;
    @Mock
    private MediaUrlResolver mediaUrlResolver;

    @InjectMocks
    private AdminCustomerBannerService adminCustomerBannerService;

    @Test
    void shouldCreateBanner() {
        AdminCustomerBannerUpsertRequest request = new AdminCustomerBannerUpsertRequest();
        request.setTitle("Cleaning banner");
        request.setImageUrl("/images/banners/a.jpg");
        request.setRedirectType(BannerRedirectType.CATEGORY);
        request.setRedirectId(2L);
        request.setActive(true);
        request.setPriority(1);

        when(mediaUrlResolver.toStorablePath("/images/banners/a.jpg")).thenReturn("/images/banners/a.jpg");
        when(mediaUrlResolver.toPublicUrl("/images/banners/a.jpg")).thenReturn("http://localhost:8082/images/banners/a.jpg");
        when(customerBannerRepository.save(ArgumentMatchers.any(CustomerBannerEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = adminCustomerBannerService.create(request);
        assertEquals("Cleaning banner", response.getData().getTitle());
    }

    @Test
    void shouldFailWhenExternalWithoutUrl() {
        AdminCustomerBannerUpsertRequest request = new AdminCustomerBannerUpsertRequest();
        request.setTitle("External banner");
        request.setImageUrl("/images/banners/a.jpg");
        request.setRedirectType(BannerRedirectType.EXTERNAL);
        request.setActive(true);
        request.setPriority(1);

        assertThrows(BadRequestException.class, () -> adminCustomerBannerService.create(request));
    }
}

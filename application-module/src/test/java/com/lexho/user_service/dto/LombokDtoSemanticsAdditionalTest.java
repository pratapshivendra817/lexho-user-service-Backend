package com.lexho.user_service.dto;

import com.lexho.user_service.cookfamilysize.dto.CookFamilySizeAdminResponse;
import com.lexho.user_service.cookfamilysize.dto.CookFamilySizeStatusRequest;
import com.lexho.user_service.customer.banner.dto.AdminCustomerBannerResponse;
import com.lexho.user_service.customer.banner.dto.CustomerBannerResponse;
import com.lexho.user_service.customer.banner.entity.BannerRedirectType;
import com.lexho.user_service.customer.dto.CookFamilySizeResponse;
import com.lexho.user_service.customer.dto.CustomerCartItemResponse;
import com.lexho.user_service.customer.dto.CustomerCartResponse;
import com.lexho.user_service.customer.dto.CustomerPopularServiceResponse;
import com.lexho.user_service.customer.dto.CustomerServiceCardResponse;
import com.lexho.user_service.homebanner.dto.HomeBannerResponse;
import com.lexho.user_service.homebanner.entity.HomeBannerAudience;
import com.lexho.user_service.homebanner.entity.HomeBannerRedirectType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LombokDtoSemanticsAdditionalTest {

    @Test
    void lombokDtosCoverEqualsHashCodeToStringAndGeneratedConstructors() {
        LocalDateTime now = LocalDateTime.parse("2026-07-17T12:00:00");

        HomeBannerResponse firstBanner = HomeBannerResponse.builder()
                .id(1L).title("Hero").subtitle("Sub").imageUrl("/hero.png")
                .redirectType(HomeBannerRedirectType.SERVICE).redirectId(2L).priority(1).active(true)
                .bannerType("hero").audience(HomeBannerAudience.BOTH).createdAt(now).updatedAt(now).build();
        HomeBannerResponse secondBanner = HomeBannerResponse.builder()
                .id(1L).title("Hero").subtitle("Sub").imageUrl("/hero.png")
                .redirectType(HomeBannerRedirectType.SERVICE).redirectId(2L).priority(1).active(true)
                .bannerType("hero").audience(HomeBannerAudience.BOTH).createdAt(now).updatedAt(now).build();

        CustomerCartItemResponse itemOne = CustomerCartItemResponse.builder()
                .itemId(1L).serviceId(2L).name("Cook").totalPrice(100.0).build();
        CustomerCartItemResponse itemTwo = CustomerCartItemResponse.builder()
                .itemId(1L).serviceId(2L).name("Cook").totalPrice(100.0).build();

        CustomerCartResponse.Address addressOne = CustomerCartResponse.Address.builder()
                .id(3L).fullAddress("Pune").city("Pune").pincode("411001").build();
        CustomerCartResponse.Address addressTwo = CustomerCartResponse.Address.builder()
                .id(3L).fullAddress("Pune").city("Pune").pincode("411001").build();

        CustomerCartResponse cartOne = CustomerCartResponse.builder()
                .cartId(4L).grandTotal(100.0).address(addressOne).items(List.of(itemOne)).build();
        CustomerCartResponse cartTwo = CustomerCartResponse.builder()
                .cartId(4L).grandTotal(100.0).address(addressTwo).items(List.of(itemTwo)).build();

        CustomerPopularServiceResponse popularOne = CustomerPopularServiceResponse.builder()
                .id(5L).name("Clean").price(BigDecimal.TEN).rating(4.5).categoryId(6L).build();
        CustomerPopularServiceResponse popularTwo = CustomerPopularServiceResponse.builder()
                .id(5L).name("Clean").price(BigDecimal.TEN).rating(4.5).categoryId(6L).build();

        CustomerServiceCardResponse serviceOne = CustomerServiceCardResponse.builder()
                .id(7L).serviceId(8L).name("Cook").serviceName("Home Cook").isNew(true).build();
        CustomerServiceCardResponse serviceTwo = CustomerServiceCardResponse.builder()
                .id(7L).serviceId(8L).name("Cook").serviceName("Home Cook").isNew(true).build();

        AdminCustomerBannerResponse adminBannerOne = AdminCustomerBannerResponse.builder()
                .id(9L).title("Admin").redirectType(BannerRedirectType.CATEGORY).active(true).priority(1)
                .createdAt(now).updatedAt(now).build();
        AdminCustomerBannerResponse adminBannerTwo = AdminCustomerBannerResponse.builder()
                .id(9L).title("Admin").redirectType(BannerRedirectType.CATEGORY).active(true).priority(1)
                .createdAt(now).updatedAt(now).build();

        CustomerBannerResponse customerBannerOne = CustomerBannerResponse.builder()
                .id(10L).imageUrl("/banner.png").redirectType(BannerRedirectType.EXTERNAL).redirectUrl("https://lexho.com").build();
        CustomerBannerResponse customerBannerTwo = CustomerBannerResponse.builder()
                .id(10L).imageUrl("/banner.png").redirectType(BannerRedirectType.EXTERNAL).redirectUrl("https://lexho.com").build();

        CustomerCartResponse mutableCart = new CustomerCartResponse();
        mutableCart.setCartId(11L);
        mutableCart.setAddress(addressOne);
        mutableCart.setItems(List.of(itemOne));

        CustomerCartItemResponse mutableItem = new CustomerCartItemResponse();
        mutableItem.setItemId(12L);
        mutableItem.setServiceId(13L);
        mutableItem.setName("Mutable");

        CustomerPopularServiceResponse mutablePopular = new CustomerPopularServiceResponse();
        mutablePopular.setId(14L);
        mutablePopular.setName("Mutable");

        CustomerServiceCardResponse mutableService = new CustomerServiceCardResponse();
        mutableService.setId(15L);
        mutableService.setName("Mutable");

        CookFamilySizeAdminResponse adminResponse = new CookFamilySizeAdminResponse(16L, "MEDIUM", "3-5", true, 1);
        CookFamilySizeResponse customerResponse = new CookFamilySizeResponse(17L, "LARGE", "6-8");
        CookFamilySizeStatusRequest statusRequest = new CookFamilySizeStatusRequest();
        statusRequest.setActive(false);

        assertThat(firstBanner).isEqualTo(secondBanner).hasSameHashCodeAs(secondBanner);
        assertThat(firstBanner.toString()).contains("Hero");
        assertThat(cartOne).isEqualTo(cartTwo).hasSameHashCodeAs(cartTwo);
        assertThat(cartOne.toString()).contains("cartId=4");
        assertThat(addressOne).isEqualTo(addressTwo).hasSameHashCodeAs(addressTwo);
        assertThat(itemOne).isEqualTo(itemTwo).hasSameHashCodeAs(itemTwo);
        assertThat(popularOne).isEqualTo(popularTwo).hasSameHashCodeAs(popularTwo);
        assertThat(serviceOne).isEqualTo(serviceTwo).hasSameHashCodeAs(serviceTwo);
        assertThat(adminBannerOne).isEqualTo(adminBannerTwo).hasSameHashCodeAs(adminBannerTwo);
        assertThat(customerBannerOne).isEqualTo(customerBannerTwo).hasSameHashCodeAs(customerBannerTwo);
        assertThat(mutableCart.getCartId()).isEqualTo(11L);
        assertThat(mutableItem.getName()).isEqualTo("Mutable");
        assertThat(mutablePopular.getId()).isEqualTo(14L);
        assertThat(mutableService.getId()).isEqualTo(15L);
        assertThat(adminResponse.toString()).contains("MEDIUM");
        assertThat(customerResponse.getFamilySizeLabel()).isEqualTo("6-8");
        assertThat(statusRequest.getActive()).isFalse();
    }
}

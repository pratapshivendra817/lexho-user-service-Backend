package com.lexho.user_service.customer;

import com.lexho.user_service.addon.dto.CartAddonResponse;
import com.lexho.user_service.customer.banner.dto.AdminCustomerBannerResponse;
import com.lexho.user_service.customer.banner.dto.CustomerBannerResponse;
import com.lexho.user_service.customer.banner.entity.BannerRedirectType;
import com.lexho.user_service.customer.dto.CookFamilySizeResponse;
import com.lexho.user_service.customer.dto.CustomerCartItemResponse;
import com.lexho.user_service.customer.dto.CustomerCartResponse;
import com.lexho.user_service.customer.dto.CustomerPopularServiceResponse;
import com.lexho.user_service.customer.dto.CustomerServiceCardResponse;
import com.lexho.user_service.customer.dto.CustomerServiceCatalogPageResponse;
import com.lexho.user_service.customer.dto.CustomerServiceCatalogResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerDtoCoverageAdditionalTest {

    @Test
    void customerDtosExposeBuildersConstructorsAndAccessors() {
        CartAddonResponse addon = new CartAddonResponse();
        addon.setAddonId(1L);
        addon.setAddonName("Extra");
        addon.setPrice(49.0);
        addon.setQuantity(1);
        addon.setTotalPrice(49.0);
        addon.setRequired(true);
        addon.setQuantityAllowed(true);
        CustomerCartItemResponse cartItem = CustomerCartItemResponse.builder()
                .itemId(1L)
                .serviceId(2L)
                .name("Cook Visit")
                .imageUrl("/cook.png")
                .serviceType("COOK")
                .bookingMode("SCHEDULED")
                .familySizeId(3L)
                .familySize("MEDIUM")
                .familySizeLabel("3-5")
                .frequency("DAILY")
                .visits(10)
                .timeSlot("10:00")
                .startDate("2026-07-20")
                .endDate("2026-07-30")
                .price(499.0)
                .quantity(2)
                .addons(List.of(addon))
                .totalPrice(1047.0)
                .build();
        CustomerCartResponse.Address address = CustomerCartResponse.Address.builder()
                .id(5L)
                .fullAddress("Pune")
                .city("Pune")
                .pincode("411001")
                .build();
        CustomerCartResponse cart = CustomerCartResponse.builder()
                .cartId(9L)
                .grandTotal(1200.0)
                .itemTotal(1000.0)
                .gstAmount(100.0)
                .serviceFee(50.0)
                .taxAmount(50.0)
                .toPay(1200.0)
                .totalSaved(100.0)
                .currency("INR")
                .scheduledTime("10:00")
                .requestStatus("OPEN")
                .matchedPartners(4)
                .assignmentMode("AUTO")
                .contactName("User")
                .contactPhone("9999999999")
                .address(address)
                .items(List.of(cartItem))
                .build();
        CustomerPopularServiceResponse popular = CustomerPopularServiceResponse.builder()
                .id(6L)
                .name("Deep Cleaning")
                .imageUrl("/clean.png")
                .price(BigDecimal.valueOf(799))
                .rating(4.9)
                .categoryId(7L)
                .build();
        CustomerServiceCardResponse serviceCard = CustomerServiceCardResponse.builder()
                .id(8L)
                .serviceId(9L)
                .name("Cook")
                .serviceName("Home Cook")
                .description("desc")
                .imageUrl("/card.png")
                .price(BigDecimal.valueOf(299))
                .discountedPrice(BigDecimal.valueOf(249))
                .rating(4.6)
                .totalReviews(55L)
                .isNew(true)
                .bookingModes(List.of("INSTANT", "SCHEDULED"))
                .hourlyRate(BigDecimal.valueOf(199))
                .minHours(2)
                .maxHours(8)
                .hourStep(1)
                .build();
        CustomerServiceCatalogResponse catalogItem = new CustomerServiceCatalogResponse();
        catalogItem.setId(10L);
        catalogItem.setServiceId(11L);
        catalogItem.setName("Catalog");
        catalogItem.setServiceName("Catalog Service");
        catalogItem.setDescription("full");
        catalogItem.setImageUrl("/catalog.png");
        catalogItem.setPrice(BigDecimal.valueOf(999));
        catalogItem.setServiceType("COOK");
        catalogItem.setIncluded(List.of("A"));
        catalogItem.setExcluded(List.of("B"));
        catalogItem.setFaqs(List.of("Q"));
        catalogItem.setInstructions(List.of("I"));
        CustomerServiceCatalogPageResponse catalogPage =
                new CustomerServiceCatalogPageResponse(List.of(catalogItem), 1L, 0, 20);
        CookFamilySizeResponse familySize = new CookFamilySizeResponse(12L, "LARGE", "6-8");
        LocalDateTime now = LocalDateTime.parse("2026-07-17T12:00:00");
        AdminCustomerBannerResponse adminBanner = AdminCustomerBannerResponse.builder()
                .id(13L)
                .categoryId(14L)
                .title("Admin Banner")
                .imageUrl("/banner.png")
                .redirectType(BannerRedirectType.CATEGORY)
                .redirectId(15L)
                .redirectUrl("/go")
                .active(true)
                .priority(1)
                .createdAt(now)
                .updatedAt(now)
                .build();
        CustomerBannerResponse customerBanner = CustomerBannerResponse.builder()
                .id(16L)
                .imageUrl("/customer.png")
                .redirectType(BannerRedirectType.EXTERNAL)
                .redirectId(17L)
                .redirectUrl("https://lexho.com")
                .build();

        assertThat(cart.getItems()).singleElement().extracting(CustomerCartItemResponse::getName).isEqualTo("Cook Visit");
        assertThat(cart.getAddress().getCity()).isEqualTo("Pune");
        assertThat(cartItem.getAddons()).singleElement().extracting(CartAddonResponse::isRequired).isEqualTo(true);
        assertThat(popular.getPrice()).isEqualByComparingTo("799");
        assertThat(serviceCard.getBookingModes()).containsExactly("INSTANT", "SCHEDULED");
        assertThat(catalogPage.getItems()).singleElement().extracting(CustomerServiceCatalogResponse::getServiceName)
                .isEqualTo("Catalog Service");
        catalogPage.setPage(1);
        catalogPage.setSize(10);
        catalogPage.setTotal(2L);
        assertThat(catalogPage.getPage()).isEqualTo(1);
        assertThat(catalogPage.getSize()).isEqualTo(10);
        assertThat(catalogPage.getTotal()).isEqualTo(2L);
        assertThat(familySize.getFamilySize()).isEqualTo("LARGE");
        assertThat(adminBanner.getTitle()).isEqualTo("Admin Banner");
        assertThat(customerBanner.getRedirectUrl()).isEqualTo("https://lexho.com");
    }
}

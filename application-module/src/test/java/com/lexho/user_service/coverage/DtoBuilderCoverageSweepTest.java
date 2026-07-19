package com.lexho.user_service.coverage;

import com.lexho.user_service.addon.dto.AddonSelectionRequest;
import com.lexho.user_service.addon.dto.CartAddonResponse;
import com.lexho.user_service.banner.enums.BannerPlacement;
import com.lexho.user_service.banner.enums.BannerPlatformType;
import com.lexho.user_service.chat.dto.ChatDeliveryReceiptResponse;
import com.lexho.user_service.chat.dto.ChatMessagePageResponse;
import com.lexho.user_service.chat.dto.ChatMessageResponse;
import com.lexho.user_service.chat.dto.ChatParticipantSummary;
import com.lexho.user_service.chat.dto.ChatReadReceiptResponse;
import com.lexho.user_service.chat.dto.ChatRoomPageResponse;
import com.lexho.user_service.chat.dto.ChatRoomResponse;
import com.lexho.user_service.chat.dto.ChatSocketEvent;
import com.lexho.user_service.chat.dto.ChatTypingEventResponse;
import com.lexho.user_service.chat.entity.ChatMessage;
import com.lexho.user_service.chat.entity.ChatParticipant;
import com.lexho.user_service.chat.entity.ChatRoom;
import com.lexho.user_service.chat.enums.ChatEventType;
import com.lexho.user_service.chat.enums.ChatMessageDeliveryStatus;
import com.lexho.user_service.chat.enums.ChatMessageType;
import com.lexho.user_service.chat.enums.ChatParticipantType;
import com.lexho.user_service.cookfamilysize.dto.CookFamilySizeAdminResponse;
import com.lexho.user_service.cookfamilysize.dto.CookFamilySizeRequest;
import com.lexho.user_service.cookfamilysize.dto.CookFamilySizeStatusRequest;
import com.lexho.user_service.customer.banner.dto.AdminCustomerBannerResponse;
import com.lexho.user_service.customer.banner.dto.AdminCustomerBannerUpsertRequest;
import com.lexho.user_service.customer.banner.dto.CustomerBannerResponse;
import com.lexho.user_service.customer.banner.entity.BannerRedirectType;
import com.lexho.user_service.customer.dto.CookFamilySizeResponse;
import com.lexho.user_service.customer.dto.CustomerAddToCartRequest;
import com.lexho.user_service.customer.dto.CustomerCartItemResponse;
import com.lexho.user_service.customer.dto.CustomerCartResponse;
import com.lexho.user_service.customer.dto.CustomerPopularServiceResponse;
import com.lexho.user_service.customer.dto.CustomerServiceCardResponse;
import com.lexho.user_service.customer.dto.CustomerUpdateCartItemRequest;
import com.lexho.user_service.dto.CartResponseDTO;
import com.lexho.user_service.dto.CartSummaryDTO;
import com.lexho.user_service.dto.HomeBannerDTO;
import com.lexho.user_service.dto.HomeCategoryDTO;
import com.lexho.user_service.dto.HomeSectionDTO;
import com.lexho.user_service.dto.HomeServiceDTO;
import com.lexho.user_service.dto.SubCategoryResponseDTO;
import com.lexho.user_service.homebanner.dto.HomeBannerReorderItemRequest;
import com.lexho.user_service.homebanner.dto.HomeBannerReorderRequest;
import com.lexho.user_service.homebanner.dto.HomeBannerResponse;
import com.lexho.user_service.homebanner.dto.HomeBannerUpsertRequest;
import com.lexho.user_service.homebanner.entity.HomeBannerAudience;
import com.lexho.user_service.homebanner.entity.HomeBannerRedirectType;
import com.lexho.user_service.partnerrequest.dto.PagedResponse;
import com.lexho.user_service.partnerrequest.dto.PartnerIncomingRequestResponse;
import com.lexho.user_service.partnerrequest.dto.PartnerRequestAddressResponse;
import com.lexho.user_service.partnerrequest.dto.PartnerStatusResponse;
import com.lexho.user_service.partnerrequest.model.PartnerServiceRequestStatus;
import com.lexho.user_service.section.dto.SectionItemResponse;
import com.lexho.user_service.section.dto.SectionPageResponse;
import com.lexho.user_service.section.dto.SectionResponse;
import com.lexho.user_service.section.dto.SectionUpdateRequest;
import com.lexho.user_service.section.enums.SectionType;
import com.lexho.user_service.upload.UploadSecurityService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DtoBuilderCoverageSweepTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void cookFamilySizeAndHomeBannerDtosExposeAllFieldsAndValidation() {
        CookFamilySizeRequest cookRequest = new CookFamilySizeRequest();
        cookRequest.setFamilySize("SMALL");
        cookRequest.setFamilySizeLabel("1-2");
        cookRequest.setActive(true);
        cookRequest.setDisplayOrder(1);
        CookFamilySizeRequest sameCookRequest = new CookFamilySizeRequest();
        sameCookRequest.setFamilySize("SMALL");
        sameCookRequest.setFamilySizeLabel("1-2");
        sameCookRequest.setActive(true);
        sameCookRequest.setDisplayOrder(1);
        assertThat(cookRequest.getFamilySize()).isEqualTo("SMALL");
        assertThat(cookRequest.getFamilySizeLabel()).isEqualTo("1-2");
        assertThat(cookRequest.getActive()).isTrue();
        assertThat(cookRequest.getDisplayOrder()).isEqualTo(1);
        assertThat(cookRequest).isEqualTo(sameCookRequest).hasSameHashCodeAs(sameCookRequest);
        assertThat(cookRequest.toString()).contains("SMALL");

        CookFamilySizeStatusRequest statusRequest = new CookFamilySizeStatusRequest();
        statusRequest.setActive(false);
        CookFamilySizeStatusRequest sameStatusRequest = new CookFamilySizeStatusRequest();
        sameStatusRequest.setActive(false);
        assertThat(statusRequest.getActive()).isFalse();
        statusRequest.setActive(true);
        assertThat(statusRequest.getActive()).isTrue();
        assertThat(statusRequest).isNotEqualTo(sameStatusRequest);
        assertThat(statusRequest.toString()).contains("active");

        CookFamilySizeAdminResponse adminResponse = new CookFamilySizeAdminResponse(1L, "MEDIUM", "3-5", true, 2);
        CookFamilySizeAdminResponse sameAdminResponse = new CookFamilySizeAdminResponse(1L, "MEDIUM", "3-5", true, 2);
        assertThat(adminResponse.getFamilySizeId()).isEqualTo(1L);
        assertThat(adminResponse.getFamilySize()).isEqualTo("MEDIUM");
        assertThat(adminResponse.getFamilySizeLabel()).isEqualTo("3-5");
        assertThat(adminResponse.getActive()).isTrue();
        assertThat(adminResponse.getDisplayOrder()).isEqualTo(2);
        assertThat(adminResponse).isEqualTo(sameAdminResponse).hasSameHashCodeAs(sameAdminResponse);
        assertThat(adminResponse.toString()).contains("MEDIUM");

        HomeBannerReorderItemRequest reorderItem = new HomeBannerReorderItemRequest();
        reorderItem.setId(9L);
        reorderItem.setPriority(4);
        HomeBannerReorderItemRequest sameReorderItem = new HomeBannerReorderItemRequest();
        sameReorderItem.setId(9L);
        sameReorderItem.setPriority(4);
        assertThat(reorderItem.getId()).isEqualTo(9L);
        assertThat(reorderItem.getPriority()).isEqualTo(4);
        assertThat(reorderItem).isEqualTo(sameReorderItem).hasSameHashCodeAs(sameReorderItem);
        assertThat(reorderItem.toString()).contains("priority=4");

        HomeBannerReorderRequest reorderRequest = new HomeBannerReorderRequest();
        reorderRequest.setItems(List.of(reorderItem));
        HomeBannerReorderRequest sameReorderRequest = new HomeBannerReorderRequest();
        sameReorderRequest.setItems(List.of(sameReorderItem));
        assertThat(reorderRequest.getItems()).containsExactly(reorderItem);
        assertThat(validator.validate(new HomeBannerReorderRequest()))
                .extracting(v -> v.getPropertyPath().toString())
                .contains("items");
        assertThat(reorderRequest).isEqualTo(sameReorderRequest).hasSameHashCodeAs(sameReorderRequest);
        assertThat(reorderRequest.toString()).contains("items");

        HomeBannerUpsertRequest upsertRequest = new HomeBannerUpsertRequest();
        upsertRequest.setTitle("Banner");
        upsertRequest.setSubtitle("Sub");
        upsertRequest.setImageUrl("/b.png");
        upsertRequest.setRedirectType(HomeBannerRedirectType.SERVICE);
        upsertRequest.setRedirectId(5L);
        upsertRequest.setPriority(0);
        upsertRequest.setActive(true);
        upsertRequest.setBannerType("hero");
        upsertRequest.setAudience(HomeBannerAudience.BOTH);
        HomeBannerUpsertRequest sameUpsertRequest = new HomeBannerUpsertRequest();
        sameUpsertRequest.setTitle("Banner");
        sameUpsertRequest.setSubtitle("Sub");
        sameUpsertRequest.setImageUrl("/b.png");
        sameUpsertRequest.setRedirectType(HomeBannerRedirectType.SERVICE);
        sameUpsertRequest.setRedirectId(5L);
        sameUpsertRequest.setPriority(0);
        sameUpsertRequest.setActive(true);
        sameUpsertRequest.setBannerType("hero");
        sameUpsertRequest.setAudience(HomeBannerAudience.BOTH);
        assertThat(validator.validate(upsertRequest)).isEmpty();
        assertThat(upsertRequest.getTitle()).isEqualTo("Banner");
        assertThat(upsertRequest.getSubtitle()).isEqualTo("Sub");
        assertThat(upsertRequest.getImageUrl()).isEqualTo("/b.png");
        assertThat(upsertRequest.getRedirectType()).isEqualTo(HomeBannerRedirectType.SERVICE);
        assertThat(upsertRequest.getRedirectId()).isEqualTo(5L);
        assertThat(upsertRequest.getPriority()).isZero();
        assertThat(upsertRequest.getActive()).isTrue();
        assertThat(upsertRequest.getBannerType()).isEqualTo("hero");
        assertThat(upsertRequest.getAudience()).isEqualTo(HomeBannerAudience.BOTH);
        assertThat(upsertRequest).isEqualTo(sameUpsertRequest).hasSameHashCodeAs(sameUpsertRequest);
        assertThat(upsertRequest.toString()).contains("Banner");

        LocalDateTime now = LocalDateTime.parse("2026-07-17T10:15:00");
        HomeBannerResponse homeBannerResponse = HomeBannerResponse.builder()
                .id(1L)
                .title("Banner")
                .subtitle("Sub")
                .imageUrl("/b.png")
                .redirectType(HomeBannerRedirectType.CATEGORY)
                .redirectId(2L)
                .priority(3)
                .active(true)
                .bannerType("hero")
                .audience(HomeBannerAudience.CUSTOMER)
                .createdAt(now)
                .updatedAt(now)
                .build();
        assertThat(homeBannerResponse.getId()).isEqualTo(1L);
        assertThat(homeBannerResponse.getTitle()).isEqualTo("Banner");
        assertThat(homeBannerResponse.getSubtitle()).isEqualTo("Sub");
        assertThat(homeBannerResponse.getImageUrl()).isEqualTo("/b.png");
        assertThat(homeBannerResponse.getRedirectType()).isEqualTo(HomeBannerRedirectType.CATEGORY);
        assertThat(homeBannerResponse.getRedirectId()).isEqualTo(2L);
        assertThat(homeBannerResponse.getPriority()).isEqualTo(3);
        assertThat(homeBannerResponse.isActive()).isTrue();
        assertThat(homeBannerResponse.getBannerType()).isEqualTo("hero");
        assertThat(homeBannerResponse.getAudience()).isEqualTo(HomeBannerAudience.CUSTOMER);
        assertThat(homeBannerResponse.getCreatedAt()).isEqualTo(now);
        assertThat(homeBannerResponse.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    void customerAndSectionDtosCoverConstructorsBuildersAndValidation() {
        AddonSelectionRequest addonSelectionRequest = new AddonSelectionRequest();
        addonSelectionRequest.setAddonId(7L);
        addonSelectionRequest.setQuantity(2);

        CustomerAddToCartRequest addToCartRequest = new CustomerAddToCartRequest();
        addToCartRequest.setServiceId(1L);
        addToCartRequest.setFamilySizeId(2L);
        addToCartRequest.setFamilySize("LARGE");
        addToCartRequest.setFamilySizeLabel("6-8");
        addToCartRequest.setQuantity(3);
        addToCartRequest.setAddons(List.of(addonSelectionRequest));
        CustomerAddToCartRequest sameAddToCartRequest = new CustomerAddToCartRequest();
        sameAddToCartRequest.setServiceId(1L);
        sameAddToCartRequest.setFamilySizeId(2L);
        sameAddToCartRequest.setFamilySize("LARGE");
        sameAddToCartRequest.setFamilySizeLabel("6-8");
        sameAddToCartRequest.setQuantity(3);
        sameAddToCartRequest.setAddons(List.of(addonSelectionRequest));
        assertThat(validator.validate(addToCartRequest)).isEmpty();
        assertThat(addToCartRequest.getAddons()).singleElement().extracting(AddonSelectionRequest::getAddonId).isEqualTo(7L);
        assertThat(addToCartRequest).isEqualTo(sameAddToCartRequest).hasSameHashCodeAs(sameAddToCartRequest);
        assertThat(addToCartRequest.toString()).contains("serviceId=1");

        CustomerUpdateCartItemRequest updateCartItemRequest = new CustomerUpdateCartItemRequest();
        updateCartItemRequest.setItemId(11L);
        updateCartItemRequest.setQuantity(4);
        updateCartItemRequest.setFamilySizeId(5L);
        updateCartItemRequest.setFamilySize("MEDIUM");
        updateCartItemRequest.setFamilySizeLabel("3-5");
        CustomerUpdateCartItemRequest sameUpdateCartItemRequest = new CustomerUpdateCartItemRequest();
        sameUpdateCartItemRequest.setItemId(11L);
        sameUpdateCartItemRequest.setQuantity(4);
        sameUpdateCartItemRequest.setFamilySizeId(5L);
        sameUpdateCartItemRequest.setFamilySize("MEDIUM");
        sameUpdateCartItemRequest.setFamilySizeLabel("3-5");
        assertThat(validator.validate(updateCartItemRequest)).isEmpty();
        assertThat(updateCartItemRequest.getItemId()).isEqualTo(11L);
        assertThat(updateCartItemRequest.getQuantity()).isEqualTo(4);
        assertThat(updateCartItemRequest.getFamilySizeId()).isEqualTo(5L);
        assertThat(updateCartItemRequest).isEqualTo(sameUpdateCartItemRequest)
                .hasSameHashCodeAs(sameUpdateCartItemRequest);
        assertThat(updateCartItemRequest.toString()).contains("itemId=11");

        CookFamilySizeResponse familySizeResponse = new CookFamilySizeResponse(1L, "SMALL", "1-2");
        familySizeResponse.setFamilySizeId(2L);
        familySizeResponse.setFamilySize("MEDIUM");
        familySizeResponse.setFamilySizeLabel("3-5");
        assertThat(familySizeResponse.getFamilySizeId()).isEqualTo(2L);
        assertThat(familySizeResponse.getFamilySize()).isEqualTo("MEDIUM");
        assertThat(familySizeResponse.getFamilySizeLabel()).isEqualTo("3-5");
        assertThat(familySizeResponse).isEqualTo(new CookFamilySizeResponse(2L, "MEDIUM", "3-5"));
        assertThat(familySizeResponse.toString()).contains("MEDIUM");

        CartAddonResponse cartAddonResponse = new CartAddonResponse();
        cartAddonResponse.setAddonId(4L);
        cartAddonResponse.setAddonName("Extra");
        cartAddonResponse.setPrice(10.0);
        cartAddonResponse.setQuantity(1);
        cartAddonResponse.setTotalPrice(10.0);
        cartAddonResponse.setRequired(true);
        cartAddonResponse.setQuantityAllowed(true);

        CustomerCartItemResponse itemResponse = CustomerCartItemResponse.builder()
                .itemId(8L)
                .serviceId(9L)
                .name("Cleaning")
                .imageUrl("/c.png")
                .serviceType("HOME")
                .bookingMode("ONCE")
                .familySizeId(2L)
                .familySize("MEDIUM")
                .familySizeLabel("3-5")
                .frequency("WEEKLY")
                .visits(2)
                .timeSlot("10:00-11:00")
                .startDate("2026-07-17")
                .endDate("2026-07-24")
                .price(299.0)
                .quantity(1)
                .addons(List.of(cartAddonResponse))
                .totalPrice(309.0)
                .build();
        assertThat(itemResponse.getItemId()).isEqualTo(8L);
        assertThat(itemResponse.getServiceId()).isEqualTo(9L);
        assertThat(itemResponse.getName()).isEqualTo("Cleaning");
        assertThat(itemResponse.getImageUrl()).isEqualTo("/c.png");
        assertThat(itemResponse.getServiceType()).isEqualTo("HOME");
        assertThat(itemResponse.getBookingMode()).isEqualTo("ONCE");
        assertThat(itemResponse.getFamilySizeId()).isEqualTo(2L);
        assertThat(itemResponse.getFamilySize()).isEqualTo("MEDIUM");
        assertThat(itemResponse.getFamilySizeLabel()).isEqualTo("3-5");
        assertThat(itemResponse.getFrequency()).isEqualTo("WEEKLY");
        assertThat(itemResponse.getVisits()).isEqualTo(2);
        assertThat(itemResponse.getTimeSlot()).isEqualTo("10:00-11:00");
        assertThat(itemResponse.getStartDate()).isEqualTo("2026-07-17");
        assertThat(itemResponse.getEndDate()).isEqualTo("2026-07-24");
        assertThat(itemResponse.getPrice()).isEqualTo(299.0);
        assertThat(itemResponse.getQuantity()).isEqualTo(1);
        assertThat(itemResponse.getAddons()).singleElement().extracting(CartAddonResponse::getAddonName).isEqualTo("Extra");
        assertThat(itemResponse.getTotalPrice()).isEqualTo(309.0);
        assertThat(itemResponse).isEqualTo(CustomerCartItemResponse.builder()
                .itemId(8L).serviceId(9L).name("Cleaning").imageUrl("/c.png").serviceType("HOME")
                .bookingMode("ONCE").familySizeId(2L).familySize("MEDIUM").familySizeLabel("3-5")
                .frequency("WEEKLY").visits(2).timeSlot("10:00-11:00").startDate("2026-07-17")
                .endDate("2026-07-24").price(299.0).quantity(1).addons(List.of(cartAddonResponse))
                .totalPrice(309.0).build());
        assertThat(itemResponse.toString()).contains("Cleaning");

        CustomerCartResponse.Address address = CustomerCartResponse.Address.builder()
                .id(3L)
                .fullAddress("MG Road")
                .city("Pune")
                .pincode("411001")
                .build();
        CustomerCartResponse cartResponse = CustomerCartResponse.builder()
                .cartId(12L)
                .grandTotal(600.0)
                .itemTotal(550.0)
                .gstAmount(25.0)
                .serviceFee(10.0)
                .taxAmount(15.0)
                .toPay(600.0)
                .totalSaved(40.0)
                .currency("INR")
                .scheduledTime("2026-07-17T10:00")
                .requestStatus("PENDING")
                .matchedPartners(2)
                .assignmentMode("AUTO")
                .contactName("User")
                .contactPhone("9999999999")
                .address(address)
                .items(List.of(itemResponse))
                .build();
        assertThat(cartResponse.getAddress().getCity()).isEqualTo("Pune");
        assertThat(cartResponse.getCartId()).isEqualTo(12L);
        assertThat(cartResponse.getGrandTotal()).isEqualTo(600.0);
        assertThat(cartResponse.getItemTotal()).isEqualTo(550.0);
        assertThat(cartResponse.getGstAmount()).isEqualTo(25.0);
        assertThat(cartResponse.getServiceFee()).isEqualTo(10.0);
        assertThat(cartResponse.getTaxAmount()).isEqualTo(15.0);
        assertThat(cartResponse.getToPay()).isEqualTo(600.0);
        assertThat(cartResponse.getTotalSaved()).isEqualTo(40.0);
        assertThat(cartResponse.getCurrency()).isEqualTo("INR");
        assertThat(cartResponse.getScheduledTime()).isEqualTo("2026-07-17T10:00");
        assertThat(cartResponse.getRequestStatus()).isEqualTo("PENDING");
        assertThat(cartResponse.getMatchedPartners()).isEqualTo(2);
        assertThat(cartResponse.getAssignmentMode()).isEqualTo("AUTO");
        assertThat(cartResponse.getContactName()).isEqualTo("User");
        assertThat(cartResponse.getContactPhone()).isEqualTo("9999999999");
        assertThat(cartResponse.getItems()).containsExactly(itemResponse);
        assertThat(cartResponse).isEqualTo(CustomerCartResponse.builder()
                .cartId(12L).grandTotal(600.0).itemTotal(550.0).gstAmount(25.0).serviceFee(10.0)
                .taxAmount(15.0).toPay(600.0).totalSaved(40.0).currency("INR")
                .scheduledTime("2026-07-17T10:00").requestStatus("PENDING").matchedPartners(2)
                .assignmentMode("AUTO").contactName("User").contactPhone("9999999999")
                .address(address).items(List.of(itemResponse)).build());
        assertThat(cartResponse.toString()).contains("cartId=12");
        assertThat(address).isEqualTo(CustomerCartResponse.Address.builder()
                .id(3L).fullAddress("MG Road").city("Pune").pincode("411001").build());
        assertThat(address.toString()).contains("Pune");

        CustomerPopularServiceResponse popularServiceResponse = CustomerPopularServiceResponse.builder()
                .id(13L)
                .name("Popular")
                .imageUrl("/p.png")
                .price(BigDecimal.TEN)
                .rating(4.5)
                .categoryId(14L)
                .build();
        assertThat(popularServiceResponse.getId()).isEqualTo(13L);
        assertThat(popularServiceResponse.getName()).isEqualTo("Popular");
        assertThat(popularServiceResponse.getImageUrl()).isEqualTo("/p.png");
        assertThat(popularServiceResponse.getPrice()).isEqualByComparingTo("10");
        assertThat(popularServiceResponse.getRating()).isEqualTo(4.5);
        assertThat(popularServiceResponse.getCategoryId()).isEqualTo(14L);
        assertThat(popularServiceResponse).isEqualTo(CustomerPopularServiceResponse.builder()
                .id(13L).name("Popular").imageUrl("/p.png").price(BigDecimal.TEN).rating(4.5).categoryId(14L).build());
        assertThat(popularServiceResponse.toString()).contains("Popular");

        CustomerServiceCardResponse serviceCardResponse = CustomerServiceCardResponse.builder()
                .id(15L)
                .serviceId(16L)
                .name("Card")
                .serviceName("Service")
                .description("desc")
                .imageUrl("/i.png")
                .price(BigDecimal.ONE)
                .discountedPrice(BigDecimal.ZERO)
                .rating(4.9)
                .totalReviews(20L)
                .isNew(true)
                .bookingModes(List.of("ONCE", "SUBSCRIPTION"))
                .hourlyRate(BigDecimal.TEN)
                .minHours(2)
                .maxHours(6)
                .hourStep(1)
                .build();
        assertThat(serviceCardResponse.getId()).isEqualTo(15L);
        assertThat(serviceCardResponse.getServiceId()).isEqualTo(16L);
        assertThat(serviceCardResponse.getName()).isEqualTo("Card");
        assertThat(serviceCardResponse.getServiceName()).isEqualTo("Service");
        assertThat(serviceCardResponse.getDescription()).isEqualTo("desc");
        assertThat(serviceCardResponse.getImageUrl()).isEqualTo("/i.png");
        assertThat(serviceCardResponse.getPrice()).isEqualByComparingTo("1");
        assertThat(serviceCardResponse.getDiscountedPrice()).isEqualByComparingTo("0");
        assertThat(serviceCardResponse.getRating()).isEqualTo(4.9);
        assertThat(serviceCardResponse.getTotalReviews()).isEqualTo(20L);
        assertThat(serviceCardResponse.isNew()).isTrue();
        assertThat(serviceCardResponse.getBookingModes()).containsExactly("ONCE", "SUBSCRIPTION");
        assertThat(serviceCardResponse.getHourlyRate()).isEqualByComparingTo("10");
        assertThat(serviceCardResponse.getMinHours()).isEqualTo(2);
        assertThat(serviceCardResponse.getMaxHours()).isEqualTo(6);
        assertThat(serviceCardResponse.getHourStep()).isEqualTo(1);
        assertThat(serviceCardResponse).isEqualTo(CustomerServiceCardResponse.builder()
                .id(15L).serviceId(16L).name("Card").serviceName("Service").description("desc")
                .imageUrl("/i.png").price(BigDecimal.ONE).discountedPrice(BigDecimal.ZERO).rating(4.9)
                .totalReviews(20L).isNew(true).bookingModes(List.of("ONCE", "SUBSCRIPTION"))
                .hourlyRate(BigDecimal.TEN).minHours(2).maxHours(6).hourStep(1).build());
        assertThat(serviceCardResponse.toString()).contains("Card");

        SectionUpdateRequest sectionUpdateRequest = new SectionUpdateRequest();
        sectionUpdateRequest.setType(SectionType.HIGHLIGHTS);
        sectionUpdateRequest.setTitle("Highlights");
        sectionUpdateRequest.setSubtitle("Sub");
        sectionUpdateRequest.setIcon("spark");
        sectionUpdateRequest.setPriority(1);
        sectionUpdateRequest.setIsActive(true);
        assertThat(sectionUpdateRequest.getType()).isEqualTo(SectionType.HIGHLIGHTS);
        assertThat(sectionUpdateRequest.getTitle()).isEqualTo("Highlights");
        assertThat(sectionUpdateRequest.getSubtitle()).isEqualTo("Sub");
        assertThat(sectionUpdateRequest.getIcon()).isEqualTo("spark");
        assertThat(sectionUpdateRequest.getPriority()).isEqualTo(1);
        assertThat(sectionUpdateRequest.getIsActive()).isTrue();

        SectionItemResponse sectionItemResponse = new SectionItemResponse();
        sectionItemResponse.setId(17L);
        sectionItemResponse.setSectionId(18L);
        sectionItemResponse.setTitle("Item");
        sectionItemResponse.setSubtitle("Sub");
        sectionItemResponse.setValue("42");
        sectionItemResponse.setIcon("star");
        sectionItemResponse.setPriority(2);
        sectionItemResponse.setActive(true);
        assertThat(sectionItemResponse.getId()).isEqualTo(17L);
        assertThat(sectionItemResponse.getSectionId()).isEqualTo(18L);
        assertThat(sectionItemResponse.getTitle()).isEqualTo("Item");
        assertThat(sectionItemResponse.getSubtitle()).isEqualTo("Sub");
        assertThat(sectionItemResponse.getValue()).isEqualTo("42");
        assertThat(sectionItemResponse.getIcon()).isEqualTo("star");
        assertThat(sectionItemResponse.getPriority()).isEqualTo(2);
        assertThat(sectionItemResponse.isActive()).isTrue();

        SectionResponse sectionResponse = new SectionResponse();
        sectionResponse.setId(19L);
        sectionResponse.setType(SectionType.TRUST);
        sectionResponse.setTitle("Trust");
        sectionResponse.setSubtitle("Sub");
        sectionResponse.setIcon("shield");
        sectionResponse.setPriority(3);
        sectionResponse.setActive(true);
        sectionResponse.setCreatedAt(LocalDateTime.parse("2026-07-17T11:00:00"));
        sectionResponse.setItems(List.of(sectionItemResponse));
        assertThat(sectionResponse.getId()).isEqualTo(19L);
        assertThat(sectionResponse.getType()).isEqualTo(SectionType.TRUST);
        assertThat(sectionResponse.getTitle()).isEqualTo("Trust");
        assertThat(sectionResponse.getSubtitle()).isEqualTo("Sub");
        assertThat(sectionResponse.getIcon()).isEqualTo("shield");
        assertThat(sectionResponse.getPriority()).isEqualTo(3);
        assertThat(sectionResponse.isActive()).isTrue();
        assertThat(sectionResponse.getCreatedAt()).isEqualTo(LocalDateTime.parse("2026-07-17T11:00:00"));
        assertThat(sectionResponse.getItems()).containsExactly(sectionItemResponse);

        SectionPageResponse sectionPageResponse = new SectionPageResponse(List.of(sectionResponse), 1L, 1, 0, 10);
        assertThat(sectionPageResponse.getContent()).containsExactly(sectionResponse);
        assertThat(sectionPageResponse.getTotalElements()).isEqualTo(1L);
        assertThat(sectionPageResponse.getTotalPages()).isEqualTo(1);
        assertThat(sectionPageResponse.getPage()).isZero();
        assertThat(sectionPageResponse.getSize()).isEqualTo(10);
    }

    @Test
    void customerBannerPartnerRequestAndLegacyDtosExposeAllFields() {
        AdminCustomerBannerUpsertRequest bannerUpsertRequest = new AdminCustomerBannerUpsertRequest();
        bannerUpsertRequest.setCategoryId(1L);
        bannerUpsertRequest.setTitle("Title");
        bannerUpsertRequest.setImageUrl("/image.png");
        bannerUpsertRequest.setBannerImageUrl("/banner.png");
        bannerUpsertRequest.setRedirectType(BannerRedirectType.CATEGORY);
        bannerUpsertRequest.setRedirectId(2L);
        bannerUpsertRequest.setRedirectValue("value");
        bannerUpsertRequest.setRedirectUrl("https://example.com");
        bannerUpsertRequest.setActive(true);
        bannerUpsertRequest.setPriority(0);
        AdminCustomerBannerUpsertRequest sameBannerUpsertRequest = new AdminCustomerBannerUpsertRequest();
        sameBannerUpsertRequest.setCategoryId(1L);
        sameBannerUpsertRequest.setTitle("Title");
        sameBannerUpsertRequest.setImageUrl("/image.png");
        sameBannerUpsertRequest.setBannerImageUrl("/banner.png");
        sameBannerUpsertRequest.setRedirectType(BannerRedirectType.CATEGORY);
        sameBannerUpsertRequest.setRedirectId(2L);
        sameBannerUpsertRequest.setRedirectValue("value");
        sameBannerUpsertRequest.setRedirectUrl("https://example.com");
        sameBannerUpsertRequest.setActive(true);
        sameBannerUpsertRequest.setPriority(0);
        assertThat(validator.validate(bannerUpsertRequest)).isEmpty();
        assertThat(bannerUpsertRequest.getCategoryId()).isEqualTo(1L);
        assertThat(bannerUpsertRequest.getTitle()).isEqualTo("Title");
        assertThat(bannerUpsertRequest.getImageUrl()).isEqualTo("/image.png");
        assertThat(bannerUpsertRequest.getBannerImageUrl()).isEqualTo("/banner.png");
        assertThat(bannerUpsertRequest.getRedirectType()).isEqualTo(BannerRedirectType.CATEGORY);
        assertThat(bannerUpsertRequest.getRedirectId()).isEqualTo(2L);
        assertThat(bannerUpsertRequest.getRedirectValue()).isEqualTo("value");
        assertThat(bannerUpsertRequest.getRedirectUrl()).isEqualTo("https://example.com");
        assertThat(bannerUpsertRequest.getActive()).isTrue();
        assertThat(bannerUpsertRequest.getPriority()).isZero();
        assertThat(bannerUpsertRequest).isEqualTo(sameBannerUpsertRequest).hasSameHashCodeAs(sameBannerUpsertRequest);
        assertThat(bannerUpsertRequest.toString()).contains("Title");

        LocalDateTime now = LocalDateTime.parse("2026-07-17T12:00:00");
        AdminCustomerBannerResponse adminBannerResponse = AdminCustomerBannerResponse.builder()
                .id(3L)
                .categoryId(4L)
                .title("Admin")
                .imageUrl("/img.png")
                .redirectType(BannerRedirectType.SERVICE)
                .redirectId(5L)
                .redirectUrl("https://lexho.com")
                .active(true)
                .priority(1)
                .createdAt(now)
                .updatedAt(now)
                .build();
        assertThat(adminBannerResponse.getId()).isEqualTo(3L);
        assertThat(adminBannerResponse.getCategoryId()).isEqualTo(4L);
        assertThat(adminBannerResponse.getTitle()).isEqualTo("Admin");
        assertThat(adminBannerResponse.getImageUrl()).isEqualTo("/img.png");
        assertThat(adminBannerResponse.getRedirectType()).isEqualTo(BannerRedirectType.SERVICE);
        assertThat(adminBannerResponse.getRedirectId()).isEqualTo(5L);
        assertThat(adminBannerResponse.getRedirectUrl()).isEqualTo("https://lexho.com");
        assertThat(adminBannerResponse.isActive()).isTrue();
        assertThat(adminBannerResponse.getPriority()).isEqualTo(1);
        assertThat(adminBannerResponse.getCreatedAt()).isEqualTo(now);
        assertThat(adminBannerResponse.getUpdatedAt()).isEqualTo(now);
        assertThat(adminBannerResponse).isEqualTo(AdminCustomerBannerResponse.builder()
                .id(3L).categoryId(4L).title("Admin").imageUrl("/img.png").redirectType(BannerRedirectType.SERVICE)
                .redirectId(5L).redirectUrl("https://lexho.com").active(true).priority(1).createdAt(now).updatedAt(now).build());
        assertThat(adminBannerResponse.toString()).contains("Admin");

        CustomerBannerResponse customerBannerResponse = CustomerBannerResponse.builder()
                .id(6L)
                .imageUrl("/customer.png")
                .redirectType(BannerRedirectType.EXTERNAL)
                .redirectId(7L)
                .redirectUrl("https://customer.lexho.com")
                .build();
        assertThat(customerBannerResponse.getId()).isEqualTo(6L);
        assertThat(customerBannerResponse.getImageUrl()).isEqualTo("/customer.png");
        assertThat(customerBannerResponse.getRedirectType()).isEqualTo(BannerRedirectType.EXTERNAL);
        assertThat(customerBannerResponse.getRedirectId()).isEqualTo(7L);
        assertThat(customerBannerResponse.getRedirectUrl()).isEqualTo("https://customer.lexho.com");
        assertThat(customerBannerResponse).isEqualTo(CustomerBannerResponse.builder()
                .id(6L).imageUrl("/customer.png").redirectType(BannerRedirectType.EXTERNAL)
                .redirectId(7L).redirectUrl("https://customer.lexho.com").build());
        assertThat(customerBannerResponse.toString()).contains("customer.png");

        PartnerRequestAddressResponse addressResponse = new PartnerRequestAddressResponse();
        addressResponse.setFullAddress("FC Road");
        addressResponse.setCity("Pune");
        addressResponse.setLatitude(18.52);
        addressResponse.setLongitude(73.85);
        assertThat(addressResponse.getFullAddress()).isEqualTo("FC Road");
        assertThat(addressResponse.getCity()).isEqualTo("Pune");
        assertThat(addressResponse.getLatitude()).isEqualTo(18.52);
        assertThat(addressResponse.getLongitude()).isEqualTo(73.85);

        PartnerIncomingRequestResponse incomingRequestResponse = new PartnerIncomingRequestResponse();
        incomingRequestResponse.setRequestId(8L);
        incomingRequestResponse.setCartId(9L);
        incomingRequestResponse.setCustomerId(10L);
        incomingRequestResponse.setServiceId(11L);
        incomingRequestResponse.setServiceType("CLEANING");
        incomingRequestResponse.setQuantity(2);
        incomingRequestResponse.setScheduledTime(LocalDateTime.parse("2026-07-17T13:00:00"));
        incomingRequestResponse.setStatus(PartnerServiceRequestStatus.PENDING);
        incomingRequestResponse.setCreatedAt(LocalDateTime.parse("2026-07-17T12:30:00"));
        incomingRequestResponse.setEtaMinutes(25);
        incomingRequestResponse.setExpiresAt(LocalDateTime.parse("2026-07-17T13:15:00"));
        incomingRequestResponse.setAddress(addressResponse);
        assertThat(incomingRequestResponse.getRequestId()).isEqualTo(8L);
        assertThat(incomingRequestResponse.getCartId()).isEqualTo(9L);
        assertThat(incomingRequestResponse.getCustomerId()).isEqualTo(10L);
        assertThat(incomingRequestResponse.getServiceId()).isEqualTo(11L);
        assertThat(incomingRequestResponse.getServiceType()).isEqualTo("CLEANING");
        assertThat(incomingRequestResponse.getQuantity()).isEqualTo(2);
        assertThat(incomingRequestResponse.getScheduledTime()).isEqualTo(LocalDateTime.parse("2026-07-17T13:00:00"));
        assertThat(incomingRequestResponse.getStatus()).isEqualTo(PartnerServiceRequestStatus.PENDING);
        assertThat(incomingRequestResponse.getCreatedAt()).isEqualTo(LocalDateTime.parse("2026-07-17T12:30:00"));
        assertThat(incomingRequestResponse.getEtaMinutes()).isEqualTo(25);
        assertThat(incomingRequestResponse.getExpiresAt()).isEqualTo(LocalDateTime.parse("2026-07-17T13:15:00"));
        assertThat(incomingRequestResponse.getAddress()).isSameAs(addressResponse);

        PagedResponse<String> pagedResponse = new PagedResponse<>(List.of("a", "b"), 2L, 2, 1, 1);
        assertThat(pagedResponse.getContent()).containsExactly("a", "b");
        assertThat(pagedResponse.getTotalElements()).isEqualTo(2L);
        assertThat(pagedResponse.getTotalPages()).isEqualTo(2);
        assertThat(pagedResponse.getPage()).isEqualTo(1);
        assertThat(pagedResponse.getSize()).isEqualTo(1);
        assertThat(pagedResponse.isLast()).isTrue();

        PartnerStatusResponse partnerStatusResponse = new PartnerStatusResponse();
        partnerStatusResponse.setPartnerId(12L);
        partnerStatusResponse.setOnline(true);
        assertThat(partnerStatusResponse.getPartnerId()).isEqualTo(12L);
        assertThat(partnerStatusResponse.isOnline()).isTrue();

        HomeCategoryDTO homeCategoryDTO = new HomeCategoryDTO(13L, "Kitchen", "pot");
        homeCategoryDTO.setId(14L);
        homeCategoryDTO.setName("Cleaning");
        homeCategoryDTO.setIcon("broom");
        assertThat(homeCategoryDTO.getId()).isEqualTo(14L);
        assertThat(homeCategoryDTO.getName()).isEqualTo("Cleaning");
        assertThat(homeCategoryDTO.getIcon()).isEqualTo("broom");

        CartResponseDTO cartResponseDTO = new CartResponseDTO(15L, 16L, "Partner", "/x.png", 99.0, 1);
        cartResponseDTO.setId(17L);
        cartResponseDTO.setPartnerId(18L);
        cartResponseDTO.setPartnerName("Updated");
        cartResponseDTO.setImageUrl("/y.png");
        cartResponseDTO.setPrice(101.0);
        cartResponseDTO.setQuantity(2);
        assertThat(cartResponseDTO.getId()).isEqualTo(17L);
        assertThat(cartResponseDTO.getPartnerId()).isEqualTo(18L);
        assertThat(cartResponseDTO.getPartnerName()).isEqualTo("Updated");
        assertThat(cartResponseDTO.getImageUrl()).isEqualTo("/y.png");
        assertThat(cartResponseDTO.getPrice()).isEqualTo(101.0);
        assertThat(cartResponseDTO.getQuantity()).isEqualTo(2);

        CartSummaryDTO cartSummaryDTO = new CartSummaryDTO(List.of(cartResponseDTO), 101.0);
        cartSummaryDTO.setItems(List.of(cartResponseDTO));
        cartSummaryDTO.setTotal(111.0);
        assertThat(cartSummaryDTO.getItems()).containsExactly(cartResponseDTO);
        assertThat(cartSummaryDTO.getTotal()).isEqualTo(111.0);

        SubCategoryResponseDTO subCategoryResponseDTO = new SubCategoryResponseDTO(19L, "Sub", "icon", 20L, "Cat");
        subCategoryResponseDTO.setId(21L);
        subCategoryResponseDTO.setName("Deep Clean");
        subCategoryResponseDTO.setIcon("spark");
        subCategoryResponseDTO.setCategoryId(22L);
        subCategoryResponseDTO.setCategoryName("Home");
        assertThat(subCategoryResponseDTO.getId()).isEqualTo(21L);
        assertThat(subCategoryResponseDTO.getName()).isEqualTo("Deep Clean");
        assertThat(subCategoryResponseDTO.getIcon()).isEqualTo("spark");
        assertThat(subCategoryResponseDTO.getCategoryId()).isEqualTo(22L);
        assertThat(subCategoryResponseDTO.getCategoryName()).isEqualTo("Home");

        HomeServiceDTO homeServiceDTO = new HomeServiceDTO();
        homeServiceDTO.setPartnerId(23L);
        homeServiceDTO.setName("Cook");
        homeServiceDTO.setPrice(500.0);
        homeServiceDTO.setOriginalPrice(600.0);
        homeServiceDTO.setRating(4.8);
        homeServiceDTO.setExperience(5);
        homeServiceDTO.setImage("/cook.png");
        assertThat(homeServiceDTO.getPartnerId()).isEqualTo(23L);
        assertThat(homeServiceDTO.getName()).isEqualTo("Cook");
        assertThat(homeServiceDTO.getPrice()).isEqualTo(500.0);
        assertThat(homeServiceDTO.getOriginalPrice()).isEqualTo(600.0);
        assertThat(homeServiceDTO.getRating()).isEqualTo(4.8);
        assertThat(homeServiceDTO.getExperience()).isEqualTo(5);
        assertThat(homeServiceDTO.getImage()).isEqualTo("/cook.png");

        HomeBannerDTO homeBannerDTO = new HomeBannerDTO();
        homeBannerDTO.setLabel("Offer");
        homeBannerDTO.setCtaLabel("Book");
        homeBannerDTO.setSecondaryCtaLabel("Later");
        homeBannerDTO.setTagline("Today");
        assertThat(homeBannerDTO.getLabel()).isEqualTo("Offer");
        assertThat(homeBannerDTO.getCtaLabel()).isEqualTo("Book");
        assertThat(homeBannerDTO.getSecondaryCtaLabel()).isEqualTo("Later");
        assertThat(homeBannerDTO.getTagline()).isEqualTo("Today");

        HomeSectionDTO homeSectionDTO = new HomeSectionDTO();
        homeSectionDTO.setTitle("Recommended");
        homeSectionDTO.setTagline("Fast");
        homeSectionDTO.setData(List.of(homeServiceDTO));
        assertThat(homeSectionDTO.getTitle()).isEqualTo("Recommended");
        assertThat(homeSectionDTO.getTagline()).isEqualTo("Fast");
        assertThat(homeSectionDTO.getData()).containsExactly(homeServiceDTO);
    }

    @Test
    void chatDtosEntitiesBannerEnumsAndUploadHelpersCoverRemainingGeneratedCode() throws Exception {
        Instant now = Instant.parse("2026-07-17T06:30:00Z");
        ChatParticipantSummary participantSummary = new ChatParticipantSummary(ChatParticipantType.CUSTOMER, 1L);
        assertThat(participantSummary.getParticipantType()).isEqualTo(ChatParticipantType.CUSTOMER);
        assertThat(participantSummary.getParticipantId()).isEqualTo(1L);

        ChatMessageResponse chatMessageResponse = ChatMessageResponse.builder()
                .messageId(2L)
                .roomId(3L)
                .senderType(ChatParticipantType.PARTNER)
                .senderId(4L)
                .messageType(ChatMessageType.TEXT)
                .content("hello")
                .deliveryStatus(ChatMessageDeliveryStatus.DELIVERED)
                .deliveredAt(now)
                .readAt(now.plusSeconds(60))
                .deleted(false)
                .createdAt(now)
                .updatedAt(now.plusSeconds(120))
                .build();
        assertThat(chatMessageResponse.getMessageId()).isEqualTo(2L);
        assertThat(chatMessageResponse.getRoomId()).isEqualTo(3L);
        assertThat(chatMessageResponse.getSenderType()).isEqualTo(ChatParticipantType.PARTNER);
        assertThat(chatMessageResponse.getSenderId()).isEqualTo(4L);
        assertThat(chatMessageResponse.getMessageType()).isEqualTo(ChatMessageType.TEXT);
        assertThat(chatMessageResponse.getContent()).isEqualTo("hello");
        assertThat(chatMessageResponse.getDeliveryStatus()).isEqualTo(ChatMessageDeliveryStatus.DELIVERED);
        assertThat(chatMessageResponse.getDeliveredAt()).isEqualTo(now);
        assertThat(chatMessageResponse.getReadAt()).isEqualTo(now.plusSeconds(60));
        assertThat(chatMessageResponse.isDeleted()).isFalse();
        assertThat(chatMessageResponse.getCreatedAt()).isEqualTo(now);
        assertThat(chatMessageResponse.getUpdatedAt()).isEqualTo(now.plusSeconds(120));

        ChatDeliveryReceiptResponse deliveryReceiptResponse = ChatDeliveryReceiptResponse.builder()
                .roomId(3L)
                .messageId(2L)
                .deliveredAt(now)
                .build();
        assertThat(deliveryReceiptResponse.getRoomId()).isEqualTo(3L);
        assertThat(deliveryReceiptResponse.getMessageId()).isEqualTo(2L);
        assertThat(deliveryReceiptResponse.getDeliveredAt()).isEqualTo(now);

        ChatReadReceiptResponse readReceiptResponse = ChatReadReceiptResponse.builder()
                .roomId(3L)
                .messageId(2L)
                .readAt(now.plusSeconds(30))
                .build();
        assertThat(readReceiptResponse.getRoomId()).isEqualTo(3L);
        assertThat(readReceiptResponse.getMessageId()).isEqualTo(2L);
        assertThat(readReceiptResponse.getReadAt()).isEqualTo(now.plusSeconds(30));

        ChatTypingEventResponse typingEventResponse = ChatTypingEventResponse.builder()
                .roomId(3L)
                .participantType(ChatParticipantType.CUSTOMER)
                .participantId(1L)
                .occurredAt(now)
                .build();
        assertThat(typingEventResponse.getRoomId()).isEqualTo(3L);
        assertThat(typingEventResponse.getParticipantType()).isEqualTo(ChatParticipantType.CUSTOMER);
        assertThat(typingEventResponse.getParticipantId()).isEqualTo(1L);
        assertThat(typingEventResponse.getOccurredAt()).isEqualTo(now);

        ChatRoomResponse roomResponse = ChatRoomResponse.builder()
                .roomId(5L)
                .orderId(6L)
                .lastMessagePreview("Latest")
                .lastMessageAt(now)
                .unreadCount(2L)
                .participants(List.of(participantSummary))
                .createdAt(now)
                .updatedAt(now.plusSeconds(10))
                .build();
        assertThat(roomResponse.getRoomId()).isEqualTo(5L);
        assertThat(roomResponse.getOrderId()).isEqualTo(6L);
        assertThat(roomResponse.getLastMessagePreview()).isEqualTo("Latest");
        assertThat(roomResponse.getLastMessageAt()).isEqualTo(now);
        assertThat(roomResponse.getUnreadCount()).isEqualTo(2L);
        assertThat(roomResponse.getParticipants()).containsExactly(participantSummary);
        assertThat(roomResponse.getCreatedAt()).isEqualTo(now);
        assertThat(roomResponse.getUpdatedAt()).isEqualTo(now.plusSeconds(10));

        ChatMessagePageResponse messagePageResponse = ChatMessagePageResponse.builder()
                .content(List.of(chatMessageResponse))
                .page(0)
                .size(20)
                .totalElements(1L)
                .totalPages(1)
                .unreadCount(2L)
                .build();
        assertThat(messagePageResponse.getContent()).containsExactly(chatMessageResponse);
        assertThat(messagePageResponse.getPage()).isZero();
        assertThat(messagePageResponse.getSize()).isEqualTo(20);
        assertThat(messagePageResponse.getTotalElements()).isEqualTo(1L);
        assertThat(messagePageResponse.getTotalPages()).isEqualTo(1);
        assertThat(messagePageResponse.getUnreadCount()).isEqualTo(2L);

        ChatRoomPageResponse roomPageResponse = ChatRoomPageResponse.builder()
                .content(List.of(roomResponse))
                .page(0)
                .size(10)
                .totalElements(1L)
                .totalPages(1)
                .build();
        assertThat(roomPageResponse.getContent()).containsExactly(roomResponse);
        assertThat(roomPageResponse.getPage()).isZero();
        assertThat(roomPageResponse.getSize()).isEqualTo(10);
        assertThat(roomPageResponse.getTotalElements()).isEqualTo(1L);
        assertThat(roomPageResponse.getTotalPages()).isEqualTo(1);

        ChatSocketEvent<String> socketEvent = ChatSocketEvent.<String>builder()
                .eventType(ChatEventType.CHAT_MESSAGE)
                .roomId(5L)
                .payload("payload")
                .occurredAt(now)
                .build();
        assertThat(socketEvent.getEventType()).isEqualTo(ChatEventType.CHAT_MESSAGE);
        assertThat(socketEvent.getRoomId()).isEqualTo(5L);
        assertThat(socketEvent.getPayload()).isEqualTo("payload");
        assertThat(socketEvent.getOccurredAt()).isEqualTo(now);

        ChatRoom room = new ChatRoom();
        room.setId(7L);
        room.setOrderId(8L);
        room.setCustomerId(9L);
        room.setPartnerId(10L);
        room.setLastMessagePreview("Preview");
        room.setLastMessageAt(now);
        room.setDeleted(true);
        room.setDeletedAt(now.plusSeconds(5));
        invokeLifecycle(room, "onCreate");
        invokeLifecycle(room, "onUpdate");
        assertThat(room.getId()).isEqualTo(7L);
        assertThat(room.getOrderId()).isEqualTo(8L);
        assertThat(room.getCustomerId()).isEqualTo(9L);
        assertThat(room.getPartnerId()).isEqualTo(10L);
        assertThat(room.getLastMessagePreview()).isEqualTo("Preview");
        assertThat(room.getLastMessageAt()).isEqualTo(now);
        assertThat(room.isDeleted()).isTrue();
        assertThat(room.getDeletedAt()).isEqualTo(now.plusSeconds(5));
        assertThat(room.getCreatedAt()).isNotNull();
        assertThat(room.getUpdatedAt()).isNotNull();

        ChatParticipant participant = new ChatParticipant();
        participant.setId(11L);
        participant.setRoom(room);
        participant.setParticipantType(ChatParticipantType.CUSTOMER);
        participant.setParticipantId(12L);
        participant.setLastReadMessageId(13L);
        participant.setLastReadAt(now);
        participant.setUnreadCount(5L);
        participant.setDeleted(true);
        participant.setDeletedAt(now.plusSeconds(15));
        invokeLifecycle(participant, "onCreate");
        invokeLifecycle(participant, "onUpdate");
        assertThat(participant.getId()).isEqualTo(11L);
        assertThat(participant.getRoom()).isSameAs(room);
        assertThat(participant.getParticipantType()).isEqualTo(ChatParticipantType.CUSTOMER);
        assertThat(participant.getParticipantId()).isEqualTo(12L);
        assertThat(participant.getLastReadMessageId()).isEqualTo(13L);
        assertThat(participant.getLastReadAt()).isEqualTo(now);
        assertThat(participant.getUnreadCount()).isEqualTo(5L);
        assertThat(participant.isDeleted()).isTrue();
        assertThat(participant.getDeletedAt()).isEqualTo(now.plusSeconds(15));
        assertThat(participant.getCreatedAt()).isNotNull();
        assertThat(participant.getUpdatedAt()).isNotNull();

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setId(14L);
        chatMessage.setRoom(room);
        chatMessage.setSenderType(ChatParticipantType.PARTNER);
        chatMessage.setSenderId(15L);
        chatMessage.setMessageType(ChatMessageType.TEXT);
        chatMessage.setContent("img");
        chatMessage.setDeliveryStatus(ChatMessageDeliveryStatus.READ);
        chatMessage.setDeliveredAt(now.plusSeconds(20));
        chatMessage.setReadAt(now.plusSeconds(25));
        chatMessage.setDeleted(true);
        chatMessage.setDeletedAt(now.plusSeconds(30));
        chatMessage.setDeletedByType(ChatParticipantType.PARTNER);
        chatMessage.setDeletedById(15L);
        invokeLifecycle(chatMessage, "onCreate");
        invokeLifecycle(chatMessage, "onUpdate");
        assertThat(chatMessage.getId()).isEqualTo(14L);
        assertThat(chatMessage.getRoom()).isSameAs(room);
        assertThat(chatMessage.getSenderType()).isEqualTo(ChatParticipantType.PARTNER);
        assertThat(chatMessage.getSenderId()).isEqualTo(15L);
        assertThat(chatMessage.getMessageType()).isEqualTo(ChatMessageType.TEXT);
        assertThat(chatMessage.getContent()).isEqualTo("img");
        assertThat(chatMessage.getDeliveryStatus()).isEqualTo(ChatMessageDeliveryStatus.READ);
        assertThat(chatMessage.getDeliveredAt()).isEqualTo(now.plusSeconds(20));
        assertThat(chatMessage.getReadAt()).isEqualTo(now.plusSeconds(25));
        assertThat(chatMessage.isDeleted()).isTrue();
        assertThat(chatMessage.getDeletedAt()).isEqualTo(now.plusSeconds(30));
        assertThat(chatMessage.getDeletedByType()).isEqualTo(ChatParticipantType.PARTNER);
        assertThat(chatMessage.getDeletedById()).isEqualTo(15L);
        assertThat(chatMessage.getCreatedAt()).isNotNull();
        assertThat(chatMessage.getUpdatedAt()).isNotNull();

        assertThat(BannerPlacement.fromValue(null)).isEqualTo(BannerPlacement.TOP);
        assertThat(BannerPlacement.fromValue(" ")).isEqualTo(BannerPlacement.TOP);
        assertThat(BannerPlacement.fromValue("home_bottom")).isEqualTo(BannerPlacement.BOTTOM);
        assertThat(BannerPlacement.fromValue("top")).isEqualTo(BannerPlacement.TOP);

        assertThat(BannerPlatformType.fromValue(null)).isEqualTo(BannerPlatformType.ALL);
        assertThat(BannerPlatformType.fromValue("mobile")).isEqualTo(BannerPlatformType.ALL);
        assertThat(BannerPlatformType.fromValue("ios")).isEqualTo(BannerPlatformType.IOS);
        assertThat(BannerPlatformType.fromValue("unknown")).isEqualTo(BannerPlatformType.ALL);

        UploadSecurityService uploadSecurityService = new UploadSecurityService();
        MockMultipartFile windowsPath = new MockMultipartFile("file", "C:\\tmp\\a b.jpg", "image/jpeg", new byte[] {1});
        MockMultipartFile safeImage = new MockMultipartFile("file", "photo.jpeg", "image/jpeg", new byte[] {1, 2, 3});
        assertThat(uploadSecurityService.sanitizedOriginalName(windowsPath)).isEqualTo("a_b.jpg");
        assertThat(uploadSecurityService.validateImage(safeImage)).isEqualTo("jpeg");

        Class<?> fileNameOnlyClass = Class.forName("com.lexho.user_service.upload.UploadSecurityService$MultipartFileNameOnly");
        Constructor<?> constructor = fileNameOnlyClass.getDeclaredConstructor(String.class);
        constructor.setAccessible(true);
        Object fileNameOnly = constructor.newInstance("sample.png");
        Method getName = fileNameOnlyClass.getDeclaredMethod("getName");
        Method getOriginalFilename = fileNameOnlyClass.getDeclaredMethod("getOriginalFilename");
        Method getContentType = fileNameOnlyClass.getDeclaredMethod("getContentType");
        Method isEmpty = fileNameOnlyClass.getDeclaredMethod("isEmpty");
        Method getSize = fileNameOnlyClass.getDeclaredMethod("getSize");
        Method getBytes = fileNameOnlyClass.getDeclaredMethod("getBytes");
        Method getInputStream = fileNameOnlyClass.getDeclaredMethod("getInputStream");
        Method transferTo = fileNameOnlyClass.getDeclaredMethod("transferTo", java.io.File.class);
        for (Method method : List.of(getName, getOriginalFilename, getContentType, isEmpty, getSize, getBytes, getInputStream, transferTo)) {
            method.setAccessible(true);
        }
        assertThat(getName.invoke(fileNameOnly)).isEqualTo("file");
        assertThat(getOriginalFilename.invoke(fileNameOnly)).isEqualTo("sample.png");
        assertThat(getContentType.invoke(fileNameOnly)).isNull();
        assertThat(isEmpty.invoke(fileNameOnly)).isEqualTo(true);
        assertThat(getSize.invoke(fileNameOnly)).isEqualTo(0L);
        assertThat((byte[]) getBytes.invoke(fileNameOnly)).isEmpty();
        assertThat(((InputStream) getInputStream.invoke(fileNameOnly)).read()).isEqualTo(-1);
        transferTo.invoke(fileNameOnly, new java.io.File("ignored"));
    }

    private static void invokeLifecycle(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(target);
    }
}

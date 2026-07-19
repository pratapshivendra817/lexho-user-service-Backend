package com.lexho.user_service.customer.dto;

import com.lexho.user_service.addon.dto.AddonSelectionRequest;
import com.lexho.user_service.customer.banner.dto.AdminCustomerBannerUpsertRequest;
import com.lexho.user_service.customer.banner.entity.BannerRedirectType;
import com.lexho.user_service.homebanner.dto.HomeBannerReorderItemRequest;
import com.lexho.user_service.homebanner.dto.HomeBannerReorderRequest;
import com.lexho.user_service.homebanner.dto.HomeBannerUpsertRequest;
import com.lexho.user_service.homebanner.entity.HomeBannerAudience;
import com.lexho.user_service.homebanner.entity.HomeBannerRedirectType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PriorityDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void customerCartRequestsCoverRequiredPositiveAndNestedValidation() {
        CustomerAddToCartRequest add = new CustomerAddToCartRequest();
        add.setQuantity(0);
        AddonSelectionRequest addon = new AddonSelectionRequest();
        addon.setQuantity(0);
        add.setAddons(List.of(addon));

        Set<ConstraintViolation<CustomerAddToCartRequest>> addViolations = validator.validate(add);

        assertThat(messages(addViolations)).containsExactlyInAnyOrder(
                "serviceId is required",
                "quantity must be at least 1",
                "addonId is required");

        CustomerUpdateCartItemRequest update = new CustomerUpdateCartItemRequest();
        update.setItemId(0L);
        update.setQuantity(-1);

        assertThat(paths(validator.validate(update))).containsExactlyInAnyOrder("itemId", "quantity");

        add.setServiceId(1L);
        add.setQuantity(1);
        addon.setAddonId(2L);
        addon.setQuantity(1);
        update.setItemId(1L);
        update.setQuantity(1);

        assertThat(validator.validate(add)).isEmpty();
        assertThat(validator.validate(update)).isEmpty();
    }

    @Test
    void customerBannerRequestCoversMissingSizeAndMinimumBoundaries() {
        AdminCustomerBannerUpsertRequest request = new AdminCustomerBannerUpsertRequest();
        request.setTitle(" ");
        request.setImageUrl("x".repeat(1001));
        request.setBannerImageUrl("x".repeat(1001));
        request.setRedirectUrl("x".repeat(1001));
        request.setPriority(-1);

        assertThat(paths(validator.validate(request))).containsExactlyInAnyOrder(
                "title",
                "imageUrl",
                "bannerImageUrl",
                "redirectUrl",
                "redirectType",
                "active",
                "priority");

        request.setTitle("x".repeat(255));
        request.setImageUrl("x".repeat(1000));
        request.setBannerImageUrl("x".repeat(1000));
        request.setRedirectUrl("x".repeat(1000));
        request.setRedirectType(BannerRedirectType.CATEGORY);
        request.setActive(true);
        request.setPriority(0);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void homeBannerRequestsCoverBlankSizeRequiredAndNestedReorderBoundaries() {
        HomeBannerUpsertRequest banner = new HomeBannerUpsertRequest();
        banner.setTitle(" ");
        banner.setSubtitle("x".repeat(501));
        banner.setImageUrl("");
        banner.setBannerType("x".repeat(51));
        banner.setPriority(-1);

        assertThat(paths(validator.validate(banner))).containsExactlyInAnyOrder(
                "title",
                "subtitle",
                "imageUrl",
                "redirectType",
                "priority",
                "active",
                "bannerType",
                "audience");

        HomeBannerReorderRequest empty = new HomeBannerReorderRequest();
        empty.setItems(List.of());
        assertThat(messages(validator.validate(empty))).containsExactly("items must not be empty");

        HomeBannerReorderItemRequest invalidItem = new HomeBannerReorderItemRequest();
        invalidItem.setPriority(-1);
        HomeBannerReorderRequest nested = new HomeBannerReorderRequest();
        nested.setItems(List.of(invalidItem));
        assertThat(paths(validator.validate(nested)))
                .containsExactlyInAnyOrder("items[0].id", "items[0].priority");

        banner.setTitle("x".repeat(255));
        banner.setSubtitle("x".repeat(500));
        banner.setImageUrl("x".repeat(1000));
        banner.setRedirectType(HomeBannerRedirectType.NONE);
        banner.setPriority(0);
        banner.setActive(true);
        banner.setBannerType("x".repeat(50));
        banner.setAudience(HomeBannerAudience.BOTH);
        invalidItem.setId(1L);
        invalidItem.setPriority(0);

        assertThat(validator.validate(banner)).isEmpty();
        assertThat(validator.validate(nested)).isEmpty();
    }

    private static Set<String> messages(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream().map(ConstraintViolation::getMessage).collect(java.util.stream.Collectors.toSet());
    }

    private static Set<String> paths(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());
    }
}

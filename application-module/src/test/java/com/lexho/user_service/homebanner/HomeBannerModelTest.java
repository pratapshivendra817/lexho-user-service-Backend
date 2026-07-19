package com.lexho.user_service.homebanner;

import com.lexho.user_service.homebanner.dto.HomeBannerReorderItemRequest;
import com.lexho.user_service.homebanner.dto.HomeBannerReorderRequest;
import com.lexho.user_service.homebanner.dto.HomeBannerResponse;
import com.lexho.user_service.homebanner.dto.HomeBannerUpsertRequest;
import com.lexho.user_service.homebanner.entity.HomeBanner;
import com.lexho.user_service.homebanner.entity.HomeBannerAudience;
import com.lexho.user_service.homebanner.entity.HomeBannerRedirectType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class HomeBannerModelTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void homeBannerLifecycleShouldApplyDefaultsAndRefreshTimestamp() {
        HomeBanner banner = new HomeBanner();
        banner.setTitle("Title");
        banner.setImageUrl("/uploads/banner.png");

        banner.onCreate();

        assertThat(banner.getCreatedAt()).isNotNull();
        assertThat(banner.getUpdatedAt()).isEqualTo(banner.getCreatedAt());
        assertThat(banner.getPriority()).isEqualTo(100);
        assertThat(banner.getRedirectType()).isEqualTo(HomeBannerRedirectType.NONE);
        assertThat(banner.getAudience()).isEqualTo(HomeBannerAudience.BOTH);

        LocalDateTime createdAt = banner.getCreatedAt();
        banner.setPriority(7);
        banner.setAudience(HomeBannerAudience.CUSTOMER);
        banner.onUpdate();

        assertThat(banner.getUpdatedAt()).isAfterOrEqualTo(createdAt);
        assertThat(banner.getPriority()).isEqualTo(7);
        assertThat(banner.getAudience()).isEqualTo(HomeBannerAudience.CUSTOMER);
    }

    @Test
    void homeBannerResponseBuilderShouldPreserveValueSemantics() {
        LocalDateTime now = LocalDateTime.now();
        HomeBannerResponse response = HomeBannerResponse.builder()
                .id(10L)
                .title("Banner")
                .subtitle("Subtitle")
                .imageUrl("/uploads/banner.png")
                .redirectType(HomeBannerRedirectType.SERVICE)
                .redirectId(20L)
                .priority(3)
                .active(true)
                .bannerType("promo")
                .audience(HomeBannerAudience.BOTH)
                .createdAt(now)
                .updatedAt(now)
                .build();

        HomeBannerResponse same = HomeBannerResponse.builder()
                .id(10L)
                .title("Banner")
                .subtitle("Subtitle")
                .imageUrl("/uploads/banner.png")
                .redirectType(HomeBannerRedirectType.SERVICE)
                .redirectId(20L)
                .priority(3)
                .active(true)
                .bannerType("promo")
                .audience(HomeBannerAudience.BOTH)
                .createdAt(now)
                .updatedAt(now)
                .build();

        assertThat(response).isEqualTo(same);
        assertThat(response.hashCode()).isEqualTo(same.hashCode());
        assertThat(response.toString()).contains("Banner", "SERVICE", "promo");
    }

    @Test
    void homeBannerRequestsShouldValidateRequiredFieldsAndAccessors() {
        HomeBannerUpsertRequest invalid = new HomeBannerUpsertRequest();
        Set<ConstraintViolation<HomeBannerUpsertRequest>> violations = validator.validate(invalid);

        assertThat(violations).extracting(v -> v.getPropertyPath().toString())
                .contains("title", "imageUrl", "redirectType", "priority", "active", "audience");

        HomeBannerUpsertRequest valid = new HomeBannerUpsertRequest();
        valid.setTitle("Title");
        valid.setSubtitle("Subtitle");
        valid.setImageUrl("/uploads/banner.png");
        valid.setRedirectType(HomeBannerRedirectType.NONE);
        valid.setRedirectId(null);
        valid.setPriority(0);
        valid.setActive(Boolean.TRUE);
        valid.setBannerType("hero");
        valid.setAudience(HomeBannerAudience.CUSTOMER);

        assertThat(validator.validate(valid)).isEmpty();
        assertThat(valid.getTitle()).isEqualTo("Title");
        assertThat(valid.getBannerType()).isEqualTo("hero");

        HomeBannerReorderItemRequest item = new HomeBannerReorderItemRequest();
        item.setId(1L);
        item.setPriority(5);
        HomeBannerReorderRequest reorderRequest = new HomeBannerReorderRequest();
        reorderRequest.setItems(List.of(item));

        assertThat(reorderRequest.getItems()).singleElement().satisfies(reorderItem -> {
            assertThat(reorderItem.getId()).isEqualTo(1L);
            assertThat(reorderItem.getPriority()).isEqualTo(5);
        });
    }
}

package com.lexho.user_service.banner;

import com.lexho.user_service.audit.dto.AdminAuditChangeResponse;
import com.lexho.user_service.audit.dto.AdminAuditLogListResponse;
import com.lexho.user_service.audit.dto.AdminAuditLogResponse;
import com.lexho.user_service.banner.dto.BannerEventRequest;
import com.lexho.user_service.banner.dto.BannerEventResponse;
import com.lexho.user_service.banner.dto.BannerPageResponse;
import com.lexho.user_service.banner.dto.BannerPromotionResponse;
import com.lexho.user_service.banner.dto.BannerRedeemRequest;
import com.lexho.user_service.banner.dto.BannerStatsResponse;
import com.lexho.user_service.banner.dto.BannerUpsertRequest;
import com.lexho.user_service.banner.enums.BannerPlacement;
import com.lexho.user_service.banner.enums.BannerPlatformType;
import com.lexho.user_service.banner.enums.BannerPromoType;
import com.lexho.user_service.banner.enums.BannerRedirectType;
import com.lexho.user_service.banner.enums.BannerStatus;
import com.lexho.user_service.geo.config.GeoEventRabbitConfig;
import com.lexho.user_service.geo.events.GeoEvent;
import com.lexho.user_service.geo.events.GeoEventType;
import com.lexho.user_service.section.dto.SectionCreateRequest;
import com.lexho.user_service.section.dto.SectionItemReorderRequest;
import com.lexho.user_service.section.dto.SectionItemRequest;
import com.lexho.user_service.section.dto.SectionItemResponse;
import com.lexho.user_service.section.dto.SectionItemStatusRequest;
import com.lexho.user_service.section.dto.SectionPageResponse;
import com.lexho.user_service.section.dto.SectionReorderRequest;
import com.lexho.user_service.section.dto.SectionResponse;
import com.lexho.user_service.section.dto.SectionStatusRequest;
import com.lexho.user_service.section.dto.SectionUpdateRequest;
import com.lexho.user_service.section.enums.SectionType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class BannerSectionAuditGeoCoverageAdditionalTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void bannerDtosValidateAndExposeAccessors() {
        BannerUpsertRequest invalid = new BannerUpsertRequest();
        invalid.setDiscountPercentage(-1.0);
        invalid.setMinOrderValue(BigDecimal.valueOf(-1));
        Set<ConstraintViolation<BannerUpsertRequest>> violations = validator.validate(invalid);
        assertThat(violations).extracting(v -> v.getPropertyPath().toString())
                .contains("title", "promoType", "status", "slotNumber", "priority", "startDate",
                        "endDate", "platformType", "placement", "redirectType", "discountPercentage", "minOrderValue");

        BannerUpsertRequest valid = new BannerUpsertRequest();
        valid.setTitle("Promo");
        valid.setSubtitle("Sub");
        valid.setDescription("Desc");
        valid.setBannerImageUrl("/banner.png");
        valid.setPromoType(BannerPromoType.PROMO);
        valid.setStatus(BannerStatus.ACTIVE);
        valid.setSlotNumber(1);
        valid.setPriority(2);
        valid.setDiscountPercentage(15.0);
        valid.setCouponCode("SAVE15");
        valid.setCouponId(3L);
        valid.setStartDate(LocalDateTime.parse("2026-07-17T10:00:00"));
        valid.setEndDate(LocalDateTime.parse("2026-07-18T10:00:00"));
        valid.setCity("Pune");
        valid.setNewUserOnly(true);
        valid.setPremiumUserOnly(false);
        valid.setMinOrderValue(BigDecimal.valueOf(100));
        valid.setPlatformType(BannerPlatformType.ALL);
        valid.setPlacement(BannerPlacement.TOP);
        valid.setRedirectType(BannerRedirectType.CATEGORY);
        valid.setRedirectValue("/category/1");
        valid.setMaxViewsPerUserPerDay(3);
        assertThat(validator.validate(valid)).isEmpty();

        BannerEventRequest eventRequest = new BannerEventRequest();
        eventRequest.setUserId(5L);
        eventRequest.setDeviceId("device");
        eventRequest.setCity("Pune");
        BannerEventResponse eventResponse = new BannerEventResponse("ok");
        BannerPageResponse<String> pageResponse = new BannerPageResponse<>(List.of("a"), 1L, 1, 0);
        BannerPromotionResponse promotionResponse = new BannerPromotionResponse();
        promotionResponse.setId(9L);
        promotionResponse.setTitle("Promo");
        promotionResponse.setSubtitle("Sub");
        promotionResponse.setDescription("Desc");
        promotionResponse.setBannerImageUrl("/banner.png");
        promotionResponse.setPromoType(BannerPromoType.PROMO);
        promotionResponse.setStatus(BannerStatus.ACTIVE);
        promotionResponse.setSlotNumber(1);
        promotionResponse.setPriority(2);
        promotionResponse.setDiscountPercentage(10.0);
        promotionResponse.setCouponCode("SAVE");
        promotionResponse.setCouponId(11L);
        promotionResponse.setStartDate(LocalDateTime.parse("2026-07-17T10:00:00"));
        promotionResponse.setEndDate(LocalDateTime.parse("2026-07-18T10:00:00"));
        promotionResponse.setCity("Pune");
        promotionResponse.setNewUserOnly(true);
        promotionResponse.setPremiumUserOnly(false);
        promotionResponse.setMinOrderValue(BigDecimal.valueOf(199));
        promotionResponse.setPlatformType(BannerPlatformType.ALL);
        promotionResponse.setPlacement(BannerPlacement.TOP);
        promotionResponse.setRedirectType(BannerRedirectType.EXTERNAL_URL);
        promotionResponse.setRedirectValue("https://lexho.com");
        promotionResponse.setClickCount(10L);
        promotionResponse.setImpressionCount(100L);
        promotionResponse.setRedemptionCount(2L);
        promotionResponse.setRevenueGenerated(BigDecimal.valueOf(999));
        promotionResponse.setMaxViewsPerUserPerDay(5);
        BannerRedeemRequest redeemRequest = new BannerRedeemRequest();
        redeemRequest.setUserId(1L);
        redeemRequest.setBookingId(2L);
        redeemRequest.setDiscountApplied(BigDecimal.ZERO);
        BannerStatsResponse stats = new BannerStatsResponse(1L, 2.5, 100L, 25L, 3L, BigDecimal.TEN);

        assertThat(eventRequest.getCity()).isEqualTo("Pune");
        assertThat(eventResponse.getMessage()).isEqualTo("ok");
        assertThat(pageResponse.getContent()).containsExactly("a");
        assertThat(promotionResponse.getRevenueGenerated()).isEqualByComparingTo("999");
        assertThat(validator.validate(redeemRequest)).isEmpty();
        redeemRequest.setDiscountApplied(BigDecimal.valueOf(-1));
        assertThat(validator.validate(redeemRequest)).extracting(ConstraintViolation::getMessage)
                .containsExactly("Discount applied cannot be negative");
        assertThat(stats.getPromoRedemptions()).isEqualTo(3L);
    }

    @Test
    void sectionAuditAndGeoDtosExposeValidationAndRecordState() {
        SectionCreateRequest create = new SectionCreateRequest();
        SectionItemRequest itemRequest = new SectionItemRequest();
        SectionItemStatusRequest itemStatusRequest = new SectionItemStatusRequest();
        SectionStatusRequest statusRequest = new SectionStatusRequest();
        SectionUpdateRequest update = new SectionUpdateRequest();

        assertThat(validator.validate(create)).extracting(v -> v.getPropertyPath().toString())
                .containsExactlyInAnyOrder("type", "isActive");
        assertThat(validator.validate(itemRequest)).extracting(v -> v.getPropertyPath().toString())
                .containsExactlyInAnyOrder("title", "icon", "priority", "isActive");
        assertThat(validator.validate(itemStatusRequest)).extracting(v -> v.getPropertyPath().toString())
                .containsExactly("isActive");
        assertThat(validator.validate(statusRequest)).extracting(v -> v.getPropertyPath().toString())
                .containsExactly("isActive");
        assertThat(validator.validate(update)).extracting(v -> v.getPropertyPath().toString())
                .containsExactlyInAnyOrder("type", "isActive");

        create.setType(SectionType.STATS);
        create.setTitle("Title");
        create.setSubtitle("Sub");
        create.setIcon("icon");
        create.setPriority(1);
        create.setIsActive(true);
        itemRequest.setTitle("Item");
        itemRequest.setSubtitle("Sub");
        itemRequest.setValue("Value");
        itemRequest.setIcon("icon");
        itemRequest.setPriority(2);
        itemRequest.setIsActive(true);
        itemStatusRequest.setIsActive(true);
        statusRequest.setIsActive(false);
        update.setType(SectionType.STATS);
        update.setTitle("Title");
        update.setSubtitle("Sub");
        update.setIcon("icon");
        update.setPriority(3);
        update.setIsActive(true);

        assertThat(validator.validate(create)).isEmpty();
        assertThat(validator.validate(itemRequest)).isEmpty();
        assertThat(validator.validate(itemStatusRequest)).isEmpty();
        assertThat(validator.validate(statusRequest)).isEmpty();
        assertThat(validator.validate(update)).isEmpty();

        SectionItemReorderRequest itemReorder = new SectionItemReorderRequest();
        itemReorder.setId(1L);
        itemReorder.setPriority(1);
        SectionReorderRequest reorder = new SectionReorderRequest();
        reorder.setId(2L);
        reorder.setPriority(2);
        SectionItemResponse itemResponse = new SectionItemResponse();
        itemResponse.setId(3L);
        itemResponse.setSectionId(4L);
        itemResponse.setTitle("Item");
        itemResponse.setSubtitle("Sub");
        itemResponse.setValue("Value");
        itemResponse.setIcon("icon");
        itemResponse.setPriority(5);
        itemResponse.setActive(true);
        SectionResponse response = new SectionResponse();
        response.setId(6L);
        response.setType(SectionType.STATS);
        response.setTitle("Title");
        response.setSubtitle("Sub");
        response.setIcon("icon");
        response.setPriority(6);
        response.setActive(true);
        response.setCreatedAt(LocalDateTime.parse("2026-07-17T09:00:00"));
        response.setItems(List.of(itemResponse));
        SectionPageResponse page = new SectionPageResponse(List.of(response), 1L, 1, 0, 10);

        assertThat(itemReorder.getPriority()).isEqualTo(1);
        assertThat(reorder.getId()).isEqualTo(2L);
        assertThat(page.getContent()).singleElement().extracting(SectionResponse::getTitle).isEqualTo("Title");

        LocalDateTime changedAt = LocalDateTime.parse("2026-07-17T10:00:00");
        AdminAuditChangeResponse change = new AdminAuditChangeResponse("1", "status", "OLD", "NEW", changedAt);
        AdminAuditLogResponse log = new AdminAuditLogResponse(
                "id", "audit", changedAt, changedAt, changedAt, "user", "admin", "username", "ADMIN",
                "UPDATE", "BANNER", "module", "entity-1", "ref-1", "req-1", "corr-1",
                Map.<String, Object>of("old", "v"), Map.<String, Object>of("new", "v2"), List.of(change), List.of(change));
        AdminAuditLogListResponse logs = new AdminAuditLogListResponse(List.of(log));

        assertThat(change.field()).isEqualTo("status");
        assertThat(log.changeHistory()).hasSize(1);
        assertThat(logs.content()).singleElement().extracting(AdminAuditLogResponse::entityType).isEqualTo("BANNER");

        GeoEvent event = new GeoEvent(GeoEventType.PARTNER_MOVED, 7L, Instant.parse("2026-07-17T10:00:00Z"),
                Map.of("lat", 18.52, "lng", 73.85));
        GeoEventRabbitConfig config = new GeoEventRabbitConfig();
        TopicExchange exchange = config.geoEventExchange("geo.exchange");
        Queue queue = config.geoEventQueue("geo.queue", "geo.dlq");
        Queue dlq = config.geoEventDlq("geo.dlq");
        Binding binding = config.geoEventBinding(queue, exchange, "geo.events");

        assertThat(event.type()).isEqualTo(GeoEventType.PARTNER_MOVED);
        assertThat(event.actorId()).isEqualTo(7L);
        assertThat(exchange.getName()).isEqualTo("geo.exchange");
        assertThat(queue.getName()).isEqualTo("geo.queue");
        assertThat(queue.getArguments()).containsEntry("x-dead-letter-routing-key", "geo.dlq");
        assertThat(dlq.getName()).isEqualTo("geo.dlq");
        assertThat(binding.getRoutingKey()).isEqualTo("geo.events.#");
    }
}

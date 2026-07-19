package com.lexho.user_service.coverage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.payment.event.PaymentCapturedEvent;
import com.lexho.payment.event.PaymentFailedEvent;
import com.lexho.payment.event.PaymentRefundProcessedEvent;
import com.lexho.user_service.audit.service.AdminAuditLogQueryService;
import com.lexho.user_service.audit.service.AuditLogService;
import com.lexho.user_service.audit.service.PaymentAuditListener;
import com.lexho.user_service.banner.entity.BannerClickLog;
import com.lexho.user_service.banner.entity.BannerImpressionLog;
import com.lexho.user_service.banner.entity.PromoRedemptionLog;
import com.lexho.user_service.banner.exception.BannerExceptionHandler;
import com.lexho.user_service.banner.exception.BannerNotFoundException;
import com.lexho.user_service.banner.exception.BannerValidationException;
import com.lexho.user_service.chat.entity.ChatMessage;
import com.lexho.user_service.chat.entity.ChatParticipant;
import com.lexho.user_service.chat.entity.ChatRoom;
import com.lexho.user_service.chat.enums.ChatMessageDeliveryStatus;
import com.lexho.user_service.chat.enums.ChatMessageType;
import com.lexho.user_service.chat.enums.ChatParticipantType;
import com.lexho.user_service.chat.model.ChatActor;
import com.lexho.user_service.config.OpenApiConfig;
import com.lexho.user_service.config.PublicApiRateLimitFilter;
import com.lexho.user_service.cookfamilysize.dto.CookFamilySizeAdminResponse;
import com.lexho.user_service.cookfamilysize.dto.CookFamilySizeRequest;
import com.lexho.user_service.cookfamilysize.dto.CookFamilySizeStatusRequest;
import com.lexho.user_service.cookfamilysize.entity.CookFamilySizeEntity;
import com.lexho.user_service.homebanner.dto.HomeBannerReorderItemRequest;
import com.lexho.user_service.homebanner.dto.HomeBannerReorderRequest;
import com.lexho.user_service.homebanner.dto.HomeBannerResponse;
import com.lexho.user_service.homebanner.dto.HomeBannerUpsertRequest;
import com.lexho.user_service.homebanner.entity.HomeBannerAudience;
import com.lexho.user_service.homebanner.entity.HomeBannerRedirectType;
import com.lexho.user_service.section.dto.SectionItemResponse;
import com.lexho.user_service.section.dto.SectionPageResponse;
import com.lexho.user_service.section.dto.SectionResponse;
import com.lexho.user_service.section.dto.SectionUpdateRequest;
import com.lexho.user_service.section.entity.Section;
import com.lexho.user_service.section.enums.SectionType;
import com.lexho.user_service.service.ApiRateLimiterService;
import com.lexho.user_service.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.models.OpenAPI;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageImpl;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CookHomeSectionAuditBannerConfigChatAdditionalTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void cookFamilySizeDtosAndEntityCoverGeneratedSemantics() throws Exception {
        CookFamilySizeRequest requestOne = new CookFamilySizeRequest();
        requestOne.setFamilySize("MEDIUM");
        requestOne.setFamilySizeLabel("3-5 Members");
        requestOne.setActive(true);
        requestOne.setDisplayOrder(1);

        CookFamilySizeRequest requestTwo = new CookFamilySizeRequest();
        requestTwo.setFamilySize("MEDIUM");
        requestTwo.setFamilySizeLabel("3-5 Members");
        requestTwo.setActive(true);
        requestTwo.setDisplayOrder(1);

        CookFamilySizeAdminResponse responseOne = new CookFamilySizeAdminResponse(1L, "MEDIUM", "3-5 Members", true, 1);
        CookFamilySizeAdminResponse responseTwo = new CookFamilySizeAdminResponse(1L, "MEDIUM", "3-5 Members", true, 1);
        CookFamilySizeStatusRequest statusRequest = new CookFamilySizeStatusRequest();
        statusRequest.setActive(true);

        assertThat(requestOne).isEqualTo(requestTwo).hasSameHashCodeAs(requestTwo);
        assertThat(requestOne.toString()).contains("MEDIUM");
        assertThat(responseOne).isEqualTo(responseTwo).hasSameHashCodeAs(responseTwo);
        assertThat(responseOne.toString()).contains("3-5 Members");
        assertThat(statusRequest.getActive()).isTrue();

        CookFamilySizeEntity entity = new CookFamilySizeEntity();
        entity.setId(10L);
        entity.setFamilySize("LARGE");
        entity.setFamilySizeLabel("6-8 Members");
        entity.setActive(true);
        entity.setDisplayOrder(2);
        invoke(entity, "prePersist");
        LocalDateTime createdAt = entity.getCreatedAt();
        invoke(entity, "preUpdate");

        assertThat(entity.getId()).isEqualTo(10L);
        assertThat(entity.getFamilySize()).isEqualTo("LARGE");
        assertThat(entity.getUpdatedAt()).isAfterOrEqualTo(createdAt);
    }

    @Test
    void homeBannerDtosCoverValidationAndValueSemantics() {
        HomeBannerUpsertRequest invalid = new HomeBannerUpsertRequest();
        assertThat(validator.validate(invalid)).extracting(v -> v.getPropertyPath().toString())
                .containsExactlyInAnyOrder("title", "imageUrl", "redirectType", "priority", "active", "audience");

        HomeBannerUpsertRequest valid = new HomeBannerUpsertRequest();
        valid.setTitle("Hero");
        valid.setSubtitle("Subtitle");
        valid.setImageUrl("/hero.png");
        valid.setRedirectType(HomeBannerRedirectType.SERVICE);
        valid.setRedirectId(2L);
        valid.setPriority(1);
        valid.setActive(true);
        valid.setBannerType("hero");
        valid.setAudience(HomeBannerAudience.BOTH);
        assertThat(validator.validate(valid)).isEmpty();

        HomeBannerReorderItemRequest itemOne = new HomeBannerReorderItemRequest();
        itemOne.setId(1L);
        itemOne.setPriority(3);
        HomeBannerReorderItemRequest itemTwo = new HomeBannerReorderItemRequest();
        itemTwo.setId(1L);
        itemTwo.setPriority(3);
        HomeBannerReorderRequest reorder = new HomeBannerReorderRequest();
        reorder.setItems(List.of(itemOne));

        LocalDateTime now = LocalDateTime.parse("2026-07-17T13:00:00");
        HomeBannerResponse responseOne = HomeBannerResponse.builder()
                .id(1L).title("Hero").subtitle("Subtitle").imageUrl("/hero.png")
                .redirectType(HomeBannerRedirectType.SERVICE).redirectId(2L).priority(1).active(true)
                .bannerType("hero").audience(HomeBannerAudience.BOTH).createdAt(now).updatedAt(now).build();
        HomeBannerResponse responseTwo = HomeBannerResponse.builder()
                .id(1L).title("Hero").subtitle("Subtitle").imageUrl("/hero.png")
                .redirectType(HomeBannerRedirectType.SERVICE).redirectId(2L).priority(1).active(true)
                .bannerType("hero").audience(HomeBannerAudience.BOTH).createdAt(now).updatedAt(now).build();

        assertThat(itemOne).isEqualTo(itemTwo).hasSameHashCodeAs(itemTwo);
        assertThat(reorder.getItems()).singleElement().extracting(HomeBannerReorderItemRequest::getPriority).isEqualTo(3);
        assertThat(responseOne).isEqualTo(responseTwo).hasSameHashCodeAs(responseTwo);
    }

    @Test
    void sectionDtosAndEntityExposeStateAndLifecycle() {
        SectionUpdateRequest request = new SectionUpdateRequest();
        request.setType(SectionType.HIGHLIGHTS);
        request.setTitle("Title");
        request.setSubtitle("Subtitle");
        request.setIcon("icon");
        request.setPriority(5);
        request.setIsActive(true);

        SectionItemResponse item = new SectionItemResponse();
        item.setId(1L);
        item.setSectionId(2L);
        item.setTitle("Item");
        item.setSubtitle("Sub");
        item.setValue("Val");
        item.setIcon("icon");
        item.setPriority(1);
        item.setActive(true);

        SectionResponse response = new SectionResponse();
        response.setId(3L);
        response.setType(SectionType.STATS);
        response.setTitle("Title");
        response.setSubtitle("Subtitle");
        response.setIcon("icon");
        response.setPriority(2);
        response.setActive(true);
        response.setCreatedAt(LocalDateTime.parse("2026-07-17T12:30:00"));
        response.setItems(List.of(item));

        SectionPageResponse page = new SectionPageResponse(List.of(response), 1L, 1, 0, 10);

        Section entity = new Section();
        entity.setId(4L);
        entity.setType(SectionType.TRUST);
        entity.setTitle("Trust");
        entity.setSubtitle("Sub");
        entity.setIcon("shield");
        entity.setPriority(4);
        entity.setActive(true);
        entity.onCreate();

        assertThat(request.getType()).isEqualTo(SectionType.HIGHLIGHTS);
        assertThat(page.getContent()).singleElement().extracting(SectionResponse::getTitle).isEqualTo("Title");
        assertThat(entity.getCreatedAt()).isNotNull();
    }

    @Test
    void auditListenerQueryFallbackAndBannerExceptionHandlerCoverRemainingBranches() throws Exception {
        AuditLogService auditLogService = mock(AuditLogService.class);
        PaymentAuditListener listener = new PaymentAuditListener(auditLogService);
        listener.onPaymentCaptured(new PaymentCapturedEvent(10L));
        listener.onRefundProcessed(new PaymentRefundProcessedEvent(11L, "refund_1", BigDecimal.TEN));
        listener.onPaymentFailed(new PaymentFailedEvent(12L, "FAILED"));

        verify(auditLogService).log(null, null, 10L, "PAYMENT_SUCCESS", null, "SUCCESS");
        verify(auditLogService).log(null, null, 11L, "PAYMENT_REFUNDED", null, "refund_1");
        verify(auditLogService).log(null, null, 12L, "PAYMENT_FAILED", null, "FAILED");

        var repository = mock(com.lexho.user_service.audit.repository.AuditLogRepository.class);
        var auditLog = new com.lexho.user_service.audit.entity.AuditLog();
        ReflectionTestUtils.setField(auditLog, "id", 1L);
        auditLog.setActorId(9L);
        auditLog.setActorIdentifier("admin@lexho.com");
        auditLog.setActorRole("ADMIN");
        auditLog.setAction("UPDATE");
        auditLog.setEntityType("BANNER");
        auditLog.setEntityId("5");
        ReflectionTestUtils.setField(auditLog, "timestamp", LocalDateTime.parse("2026-07-17T10:00:00"));
        auditLog.setOldValue("not-json");
        auditLog.setNewValue("{\"status\":\"ACTIVE\"}");
        when(repository.findById(1L)).thenReturn(java.util.Optional.of(auditLog));
        when(repository.search(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(auditLog)));

        AdminAuditLogQueryService queryService = new AdminAuditLogQueryService(repository, new ObjectMapper());
        assertThat(queryService.detail(1L).getData().oldValue()).containsEntry("value", "not-json");
        assertThat(queryService.list("2026-07-01", "2026-07-17", " UPDATE ", " admin ", " ADMIN ").getData().content())
                .hasSize(1);
        assertThatThrownBy(() -> queryService.list("bad", null, null, null, null))
                .isInstanceOf(com.lexho.user_service.exception.BadRequestException.class)
                .hasMessage("Invalid startDate");
        assertThatThrownBy(() -> queryService.list(null, "bad", null, null, null))
                .isInstanceOf(com.lexho.user_service.exception.BadRequestException.class)
                .hasMessage("Invalid endDate");

        BannerExceptionHandler handler = new BannerExceptionHandler();
        assertThat(handler.handleNotFound(new BannerNotFoundException(7L)).getStatusCode().value()).isEqualTo(404);
        assertThat(handler.handleValidation(new BannerValidationException("bad")).getBody()).containsEntry("message", "bad");

        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "banner");
        bindingResult.addError(new FieldError("banner", "title", "Title is required"));
        Method method = CookHomeSectionAuditBannerConfigChatAdditionalTest.class.getDeclaredMethod("validationTarget");
        MethodParameter parameter = new MethodParameter(method, -1);
        MethodArgumentNotValidException manve = new MethodArgumentNotValidException(parameter, bindingResult);
        assertThat(handler.handleMethodValidation(manve).getBody()).containsEntry("message", "Title is required");
        assertThat(handler.handleConstraintViolation(new ConstraintViolationException(Set.of())).getBody())
                .containsEntry("message", "Invalid request");
        assertThat(handler.handleMalformedInput(new IllegalArgumentException("bad")).getBody())
                .containsEntry("message", "Invalid request");
    }

    @SuppressWarnings("unused")
    private static void validationTarget() {
    }

    @Test
    void bannerEntitiesOpenApiChatActorAndCachedBodyHelperCoverRemainingUtilities() throws Exception {
        BannerClickLog clickLog = new BannerClickLog();
        clickLog.setId(1L);
        clickLog.setBannerId(2L);
        clickLog.setUserId(3L);
        clickLog.setDeviceId("device");
        clickLog.setCity("Pune");
        clickLog.onCreate();

        BannerImpressionLog impressionLog = new BannerImpressionLog();
        impressionLog.setId(4L);
        impressionLog.setBannerId(5L);
        impressionLog.setUserId(6L);
        impressionLog.setDeviceId("device");
        impressionLog.setCity("Mumbai");
        impressionLog.onCreate();

        PromoRedemptionLog redemptionLog = new PromoRedemptionLog();
        redemptionLog.setId(7L);
        redemptionLog.setBannerId(8L);
        redemptionLog.setUserId(9L);
        redemptionLog.setBookingId(10L);
        redemptionLog.setDiscountApplied(BigDecimal.TEN);
        redemptionLog.onCreate();

        assertThat(clickLog.getTimestamp()).isNotNull();
        assertThat(impressionLog.getCity()).isEqualTo("Mumbai");
        assertThat(redemptionLog.getDiscountApplied()).isEqualByComparingTo("10");

        OpenAPI openAPI = new OpenApiConfig().locationOpenApi();
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Lexho Customer Location APIs");
        assertThat(openAPI.getComponents().getSecuritySchemes()).containsKey("bearerAuth");

        assertThat(ChatActor.fromRole("customer", 1L).principalName()).isEqualTo("CUSTOMER:1");
        assertThat(ChatActor.fromRole("partner", 2L).principalName()).isEqualTo("PARTNER:2");
        assertThatThrownBy(() -> ChatActor.fromRole("admin", 3L)).isInstanceOf(IllegalArgumentException.class);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "user", null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        authentication.setDetails(5L);
        assertThat(ChatActor.fromAuthentication(authentication).principalName()).isEqualTo("CUSTOMER:5");
        UsernamePasswordAuthenticationToken missingRole = new UsernamePasswordAuthenticationToken("user", null, List.of());
        missingRole.setDetails(5L);
        assertThatThrownBy(() -> ChatActor.fromAuthentication(missingRole)).isInstanceOf(IllegalArgumentException.class);

        ChatRoom room = new ChatRoom();
        room.setId(1L);
        room.setOrderId(2L);
        room.setCustomerId(3L);
        room.setPartnerId(4L);
        room.setLastMessagePreview("Hello");
        room.setDeleted(false);
        invoke(room, "onCreate");
        invoke(room, "onUpdate");

        ChatParticipant participant = new ChatParticipant();
        participant.setId(5L);
        participant.setRoom(room);
        participant.setParticipantType(ChatParticipantType.CUSTOMER);
        participant.setParticipantId(3L);
        participant.setUnreadCount(2L);
        invoke(participant, "onCreate");
        invoke(participant, "onUpdate");

        ChatMessage message = new ChatMessage();
        message.setId(6L);
        message.setRoom(room);
        message.setSenderType(ChatParticipantType.PARTNER);
        message.setSenderId(4L);
        message.setMessageType(ChatMessageType.TEXT);
        message.setContent("hello");
        message.setDeliveryStatus(ChatMessageDeliveryStatus.SENT);
        invoke(message, "onCreate");
        invoke(message, "onUpdate");

        assertThat(message.getUpdatedAt()).isNotNull();
        assertThat(participant.getRoom().getId()).isEqualTo(1L);

        ApiRateLimiterService rateLimiterService = mock(ApiRateLimiterService.class);
        JwtUtil jwtUtil = mock(JwtUtil.class);
        PublicApiRateLimitFilter filter = new PublicApiRateLimitFilter(rateLimiterService, new ObjectMapper(), jwtUtil);
        Class<?> cachedBodyRequestClass = Class.forName("com.lexho.user_service.config.PublicApiRateLimitFilter$CachedBodyRequest");
        Constructor<?> constructor = cachedBodyRequestClass.getDeclaredConstructor(HttpServletRequest.class);
        constructor.setAccessible(true);
        MockHttpServletRequest sourceRequest = new MockHttpServletRequest("POST", "/otp/send");
        sourceRequest.setServletPath("/otp/send");
        sourceRequest.setContentType("application/json");
        sourceRequest.setContent("{\"mobile\":\"9999999999\"}".getBytes(StandardCharsets.UTF_8));
        Object cachedBodyRequest = constructor.newInstance(sourceRequest);
        Method getReader = cachedBodyRequestClass.getDeclaredMethod("getReader");
        getReader.setAccessible(true);
        java.io.BufferedReader reader = (java.io.BufferedReader) getReader.invoke(cachedBodyRequest);
        assertThat(reader.readLine()).contains("9999999999");
        Method getInputStream = cachedBodyRequestClass.getDeclaredMethod("getInputStream");
        getInputStream.setAccessible(true);
        ServletInputStream inputStream = (ServletInputStream) getInputStream.invoke(cachedBodyRequest);
        while (inputStream.read() != -1) {
            // exhaust
        }
        assertThat(inputStream.isFinished()).isTrue();
        assertThat(inputStream.isReady()).isTrue();
        inputStream.setReadListener(null);

        Claims claims = mock(Claims.class);
        when(claims.get("userId")).thenReturn(99L);
        when(jwtUtil.validateToken("token")).thenReturn(claims);
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/api/partner/requests/1/accept");
        request.setServletPath("/api/partner/requests/1/accept");
        request.addHeader("Authorization", "Bearer token");
        String userId = (String) ReflectionTestUtils.invokeMethod(filter, "userId", request);
        assertThat(userId).isEqualTo("99");

        MockHttpServletRequest fallbackRequest = new MockHttpServletRequest("GET", "/x");
        fallbackRequest.setRemoteAddr("127.0.0.1");
        fallbackRequest.addHeader("User-Agent", "JUnit");
        assertThat((String) ReflectionTestUtils.invokeMethod(filter, "clientKey", fallbackRequest)).contains("127.0.0.1");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest multipartRequest = new MockHttpServletRequest("POST", "/upload");
        multipartRequest.setServletPath("/upload");
        multipartRequest.setContentType("multipart/form-data");
        filter.doFilter(multipartRequest, response, (req, res) -> ((MockHttpServletResponse) res).setStatus(204));
        assertThat(response.getStatus()).isEqualTo(204);
    }

    private static void invoke(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(target);
    }
}

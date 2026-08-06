package com.lexho.user_service.controller;

import com.lexho.user_service.banner.controller.CustomerBannerPromotionController;
import com.lexho.user_service.banner.dto.BannerEventRequest;
import com.lexho.user_service.banner.dto.BannerEventResponse;
import com.lexho.user_service.banner.dto.BannerPromotionResponse;
import com.lexho.user_service.banner.dto.BannerRedeemRequest;
import com.lexho.user_service.banner.entity.BannerPromotion;
import com.lexho.user_service.banner.enums.BannerPlacement;
import com.lexho.user_service.banner.enums.BannerPlatformType;
import com.lexho.user_service.banner.enums.BannerStatus;
import com.lexho.user_service.banner.repository.BannerPromotionRepository;
import com.lexho.user_service.banner.scheduler.BannerPromotionExpiryScheduler;
import com.lexho.user_service.banner.service.CustomerBannerService;
import com.lexho.user_service.chat.controller.ChatWebSocketController;
import com.lexho.user_service.chat.dto.ChatSendMessageRequest;
import com.lexho.user_service.chat.dto.ChatTypingRequest;
import com.lexho.user_service.chat.enums.ChatEventType;
import com.lexho.user_service.chat.enums.ChatMessageType;
import com.lexho.user_service.chat.model.ChatActor;
import com.lexho.user_service.chat.service.ChatService;
import com.lexho.user_service.config.SecurityExposureProperties;
import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.geo.config.GeoEventRabbitListenerConfig;
import com.lexho.user_service.geo.events.GeoEventConsumers;
import com.lexho.user_service.geo.events.GeoEventPublisher;
import com.lexho.user_service.geo.events.GeoEventType;
import com.lexho.user_service.partnerrequest.controller.PartnerRequestController;
import com.lexho.user_service.partnerrequest.dto.PagedResponse;
import com.lexho.user_service.partnerrequest.dto.PartnerIncomingRequestResponse;
import com.lexho.user_service.partnerrequest.dto.PartnerRejectRequest;
import com.lexho.user_service.partnerrequest.dto.PartnerRequestActionResponse;
import com.lexho.user_service.partnerrequest.dto.PartnerStatusResponse;
import com.lexho.user_service.partnerrequest.dto.PartnerStatusUpdateRequest;
import com.lexho.user_service.partnerrequest.model.PartnerServiceRequestStatus;
import com.lexho.user_service.partnerrequest.scheduler.PartnerRequestExpiryScheduler;
import com.lexho.user_service.partnerrequest.service.PartnerRequestService;
import com.lexho.user_service.section.controller.AdminSectionController;
import com.lexho.user_service.section.controller.CustomerSectionController;
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
import com.lexho.user_service.section.service.SectionManagementService;
import com.lexho.user_service.service.ApiRateLimiterService;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ControllerSchedulerAndGeoCoverageAdditionalTest {

    @Test
    void sectionControllersDelegateAllEndpoints() {
        SectionManagementService service = mock(SectionManagementService.class);
        AdminSectionController adminController = new AdminSectionController(service);
        CustomerSectionController customerController = new CustomerSectionController(service);
        SectionCreateRequest createRequest = new SectionCreateRequest();
        SectionUpdateRequest updateRequest = new SectionUpdateRequest();
        SectionItemRequest itemRequest = new SectionItemRequest();
        SectionItemStatusRequest itemStatusRequest = new SectionItemStatusRequest();
        SectionStatusRequest statusRequest = new SectionStatusRequest();
        SectionResponse sectionResponse = new SectionResponse();
        SectionItemResponse itemResponse = new SectionItemResponse();
        SectionPageResponse pageResponse = new SectionPageResponse(List.of(sectionResponse), 1L, 1, 0, 10);
        List<SectionResponse> sectionResponses = List.of(sectionResponse);
        List<SectionItemResponse> itemResponses = List.of(itemResponse);
        List<SectionReorderRequest> sectionReorderRequests = List.of(new SectionReorderRequest());
        List<SectionItemReorderRequest> itemReorderRequests = List.of(new SectionItemReorderRequest());

        when(service.createSection(createRequest)).thenReturn(sectionResponse);
        when(service.addSectionItems(1L, List.of(itemRequest))).thenReturn(itemResponses);
        when(service.updateSectionItem(1L, 2L, itemRequest)).thenReturn(itemResponse);
        when(service.updateSectionItemStatus(1L, 2L, itemStatusRequest)).thenReturn(itemResponse);
        when(service.updateSection(1L, updateRequest)).thenReturn(sectionResponse);
        when(service.reorderSections(sectionReorderRequests)).thenReturn(sectionResponses);
        when(service.reorderItems(1L, itemReorderRequests)).thenReturn(itemResponses);
        when(service.updateSectionStatus(1L, statusRequest)).thenReturn(sectionResponse);
        when(service.getSections(0, 10)).thenReturn(pageResponse);
        when(service.getActiveCustomerSections()).thenReturn(sectionResponses);

        assertThat(adminController.create(createRequest).getBody().getData()).isSameAs(sectionResponse);
        assertThat(adminController.addItems(1L, List.of(itemRequest)).getBody().getData()).isEqualTo(itemResponses);
        assertThat(adminController.updateItem(1L, 2L, itemRequest).getBody().getData()).isSameAs(itemResponse);
        assertThat(adminController.deleteItem(1L, 2L).getBody().isSuccess()).isTrue();
        assertThat(adminController.updateItemStatus(1L, 2L, itemStatusRequest).getBody().getData()).isSameAs(itemResponse);
        assertThat(adminController.update(1L, updateRequest).getBody().getData()).isSameAs(sectionResponse);
        assertThat(adminController.reorderSections(sectionReorderRequests).getBody().getData()).isEqualTo(sectionResponses);
        assertThat(adminController.reorder(1L, itemReorderRequests).getBody().getData()).isEqualTo(itemResponses);
        assertThat(adminController.status(1L, statusRequest).getBody().getData()).isSameAs(sectionResponse);
        assertThat(adminController.all(0, 10).getBody().getData()).isSameAs(pageResponse);
        assertThat(customerController.list().getBody().getData()).isEqualTo(sectionResponses);

        verify(service).deleteSectionItem(1L, 2L);
    }

    @Test
    void partnerRequestControllerDelegatesAllEndpointsAndRateLimitsActions() {
        PartnerRequestService service = mock(PartnerRequestService.class);
        ApiRateLimiterService rateLimiterService = mock(ApiRateLimiterService.class);
        SecurityExposureProperties exposureProperties = new SecurityExposureProperties();
        exposureProperties.setAllowPartnerRequestDevSeed(true);
        PartnerRequestController controller = new PartnerRequestController(service, rateLimiterService, exposureProperties);
        PartnerStatusUpdateRequest statusRequest = new PartnerStatusUpdateRequest();
        statusRequest.setIsOnline(true);
        PartnerStatusResponse statusResponse = new PartnerStatusResponse();
        statusResponse.setPartnerId(7L);
        statusResponse.setOnline(true);
        PartnerIncomingRequestResponse incoming = new PartnerIncomingRequestResponse();
        incoming.setRequestId(11L);
        PagedResponse<PartnerIncomingRequestResponse> page = new PagedResponse<>(List.of(incoming), 1L, 1, 0, 20);
        PartnerRequestActionResponse actionResponse = new PartnerRequestActionResponse();
        actionResponse.setRequestId(11L);
        actionResponse.setStatus(PartnerServiceRequestStatus.ACCEPTED);
        PartnerRejectRequest rejectRequest = new PartnerRejectRequest();
        rejectRequest.setReason("busy");

        when(service.updateOnlineStatus(7L, statusRequest)).thenReturn(statusResponse);
        when(service.getIncomingRequests(7L, PartnerServiceRequestStatus.PENDING, 0, 20)).thenReturn(page);
        when(service.getRequestDetail(7L, 11L)).thenReturn(incoming);
        when(service.accept(7L, 11L)).thenReturn(actionResponse);
        when(service.reject(7L, 11L, rejectRequest)).thenReturn(actionResponse);
        when(service.createDevPendingRequest(7L)).thenReturn(incoming);

        assertThat(controller.updateStatus(7L, statusRequest).getData()).isSameAs(statusResponse);
        assertThat(controller.requests(7L, PartnerServiceRequestStatus.PENDING, 0, 20).getData()).isSameAs(page);
        assertThat(controller.detail(7L, 11L).getData()).isSameAs(incoming);
        assertThat(controller.accept(7L, 11L).getData()).isSameAs(actionResponse);
        assertThat(controller.reject(7L, 11L, rejectRequest).getData()).isSameAs(actionResponse);
        assertThat(controller.seedPendingForDev(7L).getData()).isSameAs(incoming);

        verify(rateLimiterService).check(eq("partner_accept"), eq("7"), eq(20), eq(Duration.ofMinutes(1)),
                eq("Too many accept attempts. Please slow down"));
        verify(rateLimiterService).check(eq("partner_reject"), eq("7"), eq(20), eq(Duration.ofMinutes(1)),
                eq("Too many reject attempts. Please slow down"));
    }

    @Test
    void bannerControllerAndSchedulersCoverPublicMethods() {
        CustomerBannerService customerBannerService = mock(CustomerBannerService.class);
        CustomerBannerPromotionController controller = new CustomerBannerPromotionController(customerBannerService);
        BannerEventRequest eventRequest = new BannerEventRequest();
        eventRequest.setUserId(7L);
        BannerRedeemRequest redeemRequest = new BannerRedeemRequest();
        redeemRequest.setDiscountApplied(BigDecimal.TEN);
        BannerPromotionResponse banner = new BannerPromotionResponse();
        banner.setTitle("Promo");
        BannerEventResponse eventResponse = new BannerEventResponse("ok");

        when(customerBannerService.homepageBanners("Pune", BannerPlatformType.ALL, true, false, BigDecimal.TEN, 7L, "dev"))
                .thenReturn(List.of(banner));
        when(customerBannerService.trackClick(1L, eventRequest)).thenReturn(eventResponse);
        when(customerBannerService.trackImpression(1L, eventRequest)).thenReturn(eventResponse);
        when(customerBannerService.redeem(1L, redeemRequest)).thenReturn(eventResponse);

        ResponseEntity<List<BannerPromotionResponse>> homepage = controller.homepage(
                "Pune", BannerPlatformType.ALL, true, false, BigDecimal.TEN, 7L, "dev");
        assertThat(homepage.getBody()).singleElement().extracting(BannerPromotionResponse::getTitle).isEqualTo("Promo");
        assertThat(controller.click(1L, eventRequest).getBody().getMessage()).isEqualTo("ok");
        assertThat(controller.impression(1L, eventRequest).getBody().getMessage()).isEqualTo("ok");
        assertThat(controller.redeem(1L, redeemRequest).getBody().getMessage()).isEqualTo("ok");

        BannerPromotionRepository repository = mock(BannerPromotionRepository.class);
        BannerPromotionExpiryScheduler scheduler = new BannerPromotionExpiryScheduler(repository);
        BannerPromotion expired = new BannerPromotion();
        expired.setStatus(BannerStatus.ACTIVE);
        expired.setPlacement(BannerPlacement.TOP);
        expired.setEndDate(LocalDateTime.now().minusDays(1));

        when(repository.findByStatusAndEndDateBeforeAndDeletedFalse(eq(BannerStatus.ACTIVE), any(LocalDateTime.class)))
                .thenReturn(List.of(expired))
                .thenReturn(List.of());

        scheduler.expireBanners();
        assertThat(expired.getStatus()).isEqualTo(BannerStatus.EXPIRED);
        verify(repository).saveAll(List.of(expired));

        scheduler.expireBanners();
        verify(repository, never()).saveAll(List.of());

        PartnerRequestService partnerRequestService = mock(PartnerRequestService.class);
        PartnerRequestExpiryScheduler partnerScheduler = new PartnerRequestExpiryScheduler(partnerRequestService);
        when(partnerRequestService.expirePendingRequests()).thenReturn(2).thenReturn(0);

        partnerScheduler.expirePendingRequests();
        partnerScheduler.expirePendingRequests();

        verify(partnerRequestService, org.mockito.Mockito.times(2)).expirePendingRequests();
    }

    @Test
    void chatWebSocketControllerUsesAuthenticationActorAndRejectsMissingAuthentication() {
        ChatService chatService = mock(ChatService.class);
        ChatWebSocketController controller = new ChatWebSocketController(chatService);
        ChatSendMessageRequest sendRequest = new ChatSendMessageRequest();
        sendRequest.setRoomId(10L);
        sendRequest.setContent("hello");
        sendRequest.setMessageType(ChatMessageType.TEXT);
        ChatTypingRequest typingRequest = new ChatTypingRequest();
        typingRequest.setRoomId(10L);
        UsernamePasswordAuthenticationToken principal = new UsernamePasswordAuthenticationToken(
                "partner", null, List.of(new SimpleGrantedAuthority("ROLE_PARTNER")));
        principal.setDetails(99L);

        controller.sendMessage(sendRequest, principal);
        controller.typingStart(typingRequest, principal);
        controller.typingStop(typingRequest, principal);

        verify(chatService).sendMessage(sendRequest, new ChatActor(com.lexho.user_service.chat.enums.ChatParticipantType.PARTNER, 99L));
        verify(chatService).publishTyping(10L, new ChatActor(com.lexho.user_service.chat.enums.ChatParticipantType.PARTNER, 99L), ChatEventType.TYPING_START);
        verify(chatService).publishTyping(10L, new ChatActor(com.lexho.user_service.chat.enums.ChatParticipantType.PARTNER, 99L), ChatEventType.TYPING_STOP);

        assertThatThrownBy(() -> controller.sendMessage(sendRequest, () -> "plain"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Authenticated chat actor is required");
    }

    @Test
    void geoPublisherConsumersAndListenerConfigCoverPublicMethods() {
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        GeoEventPublisher geoEventPublisher = new GeoEventPublisher(publisher);
        geoEventPublisher.publish(GeoEventType.LOCATION_UPDATED, 1L, Map.of("lat", 18.52));
        verify(publisher).publishEvent(any(com.lexho.user_service.geo.events.GeoEvent.class));

        doThrow(new RuntimeException("down")).when(publisher).publishEvent(any());
        geoEventPublisher.publish(GeoEventType.PARTNER_OFFLINE, 2L, Map.of());

        GeoEventConsumers consumers = new GeoEventConsumers();
        consumers.consumeRabbit("{json}");
        consumers.consumeRabbitDlq("{json}");

        SimpleRabbitListenerContainerFactoryConfigurer configurer = mock(SimpleRabbitListenerContainerFactoryConfigurer.class);
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        GeoEventRabbitListenerConfig config = new GeoEventRabbitListenerConfig();
        SimpleRabbitListenerContainerFactory factory = config.geoEventRabbitListenerContainerFactory(
                configurer, connectionFactory, 0, 50L, 1.0d, 20L);

        verify(configurer).configure(factory, connectionFactory);
        assertThat((Object[]) ReflectionTestUtils.getField(factory, "adviceChain")).hasSize(1);
    }
}

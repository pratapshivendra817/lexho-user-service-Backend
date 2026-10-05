package com.lexho.user_service.lexhopartner.service;

import com.lexho.notification.repository.NotificationRepository;
import com.lexho.user_service.admin.repository.SupportTicketRepository;
import com.lexho.user_service.lexhopartner.entity.PartnerPenalty;
import com.lexho.user_service.lexhopartner.repository.PartnerPenaltyRepository;
import com.lexho.user_service.localization.dynamic.DynamicContentTranslationService;
import com.lexho.user_service.partnerrequest.entity.PartnerServiceRequestEntity;
import com.lexho.user_service.partnerrequest.model.PartnerServiceRequestStatus;
import com.lexho.user_service.partnerrequest.repository.PartnerServiceRequestRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PartnerActivityFeedServiceTest {

    @Test
    void activityFeedTranslatesWarningReasonAndDispatchServiceType() {
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        SupportTicketRepository supportTicketRepository = mock(SupportTicketRepository.class);
        PartnerPenaltyRepository penaltyRepository = mock(PartnerPenaltyRepository.class);
        PartnerServiceRequestRepository requestRepository = mock(PartnerServiceRequestRepository.class);
        DynamicContentTranslationService translationService = mock(DynamicContentTranslationService.class);

        PartnerPenalty warning = new PartnerPenalty();
        warning.setId(44L);
        warning.setPartnerId(10L);
        warning.setStatus("WARNING");
        warning.setReason("Late arrival");
        warning.setCreatedAt(LocalDateTime.now().minusMinutes(10));
        warning.setUpdatedAt(LocalDateTime.now().minusMinutes(10));

        PartnerServiceRequestEntity request = new PartnerServiceRequestEntity();
        ReflectionTestUtils.setField(request, "id", 77L);
        ReflectionTestUtils.setField(request, "createdAt", LocalDateTime.now());
        request.setPartnerId(10L);
        request.setCartId(99L);
        request.setCustomerId(20L);
        request.setServiceType("Cleaning");
        request.setStatus(PartnerServiceRequestStatus.PENDING);

        when(notificationRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(10L)).thenReturn(List.of());
        when(supportTicketRepository.findByUserIdAndCategoryIdStartingWithOrderByCreatedAtDesc(
                eq(10L), eq("PARTNER:"), any(PageRequest.class))).thenReturn(new PageImpl<>(List.of()));
        when(penaltyRepository.findByPartnerIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(warning));
        when(requestRepository.findByPartnerIdOrderByCreatedAtDesc(eq(10L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(request)));
        when(translationService.translateForPartner(10L, null, "partner_warning", 44L, "reason", "Late arrival"))
                .thenReturn("Der se pahuncha");
        when(translationService.translateForPartner(10L, null, "partner_service_request", 77L, "service_type", "Cleaning"))
                .thenReturn("Safai");

        PartnerActivityFeedService service = new PartnerActivityFeedService(
                notificationRepository, supportTicketRepository, penaltyRepository, requestRepository, translationService);

        var response = service.activity(10L, 0, 20);

        assertThat(response.content()).extracting(item -> item.get("message"))
                .contains("Der se pahuncha", "Safai");
    }
}

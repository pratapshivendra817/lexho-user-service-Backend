package com.lexho.user_service.partnerrequest;

import com.lexho.user_service.partnerrequest.dto.PagedResponse;
import com.lexho.user_service.partnerrequest.dto.PartnerIncomingRequestResponse;
import com.lexho.user_service.partnerrequest.dto.PartnerRejectRequest;
import com.lexho.user_service.partnerrequest.dto.PartnerRequestActionResponse;
import com.lexho.user_service.partnerrequest.dto.PartnerRequestAddressResponse;
import com.lexho.user_service.partnerrequest.dto.PartnerStatusResponse;
import com.lexho.user_service.partnerrequest.dto.PartnerStatusUpdateRequest;
import com.lexho.user_service.partnerrequest.model.PartnerServiceRequestStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PartnerRequestDtoCoverageAdditionalTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void partnerRequestDtosExposeAccessorsValidationAndPagingState() {
        PartnerRejectRequest rejectRequest = new PartnerRejectRequest();
        rejectRequest.setReason("x".repeat(256));
        Set<ConstraintViolation<PartnerRejectRequest>> rejectViolations = validator.validate(rejectRequest);
        assertThat(rejectViolations).extracting(ConstraintViolation::getMessage)
                .containsExactly("Reason must be <= 255 characters");

        PartnerStatusUpdateRequest statusUpdateRequest = new PartnerStatusUpdateRequest();
        assertThat(validator.validate(statusUpdateRequest)).extracting(ConstraintViolation::getMessage)
                .containsExactly("isOnline is required");
        statusUpdateRequest.setIsOnline(true);
        assertThat(validator.validate(statusUpdateRequest)).isEmpty();

        PagedResponse<String> firstPage = new PagedResponse<>(List.of("a"), 5L, 3, 0, 2);
        PagedResponse<String> lastPage = new PagedResponse<>(List.of("b"), 5L, 3, 2, 2);
        assertThat(firstPage.isLast()).isFalse();
        assertThat(lastPage.isLast()).isTrue();
        assertThat(firstPage.getContent()).containsExactly("a");

        PartnerRequestAddressResponse address = new PartnerRequestAddressResponse();
        address.setFullAddress("Pune");
        address.setCity("Pune");
        address.setLatitude(18.52);
        address.setLongitude(73.85);

        PartnerIncomingRequestResponse incoming = new PartnerIncomingRequestResponse();
        incoming.setRequestId(1L);
        incoming.setCartId(2L);
        incoming.setCustomerId(3L);
        incoming.setServiceId(4L);
        incoming.setServiceType("COOK");
        incoming.setQuantity(2);
        incoming.setScheduledTime(LocalDateTime.parse("2026-07-17T18:00:00"));
        incoming.setStatus(PartnerServiceRequestStatus.PENDING);
        incoming.setCreatedAt(LocalDateTime.parse("2026-07-17T17:00:00"));
        incoming.setEtaMinutes(15);
        incoming.setExpiresAt(LocalDateTime.parse("2026-07-17T18:15:00"));
        incoming.setAddress(address);

        PartnerRequestActionResponse action = new PartnerRequestActionResponse();
        action.setRequestId(5L);
        action.setStatus(PartnerServiceRequestStatus.ACCEPTED);

        PartnerStatusResponse status = new PartnerStatusResponse();
        status.setPartnerId(6L);
        status.setOnline(true);

        assertThat(incoming.getAddress().getCity()).isEqualTo("Pune");
        assertThat(incoming.getStatus()).isEqualTo(PartnerServiceRequestStatus.PENDING);
        assertThat(action.getStatus()).isEqualTo(PartnerServiceRequestStatus.ACCEPTED);
        assertThat(status.isOnline()).isTrue();
        assertThat(statusUpdateRequest.getIsOnline()).isTrue();
    }
}

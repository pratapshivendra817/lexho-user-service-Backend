package com.lexho.user_service.controller;

import com.lexho.booking.entity.Booking;
import com.lexho.booking.enums.BookingStatus;
import com.lexho.booking.service.BookingManagementService;
import com.lexho.provider.entity.Partner;
import com.lexho.provider.repository.PartnerRepository;
import com.lexho.user.dto.BookingSummaryClientResponse;
import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.dto.PartnerCreateRequestDTO;
import com.lexho.user_service.service.DeprecatedApiUsageService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InfrastructureControllersTest {

    @Test
    void adminAndStatusControllersReturnStaticPayloads() {
        ApiResponse<Map<String, String>> dashboard = new AdminController().adminDashboard();
        ApiResponse<Map<String, Object>> status = new StatusController().getStatus();

        assertThat(dashboard.isSuccess()).isTrue();
        assertThat(dashboard.getData()).containsEntry("scope", "ADMIN");
        assertThat(status.isSuccess()).isTrue();
        assertThat(status.getData()).containsEntry("status", "UP");
        assertThat(status.getData().get("timestamp")).isNotNull();
    }

    @Test
    void opsControllerReturnsSummaryAndDeprecationSnapshot() {
        DeprecatedApiUsageService deprecatedApiUsageService = mock(DeprecatedApiUsageService.class);
        OpsController opsController = new OpsController(deprecatedApiUsageService);
        when(deprecatedApiUsageService.snapshot()).thenReturn(Map.of("count", 2, "items", List.of("legacy")));

        ApiResponse<Map<String, String>> summary = opsController.opsSummary();
        ApiResponse<Map<String, Object>> snapshot = opsController.deprecatedUsage();

        assertThat(summary.getData()).containsEntry("scope", "ADMIN_OR_PARTNER");
        assertThat(snapshot.getData()).containsEntry("count", 2);
        verify(deprecatedApiUsageService).snapshot();
    }

    @Test
    void internalBookingControllerSummarizesCompletedAndCancelledBookings() {
        BookingManagementService bookingManagementService = mock(BookingManagementService.class);
        InternalBookingController controller = new InternalBookingController(bookingManagementService);

        Booking completed = mock(Booking.class);
        Booking cancelled = mock(Booking.class);
        Booking active = mock(Booking.class);
        when(completed.getBookingStatus()).thenReturn(BookingStatus.COMPLETED);
        when(cancelled.getBookingStatus()).thenReturn(BookingStatus.CANCELLED);
        when(active.getBookingStatus()).thenReturn(BookingStatus.CONFIRMED);
        when(bookingManagementService.getActiveBookingsForCustomer(55L)).thenReturn(List.of(completed, cancelled, active));

        BookingSummaryClientResponse response = controller.userBookingSummary(55L);

        assertThat(response.getTotalBookings()).isEqualTo(3);
        assertThat(response.getCompletedBookings()).isEqualTo(1);
        assertThat(response.getCancelledBookings()).isEqualTo(1);
    }

    @Test
    void partnerControllerCoversListLookupAndDeleteBranches() {
        PartnerRepository partnerRepository = mock(PartnerRepository.class);
        PartnerController controller = new PartnerController(partnerRepository);
        Partner partner = partner(7L, "Partner A");

        when(partnerRepository.findBySubCategoryIdAndAvailableTrueOrderByRatingDesc(10L)).thenReturn(List.of(partner));
        when(partnerRepository.findBySubCategoryIdAndAvailableTrueOrderByPriceAsc(10L)).thenReturn(List.of(partner));
        when(partnerRepository.findBySubCategoryIdAndAvailableTrue(10L)).thenReturn(List.of(partner));
        when(partnerRepository.findById(7L)).thenReturn(Optional.of(partner));
        when(partnerRepository.findById(8L)).thenReturn(Optional.empty());
        when(partnerRepository.existsById(7L)).thenReturn(true);
        when(partnerRepository.existsById(8L)).thenReturn(false);
        when(partnerRepository.findByNameAndSubCategoryId("Partner B", 20L)).thenReturn(List.of());
        when(partnerRepository.save(any(Partner.class))).thenAnswer(invocation -> {
            Partner saved = invocation.getArgument(0);
            saved.setId(99L);
            return saved;
        });

        assertThat(controller.getPartners(10L, "rating").getData()).hasSize(1);
        assertThat(controller.getPartners(10L, "price").getData()).hasSize(1);
        assertThat(controller.getPartners(10L, null).getData()).hasSize(1);
        assertThat(controller.getById(7L).isSuccess()).isTrue();
        assertThat(controller.getById(8L).isSuccess()).isFalse();
        assertThat(controller.delete(7L).isSuccess()).isTrue();
        assertThat(controller.delete(8L).isSuccess()).isFalse();

        PartnerCreateRequestDTO request = new PartnerCreateRequestDTO();
        request.setName("Partner B");
        request.setCategoryId(10L);
        request.setSubCategoryId(20L);
        request.setRating(4.7);
        request.setTotalReviews(14L);
        request.setPrice(300.0);
        request.setAvailable(true);
        request.setExperience(6);
        request.setImage("img.png");
        request.setVerificationStatus("APPROVED");

        ApiResponse<?> create = controller.create(request);

        assertThat(create.isSuccess()).isTrue();
        verify(partnerRepository).deleteById(7L);
        verify(partnerRepository).save(any(Partner.class));
    }

    @Test
    void partnerControllerRejectsDuplicateAndNullCreateRequestsWithoutSaving() {
        PartnerRepository partnerRepository = mock(PartnerRepository.class);
        PartnerController controller = new PartnerController(partnerRepository);
        PartnerCreateRequestDTO request = new PartnerCreateRequestDTO();
        request.setName("Partner C");
        request.setSubCategoryId(21L);
        when(partnerRepository.findByNameAndSubCategoryId("Partner C", 21L)).thenReturn(List.of(new Partner()));

        assertThat(controller.create(null).isSuccess()).isFalse();
        assertThat(controller.create(request).isSuccess()).isFalse();
        verify(partnerRepository, never()).save(any(Partner.class));
    }

    private Partner partner(Long id, String name) {
        Partner partner = new Partner();
        partner.setId(id);
        partner.setName(name);
        partner.setCategoryId(1L);
        partner.setSubCategoryId(10L);
        partner.setRating(4.8);
        partner.setTotalReviews(22L);
        partner.setPrice(299.0);
        partner.setAvailable(true);
        partner.setExperience(7);
        partner.setImage("p.png");
        partner.setVerificationStatus("APPROVED");
        return partner;
    }
}

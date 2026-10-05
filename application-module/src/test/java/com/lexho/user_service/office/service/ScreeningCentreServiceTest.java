package com.lexho.user_service.office.service;

import com.lexho.user_service.audit.service.AuditLogService;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.CodedValidationException;
import com.lexho.user_service.office.dto.ScreeningCentreRequest;
import com.lexho.user_service.office.entity.OfficeLocation;
import com.lexho.user_service.office.repository.OfficeLocationRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScreeningCentreServiceTest {

    private final OfficeLocationRepository repository = mock(OfficeLocationRepository.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final ScreeningCentreService service = new ScreeningCentreService(repository, auditLogService);

    @Test
    void createScreeningCentreSavesExactCoordinatesAsActive() {
        when(repository.findByTypeAndActiveTrue("SCREENING_CENTRE")).thenReturn(List.of());
        when(repository.save(any(OfficeLocation.class))).thenAnswer(invocation -> saved(invocation.getArgument(0), 10L));

        var response = service.saveScreeningCentre(request("28.5123456", "77.6123456"), 99L);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.latitude()).isEqualByComparingTo("28.5123456");
        assertThat(response.longitude()).isEqualByComparingTo("77.6123456");
        assertThat(response.active()).isTrue();
        verify(repository).deactivateActiveByType("SCREENING_CENTRE", "ADMIN:99");
        verify(auditLogService).log(99L, "ADMIN", "SCREENING_CENTRE_CREATED",
                "OFFICE_LOCATION", "10", null, "28.5123456,77.6123456");
    }

    @Test
    void updateScreeningCentreDeactivatesOldActiveAndCreatesReplacement() {
        OfficeLocation old = location(1L, true);
        when(repository.findByTypeAndActiveTrue("SCREENING_CENTRE")).thenReturn(List.of(old));
        when(repository.save(any(OfficeLocation.class))).thenAnswer(invocation -> saved(invocation.getArgument(0), 11L));

        var response = service.saveScreeningCentre(request("28.7000000", "77.8000000"), 7L);

        assertThat(response.id()).isEqualTo(11L);
        assertThat(response.latitude()).isEqualByComparingTo("28.7000000");
        verify(repository).deactivateActiveByType("SCREENING_CENTRE", "ADMIN:7");
        verify(auditLogService).log(7L, "ADMIN", "SCREENING_CENTRE_UPDATED",
                "OFFICE_LOCATION", "11", "1", "28.7000000,77.8000000");
    }

    @Test
    void partnerReadReturnsOnlyPartnerFieldsAndExactCoordinates() {
        OfficeLocation active = location(5L, true);
        active.setLatitude(new BigDecimal("28.4444444"));
        active.setLongitude(new BigDecimal("77.5555555"));
        when(repository.findFirstByTypeAndActiveTrueOrderByUpdatedAtDesc("SCREENING_CENTRE"))
                .thenReturn(Optional.of(active));

        var response = service.getPartnerScreeningCentre();

        assertThat(response.name()).isEqualTo("Lexho Screening Centre");
        assertThat(response.latitude()).isEqualByComparingTo("28.4444444");
        assertThat(response.longitude()).isEqualByComparingTo("77.5555555");
    }

    @Test
    void invalidCoordinatesAndBlankTextAreRejected() {
        assertThrows(BadRequestException.class, () -> service.saveScreeningCentre(request("91.0000000", "77.0"), 1L));
        assertThrows(BadRequestException.class, () -> service.saveScreeningCentre(request("28.0", "181.0000000"), 1L));
        assertThrows(BadRequestException.class, () -> service.saveScreeningCentre(
                new ScreeningCentreRequest(" ", "Address", BigDecimal.ONE, BigDecimal.ONE, "", null, null, null, null), 1L));
        assertThrows(BadRequestException.class, () -> service.saveScreeningCentre(
                new ScreeningCentreRequest("Name", " ", BigDecimal.ONE, BigDecimal.ONE, "", null, null, null, null), 1L));
    }

    @Test
    void noActiveCentreReturnsSemanticNotConfiguredError() {
        when(repository.findFirstByTypeAndActiveTrueOrderByUpdatedAtDesc("SCREENING_CENTRE"))
                .thenReturn(Optional.empty());

        CodedValidationException exception = assertThrows(CodedValidationException.class,
                service::getPartnerScreeningCentre);

        assertThat(exception.getErrorCode()).isEqualTo("SCREENING_CENTRE_NOT_CONFIGURED");
    }

    @Test
    void statusEndpointDeactivatesAndReactivatesLatestCentre() {
        OfficeLocation active = location(3L, true);
        when(repository.findFirstByTypeAndActiveTrueOrderByUpdatedAtDesc("SCREENING_CENTRE"))
                .thenReturn(Optional.of(active));
        when(repository.save(active)).thenReturn(active);

        var deactivated = service.updateStatus(false, 9L);

        assertThat(deactivated.active()).isFalse();
        assertThat(active.isActive()).isFalse();

        OfficeLocation latest = location(4L, false);
        when(repository.findFirstByTypeOrderByUpdatedAtDesc("SCREENING_CENTRE")).thenReturn(Optional.of(latest));
        when(repository.save(latest)).thenReturn(latest);

        var activated = service.updateStatus(true, 9L);

        assertThat(activated.active()).isTrue();
        verify(repository).deactivateActiveByType("SCREENING_CENTRE", "ADMIN:9");
    }

    @Test
    void savedEntityTrimsOptionalFieldsAndStoresOptionalMapMetadata() {
        when(repository.findByTypeAndActiveTrue("SCREENING_CENTRE")).thenReturn(List.of());
        when(repository.save(any(OfficeLocation.class))).thenAnswer(invocation -> saved(invocation.getArgument(0), 20L));

        service.saveScreeningCentre(new ScreeningCentreRequest(
                " Lexho Screening Centre ",
                " Gt-53, Noida Sector-117 ",
                new BigDecimal("28.51234561"),
                new BigDecimal("77.61234561"),
                " +919876543210 ",
                " google ",
                " place-123 ",
                " Gate Number 03 ",
                " Near Automotive Ace Garage "), 2L);

        ArgumentCaptor<OfficeLocation> captor = ArgumentCaptor.forClass(OfficeLocation.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Lexho Screening Centre");
        assertThat(captor.getValue().getLatitude()).isEqualByComparingTo("28.5123456");
        assertThat(captor.getValue().getLongitude()).isEqualByComparingTo("77.6123456");
        assertThat(captor.getValue().getMapProvider()).isEqualTo("google");
        assertThat(captor.getValue().getEntranceInfo()).isEqualTo("Near Automotive Ace Garage");
    }

    private ScreeningCentreRequest request(String latitude, String longitude) {
        return new ScreeningCentreRequest(
                "Lexho Screening Centre",
                "Gt-53, Bhardwaja Saddan, Noida Sector-117, Gate Number 03, near Automotive Ace Garage",
                new BigDecimal(latitude),
                new BigDecimal(longitude),
                "+919876543210",
                null,
                null,
                null,
                null);
    }

    private OfficeLocation location(Long id, boolean active) {
        OfficeLocation location = new OfficeLocation();
        location.setId(id);
        location.setName("Lexho Screening Centre");
        location.setAddress("Gt-53, Noida Sector-117");
        location.setLatitude(new BigDecimal("28.5123456"));
        location.setLongitude(new BigDecimal("77.6123456"));
        location.setPhone("+919876543210");
        location.setType("SCREENING_CENTRE");
        location.setActive(active);
        location.setCreatedAt(LocalDateTime.of(2026, 8, 16, 10, 0));
        location.setUpdatedAt(LocalDateTime.of(2026, 8, 16, 10, 0));
        return location;
    }

    private OfficeLocation saved(OfficeLocation location, Long id) {
        location.setId(id);
        location.setCreatedAt(LocalDateTime.of(2026, 8, 16, 10, 0));
        location.setUpdatedAt(LocalDateTime.of(2026, 8, 16, 10, 0));
        return location;
    }
}

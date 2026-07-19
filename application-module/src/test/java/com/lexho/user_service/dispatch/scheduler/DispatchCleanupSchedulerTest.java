package com.lexho.user_service.dispatch.scheduler;

import com.lexho.user_service.dispatch.entity.CurrentPartnerLocationEntity;
import com.lexho.user_service.dispatch.entity.PartnerShiftEntity;
import com.lexho.user_service.dispatch.repository.CurrentPartnerLocationRepository;
import com.lexho.user_service.dispatch.repository.PartnerShiftRepository;
import com.lexho.user_service.dispatch.service.PartnerGeoIndexService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DispatchCleanupSchedulerTest {

    @Test
    void cleanupStaleDispatchStateMarksPartnersOfflineAndDeletesStaleLocations() {
        PartnerShiftRepository shiftRepository = mock(PartnerShiftRepository.class);
        CurrentPartnerLocationRepository locationRepository = mock(CurrentPartnerLocationRepository.class);
        PartnerGeoIndexService geoIndexService = mock(PartnerGeoIndexService.class);
        DispatchCleanupScheduler scheduler = new DispatchCleanupScheduler(
                shiftRepository,
                locationRepository,
                geoIndexService,
                new SimpleMeterRegistry(),
                120,
                30
        );

        PartnerShiftEntity staleShift = new PartnerShiftEntity();
        staleShift.setOnline(true);
        PartnerShiftEntity endedShift = new PartnerShiftEntity();
        endedShift.setOnline(true);
        CurrentPartnerLocationEntity staleLocation = new CurrentPartnerLocationEntity();
        staleLocation.setPartnerId(55L);
        staleLocation.setDeleted(false);

        when(shiftRepository.findByOnlineTrueAndDeletedFalseAndUpdatedAtBefore(any(LocalDateTime.class)))
                .thenReturn(List.of(staleShift));
        when(shiftRepository.findByOnlineTrueAndDeletedFalseAndShiftEndBefore(any(LocalDateTime.class)))
                .thenReturn(List.of(endedShift));
        when(locationRepository.findByDeletedFalseAndUpdatedAtBefore(any(LocalDateTime.class)))
                .thenReturn(List.of(staleLocation));

        scheduler.cleanupStaleDispatchState();

        assertThat(staleShift.isOnline()).isFalse();
        assertThat(staleShift.getUpdatedBy()).isEqualTo("SCHEDULER");
        assertThat(endedShift.isOnline()).isFalse();
        assertThat(endedShift.getUpdatedBy()).isEqualTo("SCHEDULER_SHIFT_ENDED");
        assertThat(staleLocation.isDeleted()).isTrue();
        assertThat(staleLocation.getUpdatedBy()).isEqualTo("SCHEDULER");

        verify(shiftRepository).saveAll(any(List.class));
        verify(geoIndexService).removePartner(55L);
        verify(locationRepository).saveAll(List.of(staleLocation));
    }

    @Test
    void cleanupStaleDispatchStateSkipsWritesWhenNothingIsStale() {
        PartnerShiftRepository shiftRepository = mock(PartnerShiftRepository.class);
        CurrentPartnerLocationRepository locationRepository = mock(CurrentPartnerLocationRepository.class);
        PartnerGeoIndexService geoIndexService = mock(PartnerGeoIndexService.class);
        DispatchCleanupScheduler scheduler = new DispatchCleanupScheduler(
                shiftRepository,
                locationRepository,
                geoIndexService,
                new SimpleMeterRegistry(),
                120,
                30
        );

        when(shiftRepository.findByOnlineTrueAndDeletedFalseAndUpdatedAtBefore(any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(shiftRepository.findByOnlineTrueAndDeletedFalseAndShiftEndBefore(any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(locationRepository.findByDeletedFalseAndUpdatedAtBefore(any(LocalDateTime.class)))
                .thenReturn(List.of());

        scheduler.cleanupStaleDispatchState();

        verify(shiftRepository, never()).saveAll(any(List.class));
        verify(locationRepository, never()).saveAll(any(List.class));
        verify(geoIndexService, never()).removePartner(any(Long.class));
    }
}

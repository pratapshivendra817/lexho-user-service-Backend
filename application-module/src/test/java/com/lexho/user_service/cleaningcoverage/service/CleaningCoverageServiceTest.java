package com.lexho.user_service.cleaningcoverage.service;

import com.lexho.user_service.cleaningcoverage.dto.AdminCleaningCoverageRequest;
import com.lexho.user_service.cleaningcoverage.dto.AdminCleaningCoverageResponse;
import com.lexho.user_service.cleaningcoverage.dto.AdminCleaningCoverageTaskRequest;
import com.lexho.user_service.cleaningcoverage.dto.CleaningCoverageResponse;
import com.lexho.user_service.cleaningcoverage.entity.CleaningCoverageEntity;
import com.lexho.user_service.cleaningcoverage.entity.CleaningCoverageTaskEntity;
import com.lexho.user_service.cleaningcoverage.repository.CleaningCoverageRepository;
import com.lexho.user_service.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CleaningCoverageServiceTest {

    private final CleaningCoverageRepository repository = mock(CleaningCoverageRepository.class);
    private final CleaningCoverageService service = new CleaningCoverageService(repository);

    @Test
    void listShouldUseDurationFilterAndMapCustomerResponse() {
        CleaningCoverageEntity entity = coverage("2.00", "2 BHK", true);
        when(repository.findByDurationHoursAndActiveTrueOrderBySortOrderAsc(new BigDecimal("2.00")))
                .thenReturn(List.of(entity));

        List<CleaningCoverageResponse> response = service.list(Optional.of(new BigDecimal("2.00")));

        assertEquals(1, response.size());
        assertEquals(new BigDecimal("2"), response.get(0).durationHours());
        assertEquals("2 BHK", response.get(0).reference());
        assertEquals("Kitchen", response.get(0).tasks().get(0).name());
    }

    @Test
    void createShouldTrimFieldsAndDefaultSortOrderAndActiveFlag() {
        AdminCleaningCoverageRequest request = request(null, null);
        when(repository.save(any(CleaningCoverageEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminCleaningCoverageResponse response = service.create(request).getData();

        assertEquals(new BigDecimal("1.5"), response.durationHours());
        assertEquals("Studio", response.reference());
        assertEquals(150, response.sortOrder());
        assertEquals("Kitchen", response.tasks().get(0).name());
        assertEquals(1, response.tasks().get(0).sortOrder());
    }

    @Test
    void updateShouldOverwriteExistingCoverage() {
        CleaningCoverageEntity existing = coverage("1.00", "Old", true);
        when(repository.findById(9L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        AdminCleaningCoverageResponse response = service.update(9L, request(7, false)).getData();

        assertEquals(7, response.sortOrder());
        assertFalse(response.active());
        assertEquals("Studio", response.reference());
    }

    @Test
    void deleteShouldSoftDeleteCoverage() {
        CleaningCoverageEntity existing = coverage("1.00", "Old", true);
        when(repository.findById(9L)).thenReturn(Optional.of(existing));

        assertEquals("Cleaning coverage deleted successfully", service.delete(9L).getMessage());

        assertFalse(existing.isActive());
        verify(repository).save(existing);
    }

    @Test
    void updateShouldThrowWhenCoverageDoesNotExist() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.update(404L, request(null, true)));
    }

    private AdminCleaningCoverageRequest request(Integer sortOrder, Boolean active) {
        return new AdminCleaningCoverageRequest(
                new BigDecimal("1.50"),
                " Studio ",
                sortOrder,
                active,
                List.of(new AdminCleaningCoverageTaskRequest(" Kitchen ", 30, " broom ", null))
        );
    }

    private CleaningCoverageEntity coverage(String duration, String reference, boolean active) {
        CleaningCoverageEntity entity = new CleaningCoverageEntity();
        entity.setDurationHours(new BigDecimal(duration));
        entity.setReference(reference);
        entity.setSortOrder(1);
        entity.setActive(active);
        CleaningCoverageTaskEntity task = new CleaningCoverageTaskEntity();
        task.setName("Kitchen");
        task.setEstimatedMinutes(30);
        task.setIcon("broom");
        task.setSortOrder(1);
        entity.addTask(task);
        return entity;
    }
}

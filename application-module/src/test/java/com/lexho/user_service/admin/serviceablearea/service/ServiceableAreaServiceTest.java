package com.lexho.user_service.admin.serviceablearea.service;

import com.lexho.user_service.admin.serviceablearea.dto.ServiceableAreaStatusRequest;
import com.lexho.user_service.admin.serviceablearea.dto.ServiceableAreaUpsertRequest;
import com.lexho.user_service.admin.serviceablearea.entity.ServiceableAreaEntity;
import com.lexho.user_service.admin.serviceablearea.repository.ServiceableAreaRepository;
import com.lexho.user_service.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServiceableAreaServiceTest {

    private final ServiceableAreaRepository repository = mock(ServiceableAreaRepository.class);
    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final ServiceableAreaService service = new ServiceableAreaService(repository, jdbcTemplate);

    @Test
    void listAndSearchShouldUseSafePagingAndMapResponses() {
        ServiceableAreaEntity entity = entity(1L, " Central ", " Delhi ", " Delhi ", "110001", true, false);
        when(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity), Pageable.ofSize(100).withPage(0), 1))
                .thenReturn(new PageImpl<>(List.of(entity), Pageable.ofSize(1).withPage(1), 1));

        var list = service.list(-1, 500, "unknown", "asc");
        var search = service.search("  delhi  ", 1, 0, "city", "desc");

        assertEquals(0, list.getPage());
        assertEquals(100, list.getSize());
        assertEquals(1, list.getContent().size());
        assertEquals(1, search.getPage());
        assertEquals(1, search.getSize());
        assertEquals(" Central ", search.getContent().get(0).getName());
    }

    @Test
    void createShouldTrimFieldsActivateAndSyncRule() {
        when(repository.save(any(ServiceableAreaEntity.class))).thenAnswer(invocation -> {
            ServiceableAreaEntity saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 11L);
            ReflectionTestUtils.setField(saved, "createdAt", LocalDateTime.of(2026, 6, 20, 10, 0));
            ReflectionTestUtils.setField(saved, "updatedAt", LocalDateTime.of(2026, 6, 20, 10, 5));
            return saved;
        });

        var response = service.create(request());

        assertEquals(11L, response.getId());
        assertEquals("Central", response.getName());
        assertEquals("Delhi", response.getCity());
        assertThat(response.isActive()).isTrue();
        verify(jdbcTemplate).update(any(String.class),
                eq(11L), eq("Central"), eq("Delhi"), eq("Delhi"), eq("110001"),
                eq(7L), eq(8L), eq("{json}"), eq(true), eq(false),
                eq(LocalDateTime.of(2026, 6, 20, 10, 0)), eq(LocalDateTime.of(2026, 6, 20, 10, 5)));
    }

    @Test
    void updateDeleteAndStatusShouldPersistAndSyncRule() {
        ServiceableAreaEntity existing = entity(21L, "Old", "City", "State", "220002", true, false);
        ReflectionTestUtils.setField(existing, "createdAt", LocalDateTime.of(2026, 6, 19, 8, 0));
        ReflectionTestUtils.setField(existing, "updatedAt", LocalDateTime.of(2026, 6, 19, 8, 5));
        when(repository.findById(21L)).thenReturn(Optional.of(existing));
        when(repository.save(any(ServiceableAreaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var updated = service.update(21L, request());
        ServiceableAreaStatusRequest statusRequest = new ServiceableAreaStatusRequest();
        statusRequest.setActive(false);
        var statusUpdated = service.updateStatus(21L, statusRequest);
        service.delete(21L);

        assertEquals("Central", updated.getName());
        assertThat(statusUpdated.isActive()).isFalse();
        assertThat(existing.isDeleted()).isTrue();
        verify(jdbcTemplate, org.mockito.Mockito.times(3)).update(any(String.class),
                eq(21L), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void updateDeleteAndStatusShouldRejectMissingOrDeletedRows() {
        ServiceableAreaEntity deleted = entity(31L, "X", "Y", "Z", "330003", true, true);
        when(repository.findById(30L)).thenReturn(Optional.empty());
        when(repository.findById(31L)).thenReturn(Optional.of(deleted));
        ServiceableAreaStatusRequest statusRequest = new ServiceableAreaStatusRequest();
        statusRequest.setActive(true);

        assertThrows(ResourceNotFoundException.class, () -> service.update(30L, request()));
        assertThrows(ResourceNotFoundException.class, () -> service.delete(31L));
        assertThrows(ResourceNotFoundException.class, () -> service.updateStatus(31L, statusRequest));
    }

    private ServiceableAreaUpsertRequest request() {
        ServiceableAreaUpsertRequest request = new ServiceableAreaUpsertRequest();
        request.setName("  Central ");
        request.setCity(" Delhi ");
        request.setState(" Delhi ");
        request.setPincode("110001 ");
        request.setCityId(7L);
        request.setZoneId(8L);
        request.setPolygonJson(" {json} ");
        return request;
    }

    private ServiceableAreaEntity entity(Long id, String name, String city, String state, String pincode, boolean active, boolean deleted) {
        ServiceableAreaEntity entity = new ServiceableAreaEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setCity(city);
        entity.setState(state);
        entity.setPincode(pincode);
        entity.setCityId(7L);
        entity.setZoneId(8L);
        entity.setPolygonJson("{json}");
        entity.setActive(active);
        entity.setDeleted(deleted);
        return entity;
    }
}

package com.lexho.user_service.admin;

import com.lexho.user_service.admin.serviceablearea.dto.ServiceableAreaStatusRequest;
import com.lexho.user_service.admin.serviceablearea.dto.ServiceableAreaUpsertRequest;
import com.lexho.user_service.admin.serviceablearea.entity.ServiceableAreaEntity;
import com.lexho.user_service.admin.serviceablearea.repository.ServiceableAreaRepository;
import com.lexho.user_service.admin.serviceablearea.service.ServiceableAreaService;
import com.lexho.user_service.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceableAreaServiceTest {

    @Mock
    private ServiceableAreaRepository serviceableAreaRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void createShouldSaveNormalizedFields() {
        ServiceableAreaService service = new ServiceableAreaService(serviceableAreaRepository, jdbcTemplate);
        ServiceableAreaUpsertRequest request = new ServiceableAreaUpsertRequest();
        request.setName("  Andheri West  ");
        request.setCity("  Mumbai ");
        request.setState(" Maharashtra ");
        request.setPincode(" 400058 ");

        ServiceableAreaEntity saved = new ServiceableAreaEntity();
        saved.setId(10L);
        saved.setName("Andheri West");
        saved.setCity("Mumbai");
        saved.setState("Maharashtra");
        saved.setPincode("400058");
        saved.setActive(true);
        when(serviceableAreaRepository.save(any(ServiceableAreaEntity.class))).thenReturn(saved);

        var response = service.create(request);

        ArgumentCaptor<ServiceableAreaEntity> captor = ArgumentCaptor.forClass(ServiceableAreaEntity.class);
        verify(serviceableAreaRepository).save(captor.capture());
        assertEquals("Andheri West", captor.getValue().getName());
        assertEquals("Mumbai", captor.getValue().getCity());
        assertEquals("Maharashtra", captor.getValue().getState());
        assertEquals("400058", captor.getValue().getPincode());
        assertEquals(10L, response.getId());
        assertTrue(response.isActive());
        verify(jdbcTemplate, atLeastOnce()).update(anyString(), any(Object[].class));
    }

    @Test
    void listShouldMapPageMetadata() {
        ServiceableAreaService service = new ServiceableAreaService(serviceableAreaRepository, jdbcTemplate);
        ServiceableAreaEntity entity = new ServiceableAreaEntity();
        entity.setId(1L);
        entity.setName("Area 1");
        entity.setCity("City");
        entity.setState("State");
        entity.setPincode("400001");
        entity.setActive(true);

        Pageable pageable = PageRequest.of(0, 10);
        PageImpl<ServiceableAreaEntity> page = new PageImpl<>(List.of(entity), pageable, 1);
        when(serviceableAreaRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        var response = service.list(0, 10, "id", "desc");

        assertEquals(1, response.getContent().size());
        assertEquals(0, response.getPage());
        assertEquals(10, response.getSize());
        assertEquals(1, response.getTotalElements());
    }

    @Test
    void updateStatusShouldThrowWhenAreaMissing() {
        ServiceableAreaService service = new ServiceableAreaService(serviceableAreaRepository, jdbcTemplate);
        when(serviceableAreaRepository.findById(77L)).thenReturn(Optional.empty());

        ServiceableAreaStatusRequest request = new ServiceableAreaStatusRequest();
        request.setActive(Boolean.FALSE);

        assertThrows(ResourceNotFoundException.class, () -> service.updateStatus(77L, request));
    }
}

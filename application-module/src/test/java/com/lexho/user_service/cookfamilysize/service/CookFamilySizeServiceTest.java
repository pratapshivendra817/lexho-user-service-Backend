package com.lexho.user_service.cookfamilysize.service;

import com.lexho.user_service.cookfamilysize.dto.CookFamilySizeRequest;
import com.lexho.user_service.cookfamilysize.dto.CookFamilySizeStatusRequest;
import com.lexho.user_service.cookfamilysize.entity.CookFamilySizeEntity;
import com.lexho.user_service.cookfamilysize.repository.CookFamilySizeRepository;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.CodedValidationException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.subscription.enums.FamilySize;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CookFamilySizeServiceTest {
    @Mock CookFamilySizeRepository repository;
    @InjectMocks CookFamilySizeService service;

    @Test
    void listsAndCreatesNormalizedFamilySize() {
        CookFamilySizeEntity entity = entity(1L, "SMALL");
        when(repository.findByActiveTrueOrderByDisplayOrderAscIdAsc()).thenReturn(List.of(entity));
        when(repository.findAllByOrderByDisplayOrderAscIdAsc()).thenReturn(List.of(entity));
        assertEquals(1, service.customerList().size());
        assertEquals(1, service.adminList().size());

        when(repository.findByFamilySize("MEDIUM")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        assertEquals("MEDIUM", service.create(request(" medium ")).getFamilySize());
        when(repository.findByFamilySize("SMALL")).thenReturn(Optional.of(entity));
        assertThrows(BadRequestException.class, () -> service.create(request("small")));
        assertThrows(BadRequestException.class, () -> service.create(request("invalid")));
    }

    @Test
    void updateStatusDeactivateAndMissingEntityPaths() {
        CookFamilySizeEntity entity = entity(1L, "SMALL");
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(repository.existsByFamilySizeAndIdNot("LARGE", 1L)).thenReturn(false);
        assertEquals("LARGE", service.update(1L, request("large")).getFamilySize());

        CookFamilySizeStatusRequest status = new CookFamilySizeStatusRequest(); status.setActive(false);
        assertFalse(service.updateStatus(1L, status).getActive());
        assertFalse(service.deactivate(1L).getActive());

        when(repository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.deactivate(9L));
    }

    @Test
    void requireActiveSupportsCodeIdAndMismatchValidation() {
        CookFamilySizeEntity entity = entity(1L, "SMALL");
        when(repository.findByFamilySizeAndActiveTrue("SMALL")).thenReturn(Optional.of(entity));
        assertSame(entity, service.requireActive(null, "small"));
        assertThrows(CodedValidationException.class, () -> service.requireActive(2L, "small"));
        assertThrows(CodedValidationException.class, () -> service.requireActive(null, null));

        when(repository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(entity));
        assertSame(entity, service.requireActive(1L, null));
    }

    @Test
    void labelUsesConfiguredValueOrFamilyDefaults() {
        CookFamilySizeEntity entity = entity(1L, "SMALL"); entity.setFamilySizeLabel("Custom");
        when(repository.findByFamilySizeAndActiveTrue("SMALL")).thenReturn(Optional.of(entity));
        assertEquals("Custom", service.label(FamilySize.SMALL));
        when(repository.findByFamilySizeAndActiveTrue("XLARGE")).thenReturn(Optional.empty());
        assertEquals("9+ Members", service.label(FamilySize.XLARGE));
        assertNull(service.label(null));
    }

    private CookFamilySizeRequest request(String code) {
        CookFamilySizeRequest request = new CookFamilySizeRequest();
        request.setFamilySize(code); request.setFamilySizeLabel(" Label "); request.setActive(true);
        request.setDisplayOrder(1); return request;
    }

    private CookFamilySizeEntity entity(Long id, String code) {
        CookFamilySizeEntity entity = new CookFamilySizeEntity(); entity.setId(id); entity.setFamilySize(code);
        entity.setFamilySizeLabel("Label"); entity.setActive(true); entity.setDisplayOrder(1); return entity;
    }
}

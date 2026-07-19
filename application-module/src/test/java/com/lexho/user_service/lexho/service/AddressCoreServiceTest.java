package com.lexho.user_service.lexho.service;

import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.lexho.entity.LexhoAddress;
import com.lexho.user_service.lexho.repository.LexhoAddressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressCoreServiceTest {

    @Mock private LexhoAddressRepository repository;

    private AddressCoreService service;

    @BeforeEach
    void setUp() {
        service = new AddressCoreService(repository);
    }

    @Test
    void listFindAndCurrentReturnOwnedActiveAddresses() {
        LexhoAddress first = address(1L, false);
        LexhoAddress selected = address(2L, true);
        when(repository.findByUserIdAndIsDeletedFalse(7L)).thenReturn(List.of(first, selected));
        when(repository.findByIdAndUserIdAndIsDeletedFalse(2L, 7L)).thenReturn(Optional.of(selected));

        assertThat(service.listActive(7L)).containsExactly(first, selected);
        assertThat(service.findOwnedActive(7L, 2L)).isSameAs(selected);
        assertThat(service.getCurrent(7L)).isSameAs(selected);
    }

    @Test
    void currentFallsBackToFirstAndRejectsEmptyAddressBook() {
        LexhoAddress first = address(1L, false);
        when(repository.findByUserIdAndIsDeletedFalse(7L))
                .thenReturn(List.of(first))
                .thenReturn(List.of());

        assertThat(service.getCurrent(7L)).isSameAs(first);
        assertThatThrownBy(() -> service.getCurrent(7L))
                .isInstanceOf(BadRequestException.class).hasMessage("Address not found");
    }

    @Test
    void findOwnedActiveRejectsMissingAddress() {
        when(repository.findByIdAndUserIdAndIsDeletedFalse(9L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findOwnedActive(7L, 9L))
                .isInstanceOf(BadRequestException.class).hasMessage("Address not found");
    }

    @Test
    void setDefaultClearsOldDefaultFlushesAndSavesSelectedAddress() {
        LexhoAddress oldDefault = address(1L, true);
        LexhoAddress selected = address(2L, false);
        when(repository.findByUserIdAndIsDeletedFalseForUpdate(7L))
                .thenReturn(List.of(oldDefault, selected));

        LexhoAddress result = service.setDefault(7L, 2L);

        assertThat(result).isSameAs(selected);
        assertThat(oldDefault.isDefault()).isFalse();
        assertThat(selected.isDefault()).isTrue();
        verify(repository).saveAll(List.of(oldDefault, selected));
        verify(repository).flush();
        verify(repository).saveAndFlush(selected);
    }

    @Test
    void setDefaultIsIdempotentAndRejectsUnknownSelection() {
        LexhoAddress selected = address(2L, true);
        when(repository.findByUserIdAndIsDeletedFalseForUpdate(7L))
                .thenReturn(List.of(selected))
                .thenReturn(List.of(selected));

        assertThat(service.setDefault(7L, 2L)).isSameAs(selected);
        verify(repository, never()).saveAndFlush(selected);
        assertThatThrownBy(() -> service.setDefault(7L, 99L))
                .isInstanceOf(BadRequestException.class).hasMessage("Address not found");
    }

    @Test
    void softDeleteMovesDefaultToNextAddress() {
        LexhoAddress target = address(1L, true);
        LexhoAddress next = address(2L, false);
        when(repository.findByUserIdAndIsDeletedFalse(7L)).thenReturn(List.of(target, next));

        service.softDelete(7L, 1L);

        assertThat(target.isDeleted()).isTrue();
        assertThat(target.isDefault()).isFalse();
        assertThat(next.isDefault()).isTrue();
        assertThat(target.getUpdatedBy()).isEqualTo("SYSTEM");
        assertThat(next.getUpdatedBy()).isEqualTo("SYSTEM");
        verify(repository).save(target);
        verify(repository).save(next);
    }

    @Test
    void softDeleteRejectsLastAndUnknownAddresses() {
        LexhoAddress only = address(1L, false);
        when(repository.findByUserIdAndIsDeletedFalse(7L))
                .thenReturn(List.of(only))
                .thenReturn(List.of(only, address(2L, false)));

        assertThatThrownBy(() -> service.softDelete(7L, 1L))
                .isInstanceOf(BadRequestException.class).hasMessage("Cannot delete last address");
        assertThatThrownBy(() -> service.softDelete(7L, 99L))
                .isInstanceOf(BadRequestException.class).hasMessage("Address not found");
    }

    private LexhoAddress address(Long id, boolean isDefault) {
        LexhoAddress address = new LexhoAddress();
        ReflectionTestUtils.setField(address, "id", id);
        address.setDefault(isDefault);
        return address;
    }
}

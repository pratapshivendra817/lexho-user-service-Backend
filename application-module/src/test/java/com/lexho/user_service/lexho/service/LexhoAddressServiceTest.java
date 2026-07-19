package com.lexho.user_service.lexho.service;

import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.lexho.dto.LexhoDtos;
import com.lexho.user_service.lexho.entity.LexhoAddress;
import com.lexho.user_service.lexho.repository.LexhoAddressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LexhoAddressServiceTest {

    @Mock LexhoAddressRepository addressRepository;
    @Mock AddressCoreService addressCoreService;

    private LexhoAddressService service;

    @BeforeEach
    void setUp() {
        service = new LexhoAddressService(addressRepository, addressCoreService);
    }

    @Test
    void addOrUpdateCreatesDefaultAddressAndRequiresCoordinatesForNewRows() {
        when(addressRepository.findByUserIdAndIsDeletedFalse(7L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.addOrUpdate(7L, new LexhoDtos.AddressUpsertRequest(
                null, "Home", "221B", "Delhi", null, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("latitude and longitude are required");

        when(addressRepository.save(any())).thenAnswer(invocation -> {
            LexhoAddress address = invocation.getArgument(0);
            ReflectionTestUtils.setField(address, "id", 11L);
            return address;
        });

        LexhoDtos.AddressResponse response = service.addOrUpdate(7L, new LexhoDtos.AddressUpsertRequest(
                null, " Home ", "221B", "Delhi", 28.61, 77.20));

        assertThat(response.id()).isEqualTo(11L);
        assertThat(response.label()).isEqualTo(" Home ");
        assertThat(response.isDefault()).isTrue();
    }

    @Test
    void addOrUpdateReusesExistingAddressAndNormalizesMetadata() {
        LexhoAddress existing = new LexhoAddress();
        ReflectionTestUtils.setField(existing, "id", 22L);
        existing.setState(null);
        existing.setPincode(null);
        existing.setLatitude(BigDecimal.ZERO);
        existing.setLongitude(BigDecimal.ZERO);
        when(addressCoreService.findOwnedActive(7L, 22L)).thenReturn(existing);
        when(addressRepository.save(existing)).thenReturn(existing);

        LexhoDtos.AddressResponse response = service.addOrUpdate(7L, new LexhoDtos.AddressUpsertRequest(
                22L, "Office", "10 Downing", "Noida", 28.50, 77.40));

        assertThat(response.id()).isEqualTo(22L);
        assertThat(existing.getUserId()).isEqualTo(7L);
        assertThat(existing.getHouseNo()).isEqualTo("10 Downing");
        assertThat(existing.getArea()).isEqualTo("Office");
        assertThat(existing.getLatitude()).isEqualByComparingTo("28.5");
        assertThat(existing.getLongitude()).isEqualByComparingTo("77.4");
        assertThat(existing.getState()).isEqualTo("Unknown");
        assertThat(existing.getPincode()).isEmpty();
        assertThat(existing.getUpdatedBy()).isEqualTo("CUSTOMER:7");
        assertThat(existing.getCreatedBy()).isNull();
    }

    @Test
    void listGetDeleteAndMakeDefaultDelegateToCoreService() {
        LexhoAddress address = address(31L, true);
        when(addressCoreService.listActive(7L)).thenReturn(List.of(address));
        when(addressCoreService.findOwnedActive(7L, 31L)).thenReturn(address);
        when(addressCoreService.setDefault(7L, 31L)).thenReturn(address);

        assertThat(service.list(7L)).hasSize(1);
        assertThat(service.getById(7L, 31L).id()).isEqualTo(31L);
        assertThat(service.getRawById(7L, 31L)).isSameAs(address);
        assertThat(service.makeDefault(7L, 31L).isDefault()).isTrue();

        service.delete(7L, 31L);
        verify(addressCoreService).softDelete(7L, 31L);
    }

    @Test
    void bookingCoordinateFallbackSavesOnlyWhenIncomingCoordinatesAreValidAndAddressIsMissingThem() {
        LexhoAddress missingCoords = address(41L, false);
        missingCoords.setLatitude(BigDecimal.ZERO);
        missingCoords.setLongitude(BigDecimal.ZERO);
        when(addressCoreService.findOwnedActive(7L, 41L)).thenReturn(missingCoords);
        when(addressRepository.save(missingCoords)).thenReturn(missingCoords);

        LexhoAddress updated = service.applyBookingCoordinatesFallback(7L, 41L, 28.55, 77.31);

        assertThat(updated.getLatitude()).isEqualByComparingTo("28.55");
        assertThat(updated.getLongitude()).isEqualByComparingTo("77.31");
        verify(addressRepository).save(missingCoords);

        LexhoAddress existingCoords = address(42L, false);
        existingCoords.setLatitude(new BigDecimal("12.34"));
        existingCoords.setLongitude(new BigDecimal("56.78"));
        when(addressCoreService.findOwnedActive(7L, 42L)).thenReturn(existingCoords);

        assertThat(service.applyBookingCoordinatesFallback(7L, 42L, 0.0, 77.31)).isSameAs(existingCoords);
        assertThat(service.applyBookingCoordinatesFallback(7L, 42L, 28.55, 77.31)).isSameAs(existingCoords);
        verify(addressRepository, never()).save(existingCoords);
    }

    private static LexhoAddress address(Long id, boolean isDefault) {
        LexhoAddress address = new LexhoAddress();
        ReflectionTestUtils.setField(address, "id", id);
        address.setArea("Area");
        address.setHouseNo("Line1");
        address.setCity("Delhi");
        address.setDefault(isDefault);
        return address;
    }
}

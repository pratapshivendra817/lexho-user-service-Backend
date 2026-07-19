package com.lexho.user_service.customer.location.service;

import com.lexho.user_service.customer.location.dto.AddressResponse;
import com.lexho.user_service.customer.location.dto.PagedResponse;
import com.lexho.user_service.customer.location.dto.ReverseGeocodeResponse;
import com.lexho.user_service.customer.location.dto.SaveAddressRequest;
import com.lexho.user_service.customer.location.dto.UpdateAddressRequest;
import com.lexho.user_service.customer.location.entity.AddressType;
import com.lexho.user_service.customer.location.entity.CustomerAddress;
import com.lexho.user_service.customer.location.exception.AddressNotFoundException;
import com.lexho.user_service.customer.location.mapper.CustomerAddressMapper;
import com.lexho.user_service.customer.location.repository.CustomerAddressRepository;
import com.lexho.user_service.customer.location.service.impl.AddressServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AddressServiceImplTest {

    @Test
    void save_firstAddressShouldBecomeDefault() {
        CustomerAddressRepository repo = Mockito.mock(CustomerAddressRepository.class);
        ReverseGeocodeService reverse = Mockito.mock(ReverseGeocodeService.class);
        CustomerAddressMapper mapper = Mockito.mock(CustomerAddressMapper.class);
        when(repo.existsByCustomerIdAndIsDeletedFalse(10L)).thenReturn(false);
        when(repo.save(any(CustomerAddress.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(CustomerAddress.class))).thenReturn(new com.lexho.user_service.customer.location.dto.AddressResponse());
        ReverseGeocodeResponse geo = new ReverseGeocodeResponse();
        geo.city = "Noida";
        geo.state = "UP";
        geo.pincode = "201304";
        when(reverse.reverse(28.1, 77.1)).thenReturn(geo);

        AddressServiceImpl service = new AddressServiceImpl(repo, reverse, mapper);
        SaveAddressRequest req = new SaveAddressRequest();
        req.type = AddressType.HOME;
        req.addressLine = "Sector 104";
        req.mobile = "7999700475";
        req.latitude = 28.1;
        req.longitude = 77.1;
        com.lexho.user_service.customer.location.dto.AddressResponse response = service.save(10L, req);

        org.mockito.ArgumentCaptor<CustomerAddress> addressCaptor =
                org.mockito.ArgumentCaptor.forClass(CustomerAddress.class);
        verify(repo).save(addressCaptor.capture());
        CustomerAddress saved = addressCaptor.getValue();
        assertTrue(saved.isDefault());
        assertEquals(10L, saved.getCustomerId());
        assertEquals("Noida", saved.getCity());
        assertEquals("UP", saved.getState());
        assertEquals("201304", saved.getPincode());
        assertEquals("7999700475", saved.getMobile());
        assertSame(mapper.toResponse(saved), response);
    }

    @Test
    void updateListSetDefaultAndDeleteShouldCoverMutationBranches() {
        CustomerAddressRepository repo = Mockito.mock(CustomerAddressRepository.class);
        ReverseGeocodeService reverse = Mockito.mock(ReverseGeocodeService.class);
        CustomerAddressMapper mapper = Mockito.mock(CustomerAddressMapper.class);
        AddressServiceImpl service = new AddressServiceImpl(repo, reverse, mapper);

        UUID addressId = UUID.randomUUID();
        CustomerAddress address = new CustomerAddress();
        address.setDefault(true);
        address.setAddressLine("Old");

        ReverseGeocodeResponse geo = new ReverseGeocodeResponse();
        geo.city = " Gurugram ";
        geo.state = " haryana ";
        geo.pincode = "122001";

        when(repo.findByIdAndCustomerIdAndIsDeletedFalse(addressId, 10L)).thenReturn(Optional.of(address));
        when(repo.save(any(CustomerAddress.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(CustomerAddress.class))).thenReturn(new AddressResponse());
        when(reverse.reverse(28.5, 77.1)).thenReturn(geo);

        UpdateAddressRequest update = new UpdateAddressRequest();
        update.addressLine = " Tower ,, 21 ";
        update.area = " sector 44 ";
        update.mobile = "+91 99999-00000";
        update.latitude = 28.5;
        update.longitude = 77.1;

        service.update(10L, addressId, update);

        assertEquals("Tower ,  21", address.getAddressLine());
        assertEquals("sector 44", address.getArea());
        assertEquals("919999900000", address.getMobile());
        assertEquals("Gurugram", address.getCity());
        assertEquals("haryana", address.getState());
        assertEquals("122001", address.getPincode());

        when(repo.search(10L, "home", org.springframework.data.domain.PageRequest.of(1, 5)))
                .thenReturn(new PageImpl<>(List.of(address), org.springframework.data.domain.PageRequest.of(1, 5), 1));
        PagedResponse<AddressResponse> page = service.list(10L, 1, 5, "home");
        assertEquals(1, page.items.size());
        assertEquals(1, page.page);
        assertEquals(5, page.size);

        service.setDefault(10L, addressId);
        verify(repo).clearDefault(10L);

        CustomerAddress fallback = new CustomerAddress();
        when(repo.findFirstByCustomerIdAndIsDeletedFalseOrderByIsDefaultDescCreatedAtAsc(10L)).thenReturn(Optional.of(fallback));
        service.softDelete(10L, addressId);
        assertTrue(address.isDeleted());
        assertTrue(fallback.isDefault());
    }

    @Test
    void updateAndDeleteShouldRejectUnknownAddress() {
        CustomerAddressRepository repo = Mockito.mock(CustomerAddressRepository.class);
        ReverseGeocodeService reverse = Mockito.mock(ReverseGeocodeService.class);
        CustomerAddressMapper mapper = Mockito.mock(CustomerAddressMapper.class);
        AddressServiceImpl service = new AddressServiceImpl(repo, reverse, mapper);
        UUID addressId = UUID.randomUUID();

        when(repo.findByIdAndCustomerIdAndIsDeletedFalse(addressId, 10L)).thenReturn(Optional.empty());

        assertThrows(AddressNotFoundException.class, () -> service.update(10L, addressId, new UpdateAddressRequest()));
        assertThrows(AddressNotFoundException.class, () -> service.setDefault(10L, addressId));
        assertThrows(AddressNotFoundException.class, () -> service.softDelete(10L, addressId));
    }
}

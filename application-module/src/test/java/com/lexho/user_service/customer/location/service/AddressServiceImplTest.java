package com.lexho.user_service.customer.location.service;

import com.lexho.user_service.customer.location.dto.ReverseGeocodeResponse;
import com.lexho.user_service.customer.location.dto.SaveAddressRequest;
import com.lexho.user_service.customer.location.entity.AddressType;
import com.lexho.user_service.customer.location.entity.CustomerAddress;
import com.lexho.user_service.customer.location.mapper.CustomerAddressMapper;
import com.lexho.user_service.customer.location.repository.CustomerAddressRepository;
import com.lexho.user_service.customer.location.service.impl.AddressServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
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
        service.save(10L, req);
    }
}

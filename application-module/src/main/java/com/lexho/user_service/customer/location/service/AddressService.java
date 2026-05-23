package com.lexho.user_service.customer.location.service;

import com.lexho.user_service.customer.location.dto.AddressResponse;
import com.lexho.user_service.customer.location.dto.PagedResponse;
import com.lexho.user_service.customer.location.dto.SaveAddressRequest;
import com.lexho.user_service.customer.location.dto.UpdateAddressRequest;

import java.util.UUID;

public interface AddressService {
    AddressResponse save(Long customerId, SaveAddressRequest request);
    AddressResponse update(Long customerId, UUID addressId, UpdateAddressRequest request);
    PagedResponse<AddressResponse> list(Long customerId, int page, int size, String search);
    AddressResponse setDefault(Long customerId, UUID addressId);
    void softDelete(Long customerId, UUID addressId);
}

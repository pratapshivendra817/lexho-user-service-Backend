package com.lexho.user_service.customer.location.service.impl;

import com.lexho.user_service.customer.location.dto.AddressResponse;
import com.lexho.user_service.customer.location.dto.PagedResponse;
import com.lexho.user_service.customer.location.dto.ReverseGeocodeResponse;
import com.lexho.user_service.customer.location.dto.SaveAddressRequest;
import com.lexho.user_service.customer.location.dto.UpdateAddressRequest;
import com.lexho.user_service.customer.location.entity.CustomerAddress;
import com.lexho.user_service.customer.location.exception.AddressNotFoundException;
import com.lexho.user_service.customer.location.mapper.CustomerAddressMapper;
import com.lexho.user_service.customer.location.repository.CustomerAddressRepository;
import com.lexho.user_service.customer.location.service.AddressService;
import com.lexho.user_service.customer.location.service.ReverseGeocodeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class AddressServiceImpl implements AddressService {

    private final CustomerAddressRepository repository;
    private final ReverseGeocodeService reverseGeocodeService;
    private final CustomerAddressMapper mapper;

    public AddressServiceImpl(CustomerAddressRepository repository,
                              ReverseGeocodeService reverseGeocodeService,
                              CustomerAddressMapper mapper) {
        this.repository = repository;
        this.reverseGeocodeService = reverseGeocodeService;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public AddressResponse save(Long customerId, SaveAddressRequest request) {
        ReverseGeocodeResponse reverse = reverseGeocodeService.reverse(request.latitude, request.longitude);
        CustomerAddress address = new CustomerAddress();
        address.setCustomerId(customerId);
        address.setType(request.type);
        address.setCustomLabel(request.customLabel);
        address.setFlatNo(request.flatNo);
        address.setFloor(request.floor);
        address.setBuildingName(request.buildingName);
        address.setLandmark(request.landmark);
        address.setAddressLine(request.addressLine);
        address.setArea(request.area);
        address.setSector(request.sector);
        address.setCity(reverse.city == null ? "Unknown" : reverse.city);
        address.setState(reverse.state == null ? "Unknown" : reverse.state);
        address.setCountry("India");
        address.setPincode(reverse.pincode == null ? "" : reverse.pincode.trim());
        address.setLatitude(BigDecimal.valueOf(request.latitude));
        address.setLongitude(BigDecimal.valueOf(request.longitude));
        address.setMobile(request.mobile);
        address.setDeleted(false);
        address.setDefault(!repository.existsByCustomerIdAndIsDeletedFalse(customerId));
        address.setCreatedBy("CUSTOMER:" + customerId);
        address.setUpdatedBy("CUSTOMER:" + customerId);
        return mapper.toResponse(repository.save(address));
    }

    @Override
    @Transactional
    public AddressResponse update(Long customerId, UUID addressId, UpdateAddressRequest request) {
        CustomerAddress address = repository.findByIdAndCustomerIdAndIsDeletedFalse(addressId, customerId)
                .orElseThrow(() -> new AddressNotFoundException("Address not found"));
        if (request.type != null) address.setType(request.type);
        if (request.customLabel != null) address.setCustomLabel(request.customLabel);
        if (request.flatNo != null) address.setFlatNo(request.flatNo);
        if (request.floor != null) address.setFloor(request.floor);
        if (request.buildingName != null) address.setBuildingName(request.buildingName);
        if (request.landmark != null) address.setLandmark(request.landmark);
        if (request.addressLine != null) address.setAddressLine(request.addressLine);
        if (request.area != null) address.setArea(request.area);
        if (request.sector != null) address.setSector(request.sector);
        if (request.mobile != null) address.setMobile(request.mobile);
        if (request.latitude != null) address.setLatitude(BigDecimal.valueOf(request.latitude));
        if (request.longitude != null) address.setLongitude(BigDecimal.valueOf(request.longitude));
        if (request.latitude != null && request.longitude != null) {
            ReverseGeocodeResponse reverse = reverseGeocodeService.reverse(request.latitude, request.longitude);
            if (reverse.city != null && !reverse.city.isBlank()) address.setCity(reverse.city);
            if (reverse.state != null && !reverse.state.isBlank()) address.setState(reverse.state);
            if (reverse.pincode != null && !reverse.pincode.isBlank()) address.setPincode(reverse.pincode.trim());
        }
        address.setUpdatedBy("CUSTOMER:" + customerId);
        return mapper.toResponse(repository.save(address));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<AddressResponse> list(Long customerId, int page, int size, String search) {
        Page<CustomerAddress> result = repository.search(customerId, search, PageRequest.of(page, size));
        PagedResponse<AddressResponse> response = new PagedResponse<>();
        response.items = result.getContent().stream().map(mapper::toResponse).toList();
        response.page = result.getNumber();
        response.size = result.getSize();
        response.totalElements = result.getTotalElements();
        response.totalPages = result.getTotalPages();
        return response;
    }

    @Override
    @Transactional
    public AddressResponse setDefault(Long customerId, UUID addressId) {
        CustomerAddress target = repository.findByIdAndCustomerIdAndIsDeletedFalse(addressId, customerId)
                .orElseThrow(() -> new AddressNotFoundException("Address not found"));
        repository.clearDefault(customerId);
        target.setDefault(true);
        target.setUpdatedBy("CUSTOMER:" + customerId);
        return mapper.toResponse(repository.save(target));
    }

    @Override
    @Transactional
    public void softDelete(Long customerId, UUID addressId) {
        CustomerAddress address = repository.findByIdAndCustomerIdAndIsDeletedFalse(addressId, customerId)
                .orElseThrow(() -> new AddressNotFoundException("Address not found"));
        boolean wasDefault = address.isDefault();
        address.setDeleted(true);
        address.setDefault(false);
        address.setUpdatedBy("CUSTOMER:" + customerId);
        repository.save(address);
        if (wasDefault) {
            repository.findFirstByCustomerIdAndIsDeletedFalseOrderByIsDefaultDescCreatedAtAsc(customerId).ifPresent(next -> {
                next.setDefault(true);
                next.setUpdatedBy("CUSTOMER:" + customerId);
                repository.save(next);
            });
        }
    }
}

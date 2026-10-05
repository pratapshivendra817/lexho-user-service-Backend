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
import com.lexho.user_service.customer.location.service.ReverseGeocodingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class AddressServiceImpl implements AddressService {
    private static final String ADDRESS_NOT_FOUND = "Address not found";
    private static final String CUSTOMER_ACTOR_PREFIX = "CUSTOMER:";

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
        address.setCustomLabel(normalizeText(request.customLabel));
        address.setFlatNo(normalizeText(request.flatNo));
        address.setFloor(normalizeText(request.floor));
        address.setBuildingName(normalizeText(request.buildingName));
        address.setLandmark(normalizeText(request.landmark));
        address.setAddressLine(normalizeAddressLine(request.addressLine));
        address.setArea(ReverseGeocodingService.normalizeLocality(request.area));
        address.setSector(normalizeText(request.sector));
        address.setCity(reverse.city == null ? "Unknown" : ReverseGeocodingService.normalizeLocality(reverse.city));
        address.setState(reverse.state == null ? "Unknown" : ReverseGeocodingService.normalizeAddressText(reverse.state));
        address.setCountry("India");
        address.setPincode(ReverseGeocodingService.normalizePincode(reverse.pincode));
        address.setLatitude(BigDecimal.valueOf(request.latitude));
        address.setLongitude(BigDecimal.valueOf(request.longitude));
        address.setMobile(normalizeMobile(request.mobile));
        address.setDeleted(false);
        address.setDefault(!repository.existsByCustomerIdAndIsDeletedFalse(customerId));
        address.setCreatedBy(customerActor(customerId));
        address.setUpdatedBy(customerActor(customerId));
        return mapper.toResponse(repository.save(address));
    }

    @Override
    @Transactional
    public AddressResponse update(Long customerId, UUID addressId, UpdateAddressRequest request) {
        CustomerAddress address = repository.findByIdAndCustomerIdAndIsDeletedFalse(addressId, customerId)
                .orElseThrow(() -> new AddressNotFoundException(ADDRESS_NOT_FOUND));
        applyPatch(address, request);
        refreshReverseGeocode(address, request);
        address.setUpdatedBy(customerActor(customerId));
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
                .orElseThrow(() -> new AddressNotFoundException(ADDRESS_NOT_FOUND));
        repository.clearDefault(customerId);
        target.setDefault(true);
        target.setUpdatedBy(customerActor(customerId));
        return mapper.toResponse(repository.save(target));
    }

    @Override
    @Transactional
    public void softDelete(Long customerId, UUID addressId) {
        CustomerAddress address = repository.findByIdAndCustomerIdAndIsDeletedFalse(addressId, customerId)
                .orElseThrow(() -> new AddressNotFoundException(ADDRESS_NOT_FOUND));
        boolean wasDefault = address.isDefault();
        address.setDeleted(true);
        address.setDefault(false);
        address.setUpdatedBy(customerActor(customerId));
        repository.save(address);
        if (wasDefault) {
            repository.findFirstByCustomerIdAndIsDeletedFalseOrderByIsDefaultDescCreatedAtAsc(customerId).ifPresent(next -> {
                next.setDefault(true);
                next.setUpdatedBy(customerActor(customerId));
                repository.save(next);
            });
        }
    }

    private String normalizeText(String raw) {
        return ReverseGeocodingService.normalizeAddressText(raw);
    }

    private void applyPatch(CustomerAddress address, UpdateAddressRequest request) {
        if (request.type != null) {
            address.setType(request.type);
        }
        if (request.customLabel != null) {
            address.setCustomLabel(normalizeText(request.customLabel));
        }
        if (request.flatNo != null) {
            address.setFlatNo(normalizeText(request.flatNo));
        }
        if (request.floor != null) {
            address.setFloor(normalizeText(request.floor));
        }
        if (request.buildingName != null) {
            address.setBuildingName(normalizeText(request.buildingName));
        }
        if (request.landmark != null) {
            address.setLandmark(normalizeText(request.landmark));
        }
        if (request.addressLine != null) {
            address.setAddressLine(normalizeAddressLine(request.addressLine));
        }
        if (request.area != null) {
            address.setArea(ReverseGeocodingService.normalizeLocality(request.area));
        }
        if (request.sector != null) {
            address.setSector(normalizeText(request.sector));
        }
        if (request.mobile != null) {
            address.setMobile(normalizeMobile(request.mobile));
        }
        if (request.latitude != null) {
            address.setLatitude(BigDecimal.valueOf(request.latitude));
        }
        if (request.longitude != null) {
            address.setLongitude(BigDecimal.valueOf(request.longitude));
        }
    }

    private void refreshReverseGeocode(CustomerAddress address, UpdateAddressRequest request) {
        if (request.latitude == null || request.longitude == null) {
            return;
        }
        ReverseGeocodeResponse reverse = reverseGeocodeService.reverse(request.latitude, request.longitude);
        if (reverse.city != null && !reverse.city.isBlank()) {
            address.setCity(ReverseGeocodingService.normalizeLocality(reverse.city));
        }
        if (reverse.state != null && !reverse.state.isBlank()) {
            address.setState(ReverseGeocodingService.normalizeAddressText(reverse.state));
        }
        if (reverse.pincode != null && !reverse.pincode.isBlank()) {
            address.setPincode(ReverseGeocodingService.normalizePincode(reverse.pincode));
        }
    }

    private String customerActor(Long customerId) {
        return CUSTOMER_ACTOR_PREFIX + customerId;
    }

    private String normalizeAddressLine(String raw) {
        String normalized = ReverseGeocodingService.normalizeAddressText(raw);
        if (normalized == null) {
            return null;
        }
        return normalized.replaceAll(",\\s*,+", ", ");
    }

    private String normalizeMobile(String raw) {
        if (raw == null) {
            return null;
        }
        return raw.replaceAll("\\D", "");
    }
}

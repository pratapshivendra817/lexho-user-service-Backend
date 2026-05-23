package com.lexho.user_service.lexho.service;

import com.lexho.user_service.lexho.dto.LexhoDtos;
import com.lexho.user_service.lexho.entity.LexhoAddress;
import com.lexho.user_service.lexho.repository.LexhoAddressRepository;
import com.lexho.user_service.exception.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class LexhoAddressService {

    private final LexhoAddressRepository addressRepository;
    private final AddressCoreService addressCoreService;

    public LexhoAddressService(LexhoAddressRepository addressRepository, AddressCoreService addressCoreService) {
        this.addressRepository = addressRepository;
        this.addressCoreService = addressCoreService;
    }

    @Transactional
    public LexhoDtos.AddressResponse addOrUpdate(Long userId, LexhoDtos.AddressUpsertRequest request) {
        LexhoAddress address;
        if (request.id() == null) {
            address = addressRepository.findByUserIdAndIsDeletedFalse(userId).stream()
                    .findFirst()
                    .orElseGet(LexhoAddress::new);
        } else {
            address = addressCoreService.findOwnedActive(userId, request.id());
        }

        address.setUserId(userId);
        address.setHouseNo(request.line1());
        address.setArea(request.label());
        address.setCity(request.city());
        if (request.latitude() != null) {
            address.setLatitude(BigDecimal.valueOf(request.latitude()));
        }
        if (request.longitude() != null) {
            address.setLongitude(BigDecimal.valueOf(request.longitude()));
        }
        if (address.getId() == null && (address.getLatitude() == null || address.getLongitude() == null)) {
            throw new BadRequestException("latitude and longitude are required");
        }
        address.setState(address.getState() == null ? "Unknown" : address.getState());
        address.setPincode(address.getPincode() == null ? "" : address.getPincode().trim());
        if (address.getId() == null) {
            address.setDefault(true);
        }
        address.setUpdatedBy("CUSTOMER:" + userId);
        if (address.getId() == null) {
            address.setCreatedBy("CUSTOMER:" + userId);
        }

        return toResponse(addressRepository.save(address));
    }

    public List<LexhoDtos.AddressResponse> list(Long userId) {
        return addressCoreService.listActive(userId).stream().map(this::toResponse).toList();
    }

    public LexhoDtos.AddressResponse getById(Long userId, Long addressId) {
        return toResponse(addressCoreService.findOwnedActive(userId, addressId));
    }

    public LexhoAddress getRawById(Long userId, Long addressId) {
        return addressCoreService.findOwnedActive(userId, addressId);
    }

    @Transactional
    public void delete(Long userId, Long addressId) {
        addressCoreService.softDelete(userId, addressId);
    }

    @Transactional
    public LexhoDtos.AddressResponse makeDefault(Long userId, Long addressId) {
        return toResponse(addressCoreService.setDefault(userId, addressId));
    }

    private LexhoDtos.AddressResponse toResponse(LexhoAddress address) {
        return new LexhoDtos.AddressResponse(
                address.getId(),
                address.getArea(),
                address.getHouseNo(),
                address.getCity(),
                address.isDefault()
        );
    }
}

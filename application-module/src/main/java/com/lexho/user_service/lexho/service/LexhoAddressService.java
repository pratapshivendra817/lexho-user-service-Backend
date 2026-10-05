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
    private static final String CUSTOMER_ACTOR_PREFIX = "CUSTOMER:";

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
        address.setUpdatedBy(CUSTOMER_ACTOR_PREFIX + userId);
        if (address.getId() == null) {
            address.setCreatedBy(CUSTOMER_ACTOR_PREFIX + userId);
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
    public LexhoAddress applyBookingCoordinatesFallback(Long userId, Long addressId, Double latitude, Double longitude) {
        LexhoAddress address = addressCoreService.findOwnedActive(userId, addressId);
        if (isValidCoordinate(latitude, longitude) && !hasValidCoordinates(address)) {
            address.setLatitude(BigDecimal.valueOf(latitude));
            address.setLongitude(BigDecimal.valueOf(longitude));
            address.setUpdatedBy(CUSTOMER_ACTOR_PREFIX + userId);
            return addressRepository.save(address);
        }
        return address;
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

    private boolean hasValidCoordinates(LexhoAddress address) {
        return address.getLatitude() != null
                && address.getLongitude() != null
                && BigDecimal.ZERO.compareTo(address.getLatitude()) != 0
                && BigDecimal.ZERO.compareTo(address.getLongitude()) != 0;
    }

    private boolean isValidCoordinate(Double latitude, Double longitude) {
        return latitude != null
                && longitude != null
                && Double.isFinite(latitude)
                && Double.isFinite(longitude)
                && latitude >= -90.0d
                && latitude <= 90.0d
                && longitude >= -180.0d
                && longitude <= 180.0d
                && latitude != 0.0d
                && longitude != 0.0d;
    }
}

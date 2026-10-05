package com.lexho.user_service.lexho.service;

import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.lexho.entity.LexhoAddress;
import com.lexho.user_service.lexho.repository.LexhoAddressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddressCoreService {
    private static final String ADDRESS_NOT_FOUND = "Address not found";

    private final LexhoAddressRepository addressRepository;

    public AddressCoreService(LexhoAddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    @Transactional(readOnly = true)
    public List<LexhoAddress> listActive(Long userId) {
        return activeAddresses(userId);
    }

    private List<LexhoAddress> activeAddresses(Long userId) {
        return addressRepository.findByUserIdAndIsDeletedFalse(userId);
    }

    @Transactional(readOnly = true)
    public LexhoAddress findOwnedActive(Long userId, Long addressId) {
        return addressRepository.findByIdAndUserIdAndIsDeletedFalse(addressId, userId)
                .orElseThrow(() -> new BadRequestException(ADDRESS_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public LexhoAddress getCurrent(Long userId) {
        List<LexhoAddress> addresses = activeAddresses(userId);
        return addresses.stream().filter(LexhoAddress::isDefault).findFirst()
                .orElseGet(() -> addresses.stream().findFirst()
                        .orElseThrow(() -> new BadRequestException(ADDRESS_NOT_FOUND)));
    }

    @Transactional
    public LexhoAddress setDefault(Long userId, Long addressId) {
        // Lock all active addresses for this user to serialize concurrent default changes.
        List<LexhoAddress> addresses = addressRepository.findByUserIdAndIsDeletedFalseForUpdate(userId);
        LexhoAddress selected = addresses.stream()
                .filter(a -> a.getId().equals(addressId))
                .findFirst()
                .orElse(null);
        if (selected == null) {
            throw new BadRequestException(ADDRESS_NOT_FOUND);
        }

        // Step 1: clear existing default(s), flush, then set target default.
        boolean changed = false;
        for (LexhoAddress address : addresses) {
            if (address.isDefault() && !address.getId().equals(addressId)) {
                address.setDefault(false);
                changed = true;
            }
        }
        if (changed) {
            addressRepository.saveAll(addresses);
            addressRepository.flush();
        }

        if (!selected.isDefault()) {
            selected.setDefault(true);
            addressRepository.saveAndFlush(selected);
        }
        return selected;
    }

    @Transactional
    public void softDelete(Long userId, Long addressId) {
        List<LexhoAddress> addresses = activeAddresses(userId);
        if (addresses.size() <= 1) {
            throw new BadRequestException("Cannot delete last address");
        }
        LexhoAddress target = addresses.stream()
                .filter(a -> a.getId().equals(addressId))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(ADDRESS_NOT_FOUND));

        boolean wasDefault = target.isDefault();
        target.setDeleted(true);
        target.setDefault(false);
        target.setUpdatedBy("SYSTEM");
        addressRepository.save(target);

        if (wasDefault) {
            addresses.stream()
                    .filter(a -> !a.getId().equals(addressId))
                    .findFirst()
                    .ifPresent(next -> {
                        next.setDefault(true);
                        next.setUpdatedBy("SYSTEM");
                        addressRepository.save(next);
                    });
        }
    }
}

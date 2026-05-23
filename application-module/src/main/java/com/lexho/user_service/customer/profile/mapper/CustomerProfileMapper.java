package com.lexho.user_service.customer.profile.mapper;

import com.lexho.user_service.customer.profile.entity.CustomerEntity;
import com.lexho.user_service.customer.profile.dto.CustomerProfileAddressResponse;
import com.lexho.user_service.customer.profile.dto.CustomerProfileResponse;
import com.lexho.user_service.lexho.entity.LexhoAddress;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CustomerProfileMapper {

    public CustomerProfileResponse toResponse(CustomerEntity profile, List<LexhoAddress> addresses) {
        CustomerProfileResponse response = new CustomerProfileResponse();
        response.id = profile.getId();
        response.fullName = profile.getFullName();
        response.phoneNumber = profile.getPhoneNumber();
        response.mobileNumber = profile.getPhoneNumber();
        response.email = profile.getEmail();
        response.profileImageUrl = profile.getProfileImageUrl();
        response.gender = profile.getGender() == null ? null : profile.getGender().name();
        response.createdAt = profile.getCreatedAt();
        response.updatedAt = profile.getUpdatedAt();
        response.addresses = addresses.stream().map(this::toAddress).toList();
        String[] names = splitName(profile.getFullName());
        response.firstName = names[0];
        response.lastName = names[1];
        response.profileCompletionPercentage = completionPercent(response);
        return response;
    }

    private CustomerProfileAddressResponse toAddress(LexhoAddress address) {
        CustomerProfileAddressResponse response = new CustomerProfileAddressResponse();
        response.id = address.getId();
        response.label = "Address";
        response.addressLine = (address.getHouseNo() == null ? "" : address.getHouseNo())
                + ", "
                + (address.getArea() == null ? "" : address.getArea())
                + ", "
                + (address.getCity() == null ? "" : address.getCity());
        response.latitude = address.getLatitude() == null ? null : address.getLatitude().doubleValue();
        response.longitude = address.getLongitude() == null ? null : address.getLongitude().doubleValue();
        response.isDefault = address.isDefault();
        return response;
    }

    private int completionPercent(CustomerProfileResponse response) {
        int completed = 0;
        if (response.fullName != null && !response.fullName.isBlank()) {
            completed++;
        }
        if (response.email != null && !response.email.isBlank()) {
            completed++;
        }
        if (response.phoneNumber != null && !response.phoneNumber.isBlank()) {
            completed++;
        }
        if (response.gender != null && !response.gender.isBlank()) {
            completed++;
        }
        if (response.addresses != null && !response.addresses.isEmpty()) {
            completed++;
        }
        return (int) Math.round((completed * 100.0) / 5);
    }

    private String[] splitName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return new String[]{"", ""};
        }
        String trimmed = fullName.trim();
        int idx = trimmed.indexOf(' ');
        if (idx < 0) {
            return new String[]{trimmed, ""};
        }
        return new String[]{trimmed.substring(0, idx), trimmed.substring(idx + 1).trim()};
    }
}

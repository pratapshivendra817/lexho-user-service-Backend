package com.lexho.user.mapper;

import com.lexho.common.enums.Role;
import com.lexho.user.dto.AdminUserDetailResponse;
import com.lexho.user.dto.AdminUserResponse;
import com.lexho.user.dto.BookingSummaryDto;
import com.lexho.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserManagementMapper {

    public AdminUserResponse toResponse(User user) {
        AdminUserResponse response = new AdminUserResponse();
        response.setId(user.getId());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setRole(toApiRole(user.getRole()));
        response.setProfileImage(user.getProfileImage());
        response.setCity(user.getCity());
        response.setState(user.getState());
        response.setAddress(user.getAddress());
        response.setStatus(user.getStatus().name());
        response.setTotalBookings(user.getTotalBookings() == null ? 0L : user.getTotalBookings());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }

    public AdminUserDetailResponse toDetailResponse(User user, BookingSummaryDto bookingSummary) {
        AdminUserDetailResponse response = new AdminUserDetailResponse();
        AdminUserResponse base = toResponse(user);
        response.setId(base.getId());
        response.setFullName(base.getFullName());
        response.setEmail(base.getEmail());
        response.setPhoneNumber(base.getPhoneNumber());
        response.setRole(base.getRole());
        response.setProfileImage(base.getProfileImage());
        response.setCity(base.getCity());
        response.setState(base.getState());
        response.setAddress(base.getAddress());
        response.setStatus(base.getStatus());
        response.setTotalBookings(base.getTotalBookings());
        response.setCreatedAt(base.getCreatedAt());
        response.setUpdatedAt(base.getUpdatedAt());
        response.setBookingSummary(bookingSummary);
        return response;
    }

    private String toApiRole(Role role) {
        if (role == Role.PARTNER) {
            return "PARTNER";
        }
        return role.name();
    }
}

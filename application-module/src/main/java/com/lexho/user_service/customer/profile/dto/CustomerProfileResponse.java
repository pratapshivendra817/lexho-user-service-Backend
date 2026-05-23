package com.lexho.user_service.customer.profile.dto;

import java.time.Instant;
import java.util.List;

public class CustomerProfileResponse {
    public Long id;
    public String fullName;
    public String phoneNumber;
    public String mobileNumber;
    public String email;
    public String profileImageUrl;
    public String gender;
    public String firstName;
    public String lastName;
    public String dateOfBirth;
    public List<CustomerProfileAddressResponse> addresses;
    public Integer profileCompletionPercentage;
    public Instant createdAt;
    public Instant updatedAt;
}

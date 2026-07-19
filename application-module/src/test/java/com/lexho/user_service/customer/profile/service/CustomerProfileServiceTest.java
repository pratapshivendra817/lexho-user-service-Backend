package com.lexho.user_service.customer.profile.service;

import com.lexho.user_service.admin.entity.AdminProfileContentEntity;
import com.lexho.user_service.admin.repository.AdminProfileContentRepository;
import com.lexho.user_service.customer.profile.dto.*;
import com.lexho.user_service.customer.profile.entity.CustomerAccountDeleteRequestEntity;
import com.lexho.user_service.customer.profile.entity.CustomerEntity;
import com.lexho.user_service.customer.profile.entity.CustomerGender;
import com.lexho.user_service.customer.profile.mapper.CustomerProfileMapper;
import com.lexho.user_service.customer.profile.repository.CustomerAccountDeleteRequestRepository;
import com.lexho.user_service.customer.profile.repository.CustomerRepository;
import com.lexho.user_service.customer.profile.storage.ImageStorageService;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.lexho.entity.LexhoAddress;
import com.lexho.user_service.lexho.entity.LexhoUserProfile;
import com.lexho.user_service.lexho.repository.LexhoAddressRepository;
import com.lexho.user_service.lexho.repository.LexhoUserProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerProfileServiceTest {

    @Mock CustomerRepository customerRepository;
    @Mock LexhoUserProfileRepository legacyProfileRepository;
    @Mock LexhoAddressRepository addressRepository;
    @Mock CustomerAccountDeleteRequestRepository deleteRequestRepository;
    @Mock AdminProfileContentRepository adminProfileContentRepository;
    @Mock CustomerProfileMapper profileMapper;
    @Mock ImageStorageService imageStorageService;
    @InjectMocks CustomerProfileService service;

    @Test
    void getProfileShouldBootstrapLegacyProfileAndMapAddresses() {
        LexhoUserProfile legacy = new LexhoUserProfile();
        ReflectionTestUtils.setField(legacy, "id", 7L);
        legacy.setName(" Legacy User ");
        legacy.setEmail("legacy@example.com");
        legacy.setMobile("9876543210");
        legacy.setGender("female");
        when(customerRepository.findById(7L)).thenReturn(Optional.empty());
        when(legacyProfileRepository.findById(7L)).thenReturn(Optional.of(legacy));
        when(customerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        CustomerProfileResponse mapped = new CustomerProfileResponse();
        when(profileMapper.toResponse(any(), any())).thenReturn(mapped);

        assertSame(mapped, service.getProfile(7L));
        verify(customerRepository).save(argThat(customer -> customer.getId().equals(7L)
                && customer.getFullName().equals("Legacy User")
                && customer.getGender() == CustomerGender.FEMALE));
    }

    @Test
    void getProfileShouldRejectUnknownCustomer() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());
        when(legacyProfileRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getProfile(99L));
    }

    @Test
    void updateProfileShouldNormalizeFieldsAndRejectDuplicateEmail() {
        CustomerEntity customer = customer();
        when(customerRepository.findById(7L)).thenReturn(Optional.of(customer));
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.fullName = " Jane Doe ";
        request.email = " JANE@EXAMPLE.COM ";
        request.gender = "female";
        when(customerRepository.existsByEmailIgnoreCaseAndIdNot("jane@example.com", 7L)).thenReturn(false);

        service.updateProfile(7L, request);

        assertEquals("Jane Doe", customer.getFullName());
        assertEquals("jane@example.com", customer.getEmail());
        assertEquals(CustomerGender.FEMALE, customer.getGender());
        assertEquals(customer.getPhoneNumber(), customer.getMobile());
        when(customerRepository.existsByEmailIgnoreCaseAndIdNot("jane@example.com", 7L)).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.updateProfile(7L, request));
    }

    @Test
    void profileUpdatesShouldRejectLockedAndInactiveCustomers() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.fullName = "Jane";
        CustomerEntity customer = customer();
        customer.setProfileEditLocked(true);
        when(customerRepository.findById(7L)).thenReturn(Optional.of(customer));
        assertThrows(IllegalArgumentException.class, () -> service.updateProfile(7L, request));

        customer.setProfileEditLocked(false);
        customer.setActive(false);
        CustomerEmailUpdateRequest email = new CustomerEmailUpdateRequest();
        email.email = "jane@example.com";
        assertThrows(IllegalArgumentException.class, () -> service.updateEmail(7L, email));
    }

    @Test
    void updateProfileDetailsShouldJoinNamesAndGender() {
        CustomerEntity customer = customer();
        when(customerRepository.findById(7L)).thenReturn(Optional.of(customer));
        CustomerProfileUpdateRequest request = new CustomerProfileUpdateRequest();
        request.firstName = " Jane ";
        request.lastName = " Doe ";
        request.gender = "OTHER";

        service.updateProfileDetails(7L, request);

        assertEquals("Jane Doe", customer.getFullName());
        assertEquals(CustomerGender.OTHER, customer.getGender());
        verify(customerRepository).save(customer);
    }

    @Test
    void updateEmailShouldValidateRequiredAndUniqueValue() {
        CustomerEntity customer = customer();
        when(customerRepository.findById(7L)).thenReturn(Optional.of(customer));
        CustomerEmailUpdateRequest request = new CustomerEmailUpdateRequest();
        request.email = " ";
        assertThrows(IllegalArgumentException.class, () -> service.updateEmail(7L, request));
        request.email = " EXISTING@EXAMPLE.COM ";
        when(customerRepository.existsByEmailIgnoreCaseAndIdNot("existing@example.com", 7L)).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.updateEmail(7L, request));
        request.email = "new@example.com";
        when(customerRepository.existsByEmailIgnoreCaseAndIdNot("new@example.com", 7L)).thenReturn(false);
        service.updateEmail(7L, request);
        assertEquals("new@example.com", customer.getEmail());
    }

    @Test
    void getAddressesShouldFormatAddressCoordinatesAndDefaultFlag() {
        LexhoAddress address = new LexhoAddress();
        ReflectionTestUtils.setField(address, "id", 3L);
        address.setHouseNo("12A");
        address.setArea("Sector 21");
        address.setCity("Delhi");
        address.setLatitude(BigDecimal.valueOf(28.6));
        address.setLongitude(BigDecimal.valueOf(77.2));
        address.setDefault(true);
        when(addressRepository.findByUserId(7L)).thenReturn(List.of(address));

        CustomerProfileAddressResponse response = service.getAddresses(7L).get(0);

        assertEquals("12A, Sector 21, Delhi", response.addressLine);
        assertEquals(28.6, response.latitude);
        assertTrue(response.isDefault);
    }

    @Test
    void uploadAndDeleteImageShouldReplaceStoredImage() {
        CustomerEntity customer = customer();
        customer.setProfileImageUrl("old.jpg");
        when(customerRepository.findById(7L)).thenReturn(Optional.of(customer));
        MockMultipartFile file = new MockMultipartFile("file", "new.jpg", "image/jpeg", "image".getBytes());
        when(imageStorageService.upload(file)).thenReturn("new.jpg");

        service.uploadProfileImage(7L, file);

        assertEquals("new.jpg", customer.getProfileImageUrl());
        verify(imageStorageService).delete("old.jpg");
        service.deleteProfileImage(7L);
        assertNull(customer.getProfileImageUrl());
        verify(imageStorageService).delete("new.jpg");
    }

    @Test
    void accountDeleteRequestShouldBeIdempotentAndExposeStatus() {
        when(deleteRequestRepository.existsByUserIdAndResolvedFalse(7L)).thenReturn(false, true, true);
        service.requestAccountDelete(7L);
        service.requestAccountDelete(7L);

        verify(deleteRequestRepository, times(1)).save(argThat(entity -> entity.getUserId().equals(7L)
                && !entity.isResolved() && entity.getResolutionStatus().equals("PENDING")));
        assertEquals(true, service.getAccountDeleteStatus(7L).get("requested"));
    }

    @Test
    void profileContentShouldMapStoredAndDefaultContent() {
        AdminProfileContentEntity entity = new AdminProfileContentEntity();
        entity.setAboutUs("About");
        entity.setTermsOfService("Terms");
        entity.setPrivacyPolicy("Privacy");
        when(adminProfileContentRepository.findById(1L)).thenReturn(Optional.of(entity));
        assertEquals("About", service.getProfileContent().aboutUs);

        when(adminProfileContentRepository.findById(1L)).thenReturn(Optional.empty());
        CustomerProfileContentResponse defaults = service.getProfileContent();
        assertEquals("", defaults.aboutUs);
        assertEquals("", defaults.termsOfService);
    }

    private static CustomerEntity customer() {
        CustomerEntity customer = new CustomerEntity();
        customer.setId(7L);
        customer.setFullName("Customer");
        customer.setPhoneNumber("9876543210");
        customer.setMobile("9876543210");
        customer.setActive(true);
        customer.setProfileEditLocked(false);
        return customer;
    }
}

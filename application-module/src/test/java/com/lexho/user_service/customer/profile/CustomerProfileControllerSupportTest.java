package com.lexho.user_service.customer.profile;

import com.lexho.user_service.customer.profile.controller.CustomerProfileController;
import com.lexho.user_service.customer.profile.dto.CustomerEmailUpdateRequest;
import com.lexho.user_service.customer.profile.dto.CustomerProfileAddressResponse;
import com.lexho.user_service.customer.profile.dto.CustomerProfileContentResponse;
import com.lexho.user_service.customer.profile.dto.CustomerProfileResponse;
import com.lexho.user_service.customer.profile.dto.CustomerProfileUpdateRequest;
import com.lexho.user_service.customer.profile.dto.UpdateProfileRequest;
import com.lexho.user_service.customer.profile.service.CustomerProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerProfileControllerSupportTest {

    @Test
    void customerProfileControllerDelegatesNonMultipartEndpointsAndPrefersImageFile() {
        CustomerProfileService customerProfileService = mock(CustomerProfileService.class);
        CustomerProfileController controller = new CustomerProfileController(customerProfileService);

        CustomerProfileResponse profileResponse = new CustomerProfileResponse();
        profileResponse.id = 7L;
        CustomerProfileContentResponse contentResponse = new CustomerProfileContentResponse();
        contentResponse.aboutUs = "about";
        CustomerProfileAddressResponse addressResponse = new CustomerProfileAddressResponse();
        addressResponse.addressLine = "Delhi";
        UpdateProfileRequest updateProfileRequest = new UpdateProfileRequest();
        CustomerProfileUpdateRequest profileUpdateRequest = new CustomerProfileUpdateRequest();
        CustomerEmailUpdateRequest emailUpdateRequest = new CustomerEmailUpdateRequest();

        when(customerProfileService.getProfile(7L)).thenReturn(profileResponse);
        when(customerProfileService.updateProfile(7L, updateProfileRequest)).thenReturn(profileResponse);
        when(customerProfileService.updateProfileDetails(7L, profileUpdateRequest)).thenReturn(profileResponse);
        when(customerProfileService.updateEmail(7L, emailUpdateRequest)).thenReturn(profileResponse);
        when(customerProfileService.deleteProfileImage(7L)).thenReturn(profileResponse);
        when(customerProfileService.getAddresses(7L)).thenReturn(List.of(addressResponse));
        when(customerProfileService.getAccountDeleteStatus(7L)).thenReturn(Map.of("requested", true));
        when(customerProfileService.getProfileContent()).thenReturn(contentResponse);

        assertThat(controller.getProfile(7L).getData()).isSameAs(profileResponse);
        assertThat(controller.updateProfile(7L, updateProfileRequest).getData()).isSameAs(profileResponse);
        assertThat(controller.updateProfileDetails(7L, profileUpdateRequest).getData()).isSameAs(profileResponse);
        assertThat(controller.updateEmail(7L, emailUpdateRequest).getData()).isSameAs(profileResponse);
        assertThat(controller.deleteProfileImage(7L).getData()).isSameAs(profileResponse);
        assertThat(controller.getAddresses(7L).getData()).containsExactly(addressResponse);
        assertThat(controller.requestAccountDelete(7L).getData()).containsEntry("requested", true);
        assertThat(controller.accountDeleteStatus(7L).getData()).containsEntry("requested", true);
        assertThat(controller.getCustomerProfileContent().getData()).isSameAs(contentResponse);

        MockMultipartFile image = new MockMultipartFile("image", "image.jpg", "image/jpeg", new byte[] {1});
        MockMultipartFile file = new MockMultipartFile("file", "file.jpg", "image/jpeg", new byte[] {2});
        when(customerProfileService.uploadProfileImage(7L, image)).thenReturn(profileResponse);

        ResponseEntity<?> uploadResponse = controller.uploadProfileImage(7L, image, file);

        assertThat(uploadResponse.getStatusCode().value()).isEqualTo(200);
        verify(customerProfileService).uploadProfileImage(7L, image);
        verify(customerProfileService).requestAccountDelete(7L);
    }
}

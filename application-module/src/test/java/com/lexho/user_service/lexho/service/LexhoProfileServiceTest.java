package com.lexho.user_service.lexho.service;

import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.lexho.dto.LexhoDtos;
import com.lexho.user_service.lexho.entity.LexhoUserProfile;
import com.lexho.user_service.lexho.repository.LexhoUserProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LexhoProfileServiceTest {

    private final LexhoUserProfileRepository repository = mock(LexhoUserProfileRepository.class);
    private final LexhoProfileService service = new LexhoProfileService(repository);

    @Test
    void getsUpdatesAndChangesProfileImage() {
        LexhoUserProfile profile = profile();
        when(repository.findById(1L)).thenReturn(Optional.of(profile));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.getProfile(1L).name()).isEqualTo("Old");
        var updated = service.updateProfile(1L, new LexhoDtos.UserProfileUpdateRequest("New", "new@example.com", "female"));
        assertThat(updated.name()).isEqualTo("New");
        assertThat(updated.email()).isEqualTo("new@example.com");
        assertThat(service.updateProfileImage(1L, "/image.jpg").avatar()).isEqualTo("/image.jpg");
    }

    @Test
    void preservesNullUpdateFieldsAndRejectsMissingUser() {
        LexhoUserProfile profile = profile();
        when(repository.findById(1L)).thenReturn(Optional.of(profile));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.updateProfile(1L, new LexhoDtos.UserProfileUpdateRequest(null, null, null)).name())
                .isEqualTo("Old");
        when(repository.findById(2L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getProfile(2L)).isInstanceOf(BadRequestException.class);
    }

    private LexhoUserProfile profile() {
        LexhoUserProfile profile = new LexhoUserProfile();
        ReflectionTestUtils.setField(profile, "id", 1L);
        profile.setMobile("9999999999");
        profile.setName("Old");
        profile.setEmail("old@example.com");
        return profile;
    }
}

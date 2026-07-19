package com.lexho.user_service.notification.service;

import com.lexho.admin.entity.Admin;
import com.lexho.admin.repository.AdminRepository;
import com.lexho.notification.entity.Notification;
import com.lexho.notification.enums.NotificationUserType;
import com.lexho.user.entity.User;
import com.lexho.user.repository.UserRepository;
import com.lexho.user_service.lexho.entity.LexhoUserProfile;
import com.lexho.user_service.lexho.repository.LexhoUserProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationRecipientResolverTest {
    @Mock LexhoUserProfileRepository profileRepository;
    @Mock UserRepository userRepository;
    @Mock AdminRepository adminRepository;
    @InjectMocks NotificationRecipientResolver resolver;

    @Test
    void resolvesAdminEmail() {
        Notification notification = notification(7L, NotificationUserType.ADMIN);
        Admin admin = new Admin(); admin.setEmail("admin@test.com");
        when(adminRepository.findById(7L)).thenReturn(Optional.of(admin));
        assertEquals("admin@test.com", resolver.resolve(notification).email());
        verifyNoInteractions(profileRepository, userRepository);
    }

    @Test
    void prefersProfileThenFallsBackToUser() {
        Notification notification = notification(8L, NotificationUserType.CUSTOMER);
        LexhoUserProfile profile = new LexhoUserProfile();
        profile.setMobile("9000000000"); profile.setEmail("p@test.com"); profile.setName("Profile");
        when(profileRepository.findById(8L)).thenReturn(Optional.of(profile));
        assertEquals("Profile", resolver.resolve(notification).name());

        when(profileRepository.findById(8L)).thenReturn(Optional.empty());
        User user = new User(); user.setPhoneNumber("9111111111"); user.setEmail("u@test.com"); user.setFullName("User");
        when(userRepository.findById(8L)).thenReturn(Optional.of(user));
        assertEquals("9111111111", resolver.resolve(notification).mobile());
    }

    @Test
    void returnsEmptyContactForMissingIdOrRecipient() {
        Notification missingId = notification(null, NotificationUserType.CUSTOMER);
        assertNull(resolver.resolve(missingId).email());

        Notification unknown = notification(99L, NotificationUserType.CUSTOMER);
        when(profileRepository.findById(99L)).thenReturn(Optional.empty());
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertNull(resolver.resolve(unknown).mobile());
    }

    private Notification notification(Long id, NotificationUserType type) {
        Notification notification = new Notification();
        notification.setUserId(id); notification.setUserType(type);
        return notification;
    }
}

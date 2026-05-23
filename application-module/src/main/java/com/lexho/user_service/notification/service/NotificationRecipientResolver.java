package com.lexho.user_service.notification.service;

import com.lexho.admin.entity.Admin;
import com.lexho.admin.repository.AdminRepository;
import com.lexho.notification.entity.Notification;
import com.lexho.notification.enums.NotificationUserType;
import com.lexho.user.entity.User;
import com.lexho.user.repository.UserRepository;
import com.lexho.user_service.lexho.entity.LexhoUserProfile;
import com.lexho.user_service.lexho.repository.LexhoUserProfileRepository;
import org.springframework.stereotype.Service;

@Service
public class NotificationRecipientResolver {

    private final LexhoUserProfileRepository lexhoUserProfileRepository;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;

    public NotificationRecipientResolver(LexhoUserProfileRepository lexhoUserProfileRepository,
                                         UserRepository userRepository,
                                         AdminRepository adminRepository) {
        this.lexhoUserProfileRepository = lexhoUserProfileRepository;
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
    }

    public RecipientContact resolve(Notification notification) {
        if (notification.getUserId() == null) {
            return new RecipientContact(null, null, null);
        }

        NotificationUserType userType = notification.getUserType();
        if (userType == NotificationUserType.ADMIN) {
            Admin admin = adminRepository.findById(notification.getUserId()).orElse(null);
            return new RecipientContact(null, admin == null ? null : admin.getEmail(), null);
        }

        LexhoUserProfile profile = lexhoUserProfileRepository.findById(notification.getUserId()).orElse(null);
        if (profile != null) {
            return new RecipientContact(profile.getMobile(), profile.getEmail(), profile.getName());
        }

        User user = userRepository.findById(notification.getUserId()).orElse(null);
        if (user != null) {
            return new RecipientContact(user.getPhoneNumber(), user.getEmail(), user.getFullName());
        }

        return new RecipientContact(null, null, null);
    }

    public record RecipientContact(String mobile, String email, String name) {
    }
}


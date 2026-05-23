package com.lexho.user_service.lexho.service;

import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.lexho.dto.LexhoDtos;
import com.lexho.user_service.lexho.entity.LexhoUserProfile;
import com.lexho.user_service.lexho.repository.LexhoUserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LexhoProfileService {

    private final LexhoUserProfileRepository profileRepository;

    public LexhoProfileService(LexhoUserProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    public LexhoDtos.UserProfileResponse getProfile(Long userId) {
        LexhoUserProfile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));
        return toProfileResponse(profile);
    }

    @Transactional
    public LexhoDtos.UserProfileResponse updateProfile(Long userId, LexhoDtos.UserProfileUpdateRequest request) {
        LexhoUserProfile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));
        if (request.name() != null) {
            profile.setName(request.name());
        }
        if (request.email() != null) {
            profile.setEmail(request.email());
        }
        if (request.gender() != null) {
            profile.setGender(request.gender());
        }
        return toProfileResponse(profileRepository.save(profile));
    }

    @Transactional
    public LexhoDtos.UserProfileResponse updateProfileImage(Long userId, String profileImageUrl) {
        LexhoUserProfile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));
        profile.setProfileImage(profileImageUrl);
        return toProfileResponse(profileRepository.save(profile));
    }

    private LexhoDtos.UserProfileResponse toProfileResponse(LexhoUserProfile profile) {
        return new LexhoDtos.UserProfileResponse(
                profile.getId(),
                profile.getMobile(),
                profile.getName(),
                profile.getEmail(),
                profile.getProfileImage()
        );
    }
}

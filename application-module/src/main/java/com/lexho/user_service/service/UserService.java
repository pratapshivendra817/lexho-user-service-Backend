package com.lexho.user_service.service;

import com.lexho.user.entity.User;
import com.lexho.user.repository.UserRepository;
import com.lexho.user_service.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}


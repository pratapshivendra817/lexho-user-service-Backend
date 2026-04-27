package com.lexho.user_service.service;

import com.lexho.common.enums.Role;
import com.lexho.user.entity.User;
import com.lexho.user.repository.UserRepository;
import com.lexho.user_service.config.AppProperties;
import com.lexho.user_service.dto.OtpSendResponse;
import com.lexho.user_service.dto.OtpVerifyResponse;
import com.lexho.user_service.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final OtpService otpService;
    private final JwtUtil jwtUtil;
    private final AppProperties appProperties;

    public AuthService(UserRepository userRepository,
                       OtpService otpService,
                       JwtUtil jwtUtil,
                       AppProperties appProperties) {
        this.userRepository = userRepository;
        this.otpService = otpService;
        this.jwtUtil = jwtUtil;
        this.appProperties = appProperties;
    }

    public OtpSendResponse sendOtp(String mobile) {
        otpService.generateOtp(mobile);
        return new OtpSendResponse(mobile, 300, 30);
    }

    public OtpVerifyResponse verifyOtp(String mobile, String otp) {
        otpService.verifyOtp(mobile, otp);

        User user = userRepository.findByMobile(mobile)
                .orElseGet(() -> userRepository.save(new User(mobile, true)));

        if (user.getRole() == null) {
            user.setRole(Role.CUSTOMER);
        }
        if (user.getCity() == null || user.getCity().isBlank()) {
            user.setCity(appProperties.getUser().getDefaultCity());
        }
        user.setActive(true);
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getId(), user.getMobile(), user.getRole());
        OtpVerifyResponse response = new OtpVerifyResponse(token, user.getId(), user.getMobile(), user.isActive());
        log.info("OTP verify response prepared: userId={}, mobile={}, role={}, active={}",
                response.getUserId(), response.getMobile(), user.getRole(), response.isActive());
        return response;
    }
}

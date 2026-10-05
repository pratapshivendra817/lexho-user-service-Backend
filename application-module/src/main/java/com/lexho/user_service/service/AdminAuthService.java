package com.lexho.user_service.service;

import com.lexho.common.enums.Role;
import com.lexho.admin.entity.Admin;
import com.lexho.admin.repository.AdminRepository;
import com.lexho.user_service.config.AdminAuthProperties;
import com.lexho.user_service.admin.service.AdminAuditService;
import com.lexho.user_service.dto.AdminLoginRequest;
import com.lexho.user_service.dto.AdminLoginResponse;
import com.lexho.user_service.dto.AdminTokenResponse;
import com.lexho.user_service.dto.AdminVerifyOtpRequest;
import com.lexho.user_service.exception.AccountLockedException;
import com.lexho.user_service.exception.UnauthorizedException;
import com.lexho.user_service.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class AdminAuthService {

    private static final Logger log = LoggerFactory.getLogger(AdminAuthService.class);
    public static final String ADMIN_PORTAL_ACCESS_DENIED_MESSAGE =
            "This account is not authorized to access the Admin portal. Use an active ADMIN account";

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminOtpService adminOtpService;
    private final AdminOtpDeliveryService adminOtpDeliveryService;
    private final AdminLoginRateLimiterService adminLoginRateLimiterService;
    private final JwtUtil jwtUtil;
    private final AdminAuthProperties adminAuthProperties;
    private final AdminAuditService adminAuditService;

    public AdminAuthService(AdminRepository adminRepository,
                            PasswordEncoder passwordEncoder,
                            AdminOtpService adminOtpService,
                            AdminOtpDeliveryService adminOtpDeliveryService,
                            AdminLoginRateLimiterService adminLoginRateLimiterService,
                            JwtUtil jwtUtil,
                            AdminAuthProperties adminAuthProperties,
                            AdminAuditService adminAuditService) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminOtpService = adminOtpService;
        this.adminOtpDeliveryService = adminOtpDeliveryService;
        this.adminLoginRateLimiterService = adminLoginRateLimiterService;
        this.jwtUtil = jwtUtil;
        this.adminAuthProperties = adminAuthProperties;
        this.adminAuditService = adminAuditService;
    }

    @Transactional
    public AdminLoginResponse login(AdminLoginRequest request) {
        String email = normalizeEmail(request.getEmail());
        adminLoginRateLimiterService.validateAllowed(email);

        Admin admin = adminRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> {
                    log.warn("Admin login failed: admin not found for email={}", email);
                    return new UnauthorizedException("Invalid email or password");
                });

        if (!admin.isActive()) {
            log.warn("Admin login failed: inactive admin for email={}, adminId={}", email, admin.getId());
            throw new UnauthorizedException(ADMIN_PORTAL_ACCESS_DENIED_MESSAGE);
        }

        if (admin.unlockIfCooldownExpired(LocalDateTime.now())) {
            adminRepository.save(admin);
        }

        if (admin.isAccountLocked()) {
            throw new AccountLockedException("Admin account is locked");
        }

        String rawPassword = request.getPassword();
        String storedHash = admin.getPasswordHash();
        boolean passwordMatches = matchesAdminPassword(rawPassword, storedHash, email, admin.getId());

        if (!passwordMatches) {
            log.warn("Admin login failed: invalid password for email={}, adminId={}", email, admin.getId());
            admin.registerFailedAttempt(
                    adminAuthProperties.getPassword().getMaxFailedAttempts(),
                    adminAuthProperties.getPassword().getLockDurationMinutes()
            );
            adminRepository.save(admin);

            if (admin.isAccountLocked()) {
                throw new AccountLockedException("Admin account is locked due to multiple failed attempts");
            }
            throw new UnauthorizedException("Invalid email or password");
        }

        String otp;
        try {
            otp = adminOtpService.generateOtp();
            adminOtpService.storeOtp(email, admin.getId(), otp);
            adminOtpDeliveryService.sendOtpByEmail(email, otp);
        } catch (RuntimeException ex) {
            log.error("Admin login failed: OTP generation/delivery failure for email={}, adminId={}", email, admin.getId(), ex);
            throw ex;
        }
        adminAuditService.log(admin.getId(), "ADMIN_LOGIN_OTP_SENT", "admins", admin.getId(), null);

        return new AdminLoginResponse(email, adminAuthProperties.getOtp().getExpirySeconds());
    }

    @Transactional
    public AdminTokenResponse verifyOtp(AdminVerifyOtpRequest request) {
        String email = normalizeEmail(request.getEmail());
        Admin admin = adminRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedException("Admin not found"));

        if (!admin.isActive()) {
            throw new UnauthorizedException(ADMIN_PORTAL_ACCESS_DENIED_MESSAGE);
        }

        if (admin.unlockIfCooldownExpired(LocalDateTime.now())) {
            adminRepository.save(admin);
        }

        if (admin.isAccountLocked()) {
            throw new AccountLockedException("Admin account is locked");
        }

        adminOtpService.validateOtp(email, admin.getId(), request.getOtp());
        adminOtpService.deleteOtp(email, admin.getId());

        admin.resetFailedAttempts();
        if (admin.getRole() == null) {
            admin.setRole(Role.ADMIN);
        }
        adminRepository.save(admin);

        String token = jwtUtil.generateAdminToken(admin.getId(), email, admin.getRole());
        adminAuditService.log(admin.getId(), "ADMIN_LOGIN_SUCCESS", "admins", admin.getId(), null);
        return new AdminTokenResponse(token);
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private boolean matchesAdminPassword(String rawPassword, String storedHash, String email, Long adminId) {
        if (storedHash == null) {
            log.warn("Admin login failed: stored password hash is null for email={}, adminId={}", email, adminId);
            return false;
        }

        String normalizedHash = storedHash.trim();
        if (normalizedHash.startsWith("{bcrypt}")) {
            normalizedHash = normalizedHash.substring("{bcrypt}".length()).trim();
        }
        String normalizedRawPassword = rawPassword == null ? null : rawPassword.trim();

        try {
            boolean matches = passwordEncoder.matches(normalizedRawPassword, normalizedHash);
            return matches;
        } catch (RuntimeException ex) {
            log.warn("Admin login failed: password hash validation error for email={}, adminId={}", email, adminId, ex);
            return false;
        }
    }
}

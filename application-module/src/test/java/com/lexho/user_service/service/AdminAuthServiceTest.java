package com.lexho.user_service.service;

import com.lexho.admin.entity.Admin;
import com.lexho.admin.repository.AdminRepository;
import com.lexho.common.enums.Role;
import com.lexho.user_service.admin.service.AdminAuditService;
import com.lexho.user_service.config.AdminAuthProperties;
import com.lexho.user_service.dto.AdminLoginRequest;
import com.lexho.user_service.dto.AdminLoginResponse;
import com.lexho.user_service.dto.AdminTokenResponse;
import com.lexho.user_service.dto.AdminVerifyOtpRequest;
import com.lexho.user_service.exception.AccountLockedException;
import com.lexho.user_service.exception.UnauthorizedException;
import com.lexho.user_service.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuthServiceTest {

    @Mock
    private AdminRepository adminRepository;
    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Mock
    private AdminOtpService adminOtpService;
    @Mock
    private AdminOtpDeliveryService adminOtpDeliveryService;
    @Mock
    private AdminLoginRateLimiterService adminLoginRateLimiterService;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private AdminAuditService adminAuditService;

    private AdminAuthProperties adminAuthProperties;

    @InjectMocks
    private AdminAuthService adminAuthService;

    @BeforeEach
    void setUp() {
        adminAuthProperties = new AdminAuthProperties();
        adminAuthProperties.getOtp().setExpirySeconds(180);
        adminAuthProperties.getPassword().setMaxFailedAttempts(3);
        adminAuthProperties.getPassword().setLockDurationMinutes(15);
        adminAuthService = new AdminAuthService(
                adminRepository,
                passwordEncoder,
                adminOtpService,
                adminOtpDeliveryService,
                adminLoginRateLimiterService,
                jwtUtil,
                adminAuthProperties,
                adminAuditService
        );
    }

    @Test
    void loginShouldRejectUnknownAdmin() {
        AdminLoginRequest request = loginRequest(" Admin@Lexho.com ", "Password@123");
        when(adminRepository.findByEmailIgnoreCase("admin@lexho.com")).thenReturn(Optional.empty());

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> adminAuthService.login(request));

        assertEquals("Invalid email or password", ex.getMessage());
        verify(adminLoginRateLimiterService).validateAllowed("admin@lexho.com");
    }

    @Test
    void loginShouldRejectInactiveAdmin() {
        Admin admin = activeAdmin("admin@lexho.com");
        admin.setActive(false);
        when(adminRepository.findByEmailIgnoreCase("admin@lexho.com")).thenReturn(Optional.of(admin));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> adminAuthService.login(loginRequest("admin@lexho.com", "Password@123")));

        assertEquals("Admin account is inactive", ex.getMessage());
    }

    @Test
    void loginShouldRejectLockedAdminWithoutCheckingPassword() {
        Admin admin = activeAdmin("admin@lexho.com");
        admin.setAccountLocked(true);
        admin.setLockUntil(LocalDateTime.now().plusMinutes(5));
        when(adminRepository.findByEmailIgnoreCase("admin@lexho.com")).thenReturn(Optional.of(admin));

        assertThrows(AccountLockedException.class,
                () -> adminAuthService.login(loginRequest("admin@lexho.com", "Password@123")));

        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void loginShouldSaveUnlockedAdminAndSendOtpOnSuccess() {
        Admin admin = activeAdmin("admin@lexho.com");
        admin.setAccountLocked(true);
        admin.setFailedAttempts(2);
        admin.setLockUntil(LocalDateTime.now().minusMinutes(1));
        admin.setPasswordHash(" {bcrypt} hashed-password ");
        when(adminRepository.findByEmailIgnoreCase("admin@lexho.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("Password@123", "hashed-password")).thenReturn(true);
        when(adminOtpService.generateOtp()).thenReturn("654321");

        AdminLoginResponse response = adminAuthService.login(loginRequest(" admin@lexho.com ", " Password@123 "));

        assertEquals("admin@lexho.com", response.getEmail());
        assertEquals(180, response.getOtpExpiresInSeconds());
        verify(adminRepository).save(admin);
        verify(adminOtpService).storeOtp("admin@lexho.com", 11L, "654321");
        verify(adminOtpDeliveryService).sendOtpByEmail("admin@lexho.com", "654321");
        verify(adminAuditService).log(11L, "ADMIN_LOGIN_OTP_SENT", "admins", 11L, null);
    }

    @Test
    void loginShouldTrackFailureAndThrowUnauthorizedBeforeLockThreshold() {
        Admin admin = activeAdmin("admin@lexho.com");
        admin.setPasswordHash("hash");
        admin.setFailedAttempts(0);
        when(adminRepository.findByEmailIgnoreCase("admin@lexho.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("wrong-pass", "hash")).thenReturn(false);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> adminAuthService.login(loginRequest("admin@lexho.com", "wrong-pass")));

        assertEquals("Invalid email or password", ex.getMessage());
        assertEquals(1, admin.getFailedAttempts());
        verify(adminRepository).save(admin);
    }

    @Test
    void loginShouldLockAdminAfterRepeatedFailures() {
        Admin admin = activeAdmin("admin@lexho.com");
        admin.setPasswordHash("hash");
        admin.setFailedAttempts(2);
        when(adminRepository.findByEmailIgnoreCase("admin@lexho.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("wrong-pass", "hash")).thenReturn(false);

        AccountLockedException ex = assertThrows(AccountLockedException.class,
                () -> adminAuthService.login(loginRequest("admin@lexho.com", "wrong-pass")));

        assertEquals("Admin account is locked due to multiple failed attempts", ex.getMessage());
        assertEquals(3, admin.getFailedAttempts());
        verify(adminRepository).save(admin);
    }

    @Test
    void loginShouldTreatMissingHashAsUnauthorized() {
        Admin admin = activeAdmin("admin@lexho.com");
        admin.setPasswordHash(null);
        when(adminRepository.findByEmailIgnoreCase("admin@lexho.com")).thenReturn(Optional.of(admin));

        assertThrows(UnauthorizedException.class,
                () -> adminAuthService.login(loginRequest("admin@lexho.com", "Password@123")));

        verify(adminRepository).save(admin);
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void loginShouldPropagateOtpFailureAndSkipAudit() {
        Admin admin = activeAdmin("admin@lexho.com");
        admin.setPasswordHash("hash");
        when(adminRepository.findByEmailIgnoreCase("admin@lexho.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("Password@123", "hash")).thenReturn(true);
        when(adminOtpService.generateOtp()).thenReturn("123456");
        doThrow(new IllegalStateException("mail down")).when(adminOtpDeliveryService)
                .sendOtpByEmail("admin@lexho.com", "123456");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> adminAuthService.login(loginRequest("admin@lexho.com", "Password@123")));

        assertEquals("mail down", ex.getMessage());
        verify(adminAuditService, never()).log(any(), any(), any(), any(), any());
    }

    @Test
    void verifyOtpShouldRejectUnknownAdmin() {
        when(adminRepository.findByEmailIgnoreCase("admin@lexho.com")).thenReturn(Optional.empty());

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> adminAuthService.verifyOtp(verifyOtpRequest(" Admin@Lexho.com ", "123456")));

        assertEquals("Admin not found", ex.getMessage());
    }

    @Test
    void verifyOtpShouldRejectInactiveOrLockedAdmin() {
        Admin inactive = activeAdmin("admin@lexho.com");
        inactive.setActive(false);
        when(adminRepository.findByEmailIgnoreCase("admin@lexho.com")).thenReturn(Optional.of(inactive));

        UnauthorizedException inactiveEx = assertThrows(UnauthorizedException.class,
                () -> adminAuthService.verifyOtp(verifyOtpRequest("admin@lexho.com", "123456")));
        assertEquals("Admin account is inactive", inactiveEx.getMessage());

        Admin locked = activeAdmin("admin@lexho.com");
        locked.setAccountLocked(true);
        locked.setLockUntil(LocalDateTime.now().plusMinutes(5));
        when(adminRepository.findByEmailIgnoreCase("admin@lexho.com")).thenReturn(Optional.of(locked));

        assertThrows(AccountLockedException.class,
                () -> adminAuthService.verifyOtp(verifyOtpRequest("admin@lexho.com", "123456")));
    }

    @Test
    void verifyOtpShouldResetAttemptsDefaultRoleAndReturnToken() {
        Admin admin = activeAdmin("admin@lexho.com");
        admin.setRole(null);
        admin.setFailedAttempts(2);
        admin.setAccountLocked(true);
        admin.setLockUntil(LocalDateTime.now().minusMinutes(1));
        when(adminRepository.findByEmailIgnoreCase("admin@lexho.com")).thenReturn(Optional.of(admin));
        when(jwtUtil.generateAdminToken(11L, "admin@lexho.com", Role.ADMIN)).thenReturn("jwt-token");

        AdminTokenResponse response = adminAuthService.verifyOtp(verifyOtpRequest(" admin@lexho.com ", "123456"));

        assertEquals("jwt-token", response.getToken());
        assertEquals(0, admin.getFailedAttempts());
        assertEquals(Role.ADMIN, admin.getRole());
        verify(adminRepository, org.mockito.Mockito.times(2)).save(admin);
        verify(adminOtpService).validateOtp("admin@lexho.com", 11L, "123456");
        verify(adminOtpService).deleteOtp("admin@lexho.com", 11L);
        verify(adminAuditService).log(11L, "ADMIN_LOGIN_SUCCESS", "admins", 11L, null);
    }

    private static AdminLoginRequest loginRequest(String email, String password) {
        AdminLoginRequest request = new AdminLoginRequest();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    private static AdminVerifyOtpRequest verifyOtpRequest(String email, String otp) {
        AdminVerifyOtpRequest request = new AdminVerifyOtpRequest();
        request.setEmail(email);
        request.setOtp(otp);
        return request;
    }

    private static Admin activeAdmin(String email) {
        Admin admin = new Admin();
        admin.setId(11L);
        admin.setEmail(email);
        admin.setActive(true);
        admin.setRole(Role.ADMIN);
        return admin;
    }
}

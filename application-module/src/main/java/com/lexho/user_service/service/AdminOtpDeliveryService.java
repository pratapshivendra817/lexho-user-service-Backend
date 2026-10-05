package com.lexho.user_service.service;

import com.lexho.user_service.config.AdminAuthProperties;
import com.lexho.user_service.exception.InfrastructureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class AdminOtpDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(AdminOtpDeliveryService.class);

    private final JavaMailSender javaMailSender;
    private final AdminAuthProperties adminAuthProperties;

    public AdminOtpDeliveryService(JavaMailSender javaMailSender,
                                   AdminAuthProperties adminAuthProperties) {
        this.javaMailSender = javaMailSender;
        this.adminAuthProperties = adminAuthProperties;
    }

    public void sendOtpByEmail(String email, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(adminAuthProperties.getOtp().getDeliveryFrom());
        message.setTo(email);
        message.setSubject("Lexho Admin OTP");
        message.setText("Your OTP is " + otp + ". It expires in 5 minutes.");

        try {
            javaMailSender.send(message);
        } catch (MailException ex) {
            if (!adminAuthProperties.getInfrastructure().isAllowConsoleOtpDeliveryFallback()) {
                throw new InfrastructureException("OTP delivery service is temporarily unavailable");
            }
            log.warn("Failed to send admin OTP email to {}. Falling back to console OTP delivery: {}",
                    email, ex.getMessage());
            log.warn("ADMIN OTP CONSOLE FALLBACK | email={} | otp={} | expiresInSeconds={}",
                    email, otp, adminAuthProperties.getOtp().getExpirySeconds());
        }
    }
}

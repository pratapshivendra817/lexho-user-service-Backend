package com.lexho.user_service.emergency.service;

import com.lexho.admin.repository.AdminRepository;
import com.lexho.user_service.emergency.entity.EmergencyEvent;
import com.lexho.user_service.emergency.entity.EmergencyLocation;
import com.lexho.user_service.notification.service.NotificationTriggerService;
import org.springframework.stereotype.Service;

@Service
public class EmergencyAdminNotifier {

    private final AdminRepository adminRepository;
    private final NotificationTriggerService notificationTriggerService;

    public EmergencyAdminNotifier(AdminRepository adminRepository,
                                  NotificationTriggerService notificationTriggerService) {
        this.adminRepository = adminRepository;
        this.notificationTriggerService = notificationTriggerService;
    }

    public int notifyAdmins(EmergencyEvent event, EmergencyLocation location) {
        String title = "Partner SOS Alert";
        String message = "SOS triggered by " + event.getPartnerName()
                + " at " + location.getLatitude() + "," + location.getLongitude()
                + ". Incident " + event.getIncidentCode() + ".";
        int count = 0;
        for (var admin : adminRepository.findAll()) {
            if (admin.isActive()) {
                notificationTriggerService.emergencyAlert(admin.getId(), title, message, event.getId());
                count++;
            }
        }
        return count;
    }
}

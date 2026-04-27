package com.lexho.user_service.controller;

import com.lexho.notification.entity.Notification;
import com.lexho.user_service.service.NotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<Notification> getAll(@RequestAttribute("userId") Long userId) {
        return notificationService.getAll(userId);
    }

    @PutMapping("/{id}/read")
    public String markAsRead(@RequestAttribute("userId") Long userId, @PathVariable Long id) {
        notificationService.markRead(userId, id);
        return "Marked as read";
    }
}


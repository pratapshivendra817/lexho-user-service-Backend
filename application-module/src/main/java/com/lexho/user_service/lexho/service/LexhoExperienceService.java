package com.lexho.user_service.lexho.service;

import com.lexho.user_service.lexho.dto.LexhoDtos;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class LexhoExperienceService {

    public LexhoDtos.HomeDashboardResponse homeDashboard() {
        return new LexhoDtos.HomeDashboardResponse(
                "Hi",
                "Bengaluru",
                List.of(java.util.Map.of(
                        "id", "1001",
                        "status", "CREATED",
                        "serviceName", "Deep Cleaning",
                        "createdAt", "2026-04-28T10:00:00Z"
                ))
        );
    }

    public Map<String, Object> pageStub(int page, int size, String label) {
        return Map.of(
                "content", List.of(Map.of("name", label)),
                "page", page,
                "size", size,
                "totalElements", 1
        );
    }
}

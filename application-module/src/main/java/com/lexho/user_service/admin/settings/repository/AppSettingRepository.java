package com.lexho.user_service.admin.settings.repository;

import com.lexho.user_service.admin.settings.entity.AppSettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppSettingRepository extends JpaRepository<AppSettingEntity, String> {
}


package com.qros.modules.settings.repository;

import com.qros.modules.settings.model.SystemSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemSettingsRepository extends JpaRepository<SystemSettings, Long> {}

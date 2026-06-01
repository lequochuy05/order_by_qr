package com.sacmauquan.qrordering.repository;

import com.sacmauquan.qrordering.model.SystemSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SystemSettingsRepository extends JpaRepository<SystemSettings, Integer> {
    Optional<SystemSettings> findTopByOrderByIdAsc();
}

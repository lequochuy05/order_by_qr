package com.qros.modules.settings.service;

import com.qros.modules.settings.dto.SystemSettingsDto;
import com.qros.modules.menu.dto.PublicMenuResponse;
import org.springframework.lang.NonNull;

public interface SystemSettingsService {
    SystemSettingsDto getCurrent(boolean includeSensitive);

    PublicMenuResponse.Settings getPublicSettings();

    SystemSettingsDto update(@NonNull SystemSettingsDto request);
}

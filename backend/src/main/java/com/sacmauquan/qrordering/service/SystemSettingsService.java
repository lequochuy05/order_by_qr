package com.sacmauquan.qrordering.service;

import com.sacmauquan.qrordering.dto.SystemSettingsDto;
import com.sacmauquan.qrordering.dto.CustomerPublicDto;
import org.springframework.lang.NonNull;

public interface SystemSettingsService {
    SystemSettingsDto getCurrent(boolean includeSensitive);

    CustomerPublicDto.Settings getPublicSettings();

    SystemSettingsDto update(@NonNull SystemSettingsDto request);
}

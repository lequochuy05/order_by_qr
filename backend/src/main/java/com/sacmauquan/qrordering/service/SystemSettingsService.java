package com.sacmauquan.qrordering.service;

import com.sacmauquan.qrordering.dto.SystemSettingsDto;
import org.springframework.lang.NonNull;

public interface SystemSettingsService {
    SystemSettingsDto getCurrent(boolean includeSensitive);

    SystemSettingsDto update(@NonNull SystemSettingsDto request);
}

package com.qros.core.config;

import com.qros.shared.util.AppTime;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

/**
 * Applies the restaurant business timezone to every Spring application context.
 */
@Configuration
public class TimeZoneConfig {

    @PostConstruct
    void configureTimeZone() {
        AppTime.configureSystemDefaultTimeZone();
    }
}

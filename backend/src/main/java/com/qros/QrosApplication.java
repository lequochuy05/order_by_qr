package com.qros;

import com.qros.shared.time.AppTime;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main - Main class for the application.
 */
@SpringBootApplication
@EnableCaching
@EnableJpaAuditing
@EnableAsync
@EnableScheduling
@ConfigurationPropertiesScan
public class QrosApplication {
    public static void main(String[] args) {
        AppTime.configureSystemDefaultTimeZone();
        SpringApplication.run(QrosApplication.class, args);
    }
}

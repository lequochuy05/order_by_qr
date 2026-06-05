package com.sacmauquan.qrordering;

import com.sacmauquan.qrordering.util.AppTime;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
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
public class Main {
    public static void main(String[] args) {
        AppTime.configureSystemDefaultTimeZone();
        SpringApplication.run(Main.class, args);
    }
}

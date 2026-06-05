package com.sacmauquan.qrordering.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.TimeZone;

/**
 * Central time source for business timestamps.
 */
public final class AppTime {

    public static final String ZONE_ID = "Asia/Ho_Chi_Minh";
    public static final ZoneId ZONE = ZoneId.of(ZONE_ID);

    private AppTime() {
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }

    public static void configureSystemDefaultTimeZone() {
        System.setProperty("user.timezone", ZONE_ID);
        TimeZone.setDefault(TimeZone.getTimeZone(ZONE));
    }
}

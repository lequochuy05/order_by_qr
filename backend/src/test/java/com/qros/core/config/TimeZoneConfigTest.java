package com.qros.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.qros.shared.time.AppTime;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;

class TimeZoneConfigTest {

    @Test
    void applicationTimeUsesHoChiMinhZone() {
        assertThat(AppTime.ZONE_ID).isEqualTo("Asia/Ho_Chi_Minh");
        assertThat(AppTime.ZONE).isEqualTo(ZoneId.of("Asia/Ho_Chi_Minh"));
    }

    @Test
    void configureSystemDefaultTimeZoneUsesApplicationZone() {
        TimeZone previous = TimeZone.getDefault();
        String previousProperty = System.getProperty("user.timezone");

        try {
            AppTime.configureSystemDefaultTimeZone();

            assertThat(TimeZone.getDefault().toZoneId()).isEqualTo(AppTime.ZONE);
            assertThat(System.getProperty("user.timezone")).isEqualTo(AppTime.ZONE_ID);
        } finally {
            TimeZone.setDefault(previous);
            if (previousProperty == null) {
                System.clearProperty("user.timezone");
            } else {
                System.setProperty("user.timezone", previousProperty);
            }
        }
    }

    @Test
    void applicationYamlConfiguresSerializationJdbcAndDatabaseSessionTimeZones() throws Exception {
        String yaml = Files.readString(Path.of("src/main/resources/application.yml"));

        assertThat(yaml).contains("time-zone: Asia/Ho_Chi_Minh");
        assertThat(yaml).contains("time_zone: Asia/Ho_Chi_Minh");
        assertThat(yaml).contains("connection-init-sql: \"SET TIME ZONE 'Asia/Ho_Chi_Minh'\"");
    }
}

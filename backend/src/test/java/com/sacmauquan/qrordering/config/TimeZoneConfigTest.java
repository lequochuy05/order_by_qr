package com.sacmauquan.qrordering.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

import com.sacmauquan.qrordering.util.AppTime;

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
    void applicationPropertiesConfigureSerializationJdbcAndDatabaseSessionTimeZones() throws Exception {
        String properties = Files.readString(Path.of("src/main/resources/application.properties"));

        assertThat(properties).contains("spring.jackson.time-zone=Asia/Ho_Chi_Minh");
        assertThat(properties).contains("spring.jpa.properties.hibernate.jdbc.time_zone=Asia/Ho_Chi_Minh");
        assertThat(properties).contains("spring.datasource.hikari.connection-init-sql=SET TIME ZONE 'Asia/Ho_Chi_Minh'");
    }
}

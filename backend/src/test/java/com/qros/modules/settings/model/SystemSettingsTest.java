package com.qros.modules.settings.model;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Version;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.junit.jupiter.api.Test;

class SystemSettingsTest {

    @Test
    void singletonUsesOptimisticLockingWithoutSoftDeleteAnnotations() throws Exception {
        assertThat(SystemSettings.class.getDeclaredField("version").isAnnotationPresent(Version.class))
                .isTrue();
        assertThat(SystemSettings.class.isAnnotationPresent(SQLDelete.class)).isFalse();
        assertThat(SystemSettings.class.isAnnotationPresent(SQLRestriction.class))
                .isFalse();
        assertThat(SystemSettings.class.getSuperclass()).isEqualTo(Object.class);
    }
}

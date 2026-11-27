package com.qros.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Entity;
import java.util.List;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.PostgreSQLDialect;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

class HibernateEntityMetadataTest {

    @Test
    void allJpaEntitiesBuildCleanHibernateMetadataIncludingNotFoundAndSqlDeleteMappings() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));

        List<Class<?>> entityClasses = new java.util.ArrayList<>();
        for (BeanDefinition def : scanner.findCandidateComponents("com.qros")) {
            entityClasses.add(Class.forName(def.getBeanClassName()));
        }

        assertThat(entityClasses).isNotEmpty();

        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .applySetting(AvailableSettings.DIALECT, PostgreSQLDialect.class.getName())
                .applySetting(AvailableSettings.ALLOW_METADATA_ON_BOOT, false)
                .build();
        try {
            MetadataSources sources = new MetadataSources(registry);
            entityClasses.forEach(sources::addAnnotatedClass);
            Metadata metadata = sources.buildMetadata();

            assertThat(metadata.getEntityBindings()).hasSize(entityClasses.size());
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }
}

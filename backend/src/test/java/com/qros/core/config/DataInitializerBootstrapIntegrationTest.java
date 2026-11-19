package com.qros.core.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.qros.modules.user.model.User;
import com.qros.modules.user.model.enums.UserRole;
import com.qros.modules.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = DataInitializerBootstrapIntegrationTest.TestApplication.class)
@ActiveProfiles("dev")
class DataInitializerBootstrapIntegrationTest {

    @Autowired
    private DataInitializer initializer;

    @Autowired
    private AppProperties appProperties;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @Test
    void doesNotBootstrapAnAdministratorWithoutExplicitConfiguration() {
        initializer.run();

        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    void rejectsEnabledBootstrapWhenCredentialsAreMissing() {
        appProperties.getSecurity().setEnableDefaultAdmin(true);

        assertThatThrownBy(initializer::run)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DEV_ADMIN_EMAIL and DEV_ADMIN_PASSWORD");
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    void createsTheConfiguredAdministratorWhenExplicitlyEnabled() {
        appProperties.getSecurity().setEnableDefaultAdmin(true);
        appProperties.getSecurity().setDefaultAdminEmail("admin@example.test");
        appProperties.getSecurity().setDefaultAdminPassword("explicit-test-password");
        when(userRepository.findByEmailIgnoreCase("admin@example.test")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("explicit-test-password")).thenReturn("encoded-test-password");

        initializer.run();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User createdUser = userCaptor.getValue();
        assertThat(createdUser.getEmail()).isEqualTo("admin@example.test");
        assertThat(createdUser.getPassword()).isEqualTo("encoded-test-password");
        assertThat(createdUser.getRole()).isEqualTo(UserRole.ADMIN);
        verify(passwordEncoder).encode("explicit-test-password");
    }

    @Configuration
    @EnableConfigurationProperties(AppProperties.class)
    @Import(DataInitializer.class)
    static class TestApplication {}
}

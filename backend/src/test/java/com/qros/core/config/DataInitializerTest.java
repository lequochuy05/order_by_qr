package com.qros.core.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.qros.modules.user.model.User;
import com.qros.modules.user.model.enums.UserRole;
import com.qros.modules.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

class DataInitializerTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final AppProperties appProperties = new AppProperties();
    private final DataInitializer initializer = new DataInitializer(userRepository, passwordEncoder, appProperties);

    @Test
    void doesNotCreateAccountsWhenBootstrapIsDisabled() {
        initializer.run();

        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    void failsIfBootstrapIsEnabledWithoutExplicitCredentials() {
        appProperties.getSecurity().setEnableDefaultAdmin(true);

        assertThatThrownBy(initializer::run)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DEV_ADMIN_EMAIL and DEV_ADMIN_PASSWORD");
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    void createsOnlyConfiguredAdministrator() {
        appProperties.getSecurity().setEnableDefaultAdmin(true);
        appProperties.getSecurity().setDefaultAdminEmail("local-admin@example.test");
        appProperties.getSecurity().setDefaultAdminPassword("unique-local-password");
        when(userRepository.findByEmailIgnoreCase("local-admin@example.test")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("unique-local-password")).thenReturn("encoded-password");

        initializer.run();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User user = userCaptor.getValue();
        assertThat(user.getEmail()).isEqualTo("local-admin@example.test");
        assertThat(user.getPassword()).isEqualTo("encoded-password");
        assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
        verify(passwordEncoder).encode("unique-local-password");
    }
}

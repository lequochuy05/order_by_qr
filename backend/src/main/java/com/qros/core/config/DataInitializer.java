package com.qros.core.config;

import com.qros.modules.user.model.User;
import com.qros.modules.user.model.enums.UserRole;
import com.qros.modules.user.model.enums.UserStatus;
import com.qros.modules.user.repository.UserRepository;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates an explicitly configured development administrator when requested.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepo;
    private final PasswordEncoder pwEncoder;
    private final AppProperties appProperties;

    /**
     * Executes when the application starts.
     * Creates no accounts unless development bootstrap is explicitly enabled.
     */
    @Override
    public void run(String... args) {
        if (!appProperties.getSecurity().isEnableDefaultAdmin()) {
            log.info("Development admin bootstrap is disabled");
            return;
        }

        String email = appProperties.getSecurity().getDefaultAdminEmail();
        String password = appProperties.getSecurity().getDefaultAdminPassword();
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "Development admin bootstrap requires DEV_ADMIN_EMAIL and DEV_ADMIN_PASSWORD");
        }

        seedUserIfNotExists(email, password, "Administrator", UserRole.ADMIN);
    }

    private void seedUserIfNotExists(String email, String rawPassword, String fullName, UserRole role) {
        if (userRepo.findByEmailIgnoreCase(email).isEmpty()) {
            User u = User.builder()
                    .email(email)
                    .fullName(fullName)
                    .password(pwEncoder.encode(rawPassword))
                    .role(role)
                    .status(UserStatus.ACTIVE)
                    .mustChangePassword(false)
                    .build();
            userRepo.save(Objects.requireNonNull(u));
            log.info("Seeded explicitly configured development account: {} ({})", email, role);
        }
    }
}

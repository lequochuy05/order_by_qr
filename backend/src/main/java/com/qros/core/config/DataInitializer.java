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
 * DataInitializer - Initialize essential data when the application starts.
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
     * Checks if the user table is empty and creates a default admin account if necessary.
     *
     * @param args Command line arguments
     * @throws Exception If an error occurs during initialization
     */
    @Override
    public void run(String... args) throws Exception {
        if (!appProperties.getSecurity().isEnableDefaultAdmin()) {
            log.info("Default admin bootstrap is disabled");
            return;
        }

        seedUserIfNotExists("admin@gmail.com", "admin123", "Administrator", UserRole.ADMIN);
        seedUserIfNotExists("manager@gmail.com", "manager123", "Restaurant Manager", UserRole.MANAGER);
        seedUserIfNotExists("chef@gmail.com", "chef123", "Head Chef", UserRole.CHEF);
        seedUserIfNotExists("staff@gmail.com", "staff123", "Service Staff", UserRole.STAFF);
        seedUserIfNotExists("cashier@gmail.com", "cashier123", "Cashier", UserRole.CASHIER);
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
            log.warn("Seeded dev test account: {} ({})", email, role);
        }
    }
}

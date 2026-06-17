package com.qros.core.config;

import com.qros.modules.user.model.User;
import com.qros.modules.user.model.enums.UserRole;
import com.qros.modules.user.model.enums.UserStatus;
import com.qros.modules.user.repository.UserRepository;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * DataInitializer - Initialize essential data when the application starts.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepo;
    private final PasswordEncoder pwEncoder;

    @Value("${app.security.enable-default-admin:false}")
    private boolean enableDefaultAdmin;

    /**
     * Executes when the application starts.
     * Checks if the user table is empty and creates a default admin account if necessary.
     *
     * @param args Command line arguments
     * @throws Exception If an error occurs during initialization
     */
    @Override
    public void run(String... args) throws Exception {
        if (!enableDefaultAdmin) {
            log.info("Default admin bootstrap is disabled");
            return;
        }

        if (userRepo.count() == 0) {
            User u = User.builder()
                    .email("admin@gmail.com")
                    .fullName("Admin")
                    .password(pwEncoder.encode("admin123"))
                    .role(UserRole.MANAGER)
                    .status(UserStatus.ACTIVE)
                    .build();
            userRepo.save(Objects.requireNonNull(u));

            log.warn("Created default admin account: admin@gmail.com / admin123");
        }
    }
}

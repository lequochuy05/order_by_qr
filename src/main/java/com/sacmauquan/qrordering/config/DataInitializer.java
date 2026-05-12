package com.sacmauquan.qrordering.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import com.sacmauquan.qrordering.model.User;
import com.sacmauquan.qrordering.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Objects;

/**
 * DataInitializer - Initialize essential data when the application starts.
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepo;
    private final PasswordEncoder pwEncoder;

    /**
     * Executes when the application starts.
     * Checks if the user table is empty and creates a default admin account if necessary.
     * 
     * @param args Command line arguments
     * @throws Exception If an error occurs during initialization
     */
    @Override
    public void run(String... args) throws Exception {
        if (userRepo.count() == 0) {
            User u = User.builder()
                    .email("admin@gmail.com")
                    .fullName("Admin")
                    .password(pwEncoder.encode("admin123"))
                    .role(User.Role.MANAGER)
                    .status(User.UserStatus.ACTIVE)
                    .build();
            userRepo.save(Objects.requireNonNull(u));

            System.out.println("Created default admin account: admin@gmail.com / admin123");
        }
    }
}

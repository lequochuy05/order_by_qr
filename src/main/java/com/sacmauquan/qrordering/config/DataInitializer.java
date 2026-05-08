package com.sacmauquan.qrordering.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import com.sacmauquan.qrordering.model.User;
import com.sacmauquan.qrordering.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final UserRepository userRepo;
    private final PasswordEncoder pwEncoder;

    @Override
    public void run(String... args) throws Exception {
        // check if table user is empty then create default admin account
        if (userRepo.count() == 0) {
            User u = User.builder()
                    .email("admin@gmail.com")
                    .fullName("Admin")
                    .password(pwEncoder.encode("admin123"))
                    .role(User.Role.MANAGER)
                    .status(User.UserStatus.ACTIVE)
                    .build();
            userRepo.save(u);

            System.out.println("Created default admin account: admin@gmail.com / admin123");
        }
    }
}

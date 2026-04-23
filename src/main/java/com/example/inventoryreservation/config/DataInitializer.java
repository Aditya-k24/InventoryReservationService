package com.example.inventoryreservation.config;

import com.example.inventoryreservation.domain.AppUser;
import com.example.inventoryreservation.repository.AppUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class DataInitializer implements CommandLineRunner {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.findByEmail("admin@example.com").isEmpty()) {
            userRepository.save(new AppUser("admin@example.com",
                passwordEncoder.encode("changeme"), "ADMIN"));
        }
        if (userRepository.findByEmail("ops@example.com").isEmpty()) {
            userRepository.save(new AppUser("ops@example.com",
                passwordEncoder.encode("changeme"), "OPERATOR"));
        }
    }
}

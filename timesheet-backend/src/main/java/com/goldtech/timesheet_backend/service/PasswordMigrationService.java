// PasswordMigrationService.java - Automatically hash plaintext passwords on startup
package com.goldtech.timesheet_backend.service;

import com.goldtech.timesheet_backend.entity.User;
import com.goldtech.timesheet_backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PasswordMigrationService implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(PasswordMigrationService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        logger.info("Starting password migration - checking for plaintext passwords...");

        List<User> allUsers = userRepository.findAll();
        int migratedCount = 0;

        for (User user : allUsers) {
            String currentPassword = user.getPassword();

            // Check if password is NOT already hashed (BCrypt hashes start with $2a$, $2b$, or $2y$)
            if (!isAlreadyHashed(currentPassword)) {
                logger.info("Migrating plaintext password for user: {}", user.getEmail());

                // Hash the plaintext password
                String hashedPassword = passwordEncoder.encode(currentPassword);
                user.setPassword(hashedPassword);
                userRepository.save(user);

                migratedCount++;
                logger.info("Password migrated for user: {} (hash: {})", user.getEmail(), hashedPassword);
            }
        }

        if (migratedCount > 0) {
            logger.info("Password migration completed. {} passwords were hashed.", migratedCount);
        } else {
            logger.info("No plaintext passwords found. All passwords are already hashed.");
        }
    }

    private boolean isAlreadyHashed(String password) {
        // BCrypt hashes start with $2a$, $2b$, or $2y$ followed by cost parameter
        return password != null &&
                (password.startsWith("$2a$") ||
                        password.startsWith("$2b$") ||
                        password.startsWith("$2y$")) &&
                password.length() == 60; // BCrypt hashes are always 60 characters
    }
}
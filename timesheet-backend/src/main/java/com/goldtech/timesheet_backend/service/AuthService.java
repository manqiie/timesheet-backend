//service/AuthService.java
package com.goldtech.timesheet_backend.service;

import com.goldtech.timesheet_backend.config.JwtUtils;
import com.goldtech.timesheet_backend.dto.auth.LoginRequest;
import com.goldtech.timesheet_backend.dto.auth.LoginResponse;
import com.goldtech.timesheet_backend.dto.auth.LogoutResponse;
import com.goldtech.timesheet_backend.dto.user.UserDto;
import com.goldtech.timesheet_backend.entity.User;
import com.goldtech.timesheet_backend.mapper.UserMapper;
import com.goldtech.timesheet_backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    public LoginResponse login(LoginRequest loginRequest) {
        try {
            logger.debug("Attempting login for: {}", loginRequest.getEmail());

            // Find user by email or employee ID
            Optional<User> userOptional = userRepository.findByEmailOrEmployeeId(loginRequest.getEmail());

            if (userOptional.isEmpty()) {
                logger.warn("User not found: {}", loginRequest.getEmail());
                return LoginResponse.failure("Invalid credentials");
            }

            User user = userOptional.get();

            // Check if user is active
            if (user.getStatus() != User.UserStatus.ACTIVE) {
                logger.warn("Inactive user attempted login: {}", loginRequest.getEmail());
                return LoginResponse.failure("Account is inactive");
            }

            // Verify password
            if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                logger.warn("Invalid password for user: {}", loginRequest.getEmail());
                return LoginResponse.failure("Invalid credentials");
            }

            // Update last login time
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            // Generate JWT token
            String token = jwtUtils.generateJwtToken(user.getEmail());

            // Convert to DTO
            UserDto userDto = userMapper.toDto(user);

            logger.info("User logged in successfully: {}", user.getEmail());
            return LoginResponse.success(token, userDto);

        } catch (Exception e) {
            logger.error("Error during login process", e);
            return LoginResponse.failure("Login failed. Please try again.");
        }
    }

    public LogoutResponse logout(String userEmail) {
        try {
            logger.debug("Logging out user: {}", userEmail);

            // In a stateless JWT system, logout is mainly handled on the frontend
            // by removing the token. However, we can log the event here.

            Optional<User> userOptional = userRepository.findByEmail(userEmail);
            if (userOptional.isPresent()) {
                logger.info("User logged out: {}", userEmail);
            }

            return LogoutResponse.success();

        } catch (Exception e) {
            logger.error("Error during logout process", e);
            return LogoutResponse.failure("Logout failed");
        }
    }

    public UserDto getCurrentUser(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isEmpty()) {
            return null;
        }

        User user = userOptional.get();

        // Check if user is still active
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            return null;
        }

        return userMapper.toDto(user);
    }

    public boolean validateToken(String token) {
        return jwtUtils.validateJwtToken(token);
    }

    public String getEmailFromToken(String token) {
        return jwtUtils.getEmailFromJwtToken(token);
    }
}
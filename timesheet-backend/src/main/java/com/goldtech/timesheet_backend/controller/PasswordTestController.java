// Add this temporary controller to test password verification
// timesheet-backend/src/main/java/com/goldtech/timesheet_backend/controller/PasswordTestController.java

package com.goldtech.timesheet_backend.controller;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.goldtech.timesheet_backend.repository.UserRepository;
import com.goldtech.timesheet_backend.entity.User;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/test")
public class PasswordTestController {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/password/{email}")
    public Map<String, Object> testPassword(@PathVariable String email, @RequestParam String password) {
        Map<String, Object> result = new HashMap<>();

        try {
            Optional<User> userOpt = userRepository.findByEmail(email);

            if (userOpt.isEmpty()) {
                result.put("error", "User not found");
                return result;
            }

            User user = userOpt.get();
            String storedHash = user.getPassword();

            // Detailed analysis
            result.put("email", email);
            result.put("inputPassword", password);
            result.put("inputPasswordLength", password.length());

            // Stored hash analysis
            result.put("storedHashFull", storedHash);
            result.put("storedHashLength", storedHash.length());
            result.put("storedHashBytes", storedHash.getBytes().length);

            // Check for whitespace/hidden characters
            result.put("storedHashTrimmed", storedHash.trim());
            result.put("storedHashTrimsToSame", storedHash.equals(storedHash.trim()));

            // BCrypt encoder info
            result.put("encoderClass", passwordEncoder.getClass().getSimpleName());
            result.put("encoderString", passwordEncoder.toString());

            // Test the password with original stored hash
            boolean matches = passwordEncoder.matches(password, storedHash);
            result.put("passwordMatches", matches);

            // Test with trimmed hash
            boolean matchesTrimmed = passwordEncoder.matches(password, storedHash.trim());
            result.put("passwordMatchesTrimmed", matchesTrimmed);

            // Generate a new hash and test immediately
            String newHash = passwordEncoder.encode(password);
            result.put("newHashFull", newHash);
            result.put("newHashLength", newHash.length());

            boolean newHashMatches = passwordEncoder.matches(password, newHash);
            result.put("newHashMatches", newHashMatches);

            // Test a known working hash
            String knownGoodHash = "$2a$12$TQU1FyLe1h.kujI5zJnrHeCGzOYpW6rW8rn3xhBxmDlBhHjx6oi7i";
            boolean knownHashMatches = passwordEncoder.matches(password, knownGoodHash);
            result.put("knownHashMatches", knownHashMatches);

            // Direct BCrypt test
            BCryptPasswordEncoder directEncoder = new BCryptPasswordEncoder(12);
            boolean directMatches = directEncoder.matches(password, storedHash);
            result.put("directBCryptMatches", directMatches);

        } catch (Exception e) {
            result.put("error", e.getMessage());
            result.put("exception", e.getClass().getSimpleName());
            result.put("stackTrace", java.util.Arrays.toString(e.getStackTrace()));
        }

        return result;
    }

    // Add BCrypt import at the top
    private org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder getBCryptEncoder() {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(12);
    }
}
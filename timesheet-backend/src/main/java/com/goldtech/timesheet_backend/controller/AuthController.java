//controller/AuthController
package com.goldtech.timesheet_backend.controller;

import com.goldtech.timesheet_backend.dto.auth.LoginRequest;
import com.goldtech.timesheet_backend.dto.auth.LoginResponse;
import com.goldtech.timesheet_backend.dto.auth.LogoutResponse;
import com.goldtech.timesheet_backend.dto.user.UserDto;
import com.goldtech.timesheet_backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"}, allowCredentials = "true")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        logger.debug("Login request received for: {}", loginRequest.getEmail());

        LoginResponse response = authService.login(loginRequest);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);

        if (token != null && authService.validateToken(token)) {
            String email = authService.getEmailFromToken(token);
            LogoutResponse response = authService.logout(email);
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.ok(LogoutResponse.success());
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);

        if (token == null || !authService.validateToken(token)) {
            return ResponseEntity.status(401).body("Invalid or expired token");
        }

        String email = authService.getEmailFromToken(token);
        UserDto user = authService.getCurrentUser(email);

        if (user == null) {
            return ResponseEntity.status(404).body("User not found");
        }

        return ResponseEntity.ok(user);
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);

        if (token == null) {
            return ResponseEntity.status(401).body("No token provided");
        }

        boolean isValid = authService.validateToken(token);

        if (isValid) {
            String email = authService.getEmailFromToken(token);
            UserDto user = authService.getCurrentUser(email);

            if (user != null) {
                return ResponseEntity.ok().body(new TokenValidationResponse(true, "Token is valid", user));
            } else {
                return ResponseEntity.status(404).body(new TokenValidationResponse(false, "User not found", null));
            }
        } else {
            return ResponseEntity.status(401).body(new TokenValidationResponse(false, "Invalid or expired token", null));
        }
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        return null;
    }

    // Inner class for token validation response
    public static class TokenValidationResponse {
        private boolean valid;
        private String message;
        private UserDto user;

        public TokenValidationResponse(boolean valid, String message, UserDto user) {
            this.valid = valid;
            this.message = message;
            this.user = user;
        }

        // Getters and setters
        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public UserDto getUser() {
            return user;
        }

        public void setUser(UserDto user) {
            this.user = user;
        }
    }
}
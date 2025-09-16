// UserController.java - Complete User Management with CRUD + Suspend operations
package com.goldtech.timesheet_backend.controller;

import com.goldtech.timesheet_backend.dto.user.CreateUserRequest;
import com.goldtech.timesheet_backend.dto.user.UpdateUserRequest;
import com.goldtech.timesheet_backend.dto.user.UserDto;
import com.goldtech.timesheet_backend.dto.user.UserResponse;
import com.goldtech.timesheet_backend.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"}, allowCredentials = "true")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    // Get all users with pagination and filtering
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<UserResponse> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "fullName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String position,
            @RequestParam(required = false) String projectSite,
            @RequestParam(required = false) String company
    ) {
        try {
            logger.debug("Getting users with filters - page: {}, size: {}, search: {}", page, size, search);

            Sort sort = Sort.by(sortDir.equalsIgnoreCase("desc") ?
                    Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<UserDto> users = userService.getAllUsers(
                    pageable, search, status, role, department, position, projectSite, company
            );

            UserResponse response = UserResponse.success(users.getContent(), users.getTotalElements());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting users", e);
            return ResponseEntity.status(500)
                    .body(UserResponse.error("Failed to retrieve users: " + e.getMessage()));
        }
    }

    // Get user by ID
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        try {
            UserDto user = userService.getUserById(id);
            if (user != null) {
                return ResponseEntity.ok(UserResponse.success(user));
            } else {
                return ResponseEntity.status(404)
                        .body(UserResponse.error("User not found"));
            }
        } catch (Exception e) {
            logger.error("Error getting user by ID: {}", id, e);
            return ResponseEntity.status(500)
                    .body(UserResponse.error("Failed to retrieve user: " + e.getMessage()));
        }
    }

    // Create new user
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        try {
            logger.debug("Creating new user: {}", request.getEmail());

            UserDto createdUser = userService.createUser(request);
            return ResponseEntity.ok(UserResponse.success(createdUser, "User created successfully"));

        } catch (IllegalArgumentException e) {
            logger.warn("Validation error creating user: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(UserResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error creating user", e);
            return ResponseEntity.status(500)
                    .body(UserResponse.error("Failed to create user: " + e.getMessage()));
        }
    }

    // Update user
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        try {
            logger.debug("Updating user: {}", id);

            UserDto updatedUser = userService.updateUser(id, request);
            if (updatedUser != null) {
                return ResponseEntity.ok(UserResponse.success(updatedUser, "User updated successfully"));
            } else {
                return ResponseEntity.status(404)
                        .body(UserResponse.error("User not found"));
            }

        } catch (IllegalArgumentException e) {
            logger.warn("Validation error updating user: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(UserResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating user: {}", id, e);
            return ResponseEntity.status(500)
                    .body(UserResponse.error("Failed to update user: " + e.getMessage()));
        }
    }

    // Toggle user status (activate/deactivate)
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> toggleUserStatus(@PathVariable Long id) {
        try {
            logger.debug("Toggling status for user: {}", id);

            UserDto updatedUser = userService.toggleUserStatus(id);
            if (updatedUser != null) {
                String message = updatedUser.getStatus().name().equals("ACTIVE") ?
                        "User activated successfully" : "User deactivated successfully";
                return ResponseEntity.ok(UserResponse.success(updatedUser, message));
            } else {
                return ResponseEntity.status(404)
                        .body(UserResponse.error("User not found"));
            }

        } catch (Exception e) {
            logger.error("Error toggling user status: {}", id, e);
            return ResponseEntity.status(500)
                    .body(UserResponse.error("Failed to update user status: " + e.getMessage()));
        }
    }

    // Delete user (hard delete - use with caution)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> deleteUser(@PathVariable Long id) {
        try {
            logger.debug("Deleting user: {}", id);

            boolean deleted = userService.deleteUser(id);
            if (deleted) {
                return ResponseEntity.ok(UserResponse.success("User deleted successfully"));
            } else {
                return ResponseEntity.status(404)
                        .body(UserResponse.error("User not found"));
            }

        } catch (Exception e) {
            logger.error("Error deleting user: {}", id, e);
            return ResponseEntity.status(500)
                    .body(UserResponse.error("Failed to delete user: " + e.getMessage()));
        }
    }

    // Get managers for dropdown
    @GetMapping("/managers")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<UserResponse> getManagers() {
        try {
            List<UserDto> managers = userService.getManagers();
            return ResponseEntity.ok(UserResponse.success(managers));
        } catch (Exception e) {
            logger.error("Error getting managers", e);
            return ResponseEntity.status(500)
                    .body(UserResponse.error("Failed to retrieve managers: " + e.getMessage()));
        }
    }

    // Get roles for dropdown
    @GetMapping("/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getRoles() {
        try {
            List<Map<String, Object>> roles = userService.getRoles();
            return ResponseEntity.ok(UserResponse.success(roles));
        } catch (Exception e) {
            logger.error("Error getting roles", e);
            return ResponseEntity.status(500)
                    .body(UserResponse.error("Failed to retrieve roles: " + e.getMessage()));
        }
    }

    // Get user statistics
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getUserStats() {
        try {
            Map<String, Object> stats = userService.getUserStats();
            return ResponseEntity.ok(UserResponse.success(stats));
        } catch (Exception e) {
            logger.error("Error getting user stats", e);
            return ResponseEntity.status(500)
                    .body(UserResponse.error("Failed to retrieve user statistics: " + e.getMessage()));
        }
    }

    // Bulk update users
    @PatchMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> bulkUpdateUsers(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> userIds = (List<Long>) request.get("userIds");
            @SuppressWarnings("unchecked")
            Map<String, Object> updates = (Map<String, Object>) request.get("updates");

            List<UserDto> updatedUsers = userService.bulkUpdateUsers(userIds, updates);
            return ResponseEntity.ok(UserResponse.success(updatedUsers, "Users updated successfully"));

        } catch (Exception e) {
            logger.error("Error in bulk update", e);
            return ResponseEntity.status(500)
                    .body(UserResponse.error("Failed to update users: " + e.getMessage()));
        }
    }

    // Reset password
    @PatchMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> resetPassword(
            @PathVariable Long id,
            @RequestBody Map<String, String> request
    ) {
        try {
            String newPassword = request.get("newPassword");
            if (newPassword == null || newPassword.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(UserResponse.error("New password is required"));
            }

            boolean success = userService.resetPassword(id, newPassword);
            if (success) {
                return ResponseEntity.ok(UserResponse.success("Password reset successfully"));
            } else {
                return ResponseEntity.status(404)
                        .body(UserResponse.error("User not found"));
            }

        } catch (Exception e) {
            logger.error("Error resetting password for user: {}", id, e);
            return ResponseEntity.status(500)
                    .body(UserResponse.error("Failed to reset password: " + e.getMessage()));
        }
    }
}
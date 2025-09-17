// UserService.java - Updated with supervisor changes and hierarchical filtering
package com.goldtech.timesheet_backend.service;

import com.goldtech.timesheet_backend.dto.user.CreateUserRequest;
import com.goldtech.timesheet_backend.dto.user.UpdateUserRequest;
import com.goldtech.timesheet_backend.dto.user.UserDto;
import com.goldtech.timesheet_backend.entity.Role;
import com.goldtech.timesheet_backend.entity.User;
import com.goldtech.timesheet_backend.mapper.UserMapper;
import com.goldtech.timesheet_backend.repository.RoleRepository;
import com.goldtech.timesheet_backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Get all users with advanced filtering
    public Page<UserDto> getAllUsers(
            Pageable pageable,
            String search,
            String status,
            String role,
            String department,
            String position,
            String projectSite
    ) {
        Specification<User> spec = createUserSpecification(
                search, status, role, department, position, projectSite
        );

        Page<User> users = userRepository.findAll(spec, pageable);
        return users.map(userMapper::toDto);
    }

    // Create dynamic specification for filtering
    private Specification<User> createUserSpecification(
            String search, String status, String role, String department,
            String position, String projectSite
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Text search across multiple fields
            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.toLowerCase() + "%";
                Predicate searchPredicate = criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("fullName")), searchPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), searchPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("employeeId")), searchPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("position")), searchPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("department")), searchPattern)
                );
                predicates.add(searchPredicate);
            }

            // Status filter
            if (status != null && !status.equals("all")) {
                predicates.add(criteriaBuilder.equal(root.get("status"), User.UserStatus.valueOf(status)));
            }

            // Role filter
            if (role != null && !role.equals("all")) {
                Join<User, Role> rolesJoin = root.join("roles");
                predicates.add(criteriaBuilder.equal(rolesJoin.get("name"), role));
            }

            // Department filter
            if (department != null && !department.equals("all")) {
                predicates.add(criteriaBuilder.equal(root.get("department"), department));
            }

            // Position filter
            if (position != null && !position.equals("all")) {
                predicates.add(criteriaBuilder.equal(root.get("position"), position));
            }

            // Project site filter
            if (projectSite != null && !projectSite.equals("all")) {
                predicates.add(criteriaBuilder.equal(root.get("projectSite"), projectSite));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    // Get user by ID
    public UserDto getUserById(Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        return userOpt.map(userMapper::toDto).orElse(null);
    }

    // Create new user
    public UserDto createUser(CreateUserRequest request) {
        logger.debug("Creating user: {}", request.getEmail());

        // Validation
        validateCreateUserRequest(request);

        // Create user entity
        User user = new User();
        user.setEmployeeId(request.getEmployeeId());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setPosition(request.getPosition());
        user.setDepartment(request.getDepartment());
        user.setProjectSite(request.getProjectSite());
        user.setJoinDate(request.getJoinDate());
        user.setStatus(User.UserStatus.ACTIVE);

        // Set supervisor (changed from manager)
        if (request.getSupervisorId() != null) {
            Optional<User> supervisorOpt = userRepository.findById(request.getSupervisorId());
            supervisorOpt.ifPresent(user::setSupervisor);
        }

        // Set roles
        Set<Role> roles = new HashSet<>();
        for (Long roleId : request.getRoles()) {
            Optional<Role> roleOpt = roleRepository.findById(roleId);
            roleOpt.ifPresent(roles::add);
        }
        user.setRoles(roles);

        User savedUser = userRepository.save(user);
        logger.info("User created successfully: {}", savedUser.getEmail());

        return userMapper.toDto(savedUser);
    }

    // Update user
    public UserDto updateUser(Long id, UpdateUserRequest request) {
        logger.debug("Updating user: {}", id);

        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return null;
        }

        User user = userOpt.get();

        // Validate update
        validateUpdateUserRequest(request, id);

        // Update fields
        if (request.getEmployeeId() != null) {
            user.setEmployeeId(request.getEmployeeId().isEmpty() ? null : request.getEmployeeId());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().isEmpty() ? null : request.getPhone());
        }
        if (request.getPosition() != null) {
            user.setPosition(request.getPosition());
        }
        if (request.getDepartment() != null) {
            user.setDepartment(request.getDepartment());
        }
        if (request.getProjectSite() != null) {
            user.setProjectSite(request.getProjectSite().isEmpty() ? null : request.getProjectSite());
        }
        if (request.getJoinDate() != null) {
            user.setJoinDate(request.getJoinDate());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }

        // Update supervisor (changed from manager)
        if (request.getSupervisorId() != null) {
            if (request.getSupervisorId() == 0) {
                user.setSupervisor(null);
            } else {
                Optional<User> supervisorOpt = userRepository.findById(request.getSupervisorId());
                supervisorOpt.ifPresent(user::setSupervisor);
            }
        }

        // Update roles
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            Set<Role> roles = new HashSet<>();
            for (Long roleId : request.getRoles()) {
                Optional<Role> roleOpt = roleRepository.findById(roleId);
                roleOpt.ifPresent(roles::add);
            }
            user.setRoles(roles);
        }

        User savedUser = userRepository.save(user);
        logger.info("User updated successfully: {}", savedUser.getEmail());

        return userMapper.toDto(savedUser);
    }

    // Toggle user status
    public UserDto toggleUserStatus(Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return null;
        }

        User user = userOpt.get();
        User.UserStatus newStatus = user.getStatus() == User.UserStatus.ACTIVE ?
                User.UserStatus.INACTIVE : User.UserStatus.ACTIVE;

        user.setStatus(newStatus);
        User savedUser = userRepository.save(user);

        logger.info("User status toggled: {} - {}", savedUser.getEmail(), newStatus);
        return userMapper.toDto(savedUser);
    }

    // Delete user
    public boolean deleteUser(Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();

        // Check if user has subordinates
        List<User> subordinates = userRepository.findBySupervisorIdAndStatus(id, User.UserStatus.ACTIVE);
        if (!subordinates.isEmpty()) {
            throw new IllegalStateException("Cannot delete user with active subordinates. Please reassign subordinates first.");
        }

        userRepository.delete(user);
        logger.info("User deleted: {}", user.getEmail());
        return true;
    }

    // Get supervisors (changed from getManagers)
    public List<UserDto> getSupervisors() {
        List<User> supervisors = userRepository.findSupervisors(User.UserStatus.ACTIVE);
        return userMapper.toDtoList(supervisors);
    }

    // Get supervisors by project site
    public List<UserDto> getSupervisorsByProjectSite(String projectSite) {
        List<User> supervisors = userRepository.findSupervisorsByProjectSite(projectSite, User.UserStatus.ACTIVE);
        return userMapper.toDtoList(supervisors);
    }

    // Get roles
    public List<Map<String, Object>> getRoles() {
        List<Role> roles = roleRepository.findAll();
        List<Map<String, Object>> roleList = new ArrayList<>();

        for (Role role : roles) {
            Map<String, Object> roleMap = new HashMap<>();
            roleMap.put("id", role.getId());
            roleMap.put("name", role.getName());
            roleMap.put("description", role.getDescription());
            roleList.add(roleMap);
        }

        return roleList;
    }

    // Get user statistics
    public Map<String, Object> getUserStats() {
        Map<String, Object> stats = new HashMap<>();

        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByStatus(User.UserStatus.ACTIVE);
        long inactiveUsers = userRepository.countByStatus(User.UserStatus.INACTIVE);

        stats.put("total", totalUsers);
        stats.put("active", activeUsers);
        stats.put("inactive", inactiveUsers);

        // Role statistics
        Map<String, Long> roleStats = new HashMap<>();
        List<Role> roles = roleRepository.findAll();
        for (Role role : roles) {
            List<User> usersWithRole = userRepository.findByRoleName(role.getName());
            roleStats.put(role.getName(), (long) usersWithRole.size());
        }
        stats.put("roleStats", roleStats);

        return stats;
    }

    // Bulk update users
    public List<UserDto> bulkUpdateUsers(List<Long> userIds, Map<String, Object> updates) {
        List<User> users = userRepository.findAllById(userIds);

        for (User user : users) {
            // Apply updates
            if (updates.containsKey("status")) {
                String status = (String) updates.get("status");
                user.setStatus(User.UserStatus.valueOf(status));
            }
            if (updates.containsKey("department")) {
                user.setDepartment((String) updates.get("department"));
            }
            if (updates.containsKey("projectSite")) {
                user.setProjectSite((String) updates.get("projectSite"));
            }
            if (updates.containsKey("supervisorId")) {
                Long supervisorId = Long.valueOf(updates.get("supervisorId").toString());
                if (supervisorId == 0) {
                    user.setSupervisor(null);
                } else {
                    Optional<User> supervisorOpt = userRepository.findById(supervisorId);
                    supervisorOpt.ifPresent(user::setSupervisor);
                }
            }
        }

        List<User> savedUsers = userRepository.saveAll(users);
        return userMapper.toDtoList(savedUsers);
    }

    // Reset password
    public boolean resetPassword(Long id, String newPassword) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        logger.info("Password reset for user: {}", user.getEmail());
        return true;
    }

    // ========== HIERARCHICAL FILTER METHODS ==========

    // Get all project sites
    public List<String> getProjectSites() {
        return userRepository.findAllProjectSites();
    }

    // Get all departments
    public List<String> getAllDepartments() {
        return userRepository.findAllDepartments();
    }

    // Get departments by project site
    public List<String> getDepartmentsByProjectSite(String projectSite) {
        return userRepository.findDepartmentsByProjectSite(projectSite);
    }

    // Get positions by project site and department
    public List<String> getPositionsByFilters(String projectSite, String department) {
        return userRepository.findPositionsByProjectSiteAndDepartment(projectSite, department);
    }

    // Get roles by project site
    public List<String> getRolesByProjectSite(String projectSite) {
        return userRepository.findRolesByProjectSite(projectSite);
    }

    // Get roles by project site and department
    public List<String> getRolesByFilters(String projectSite, String department) {
        return userRepository.findRolesByProjectSiteAndDepartment(projectSite, department);
    }

    // Validation methods
    private void validateCreateUserRequest(CreateUserRequest request) {
        // Check email uniqueness
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }

        // Check employee ID uniqueness
        if (request.getEmployeeId() != null && !request.getEmployeeId().trim().isEmpty() &&
                userRepository.existsByEmployeeId(request.getEmployeeId())) {
            throw new IllegalArgumentException("Employee ID already exists: " + request.getEmployeeId());
        }

        // Validate supervisor exists (changed from manager)
        if (request.getSupervisorId() != null && !userRepository.existsById(request.getSupervisorId())) {
            throw new IllegalArgumentException("Supervisor not found with ID: " + request.getSupervisorId());
        }

        // Validate roles exist
        for (Long roleId : request.getRoles()) {
            if (!roleRepository.existsById(roleId)) {
                throw new IllegalArgumentException("Role not found with ID: " + roleId);
            }
        }
    }

    private void validateUpdateUserRequest(UpdateUserRequest request, Long userId) {
        // Check email uniqueness (excluding current user)
        if (request.getEmail() != null) {
            Optional<User> existingUser = userRepository.findByEmail(request.getEmail());
            if (existingUser.isPresent() && !existingUser.get().getId().equals(userId)) {
                throw new IllegalArgumentException("Email already exists: " + request.getEmail());
            }
        }

        // Check employee ID uniqueness (excluding current user)
        if (request.getEmployeeId() != null && !request.getEmployeeId().trim().isEmpty()) {
            Optional<User> existingUser = userRepository.findByEmployeeId(request.getEmployeeId());
            if (existingUser.isPresent() && !existingUser.get().getId().equals(userId)) {
                throw new IllegalArgumentException("Employee ID already exists: " + request.getEmployeeId());
            }
        }

        // Validate supervisor exists (changed from manager)
        if (request.getSupervisorId() != null && request.getSupervisorId() != 0 &&
                !userRepository.existsById(request.getSupervisorId())) {
            throw new IllegalArgumentException("Supervisor not found with ID: " + request.getSupervisorId());
        }

        // Validate roles exist
        if (request.getRoles() != null) {
            for (Long roleId : request.getRoles()) {
                if (!roleRepository.existsById(roleId)) {
                    throw new IllegalArgumentException("Role not found with ID: " + roleId);
                }
            }
        }
    }
}
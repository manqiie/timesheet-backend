// UserMapper.java - CLEAN VERSION - Only supervisor, no manager
package com.goldtech.timesheet_backend.mapper;

import com.goldtech.timesheet_backend.dto.user.RoleDto;
import com.goldtech.timesheet_backend.dto.user.UserDto;
import com.goldtech.timesheet_backend.entity.Role;
import com.goldtech.timesheet_backend.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserDto toDto(User user) {
        if (user == null) {
            return null;
        }

        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setEmployeeId(user.getEmployeeId());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setPhone(user.getPhone());
        dto.setPosition(user.getPosition());
        dto.setDepartment(user.getDepartment());
        dto.setProjectSite(user.getProjectSite());
        dto.setJoinDate(user.getJoinDate());
        dto.setStatus(user.getStatus());
        dto.setLastLoginAt(user.getLastLoginAt());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());

        // CLEAN: Only set supervisor fields - no manager fields
        if (user.getSupervisor() != null) {
            dto.setSupervisorId(user.getSupervisor().getId());
            dto.setSupervisorName(user.getSupervisor().getFullName());
        }

        // Map roles
        if (user.getRoles() != null) {
            List<RoleDto> roleDtos = user.getRoles().stream()
                    .map(this::roleToDto)
                    .collect(Collectors.toList());
            dto.setRoles(roleDtos);

            // Set primary role and permissions for frontend compatibility
            setPrimaryRoleAndPermissions(dto, user);
        }

        return dto;
    }

    public RoleDto roleToDto(Role role) {
        if (role == null) {
            return null;
        }

        RoleDto dto = new RoleDto();
        dto.setId(role.getId());
        dto.setName(role.getName());
        dto.setDescription(role.getDescription());
        dto.setCreatedAt(role.getCreatedAt());
        dto.setUpdatedAt(role.getUpdatedAt());

        return dto;
    }

    public User toEntity(UserDto dto) {
        if (dto == null) {
            return null;
        }

        User user = new User();
        user.setId(dto.getId());
        user.setEmployeeId(dto.getEmployeeId());
        user.setEmail(dto.getEmail());
        user.setFullName(dto.getFullName());
        user.setPhone(dto.getPhone());
        user.setPosition(dto.getPosition());
        user.setDepartment(dto.getDepartment());
        user.setProjectSite(dto.getProjectSite());
        user.setJoinDate(dto.getJoinDate());
        user.setStatus(dto.getStatus());
        user.setLastLoginAt(dto.getLastLoginAt());

        return user;
    }

    public List<UserDto> toDtoList(List<User> users) {
        return users.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Set primary role and permissions for frontend compatibility
     */
    private void setPrimaryRoleAndPermissions(UserDto dto, User user) {
        // Determine primary role (admin > supervisor > employee)
        String primaryRole = "employee"; // default

        for (Role role : user.getRoles()) {
            switch (role.getName().toLowerCase()) {
                case "admin":
                    primaryRole = "admin";
                    break;
                case "supervisor":
                    if (!"admin".equals(primaryRole)) {
                        primaryRole = "supervisor";
                    }
                    break;
            }
        }

        dto.setRole(primaryRole);

        // Set permissions based on roles
        List<String> permissions = getPermissions(user);
        dto.setPermissions(permissions);
    }

    /**
     * Get permissions based on user roles
     */
    private List<String> getPermissions(User user) {
        List<String> permissions = new java.util.ArrayList<>();

        for (Role role : user.getRoles()) {
            switch (role.getName().toLowerCase()) {
                case "admin":
                    permissions.addAll(List.of(
                            "timesheet.create", "timesheet.view", "timesheet.edit",
                            "timesheet.approve", "timesheet.manage",
                            "employee.create", "employee.view", "employee.edit", "employee.manage",
                            "system.admin"
                    ));
                    break;

                case "supervisor":
                    permissions.addAll(List.of(
                            "timesheet.create", "timesheet.view", "timesheet.edit",
                            "timesheet.approve", "employee.view"
                    ));
                    break;

                case "employee":
                    permissions.addAll(List.of(
                            "timesheet.create", "timesheet.view", "timesheet.edit"
                    ));
                    break;
            }
        }

        return permissions.stream().distinct().collect(Collectors.toList());
    }
}
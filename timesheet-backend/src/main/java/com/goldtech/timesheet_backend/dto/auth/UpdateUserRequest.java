// UpdateUserRequest.java - Updated with supervisor changes
package com.goldtech.timesheet_backend.dto.user;

import com.goldtech.timesheet_backend.entity.User;
import jakarta.validation.constraints.Email;

import java.time.LocalDate;
import java.util.List;

public class UpdateUserRequest {

    private String employeeId;

    @Email(message = "Invalid email format")
    private String email;

    private String fullName;
    private String phone;
    private String position;
    private String department;
    private String projectSite;
    private LocalDate joinDate;
    private Long supervisorId; // Changed from managerId
    private List<Long> roles;
    private User.UserStatus status;

    // Constructors
    public UpdateUserRequest() {}

    // Getters and Setters
    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getProjectSite() {
        return projectSite;
    }

    public void setProjectSite(String projectSite) {
        this.projectSite = projectSite;
    }

    public LocalDate getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(LocalDate joinDate) {
        this.joinDate = joinDate;
    }

    public Long getSupervisorId() { // Changed from getManagerId
        return supervisorId;
    }

    public void setSupervisorId(Long supervisorId) { // Changed from setManagerId
        this.supervisorId = supervisorId;
    }

    public List<Long> getRoles() {
        return roles;
    }

    public void setRoles(List<Long> roles) {
        this.roles = roles;
    }

    public User.UserStatus getStatus() {
        return status;
    }

    public void setStatus(User.UserStatus status) {
        this.status = status;
    }
}
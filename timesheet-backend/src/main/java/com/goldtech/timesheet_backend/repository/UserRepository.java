// UserRepository.java
package com.goldtech.timesheet_backend.repository;

import com.goldtech.timesheet_backend.entity.User;
import com.goldtech.timesheet_backend.entity.User.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Find by email
    Optional<User> findByEmail(String email);

    // Find by employee ID
    Optional<User> findByEmployeeId(String employeeId);

    // Find by email or employee ID (for login)
    @Query("SELECT u FROM User u WHERE u.email = :identifier OR u.employeeId = :identifier")
    Optional<User> findByEmailOrEmployeeId(@Param("identifier") String identifier);

    // Check if email exists
    boolean existsByEmail(String email);

    // Check if employee ID exists
    boolean existsByEmployeeId(String employeeId);

    // Find by status
    List<User> findByStatus(UserStatus status);

    // Find active users
    List<User> findByStatusOrderByFullNameAsc(UserStatus status);

    // Find users by role
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    List<User> findByRoleName(@Param("roleName") String roleName);

    // Find managers (users with manager or admin role)
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name IN ('manager', 'admin') AND u.status = :status")
    List<User> findManagers(@Param("status") UserStatus status);

    // Find subordinates by manager ID
    List<User> findByManagerIdAndStatus(Long managerId, UserStatus status);

    // Find by department
    List<User> findByDepartmentAndStatus(String department, UserStatus status);

    // Find by project site
    List<User> findByProjectSiteAndStatus(String projectSite, UserStatus status);

    // Search users by name or email
    @Query("SELECT u FROM User u WHERE " +
            "(LOWER(u.fullName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(u.employeeId) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
            "AND u.status = :status")
    List<User> searchUsers(@Param("searchTerm") String searchTerm, @Param("status") UserStatus status);
}
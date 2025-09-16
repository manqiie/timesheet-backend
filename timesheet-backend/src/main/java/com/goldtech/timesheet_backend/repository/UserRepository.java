// Updated UserRepository.java with additional query methods
package com.goldtech.timesheet_backend.repository;

import com.goldtech.timesheet_backend.entity.User;
import com.goldtech.timesheet_backend.entity.User.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

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

    // Count by status
    long countByStatus(UserStatus status);

    // Find by status
    List<User> findByStatus(UserStatus status);

    // Find active users
    List<User> findByStatusOrderByFullNameAsc(UserStatus status);

    // Find users by role
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    List<User> findByRoleName(@Param("roleName") String roleName);

    // Find managers (users with manager or admin role)
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name IN ('manager', 'admin') AND u.status = :status ORDER BY u.fullName ASC")
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

    // Get all departments
    @Query("SELECT DISTINCT u.department FROM User u WHERE u.department IS NOT NULL ORDER BY u.department")
    List<String> findAllDepartments();

    // Get all positions
    @Query("SELECT DISTINCT u.position FROM User u WHERE u.position IS NOT NULL ORDER BY u.position")
    List<String> findAllPositions();

    // Get all project sites
    @Query("SELECT DISTINCT u.projectSite FROM User u WHERE u.projectSite IS NOT NULL ORDER BY u.projectSite")
    List<String> findAllProjectSites();

    // Get all companies
    @Query("SELECT DISTINCT u.company FROM User u WHERE u.company IS NOT NULL ORDER BY u.company")
    List<String> findAllCompanies();

    // Find users by multiple IDs
    @Query("SELECT u FROM User u WHERE u.id IN :ids")
    List<User> findByIdIn(@Param("ids") List<Long> ids);

    // Check if user has subordinates
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.manager.id = :managerId AND u.status = 'ACTIVE'")
    boolean hasActiveSubordinates(@Param("managerId") Long managerId);

    // Find users by role and status
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName AND u.status = :status ORDER BY u.fullName ASC")
    List<User> findByRoleNameAndStatus(@Param("roleName") String roleName, @Param("status") UserStatus status);

    // Advanced search with multiple criteria
    @Query("SELECT u FROM User u LEFT JOIN u.roles r WHERE " +
            "(:search IS NULL OR " +
            "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.employeeId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.position) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.department) LIKE LOWER(CONCAT('%', :search, '%'))" +
            ") AND " +
            "(:status IS NULL OR u.status = :status) AND " +
            "(:role IS NULL OR r.name = :role) AND " +
            "(:department IS NULL OR u.department = :department) AND " +
            "(:position IS NULL OR u.position = :position) AND " +
            "(:projectSite IS NULL OR u.projectSite = :projectSite) AND " +
            "(:company IS NULL OR u.company = :company)")
    List<User> findWithFilters(
            @Param("search") String search,
            @Param("status") UserStatus status,
            @Param("role") String role,
            @Param("department") String department,
            @Param("position") String position,
            @Param("projectSite") String projectSite,
            @Param("company") String company
    );
}
// Updated UserRepository.java with supervisor changes
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

    // Find supervisors (users with supervisor or admin role)
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name IN ('supervisor', 'admin') AND u.status = :status ORDER BY u.fullName ASC")
    List<User> findSupervisors(@Param("status") UserStatus status);

    // Find subordinates by supervisor ID
    List<User> findBySupervisorIdAndStatus(Long supervisorId, UserStatus status);

    // Find by department
    List<User> findByDepartmentAndStatus(String department, UserStatus status);

    // Find by project site
    List<User> findByProjectSiteAndStatus(String projectSite, UserStatus status);

    // Find supervisors by project site
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name IN ('supervisor', 'admin') AND u.projectSite = :projectSite AND u.status = :status ORDER BY u.fullName ASC")
    List<User> findSupervisorsByProjectSite(@Param("projectSite") String projectSite, @Param("status") UserStatus status);

    // Find departments by project site
    @Query("SELECT DISTINCT u.department FROM User u WHERE u.projectSite = :projectSite AND u.department IS NOT NULL ORDER BY u.department")
    List<String> findDepartmentsByProjectSite(@Param("projectSite") String projectSite);

    // Find positions by project site and department
    @Query("SELECT DISTINCT u.position FROM User u WHERE " +
            "(:projectSite IS NULL OR u.projectSite = :projectSite) AND " +
            "(:department IS NULL OR u.department = :department) AND " +
            "u.position IS NOT NULL ORDER BY u.position")
    List<String> findPositionsByProjectSiteAndDepartment(@Param("projectSite") String projectSite, @Param("department") String department);

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

    // Find users by multiple IDs
    @Query("SELECT u FROM User u WHERE u.id IN :ids")
    List<User> findByIdIn(@Param("ids") List<Long> ids);

    // Check if user has subordinates
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.supervisor.id = :supervisorId AND u.status = 'ACTIVE'")
    boolean hasActiveSubordinates(@Param("supervisorId") Long supervisorId);

    // Find users by role and status
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName AND u.status = :status ORDER BY u.fullName ASC")
    List<User> findByRoleNameAndStatus(@Param("roleName") String roleName, @Param("status") UserStatus status);

    // Advanced search with multiple criteria and hierarchical filtering
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
            "(:projectSite IS NULL OR u.projectSite = :projectSite)")
    List<User> findWithFilters(
            @Param("search") String search,
            @Param("status") UserStatus status,
            @Param("role") String role,
            @Param("department") String department,
            @Param("position") String position,
            @Param("projectSite") String projectSite
    );

    // Get roles by project site
    @Query("SELECT DISTINCT r.name FROM User u JOIN u.roles r WHERE " +
            "(:projectSite IS NULL OR u.projectSite = :projectSite) " +
            "ORDER BY r.name")
    List<String> findRolesByProjectSite(@Param("projectSite") String projectSite);

    // Get roles by project site and department
    @Query("SELECT DISTINCT r.name FROM User u JOIN u.roles r WHERE " +
            "(:projectSite IS NULL OR u.projectSite = :projectSite) AND " +
            "(:department IS NULL OR u.department = :department) " +
            "ORDER BY r.name")
    List<String> findRolesByProjectSiteAndDepartment(@Param("projectSite") String projectSite, @Param("department") String department);
}
// Complete MonthlyTimesheetRepository.java - Clean version with versioning support
package com.goldtech.timesheet_backend.repository;

import com.goldtech.timesheet_backend.entity.MonthlyTimesheet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MonthlyTimesheetRepository extends JpaRepository<MonthlyTimesheet, Long> {

    // ========== NEW VERSIONING-AWARE METHODS (PRIMARY) ==========

    // Find CURRENT VERSION by user, year, and month
    @Query("SELECT mt FROM MonthlyTimesheet mt WHERE mt.user.id = :userId " +
            "AND mt.year = :year AND mt.month = :month AND mt.isCurrentVersion = true")
    Optional<MonthlyTimesheet> findCurrentVersionByUserIdAndYearAndMonth(@Param("userId") Long userId,
                                                                         @Param("year") Integer year,
                                                                         @Param("month") Integer month);

    // Find ALL VERSIONS by user, year, and month (for history)
    @Query("SELECT mt FROM MonthlyTimesheet mt WHERE mt.user.id = :userId " +
            "AND mt.year = :year AND mt.month = :month ORDER BY mt.version DESC")
    List<MonthlyTimesheet> getAllVersionsByUserIdAndYearAndMonth(@Param("userId") Long userId,
                                                                 @Param("year") Integer year,
                                                                 @Param("month") Integer month);

    // Alias method for consistency
    default List<MonthlyTimesheet> findAllVersionsByUserIdAndYearAndMonth(Long userId, Integer year, Integer month) {
        return getAllVersionsByUserIdAndYearAndMonth(userId, year, month);
    }

    // Find CURRENT VERSIONS by user (for general timesheet lists)
    @Query("SELECT mt FROM MonthlyTimesheet mt WHERE mt.user.id = :userId " +
            "AND mt.isCurrentVersion = true ORDER BY mt.year DESC, mt.month DESC")
    List<MonthlyTimesheet> findCurrentVersionsByUserId(@Param("userId") Long userId);

    // ========== SUPERVISOR/APPROVAL METHODS (VERSIONING-AWARE) ==========

    // Find current version timesheets assigned to supervisor by status
    @Query("SELECT mt FROM MonthlyTimesheet mt WHERE mt.approvedBy.id = :supervisorId " +
            "AND mt.status = :status AND mt.isCurrentVersion = true ORDER BY mt.submittedAt ASC")
    List<MonthlyTimesheet> findCurrentVersionsByApprovedByIdAndStatus(@Param("supervisorId") Long supervisorId,
                                                                      @Param("status") MonthlyTimesheet.TimesheetStatus status);

    // Find current version timesheets assigned to supervisor with multiple statuses
    @Query("SELECT mt FROM MonthlyTimesheet mt WHERE mt.approvedBy.id = :supervisorId " +
            "AND mt.status IN :statuses AND mt.isCurrentVersion = true ORDER BY mt.submittedAt DESC")
    List<MonthlyTimesheet> findCurrentVersionsByApprovedByIdAndStatusIn(@Param("supervisorId") Long supervisorId,
                                                                        @Param("statuses") List<MonthlyTimesheet.TimesheetStatus> statuses);

    // Find ALL VERSIONS for supervisor (for complete history)
    @Query("SELECT mt FROM MonthlyTimesheet mt WHERE mt.approvedBy.id = :supervisorId " +
            "AND mt.status IN :statuses ORDER BY mt.year DESC, mt.month DESC, mt.version DESC")
    List<MonthlyTimesheet> findAllVersionsByApprovedByIdAndStatusIn(@Param("supervisorId") Long supervisorId,
                                                                    @Param("statuses") List<MonthlyTimesheet.TimesheetStatus> statuses);

    // Count current version timesheets by supervisor and status
    @Query("SELECT COUNT(mt) FROM MonthlyTimesheet mt WHERE mt.approvedBy.id = :supervisorId " +
            "AND mt.status = :status AND mt.isCurrentVersion = true")
    long countCurrentVersionsByApprovedByIdAndStatus(@Param("supervisorId") Long supervisorId,
                                                     @Param("status") MonthlyTimesheet.TimesheetStatus status);

    // Count current version approved timesheets in date range
    @Query("SELECT COUNT(mt) FROM MonthlyTimesheet mt WHERE mt.approvedBy.id = :supervisorId " +
            "AND mt.status = :status AND mt.approvedAt BETWEEN :startDate AND :endDate " +
            "AND mt.isCurrentVersion = true")
    long countCurrentVersionsByApprovedByIdAndStatusAndApprovedAtBetween(@Param("supervisorId") Long supervisorId,
                                                                         @Param("status") MonthlyTimesheet.TimesheetStatus status,
                                                                         @Param("startDate") LocalDateTime startDate,
                                                                         @Param("endDate") LocalDateTime endDate);

    // ========== LEGACY SUPERVISOR METHODS (using user.supervisor relationship) ==========

    // Find current version pending approval for supervisor
    @Query("SELECT mt FROM MonthlyTimesheet mt " +
            "JOIN mt.user u " +
            "WHERE u.supervisor.id = :supervisorId AND mt.status = 'submitted' " +
            "AND mt.isCurrentVersion = true " +
            "ORDER BY mt.submittedAt ASC")
    List<MonthlyTimesheet> findCurrentVersionsPendingApprovalBySupervisor(@Param("supervisorId") Long supervisorId);

    // Find current versions for supervisor using user.supervisor relationship
    @Query("SELECT mt FROM MonthlyTimesheet mt " +
            "JOIN mt.user u " +
            "WHERE u.supervisor.id = :supervisorId AND mt.status IN :statuses " +
            "AND mt.isCurrentVersion = true " +
            "ORDER BY mt.year DESC, mt.month DESC")
    List<MonthlyTimesheet> findCurrentVersionsBySupervisorAndStatusIn(@Param("supervisorId") Long supervisorId,
                                                                      @Param("statuses") List<MonthlyTimesheet.TimesheetStatus> statuses);

    // Find all versions for supervisor using user.supervisor relationship (for complete history)
    @Query("SELECT mt FROM MonthlyTimesheet mt " +
            "JOIN mt.user u " +
            "WHERE u.supervisor.id = :supervisorId AND mt.status IN :statuses " +
            "ORDER BY mt.year DESC, mt.month DESC, mt.version DESC")
    List<MonthlyTimesheet> findAllVersionsBySupervisorAndStatusIn(@Param("supervisorId") Long supervisorId,
                                                                  @Param("statuses") List<MonthlyTimesheet.TimesheetStatus> statuses);

    // ========== GENERAL QUERY METHODS (VERSIONING-AWARE) ==========

    // Find timesheets by status (current versions only)
    @Query("SELECT mt FROM MonthlyTimesheet mt WHERE mt.status = :status " +
            "AND mt.isCurrentVersion = true ORDER BY mt.submittedAt ASC")
    List<MonthlyTimesheet> findCurrentVersionsByStatus(@Param("status") MonthlyTimesheet.TimesheetStatus status);

    // Find by year and month (current versions only)
    @Query("SELECT mt FROM MonthlyTimesheet mt JOIN FETCH mt.user u " +
            "WHERE mt.year = :year AND mt.month = :month AND mt.isCurrentVersion = true " +
            "ORDER BY u.fullName ASC")
    List<MonthlyTimesheet> findCurrentVersionsByYearAndMonthOrderByUserFullNameAsc(@Param("year") Integer year,
                                                                                   @Param("month") Integer month);

    // Check if timesheet exists (current version only)
    @Query("SELECT COUNT(mt) > 0 FROM MonthlyTimesheet mt WHERE mt.user.id = :userId " +
            "AND mt.year = :year AND mt.month = :month AND mt.isCurrentVersion = true")
    boolean existsCurrentVersionByUserIdAndYearAndMonth(@Param("userId") Long userId,
                                                        @Param("year") Integer year,
                                                        @Param("month") Integer month);

    // Find submitted timesheets (current versions only)
    @Query("SELECT mt FROM MonthlyTimesheet mt WHERE mt.status IN ('submitted', 'pending', 'approved', 'rejected') " +
            "AND mt.isCurrentVersion = true " +
            "ORDER BY mt.year DESC, mt.month DESC, mt.submittedAt DESC")
    List<MonthlyTimesheet> findCurrentVersionsSubmittedTimesheets();

    // Count timesheets by status (current versions only)
    @Query("SELECT COUNT(mt) FROM MonthlyTimesheet mt WHERE mt.status = :status AND mt.isCurrentVersion = true")
    long countCurrentVersionsByStatus(@Param("status") MonthlyTimesheet.TimesheetStatus status);

    // Find timesheets by multiple statuses (current versions only)
    @Query("SELECT mt FROM MonthlyTimesheet mt WHERE mt.status IN :statuses AND mt.isCurrentVersion = true " +
            "ORDER BY mt.year DESC, mt.month DESC")
    List<MonthlyTimesheet> findCurrentVersionsByStatusIn(@Param("statuses") List<MonthlyTimesheet.TimesheetStatus> statuses);

    // ========== VERSIONING UTILITY METHODS ==========

    // Find previous version of a timesheet
    @Query("SELECT mt FROM MonthlyTimesheet mt WHERE mt.id = :previousVersionId")
    Optional<MonthlyTimesheet> findPreviousVersion(@Param("previousVersionId") Long previousVersionId);

    // Get version history for a specific user/year/month
    @Query("SELECT mt FROM MonthlyTimesheet mt WHERE mt.user.id = :userId " +
            "AND mt.year = :year AND mt.month = :month ORDER BY mt.version ASC")
    List<MonthlyTimesheet> getVersionHistory(@Param("userId") Long userId,
                                             @Param("year") Integer year,
                                             @Param("month") Integer month);

    // Find latest version number for a user/year/month combination
    @Query("SELECT COALESCE(MAX(mt.version), 0) FROM MonthlyTimesheet mt " +
            "WHERE mt.user.id = :userId AND mt.year = :year AND mt.month = :month")
    Integer findLatestVersionNumber(@Param("userId") Long userId,
                                    @Param("year") Integer year,
                                    @Param("month") Integer month);

    // ========== BACKWARD COMPATIBILITY METHODS (Delegate to versioning-aware methods) ==========

    // Legacy method - gets current version
    default Optional<MonthlyTimesheet> findByUserIdAndYearAndMonth(Long userId, Integer year, Integer month) {
        return findCurrentVersionByUserIdAndYearAndMonth(userId, year, month);
    }

    // Legacy method - gets all versions for history
    List<MonthlyTimesheet> findByUserIdOrderByYearDescMonthDesc(Long userId);

    // Legacy supervisor methods (backward compatibility)
    default List<MonthlyTimesheet> findByApprovedByIdAndStatus(Long supervisorId, MonthlyTimesheet.TimesheetStatus status) {
        return findCurrentVersionsByApprovedByIdAndStatus(supervisorId, status);
    }

    default List<MonthlyTimesheet> findByApprovedByIdAndStatusIn(Long supervisorId, List<MonthlyTimesheet.TimesheetStatus> statuses) {
        return findCurrentVersionsByApprovedByIdAndStatusIn(supervisorId, statuses);
    }

    default long countByApprovedByIdAndStatus(Long supervisorId, MonthlyTimesheet.TimesheetStatus status) {
        return countCurrentVersionsByApprovedByIdAndStatus(supervisorId, status);
    }

    default long countByApprovedByIdAndStatusAndApprovedAtBetween(Long supervisorId,
                                                                  MonthlyTimesheet.TimesheetStatus status,
                                                                  LocalDateTime startDate,
                                                                  LocalDateTime endDate) {
        return countCurrentVersionsByApprovedByIdAndStatusAndApprovedAtBetween(supervisorId, status, startDate, endDate);
    }

    default List<MonthlyTimesheet> findPendingApprovalBySupervisor(Long supervisorId) {
        return findCurrentVersionsPendingApprovalBySupervisor(supervisorId);
    }

    default List<MonthlyTimesheet> findBySupervisorAndStatusIn(Long supervisorId, List<MonthlyTimesheet.TimesheetStatus> statuses) {
        return findCurrentVersionsBySupervisorAndStatusIn(supervisorId, statuses);
    }

    // Legacy general methods (backward compatibility)
    default List<MonthlyTimesheet> findByStatus(MonthlyTimesheet.TimesheetStatus status) {
        return findCurrentVersionsByStatus(status);
    }

    default List<MonthlyTimesheet> findByYearAndMonthOrderByUserFullNameAsc(Integer year, Integer month) {
        return findCurrentVersionsByYearAndMonthOrderByUserFullNameAsc(year, month);
    }

    default boolean existsByUserIdAndYearAndMonth(Long userId, Integer year, Integer month) {
        return existsCurrentVersionByUserIdAndYearAndMonth(userId, year, month);
    }

    default List<MonthlyTimesheet> findSubmittedTimesheets() {
        return findCurrentVersionsSubmittedTimesheets();
    }

    default long countByStatus(MonthlyTimesheet.TimesheetStatus status) {
        return countCurrentVersionsByStatus(status);
    }

    default List<MonthlyTimesheet> findByStatusIn(List<MonthlyTimesheet.TimesheetStatus> statuses) {
        return findCurrentVersionsByStatusIn(statuses);
    }
}
// Enhanced MonthlyTimesheetRepository.java - Added missing methods for approval flow
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

    // Find by user, year, and month
    Optional<MonthlyTimesheet> findByUserIdAndYearAndMonth(Long userId, Integer year, Integer month);

    // Find timesheets by user
    List<MonthlyTimesheet> findByUserIdOrderByYearDescMonthDesc(Long userId);

    // Find timesheets by status
    List<MonthlyTimesheet> findByStatus(MonthlyTimesheet.TimesheetStatus status);

    // Find timesheets by status with ordering
    List<MonthlyTimesheet> findByStatusOrderBySubmittedAtAsc(MonthlyTimesheet.TimesheetStatus status);

    // NEW: Find timesheets assigned to specific supervisor (approver) by status
    @Query("SELECT mt FROM MonthlyTimesheet mt WHERE mt.approvedBy.id = :supervisorId AND mt.status = :status ORDER BY mt.submittedAt ASC")
    List<MonthlyTimesheet> findByApprovedByIdAndStatus(@Param("supervisorId") Long supervisorId,
                                                       @Param("status") MonthlyTimesheet.TimesheetStatus status);

    // NEW: Find timesheets assigned to specific supervisor with multiple statuses
    @Query("SELECT mt FROM MonthlyTimesheet mt WHERE mt.approvedBy.id = :supervisorId AND mt.status IN :statuses ORDER BY mt.submittedAt DESC")
    List<MonthlyTimesheet> findByApprovedByIdAndStatusIn(@Param("supervisorId") Long supervisorId,
                                                         @Param("statuses") List<MonthlyTimesheet.TimesheetStatus> statuses);

    // NEW: Count timesheets by supervisor and status
    @Query("SELECT COUNT(mt) FROM MonthlyTimesheet mt WHERE mt.approvedBy.id = :supervisorId AND mt.status = :status")
    long countByApprovedByIdAndStatus(@Param("supervisorId") Long supervisorId,
                                      @Param("status") MonthlyTimesheet.TimesheetStatus status);

    // NEW: Count approved timesheets in date range for dashboard
    @Query("SELECT COUNT(mt) FROM MonthlyTimesheet mt WHERE mt.approvedBy.id = :supervisorId " +
            "AND mt.status = :status AND mt.approvedAt BETWEEN :startDate AND :endDate")
    long countByApprovedByIdAndStatusAndApprovedAtBetween(@Param("supervisorId") Long supervisorId,
                                                          @Param("status") MonthlyTimesheet.TimesheetStatus status,
                                                          @Param("startDate") LocalDateTime startDate,
                                                          @Param("endDate") LocalDateTime endDate);

    // LEGACY: Find timesheets pending approval for supervisor (using user.supervisor relationship)
    @Query("SELECT mt FROM MonthlyTimesheet mt " +
            "JOIN mt.user u " +
            "WHERE u.supervisor.id = :supervisorId AND mt.status = 'submitted' " +
            "ORDER BY mt.submittedAt ASC")
    List<MonthlyTimesheet> findPendingApprovalBySupervisor(@Param("supervisorId") Long supervisorId);

    // LEGACY: Find all timesheets for supervisor (all statuses) using user.supervisor relationship
    @Query("SELECT mt FROM MonthlyTimesheet mt " +
            "JOIN mt.user u " +
            "WHERE u.supervisor.id = :supervisorId AND mt.status IN :statuses " +
            "ORDER BY mt.year DESC, mt.month DESC, mt.submittedAt DESC")
    List<MonthlyTimesheet> findBySupervisorAndStatusIn(@Param("supervisorId") Long supervisorId,
                                                       @Param("statuses") List<MonthlyTimesheet.TimesheetStatus> statuses);

    // Find timesheets by year and month
    List<MonthlyTimesheet> findByYearAndMonthOrderByUserFullNameAsc(Integer year, Integer month);

    // Check if timesheet exists
    boolean existsByUserIdAndYearAndMonth(Long userId, Integer year, Integer month);

    // Find submitted timesheets
    @Query("SELECT mt FROM MonthlyTimesheet mt WHERE mt.status IN ('submitted', 'pending', 'approved', 'rejected') " +
            "ORDER BY mt.year DESC, mt.month DESC, mt.submittedAt DESC")
    List<MonthlyTimesheet> findSubmittedTimesheets();

    // Count timesheets by status
    long countByStatus(MonthlyTimesheet.TimesheetStatus status);

    // Find timesheets by multiple statuses
    @Query("SELECT mt FROM MonthlyTimesheet mt WHERE mt.status IN :statuses " +
            "ORDER BY mt.year DESC, mt.month DESC")
    List<MonthlyTimesheet> findByStatusIn(@Param("statuses") List<MonthlyTimesheet.TimesheetStatus> statuses);
}
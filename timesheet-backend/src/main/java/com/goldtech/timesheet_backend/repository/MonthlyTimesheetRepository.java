// 4. Monthly Timesheet Repository
// src/main/java/com/goldtech/timesheet_backend/repository/MonthlyTimesheetRepository.java
package com.goldtech.timesheet_backend.repository;

import com.goldtech.timesheet_backend.entity.MonthlyTimesheet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MonthlyTimesheetRepository extends JpaRepository<MonthlyTimesheet, Long> {

    // Find by user, year, and month
    Optional<MonthlyTimesheet> findByUserIdAndYearAndMonth(Long userId, Integer year, Integer month);

    // Find timesheets by user
    List<MonthlyTimesheet> findByUserIdOrderByYearDescMonthDesc(Long userId);

    // Find timesheets by status
    List<MonthlyTimesheet> findByStatusOrderBySubmittedAtAsc(MonthlyTimesheet.TimesheetStatus status);

    // Find timesheets pending approval for supervisor
    @Query("SELECT mt FROM MonthlyTimesheet mt " +
            "JOIN mt.user u " +
            "WHERE u.supervisor.id = :supervisorId AND mt.status = 'submitted' " +
            "ORDER BY mt.submittedAt ASC")
    List<MonthlyTimesheet> findPendingApprovalBySupervisor(@Param("supervisorId") Long supervisorId);

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
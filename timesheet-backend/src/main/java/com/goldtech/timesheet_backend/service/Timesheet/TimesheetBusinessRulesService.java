// TimesheetBusinessRulesService.java - Extract business logic
package com.goldtech.timesheet_backend.service;

import com.goldtech.timesheet_backend.entity.MonthlyTimesheet;
import com.goldtech.timesheet_backend.repository.MonthlyTimesheetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class TimesheetBusinessRulesService {

    @Autowired
    private MonthlyTimesheetRepository monthlyTimesheetRepository;

    /**
     * Check if a timesheet can be submitted based on business rules
     * Current month: Always allowed
     * Previous month: Only within first 10 days of current month
     */
    public boolean canSubmitTimesheet(Long userId, Integer year, Integer month) {
        LocalDate today = LocalDate.now();
        LocalDate timesheetMonth = LocalDate.of(year, month, 1);

        // Current month - always allowed
        if (timesheetMonth.getYear() == today.getYear() &&
                timesheetMonth.getMonthValue() == today.getMonthValue()) {
            return true;
        }

        // Previous month - only allowed within first 10 days of current month
        if (timesheetMonth.getYear() == today.getYear() &&
                timesheetMonth.getMonthValue() == today.getMonthValue() - 1) {
            return today.getDayOfMonth() <= 10;
        }

        // Previous year December - only allowed within first 10 days of January
        if (timesheetMonth.getYear() == today.getYear() - 1 &&
                timesheetMonth.getMonthValue() == 12 &&
                today.getMonthValue() == 1) {
            return today.getDayOfMonth() <= 10;
        }

        return false;
    }

    /**
     * Check if a timesheet can be resubmitted (after rejection)
     */
    public boolean canResubmitTimesheet(Long userId, Integer year, Integer month) {
        Optional<MonthlyTimesheet> timesheetOpt = monthlyTimesheetRepository
                .findByUserIdAndYearAndMonth(userId, year, month);

        return timesheetOpt.isPresent() &&
                timesheetOpt.get().getStatus() == MonthlyTimesheet.TimesheetStatus.rejected;
    }

    /**
     * Check if timesheet can be edited (not submitted/approved)
     */
    public boolean canEditTimesheet(Long userId, Integer year, Integer month) {
        Optional<MonthlyTimesheet> monthlyTimesheet = monthlyTimesheetRepository
                .findByUserIdAndYearAndMonth(userId, year, month);

        if (monthlyTimesheet.isEmpty()) {
            return true; // No timesheet exists, can create entries
        }

        MonthlyTimesheet.TimesheetStatus status = monthlyTimesheet.get().getStatus();
        return status == MonthlyTimesheet.TimesheetStatus.draft ||
                status == MonthlyTimesheet.TimesheetStatus.rejected;
    }

    /**
     * Validate if timesheet has sufficient entries for submission
     */
    public void validateTimesheetForSubmission(Long userId, Integer year, Integer month, long entryCount) {
        if (entryCount == 0) {
            throw new IllegalArgumentException("Cannot submit empty timesheet");
        }

        // Additional validation rules can be added here
        // For example: minimum working days, required documentation, etc.
    }

    /**
     * Get submission deadline message
     */
    public String getSubmissionDeadlineMessage(Integer year, Integer month) {
        LocalDate today = LocalDate.now();
        LocalDate timesheetMonth = LocalDate.of(year, month, 1);

        if (timesheetMonth.isBefore(today.withDayOfMonth(1))) {
            return "Previous month timesheet can only be submitted within the first 10 days of the current month";
        }

        return "This timesheet cannot be submitted at this time";
    }
}
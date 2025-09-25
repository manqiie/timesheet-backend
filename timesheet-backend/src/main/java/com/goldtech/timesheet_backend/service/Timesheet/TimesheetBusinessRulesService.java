// Updated TimesheetBusinessRulesService.java - With versioning support
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
     * Check if a timesheet can be resubmitted (after rejection) - UPDATED for versioning
     */
    public boolean canResubmitTimesheet(Long userId, Integer year, Integer month) {
        // Must be within submission window first
        if (!canSubmitTimesheet(userId, year, month)) {
            return false;
        }

        // Check if current version is rejected
        Optional<MonthlyTimesheet> currentTimesheetOpt = monthlyTimesheetRepository
                .findCurrentVersionByUserIdAndYearAndMonth(userId, year, month);

        return currentTimesheetOpt.isPresent() &&
                currentTimesheetOpt.get().getStatus() == MonthlyTimesheet.TimesheetStatus.rejected;
    }

    /**
     * Check if timesheet can be edited (not submitted/approved) - UPDATED for versioning
     */
    public boolean canEditTimesheet(Long userId, Integer year, Integer month) {
        Optional<MonthlyTimesheet> currentTimesheetOpt = monthlyTimesheetRepository
                .findCurrentVersionByUserIdAndYearAndMonth(userId, year, month);

        if (currentTimesheetOpt.isEmpty()) {
            return true; // No timesheet exists, can create entries
        }

        MonthlyTimesheet.TimesheetStatus status = currentTimesheetOpt.get().getStatus();
        return status == MonthlyTimesheet.TimesheetStatus.draft ||
                status == MonthlyTimesheet.TimesheetStatus.rejected;
    }

    /**
     * Check if a timesheet can be viewed (exists and user has access)
     */
    public boolean canViewTimesheet(Long userId, Integer year, Integer month) {
        return monthlyTimesheetRepository
                .existsCurrentVersionByUserIdAndYearAndMonth(userId, year, month);
    }

    /**
     * Check if supervisor can approve timesheet - UPDATED for versioning
     */
    public boolean canApproveTimesheet(Long timesheetId, Long supervisorId) {
        Optional<MonthlyTimesheet> timesheetOpt = monthlyTimesheetRepository.findById(timesheetId);

        if (timesheetOpt.isEmpty()) {
            return false;
        }

        MonthlyTimesheet timesheet = timesheetOpt.get();

        // Must be current version, submitted status, and assigned to this supervisor
        return timesheet.getIsCurrentVersion() &&
                timesheet.getStatus() == MonthlyTimesheet.TimesheetStatus.submitted &&
                timesheet.getApprovedBy() != null &&
                timesheet.getApprovedBy().getId().equals(supervisorId);
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

    /**
     * Get timesheet version information for display purposes
     */
    public String getTimesheetVersionInfo(Long userId, Integer year, Integer month) {
        Optional<MonthlyTimesheet> currentTimesheetOpt = monthlyTimesheetRepository
                .findCurrentVersionByUserIdAndYearAndMonth(userId, year, month);

        if (currentTimesheetOpt.isEmpty()) {
            return "No timesheet found";
        }

        MonthlyTimesheet currentTimesheet = currentTimesheetOpt.get();

        if (currentTimesheet.getVersion() == 1) {
            return "Original submission";
        } else {
            return String.format("Version %d (Resubmission)", currentTimesheet.getVersion());
        }
    }

    /**
     * Check if timesheet has multiple versions (was resubmitted)
     */
    public boolean hasMultipleVersions(Long userId, Integer year, Integer month) {
        Integer maxVersion = monthlyTimesheetRepository.findLatestVersionNumber(userId, year, month);
        return maxVersion != null && maxVersion > 1;
    }

    /**
     * Get count of versions for a timesheet
     */
    public int getVersionCount(Long userId, Integer year, Integer month) {
        Integer maxVersion = monthlyTimesheetRepository.findLatestVersionNumber(userId, year, month);
        return maxVersion != null ? maxVersion : 0;
    }
}
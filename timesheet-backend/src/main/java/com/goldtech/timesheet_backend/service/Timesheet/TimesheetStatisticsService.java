// TimesheetStatisticsService.java - Extract statistics and calculations
package com.goldtech.timesheet_backend.service;

import com.goldtech.timesheet_backend.dto.timesheet.AvailableMonthDto;
import com.goldtech.timesheet_backend.dto.timesheet.TimesheetStatsDto;
import com.goldtech.timesheet_backend.entity.DayEntry;
import com.goldtech.timesheet_backend.entity.MonthlyTimesheet;
import com.goldtech.timesheet_backend.repository.MonthlyTimesheetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TimesheetStatisticsService {

    @Autowired
    private MonthlyTimesheetRepository monthlyTimesheetRepository;

    /**
     * Calculate timesheet statistics with overnight shift support
     */
    public TimesheetStatsDto calculateStats(List<DayEntry> entries) {
        TimesheetStatsDto stats = new TimesheetStatsDto();

        stats.setTotalEntries(entries.size());

        long workingDays = entries.stream()
                .filter(entry -> entry.getEntryType() == DayEntry.EntryType.working_hours)
                .count();
        stats.setWorkingDays((int) workingDays);

        long leaveDays = entries.stream()
                .filter(entry -> entry.getEntryType() != DayEntry.EntryType.working_hours)
                .count();
        stats.setLeaveDays((int) leaveDays);

        // Calculate total hours with overnight shift support
        double totalHours = calculateTotalWorkingHours(entries);
        stats.setTotalHours(totalHours);

        // Calculate leave breakdown
        Map<String, Integer> leaveBreakdown = calculateLeaveBreakdown(entries);
        stats.setLeaveBreakdown(leaveBreakdown);

        return stats;
    }

    /**
     * Calculate total working hours with overnight shift support
     */
    public double calculateTotalWorkingHours(List<DayEntry> entries) {
        return entries.stream()
                .filter(entry -> entry.getEntryType() == DayEntry.EntryType.working_hours)
                .filter(entry -> entry.getStartTime() != null && entry.getEndTime() != null)
                .mapToDouble(this::calculateHoursForEntry)
                .sum();
    }

    /**
     * Calculate hours for a single entry with overnight shift support
     */
    public double calculateHoursForEntry(DayEntry entry) {
        LocalTime start = entry.getStartTime();
        LocalTime end = entry.getEndTime();

        // Calculate duration with overnight shift support
        LocalDateTime startDateTime = LocalDateTime.of(LocalDate.of(2000, 1, 1), start);
        LocalDateTime endDateTime = LocalDateTime.of(LocalDate.of(2000, 1, 1), end);

        // If end time is before or equal to start time, assume next day (overnight shift)
        if (end.isBefore(start) || end.equals(start)) {
            endDateTime = endDateTime.plusDays(1);
        }

        long minutes = java.time.Duration.between(startDateTime, endDateTime).toMinutes();
        return minutes / 60.0;
    }

    /**
     * Calculate leave type breakdown
     */
    public Map<String, Integer> calculateLeaveBreakdown(List<DayEntry> entries) {
        return entries.stream()
                .filter(entry -> entry.getEntryType() != DayEntry.EntryType.working_hours)
                .collect(Collectors.groupingBy(
                        entry -> formatLeaveTypeName(entry.getEntryType().toString()),
                        Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
                ));
    }

    /**
     * Get available months for timesheet submission
     */
    public List<AvailableMonthDto> getAvailableMonths(Long userId) {
        LocalDate today = LocalDate.now();
        List<AvailableMonthDto> availableMonths = new ArrayList<>();

        // Current month
        AvailableMonthDto currentMonth = createAvailableMonthDto(
                userId, today.getYear(), today.getMonthValue(), true);
        availableMonths.add(currentMonth);

        // Previous month (if within 10 days)
        if (today.getDayOfMonth() <= 10) {
            LocalDate previousMonth = today.minusMonths(1);
            AvailableMonthDto prevMonth = createAvailableMonthDto(
                    userId, previousMonth.getYear(), previousMonth.getMonthValue(), false);

            // Only show if not submitted or if rejected (can resubmit)
            if (canShowPreviousMonth(userId, previousMonth.getYear(), previousMonth.getMonthValue())) {
                availableMonths.add(0, prevMonth);
            }
        }

        return availableMonths;
    }

    /**
     * Get supervisor approval statistics
     */
    public Map<String, Object> getSupervisorStats(Long supervisorId) {
        Map<String, Object> stats = new HashMap<>();

        long pendingCount = monthlyTimesheetRepository.countByApprovedByIdAndStatus(
                supervisorId, MonthlyTimesheet.TimesheetStatus.submitted);

        LocalDateTime monthStart = LocalDateTime.now()
                .withDayOfMonth(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0);

        long approvedThisMonth = monthlyTimesheetRepository
                .countByApprovedByIdAndStatusAndApprovedAtBetween(
                        supervisorId,
                        MonthlyTimesheet.TimesheetStatus.approved,
                        monthStart,
                        LocalDateTime.now()
                );

        stats.put("pendingApprovals", pendingCount);
        stats.put("approvedThisMonth", approvedThisMonth);
        stats.put("averageApprovalTime", calculateAverageApprovalTime(supervisorId));

        return stats;
    }

    /**
     * Get month name from number
     */
    public String getMonthName(Integer month) {
        return LocalDate.of(2024, month, 1)
                .getMonth()
                .getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }

    // Private helper methods

    private AvailableMonthDto createAvailableMonthDto(Long userId, Integer year, Integer month, Boolean isCurrentMonth) {
        AvailableMonthDto dto = new AvailableMonthDto();
        dto.setYear(year);
        dto.setMonth(month);
        dto.setMonthName(getMonthName(month));
        dto.setIsCurrentMonth(isCurrentMonth);

        Optional<MonthlyTimesheet> timesheet = monthlyTimesheetRepository
                .findByUserIdAndYearAndMonth(userId, year, month);

        dto.setIsSubmitted(timesheet.isPresent() &&
                timesheet.get().getStatus() != MonthlyTimesheet.TimesheetStatus.draft);

        return dto;
    }

    private boolean canShowPreviousMonth(Long userId, Integer year, Integer month) {
        Optional<MonthlyTimesheet> timesheet = monthlyTimesheetRepository
                .findByUserIdAndYearAndMonth(userId, year, month);

        if (timesheet.isEmpty()) {
            return true; // No timesheet exists, can create
        }

        MonthlyTimesheet.TimesheetStatus status = timesheet.get().getStatus();
        return status == MonthlyTimesheet.TimesheetStatus.draft ||
                status == MonthlyTimesheet.TimesheetStatus.rejected;
    }

    private String formatLeaveTypeName(String leaveType) {
        // Convert snake_case to readable format
        return Arrays.stream(leaveType.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    private double calculateAverageApprovalTime(Long supervisorId) {
        // This would require additional database queries to calculate
        // average time between submission and approval
        return 0.0; // Placeholder - you can implement this later
    }
}
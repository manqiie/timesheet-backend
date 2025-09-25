// Updated TimesheetApprovalService.java - With versioning support
package com.goldtech.timesheet_backend.service;

import com.goldtech.timesheet_backend.dto.timesheet.TimesheetResponseDto;
import com.goldtech.timesheet_backend.dto.timesheet.TimesheetStatsDto;
import com.goldtech.timesheet_backend.entity.DayEntry;
import com.goldtech.timesheet_backend.entity.MonthlyTimesheet;
import com.goldtech.timesheet_backend.entity.User;
import com.goldtech.timesheet_backend.repository.DayEntryRepository;
import com.goldtech.timesheet_backend.repository.MonthlyTimesheetRepository;
import com.goldtech.timesheet_backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;

@Service
@Transactional
public class TimesheetApprovalService {

    private static final Logger logger = LoggerFactory.getLogger(TimesheetApprovalService.class);

    @Autowired
    private MonthlyTimesheetRepository monthlyTimesheetRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DayEntryRepository dayEntryRepository;

    @Autowired
    private TimesheetService timesheetService;

    @Autowired
    private TimesheetBusinessRulesService businessRulesService;

    /**
     * Get pending timesheets for a supervisor - UPDATED for versioning (only current versions)
     */
    public List<TimesheetResponseDto> getPendingTimesheets(Long supervisorId) {
        logger.debug("Getting pending timesheets for supervisor {}", supervisorId);

        // Get current version timesheets where this supervisor is the approver and status is submitted
        List<MonthlyTimesheet> pendingTimesheets = monthlyTimesheetRepository
                .findCurrentVersionsByApprovedByIdAndStatus(supervisorId, MonthlyTimesheet.TimesheetStatus.submitted);

        logger.info("Found {} pending timesheets (current versions) for supervisor {}", pendingTimesheets.size(), supervisorId);

        return pendingTimesheets.stream()
                .map(this::convertToResponseDtoWithStats)
                .collect(Collectors.toList());
    }

    /**
     * Get all timesheets for approval by supervisor - UPDATED with option to include all versions
     */
    public List<TimesheetResponseDto> getTimesheetsForApproval(Long supervisorId, String statusFilter) {
        logger.debug("Getting timesheets for approval by supervisor {} with status {}", supervisorId, statusFilter);

        List<MonthlyTimesheet> timesheets;

        if ("all".equals(statusFilter)) {
            // Get ALL VERSIONS of timesheets assigned to this supervisor for complete history
            List<MonthlyTimesheet.TimesheetStatus> statuses = List.of(
                    MonthlyTimesheet.TimesheetStatus.submitted,
                    MonthlyTimesheet.TimesheetStatus.approved,
                    MonthlyTimesheet.TimesheetStatus.rejected
            );
            timesheets = monthlyTimesheetRepository.findAllVersionsByApprovedByIdAndStatusIn(supervisorId, statuses);
            logger.info("Found {} timesheets (ALL VERSIONS) for supervisor {} with status {}",
                    timesheets.size(), supervisorId, statusFilter);
        } else if ("pending".equals(statusFilter)) {
            // For pending, only get current versions
            timesheets = monthlyTimesheetRepository.findCurrentVersionsByApprovedByIdAndStatus(
                    supervisorId, MonthlyTimesheet.TimesheetStatus.submitted);
            logger.info("Found {} pending timesheets (current versions) for supervisor {}",
                    timesheets.size(), supervisorId);
        } else {
            // For specific status, get current versions only
            MonthlyTimesheet.TimesheetStatus status = MonthlyTimesheet.TimesheetStatus.valueOf(statusFilter);
            timesheets = monthlyTimesheetRepository.findCurrentVersionsByApprovedByIdAndStatus(supervisorId, status);
            logger.info("Found {} timesheets (current versions) for supervisor {} with status {}",
                    timesheets.size(), supervisorId, statusFilter);
        }

        return timesheets.stream()
                .map(this::convertToResponseDtoWithStats)
                .collect(Collectors.toList());
    }

    /**
     * Process timesheet approval or rejection - UPDATED for versioning
     */
    public TimesheetResponseDto processApproval(Long timesheetId, Long supervisorId, String decision, String comments) {
        logger.debug("Processing approval for timesheet {} by supervisor {}: {}", timesheetId, supervisorId, decision);

        Optional<MonthlyTimesheet> timesheetOpt = monthlyTimesheetRepository.findById(timesheetId);
        if (timesheetOpt.isEmpty()) {
            throw new IllegalArgumentException("Timesheet not found");
        }

        MonthlyTimesheet timesheet = timesheetOpt.get();

        // Verify supervisor has authority using business rules
        if (!businessRulesService.canApproveTimesheet(timesheetId, supervisorId)) {
            throw new IllegalArgumentException("You are not authorized to approve this timesheet or it's not in a valid state");
        }

        // Update timesheet status
        if ("approved".equals(decision)) {
            timesheet.setStatus(MonthlyTimesheet.TimesheetStatus.approved);
        } else if ("rejected".equals(decision)) {
            timesheet.setStatus(MonthlyTimesheet.TimesheetStatus.rejected);
        } else {
            throw new IllegalArgumentException("Invalid decision: " + decision);
        }

        // Set approval timestamp and comments
        timesheet.setApprovedAt(LocalDateTime.now());
        timesheet.setApprovalComments(comments);

        timesheet = monthlyTimesheetRepository.save(timesheet);

        logger.info("Timesheet {} {} by supervisor {} for employee {} (Version: {})",
                timesheetId, decision, supervisorId, timesheet.getUser().getFullName(), timesheet.getVersion());

        // Return full timesheet details
        return timesheetService.getTimesheet(timesheet.getUser().getId(), timesheet.getYear(), timesheet.getMonth());
    }

    /**
     * Get timesheet details for approval review - UPDATED for versioning
     */
    public TimesheetResponseDto getTimesheetForApproval(Long timesheetId, Long supervisorId) {
        logger.debug("Getting timesheet {} for approval by supervisor {}", timesheetId, supervisorId);

        Optional<MonthlyTimesheet> timesheetOpt = monthlyTimesheetRepository.findById(timesheetId);
        if (timesheetOpt.isEmpty()) {
            throw new IllegalArgumentException("Timesheet not found");
        }

        MonthlyTimesheet timesheet = timesheetOpt.get();

        // Verify supervisor has authority to view this timesheet
        if (timesheet.getApprovedBy() == null || !timesheet.getApprovedBy().getId().equals(supervisorId)) {
            throw new IllegalArgumentException("You are not authorized to view this timesheet");
        }

        // Return full timesheet details - this will get the current version data
        // but the specific timesheet version data will be from the requested ID
        TimesheetResponseDto response = timesheetService.getTimesheet(
                timesheet.getUser().getId(), timesheet.getYear(), timesheet.getMonth());

        // Override with the specific version's metadata
        response.setTimesheetId(timesheet.getId());
        response.setStatus(timesheet.getStatus().toString());
        response.setSubmittedAt(timesheet.getSubmittedAt());
        response.setApprovedAt(timesheet.getApprovedAt());
        response.setApprovalComments(timesheet.getApprovalComments());
        response.setCreatedAt(timesheet.getCreatedAt());
        response.setUpdatedAt(timesheet.getUpdatedAt());

        return response;
    }

    /**
     * Get summary of pending approvals for supervisor dashboard - UPDATED for versioning
     */
    public Map<String, Object> getApprovalSummary(Long supervisorId) {
        Map<String, Object> summary = new HashMap<>();

        // Count current version pending timesheets
        long pendingCount = monthlyTimesheetRepository.countCurrentVersionsByApprovedByIdAndStatus(
                supervisorId, MonthlyTimesheet.TimesheetStatus.submitted);

        // Count current version approved this month
        long approvedThisMonth = monthlyTimesheetRepository.countCurrentVersionsByApprovedByIdAndStatusAndApprovedAtBetween(
                supervisorId,
                MonthlyTimesheet.TimesheetStatus.approved,
                LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0),
                LocalDateTime.now()
        );

        summary.put("pendingCount", pendingCount);
        summary.put("approvedThisMonth", approvedThisMonth);

        return summary;
    }

    /**
     * Get version history for a specific user's timesheet
     */
    public List<TimesheetResponseDto> getTimesheetVersionHistory(Long userId, Integer year, Integer month) {
        logger.debug("Getting version history for user {} - {}/{}", userId, year, month);

        List<MonthlyTimesheet> versions = monthlyTimesheetRepository
                .getAllVersionsByUserIdAndYearAndMonth(userId, year, month);

        return versions.stream()
                .map(this::convertToResponseDtoWithStats)
                .collect(Collectors.toList());
    }

    /**
     * Convert MonthlyTimesheet entity to TimesheetResponseDto with statistics and version info
     */
    private TimesheetResponseDto convertToResponseDtoWithStats(MonthlyTimesheet timesheet) {
        TimesheetResponseDto dto = new TimesheetResponseDto();
        dto.setTimesheetId(timesheet.getId());
        dto.setYear(timesheet.getYear());
        dto.setMonth(timesheet.getMonth());
        dto.setMonthName(getMonthName(timesheet.getMonth()));
        dto.setStatus(timesheet.getStatus().toString());
        dto.setSubmittedAt(timesheet.getSubmittedAt());

        if (timesheet.getApprovedBy() != null) {
            dto.setApprovedBy(timesheet.getApprovedBy().getFullName());
        }
        dto.setApprovedAt(timesheet.getApprovedAt());
        dto.setApprovalComments(timesheet.getApprovalComments());

        dto.setCreatedAt(timesheet.getCreatedAt());
        dto.setUpdatedAt(timesheet.getUpdatedAt());

        // Add employee information for supervisor view
        User employee = timesheet.getUser();
        dto.setEmployeeName(employee.getFullName());
        dto.setEmployeeId(employee.getEmployeeId());
        dto.setEmployeePosition(employee.getPosition());
        dto.setEmployeeProjectSite(employee.getProjectSite());

        // Calculate statistics from day entries
        List<DayEntry> entries = dayEntryRepository.findByUserIdAndYearAndMonth(
                employee.getId(), timesheet.getYear(), timesheet.getMonth());

        TimesheetStatsDto stats = calculateStats(entries);
        dto.setStats(stats);

        return dto;
    }

    /**
     * Calculate statistics from day entries with overnight shift support
     */
    private TimesheetStatsDto calculateStats(List<DayEntry> entries) {
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

        // Calculate total hours for working days with overnight shift support
        double totalHours = entries.stream()
                .filter(entry -> entry.getEntryType() == DayEntry.EntryType.working_hours)
                .filter(entry -> entry.getStartTime() != null && entry.getEndTime() != null)
                .mapToDouble(entry -> {
                    LocalTime start = entry.getStartTime();
                    LocalTime end = entry.getEndTime();

                    // Support overnight shifts
                    LocalDateTime startDateTime = LocalDateTime.of(LocalDateTime.now().toLocalDate(), start);
                    LocalDateTime endDateTime = LocalDateTime.of(LocalDateTime.now().toLocalDate(), end);

                    // If end time is before or equal to start time, assume next day
                    if (end.isBefore(start) || end.equals(start)) {
                        endDateTime = endDateTime.plusDays(1);
                    }

                    return java.time.Duration.between(startDateTime, endDateTime).toMinutes() / 60.0;
                })
                .sum();
        stats.setTotalHours(totalHours);

        // Calculate leave breakdown
        Map<String, Integer> leaveBreakdown = entries.stream()
                .filter(entry -> entry.getEntryType() != DayEntry.EntryType.working_hours)
                .collect(Collectors.groupingBy(
                        entry -> entry.getEntryType().toString(),
                        Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
                ));
        stats.setLeaveBreakdown(leaveBreakdown);

        return stats;
    }

    private String getMonthName(Integer month) {
        return java.time.Month.of(month).getDisplayName(
                java.time.format.TextStyle.FULL,
                java.util.Locale.ENGLISH
        );
    }
}
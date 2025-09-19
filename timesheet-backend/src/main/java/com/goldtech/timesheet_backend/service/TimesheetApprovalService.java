// src/main/java/com/goldtech/timesheet_backend/service/TimesheetApprovalService.java
package com.goldtech.timesheet_backend.service;

import com.goldtech.timesheet_backend.dto.timesheet.TimesheetResponseDto;
import com.goldtech.timesheet_backend.entity.MonthlyTimesheet;
import com.goldtech.timesheet_backend.entity.User;
import com.goldtech.timesheet_backend.repository.MonthlyTimesheetRepository;
import com.goldtech.timesheet_backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class TimesheetApprovalService {

    private static final Logger logger = LoggerFactory.getLogger(TimesheetApprovalService.class);

    @Autowired
    private MonthlyTimesheetRepository monthlyTimesheetRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TimesheetService timesheetService;

    /**
     * Get pending timesheets for a supervisor
     */
    public List<TimesheetResponseDto> getPendingTimesheets(Long supervisorId) {
        logger.debug("Getting pending timesheets for supervisor {}", supervisorId);

        List<MonthlyTimesheet> pendingTimesheets = monthlyTimesheetRepository
                .findPendingApprovalBySupervisor(supervisorId);

        return pendingTimesheets.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Get all timesheets for approval by supervisor (pending, approved, rejected)
     */
    public List<TimesheetResponseDto> getTimesheetsForApproval(Long supervisorId, String statusFilter) {
        logger.debug("Getting timesheets for approval by supervisor {} with status {}", supervisorId, statusFilter);

        List<MonthlyTimesheet> timesheets;

        if ("all".equals(statusFilter)) {
            // Get all submitted timesheets for supervisor's subordinates using efficient query
            List<MonthlyTimesheet.TimesheetStatus> statuses = List.of(
                    MonthlyTimesheet.TimesheetStatus.submitted,
                    MonthlyTimesheet.TimesheetStatus.pending,
                    MonthlyTimesheet.TimesheetStatus.approved,
                    MonthlyTimesheet.TimesheetStatus.rejected
            );
            timesheets = monthlyTimesheetRepository.findBySupervisorAndStatusIn(supervisorId, statuses);
        } else if ("pending".equals(statusFilter)) {
            timesheets = monthlyTimesheetRepository.findPendingApprovalBySupervisor(supervisorId);
        } else {
            // Filter by specific status using efficient query
            MonthlyTimesheet.TimesheetStatus status = MonthlyTimesheet.TimesheetStatus.valueOf(statusFilter);
            List<MonthlyTimesheet.TimesheetStatus> statusList = List.of(status);
            timesheets = monthlyTimesheetRepository.findBySupervisorAndStatusIn(supervisorId, statusList);
        }

        return timesheets.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Process timesheet approval or rejection
     */
    public TimesheetResponseDto processApproval(Long timesheetId, Long supervisorId, String decision, String comments) {
        logger.debug("Processing approval for timesheet {} by supervisor {}: {}", timesheetId, supervisorId, decision);

        Optional<MonthlyTimesheet> timesheetOpt = monthlyTimesheetRepository.findById(timesheetId);
        if (timesheetOpt.isEmpty()) {
            throw new IllegalArgumentException("Timesheet not found");
        }

        MonthlyTimesheet timesheet = timesheetOpt.get();

        // Verify supervisor has authority to approve this timesheet
        if (timesheet.getUser().getSupervisor() == null ||
                !timesheet.getUser().getSupervisor().getId().equals(supervisorId)) {
            throw new IllegalArgumentException("You are not authorized to approve this timesheet");
        }

        // Verify timesheet is in submittable state
        if (timesheet.getStatus() != MonthlyTimesheet.TimesheetStatus.submitted &&
                timesheet.getStatus() != MonthlyTimesheet.TimesheetStatus.pending) {
            throw new IllegalArgumentException("Timesheet is not in a state that can be approved/rejected");
        }

        // Get supervisor user
        Optional<User> supervisorOpt = userRepository.findById(supervisorId);
        if (supervisorOpt.isEmpty()) {
            throw new IllegalArgumentException("Supervisor not found");
        }

        User supervisor = supervisorOpt.get();

        // Update timesheet status
        if ("approved".equals(decision)) {
            timesheet.setStatus(MonthlyTimesheet.TimesheetStatus.approved);
        } else if ("rejected".equals(decision)) {
            timesheet.setStatus(MonthlyTimesheet.TimesheetStatus.rejected);
        } else {
            throw new IllegalArgumentException("Invalid decision: " + decision);
        }

        timesheet.setApprovedBy(supervisor);
        timesheet.setApprovedAt(LocalDateTime.now());
        timesheet.setApprovalComments(comments);

        timesheet = monthlyTimesheetRepository.save(timesheet);

        logger.info("Timesheet {} {} by supervisor {}", timesheetId, decision, supervisorId);

        // Return full timesheet details
        return timesheetService.getTimesheet(timesheet.getUser().getId(), timesheet.getYear(), timesheet.getMonth());
    }

    /**
     * Get timesheet details for approval review
     */
    public TimesheetResponseDto getTimesheetForApproval(Long timesheetId, Long supervisorId) {
        logger.debug("Getting timesheet {} for approval by supervisor {}", timesheetId, supervisorId);

        Optional<MonthlyTimesheet> timesheetOpt = monthlyTimesheetRepository.findById(timesheetId);
        if (timesheetOpt.isEmpty()) {
            throw new IllegalArgumentException("Timesheet not found");
        }

        MonthlyTimesheet timesheet = timesheetOpt.get();

        // Verify supervisor has authority to view this timesheet
        if (timesheet.getUser().getSupervisor() == null ||
                !timesheet.getUser().getSupervisor().getId().equals(supervisorId)) {
            throw new IllegalArgumentException("You are not authorized to view this timesheet");
        }

        // Return full timesheet details
        return timesheetService.getTimesheet(timesheet.getUser().getId(), timesheet.getYear(), timesheet.getMonth());
    }

    /**
     * Convert MonthlyTimesheet entity to TimesheetResponseDto for listing
     */
    private TimesheetResponseDto convertToResponseDto(MonthlyTimesheet timesheet) {
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

        return dto;
    }

    private String getMonthName(Integer month) {
        return java.time.Month.of(month).getDisplayName(
                java.time.format.TextStyle.FULL,
                java.util.Locale.ENGLISH
        );
    }
}
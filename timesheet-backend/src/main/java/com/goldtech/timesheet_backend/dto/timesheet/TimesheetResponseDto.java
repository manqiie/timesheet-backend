// 5. Updated Timesheet Response DTO - Add employee info for supervisors
// src/main/java/com/goldtech/timesheet_backend/dto/timesheet/TimesheetResponseDto.java
package com.goldtech.timesheet_backend.dto.timesheet;

import java.time.LocalDateTime;
import java.util.Map;

public class TimesheetResponseDto {
    private Long timesheetId;
    private Integer year;
    private Integer month;
    private String monthName;
    private String status;
    private LocalDateTime submittedAt;
    private String approvedBy;
    private LocalDateTime approvedAt;
    private String approvalComments;
    private Map<String, DayEntryDto> entries; // Key: "YYYY-MM-DD", Value: DayEntryDto
    private TimesheetStatsDto stats;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Employee information (for supervisor view)
    private String employeeName;
    private String employeeId;
    private String employeePosition;
    private String employeeProjectSite;

    // Constructors
    public TimesheetResponseDto() {}

    // Getters and Setters
    public Long getTimesheetId() { return timesheetId; }
    public void setTimesheetId(Long timesheetId) { this.timesheetId = timesheetId; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public Integer getMonth() { return month; }
    public void setMonth(Integer month) { this.month = month; }

    public String getMonthName() { return monthName; }
    public void setMonthName(String monthName) { this.monthName = monthName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public String getApprovalComments() { return approvalComments; }
    public void setApprovalComments(String approvalComments) { this.approvalComments = approvalComments; }

    public Map<String, DayEntryDto> getEntries() { return entries; }
    public void setEntries(Map<String, DayEntryDto> entries) { this.entries = entries; }

    public TimesheetStatsDto getStats() { return stats; }
    public void setStats(TimesheetStatsDto stats) { this.stats = stats; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Employee information getters and setters
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getEmployeePosition() { return employeePosition; }
    public void setEmployeePosition(String employeePosition) { this.employeePosition = employeePosition; }

    public String getEmployeeProjectSite() { return employeeProjectSite; }
    public void setEmployeeProjectSite(String employeeProjectSite) { this.employeeProjectSite = employeeProjectSite; }
}
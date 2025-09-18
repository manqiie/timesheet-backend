// 6. Timesheet Stats DTO
// src/main/java/com/goldtech/timesheet_backend/dto/timesheet/TimesheetStatsDto.java
package com.goldtech.timesheet_backend.dto.timesheet;

import java.util.Map;

public class TimesheetStatsDto {
    private Integer totalEntries;
    private Integer workingDays;
    private Integer leaveDays;
    private Double totalHours;
    private Map<String, Integer> leaveBreakdown; // Key: leave type, Value: count

    // Constructors
    public TimesheetStatsDto() {}

    // Getters and Setters
    public Integer getTotalEntries() { return totalEntries; }
    public void setTotalEntries(Integer totalEntries) { this.totalEntries = totalEntries; }

    public Integer getWorkingDays() { return workingDays; }
    public void setWorkingDays(Integer workingDays) { this.workingDays = workingDays; }

    public Integer getLeaveDays() { return leaveDays; }
    public void setLeaveDays(Integer leaveDays) { this.leaveDays = leaveDays; }

    public Double getTotalHours() { return totalHours; }
    public void setTotalHours(Double totalHours) { this.totalHours = totalHours; }

    public Map<String, Integer> getLeaveBreakdown() { return leaveBreakdown; }
    public void setLeaveBreakdown(Map<String, Integer> leaveBreakdown) { this.leaveBreakdown = leaveBreakdown; }
}
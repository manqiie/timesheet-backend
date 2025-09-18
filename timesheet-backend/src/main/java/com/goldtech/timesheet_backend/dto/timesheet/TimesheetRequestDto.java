// 4. Timesheet Request DTO
// src/main/java/com/goldtech/timesheet_backend/dto/timesheet/TimesheetRequestDto.java
package com.goldtech.timesheet_backend.dto.timesheet;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class TimesheetRequestDto {
    @NotNull(message = "Year is required")
    @Min(value = 2020, message = "Year must be 2020 or later")
    @Max(value = 2030, message = "Year must be 2030 or earlier")
    private Integer year;

    @NotNull(message = "Month is required")
    @Min(value = 1, message = "Month must be between 1 and 12")
    @Max(value = 12, message = "Month must be between 1 and 12")
    private Integer month;

    // Constructors
    public TimesheetRequestDto() {}

    public TimesheetRequestDto(Integer year, Integer month) {
        this.year = year;
        this.month = month;
    }

    // Getters and Setters
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public Integer getMonth() { return month; }
    public void setMonth(Integer month) { this.month = month; }
}
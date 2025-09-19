// New DTOs for timesheet functionality

// 1. AvailableMonthDto.java
// src/main/java/com/goldtech/timesheet_backend/dto/timesheet/AvailableMonthDto.java
package com.goldtech.timesheet_backend.dto.timesheet;

public class AvailableMonthDto {
    private Integer year;
    private Integer month;
    private String monthName;
    private Boolean isCurrentMonth;
    private Boolean isSubmitted;

    // Constructors
    public AvailableMonthDto() {}

    // Getters and Setters
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public Integer getMonth() { return month; }
    public void setMonth(Integer month) { this.month = month; }

    public String getMonthName() { return monthName; }
    public void setMonthName(String monthName) { this.monthName = monthName; }

    public Boolean getIsCurrentMonth() { return isCurrentMonth; }
    public void setIsCurrentMonth(Boolean isCurrentMonth) { this.isCurrentMonth = isCurrentMonth; }

    public Boolean getIsSubmitted() { return isSubmitted; }
    public void setIsSubmitted(Boolean isSubmitted) { this.isSubmitted = isSubmitted; }
}


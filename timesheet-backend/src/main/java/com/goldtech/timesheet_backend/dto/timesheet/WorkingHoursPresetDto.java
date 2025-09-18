// 1. Working Hours Preset DTO
// src/main/java/com/goldtech/timesheet_backend/dto/timesheet/WorkingHoursPresetDto.java
package com.goldtech.timesheet_backend.dto.timesheet;

import java.time.LocalDateTime;

public class WorkingHoursPresetDto {
    private Long id;
    private String name;
    private String startTime; // Format: "HH:mm"
    private String endTime;   // Format: "HH:mm"
    private Boolean isDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public WorkingHoursPresetDto() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public Boolean getIsDefault() { return isDefault; }
    public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
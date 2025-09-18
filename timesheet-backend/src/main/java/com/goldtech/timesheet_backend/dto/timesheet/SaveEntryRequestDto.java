// 7. Save Entry Request DTO
// src/main/java/com/goldtech/timesheet_backend/dto/timesheet/SaveEntryRequestDto.java
package com.goldtech.timesheet_backend.dto.timesheet;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SaveEntryRequestDto {
    @NotBlank(message = "Date is required")
    private String date; // Format: "YYYY-MM-DD"

    @NotBlank(message = "Entry type is required")
    private String type;

    private String startTime; // Format: "HH:mm" (for working hours)
    private String endTime;   // Format: "HH:mm" (for working hours)
    private String halfDayPeriod; // "AM" or "PM" for half day entries
    private String dateEarned; // Format: "YYYY-MM-DD" (for off_in_lieu)
    private String notes;
    private String primaryDocumentDay; // For document references
    private Boolean isPrimaryDocument;

    // Constructors
    public SaveEntryRequestDto() {}

    // Getters and Setters
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getHalfDayPeriod() { return halfDayPeriod; }
    public void setHalfDayPeriod(String halfDayPeriod) { this.halfDayPeriod = halfDayPeriod; }

    public String getDateEarned() { return dateEarned; }
    public void setDateEarned(String dateEarned) { this.dateEarned = dateEarned; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getPrimaryDocumentDay() { return primaryDocumentDay; }
    public void setPrimaryDocumentDay(String primaryDocumentDay) { this.primaryDocumentDay = primaryDocumentDay; }

    public Boolean getIsPrimaryDocument() { return isPrimaryDocument; }
    public void setIsPrimaryDocument(Boolean isPrimaryDocument) { this.isPrimaryDocument = isPrimaryDocument; }
}
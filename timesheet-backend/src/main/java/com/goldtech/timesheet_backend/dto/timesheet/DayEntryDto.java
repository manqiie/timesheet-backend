// 2. Day Entry DTO
// src/main/java/com/goldtech/timesheet_backend/dto/timesheet/DayEntryDto.java
package com.goldtech.timesheet_backend.dto.timesheet;

import java.time.LocalDateTime;
import java.util.List;

public class DayEntryDto {
    private Long id;
    private String date; // Format: "YYYY-MM-DD"
    private String type; // Entry type
    private String startTime; // Format: "HH:mm" (for working hours)
    private String endTime;   // Format: "HH:mm" (for working hours)
    private String halfDayPeriod; // "AM" or "PM" for half day entries
    private String dateEarned; // Format: "YYYY-MM-DD" (for off_in_lieu)
    private String primaryDocumentDay; // Format: "YYYY-MM-DD"
    private Boolean isPrimaryDocument;
    private String notes;
    private List<DocumentDto> supportingDocuments;
    private String documentReference; // For frontend compatibility
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public DayEntryDto() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public String getPrimaryDocumentDay() { return primaryDocumentDay; }
    public void setPrimaryDocumentDay(String primaryDocumentDay) { this.primaryDocumentDay = primaryDocumentDay; }

    public Boolean getIsPrimaryDocument() { return isPrimaryDocument; }
    public void setIsPrimaryDocument(Boolean isPrimaryDocument) { this.isPrimaryDocument = isPrimaryDocument; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public List<DocumentDto> getSupportingDocuments() { return supportingDocuments; }
    public void setSupportingDocuments(List<DocumentDto> supportingDocuments) { this.supportingDocuments = supportingDocuments; }

    public String getDocumentReference() { return documentReference; }
    public void setDocumentReference(String documentReference) { this.documentReference = documentReference; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
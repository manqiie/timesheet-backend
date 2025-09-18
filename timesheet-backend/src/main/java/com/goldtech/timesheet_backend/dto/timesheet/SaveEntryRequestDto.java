// Updated SaveEntryRequestDto.java - Add support for documents
package com.goldtech.timesheet_backend.dto.timesheet;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

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

    // Add support for documents
    private List<SupportingDocumentDto> supportingDocuments;

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

    public List<SupportingDocumentDto> getSupportingDocuments() { return supportingDocuments; }
    public void setSupportingDocuments(List<SupportingDocumentDto> supportingDocuments) { this.supportingDocuments = supportingDocuments; }

    // Inner class for supporting documents
    public static class SupportingDocumentDto {
        private String name;
        private String type;
        private Long size;
        private String base64Data; // Base64 encoded file content

        public SupportingDocumentDto() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public Long getSize() { return size; }
        public void setSize(Long size) { this.size = size; }

        public String getBase64Data() { return base64Data; }
        public void setBase64Data(String base64Data) { this.base64Data = base64Data; }
    }
}
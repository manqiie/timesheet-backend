// TimesheetValidationService.java - Extract validation logic
package com.goldtech.timesheet_backend.service;

import com.goldtech.timesheet_backend.dto.timesheet.SaveEntryRequestDto;
import com.goldtech.timesheet_backend.entity.DayEntry;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
public class TimesheetValidationService {

    /**
     * Validate working hours with overnight shift support
     */
    public void validateWorkingHours(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Both start and end times are required for working hours");
        }

        // Calculate duration with overnight support
        LocalDateTime startDateTime = LocalDateTime.of(LocalDate.of(2000, 1, 1), startTime);
        LocalDateTime endDateTime = LocalDateTime.of(LocalDate.of(2000, 1, 1), endTime);

        // If end time is before or equal to start time, assume next day (overnight shift)
        if (endTime.isBefore(startTime) || endTime.equals(startTime)) {
            endDateTime = endDateTime.plusDays(1);
        }

        long totalMinutes = java.time.Duration.between(startDateTime, endDateTime).toMinutes();

        if (totalMinutes <= 0) {
            throw new IllegalArgumentException("Invalid time range");
        }


        // Minimum 30 minutes
        if (totalMinutes < 30) {
            throw new IllegalArgumentException("Working hours must be at least 30 minutes");
        }
    }

    /**
     * Validate day entry request
     */
    public void validateSaveEntryRequest(SaveEntryRequestDto request) {
        // Validate date format
        try {
            LocalDate.parse(request.getDate());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format: " + request.getDate());
        }

        // Validate entry type
        try {
            DayEntry.EntryType entryType = DayEntry.EntryType.valueOf(request.getType());

            // Type-specific validations
            if (entryType == DayEntry.EntryType.working_hours) {
                validateWorkingHoursRequest(request);
            } else if (entryType == DayEntry.EntryType.off_in_lieu) {
                validateOffInLieuRequest(request);
            } else if (isHalfDayLeave(entryType)) {
                validateHalfDayRequest(request);
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid entry type: " + request.getType());
        }

        // Validate supporting documents
        if (request.getSupportingDocuments() != null) {
            for (SaveEntryRequestDto.SupportingDocumentDto doc : request.getSupportingDocuments()) {
                validateDocument(doc);
            }
        }
    }

    private void validateWorkingHoursRequest(SaveEntryRequestDto request) {
        if (request.getStartTime() == null || request.getStartTime().trim().isEmpty()) {
            throw new IllegalArgumentException("Start time is required for working hours");
        }
        if (request.getEndTime() == null || request.getEndTime().trim().isEmpty()) {
            throw new IllegalArgumentException("End time is required for working hours");
        }

        try {
            LocalTime startTime = LocalTime.parse(request.getStartTime());
            LocalTime endTime = LocalTime.parse(request.getEndTime());
            validateWorkingHours(startTime, endTime);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid time format or range");
        }
    }

    private void validateOffInLieuRequest(SaveEntryRequestDto request) {
        if (request.getDateEarned() == null || request.getDateEarned().trim().isEmpty()) {
            throw new IllegalArgumentException("Date earned is required for off-in-lieu");
        }

        try {
            LocalDate dateEarned = LocalDate.parse(request.getDateEarned());
            LocalDate entryDate = LocalDate.parse(request.getDate());

            if (dateEarned.isAfter(entryDate)) {
                throw new IllegalArgumentException("Date earned cannot be after the off-in-lieu date");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format for date earned");
        }
    }

    private void validateHalfDayRequest(SaveEntryRequestDto request) {
        if (request.getHalfDayPeriod() == null || request.getHalfDayPeriod().trim().isEmpty()) {
            throw new IllegalArgumentException("Half day period (AM/PM) is required for half-day leave");
        }

        try {
            DayEntry.HalfDayPeriod.valueOf(request.getHalfDayPeriod());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid half day period. Must be AM or PM");
        }
    }

    private boolean isHalfDayLeave(DayEntry.EntryType entryType) {
        return entryType == DayEntry.EntryType.annual_leave_halfday ||
                entryType == DayEntry.EntryType.childcare_leave_halfday ||
                entryType == DayEntry.EntryType.nopay_leave_halfday;
    }

    private void validateDocument(SaveEntryRequestDto.SupportingDocumentDto doc) {
        if (doc.getName() == null || doc.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Document name is required");
        }

        if (doc.getBase64Data() == null || doc.getBase64Data().trim().isEmpty()) {
            throw new IllegalArgumentException("Document content is required");
        }

        if (doc.getSize() == null || doc.getSize() <= 0) {
            throw new IllegalArgumentException("Invalid document size");
        }

        // Validate file size (5MB limit)
        if (doc.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("Document size cannot exceed 5MB");
        }

        // Validate file type
        String fileName = doc.getName().toLowerCase();
        boolean validType = fileName.endsWith(".pdf") || fileName.endsWith(".jpg") ||
                fileName.endsWith(".jpeg") || fileName.endsWith(".png") ||
                fileName.endsWith(".doc") || fileName.endsWith(".docx");

        if (!validType) {
            throw new IllegalArgumentException("Invalid file type. Allowed: PDF, JPG, JPEG, PNG, DOC, DOCX");
        }
    }
}
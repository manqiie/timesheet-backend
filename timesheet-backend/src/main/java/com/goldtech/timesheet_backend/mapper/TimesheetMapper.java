// TimesheetMapper.java - Handle all DTO conversions (Extract from TimesheetService)
package com.goldtech.timesheet_backend.service;

import com.goldtech.timesheet_backend.dto.timesheet.*;
import com.goldtech.timesheet_backend.entity.*;
import com.goldtech.timesheet_backend.repository.DayEntryDocumentRepository;
import com.goldtech.timesheet_backend.repository.DayEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TimesheetMapper {

    @Autowired
    private DayEntryDocumentRepository dayEntryDocumentRepository;

    @Autowired
    private DayEntryRepository dayEntryRepository;

    @Autowired
    private TimesheetStatisticsService statisticsService;

    /**
     * Convert DayEntry entity to DTO
     */
    public DayEntryDto convertToDto(DayEntry entry) {
        DayEntryDto dto = new DayEntryDto();
        dto.setId(entry.getId());
        dto.setDate(entry.getDate().toString());
        dto.setType(entry.getEntryType().toString());

        // Set time fields
        if (entry.getStartTime() != null) dto.setStartTime(entry.getStartTime().toString());
        if (entry.getEndTime() != null) dto.setEndTime(entry.getEndTime().toString());
        if (entry.getHalfDayPeriod() != null) dto.setHalfDayPeriod(entry.getHalfDayPeriod().toString());
        if (entry.getDateEarned() != null) dto.setDateEarned(entry.getDateEarned().toString());

        if (entry.getPrimaryDocumentDay() != null) {
            dto.setPrimaryDocumentDay(entry.getPrimaryDocumentDay().toString());
            dto.setDocumentReference(entry.getPrimaryDocumentDay().toString());
        }

        dto.setIsPrimaryDocument(entry.getIsPrimaryDocument());
        dto.setNotes(entry.getNotes());
        dto.setCreatedAt(entry.getCreatedAt());
        dto.setUpdatedAt(entry.getUpdatedAt());

        // Load and convert documents
        List<DayEntryDocument> documents = dayEntryDocumentRepository.findByDayEntryId(entry.getId());
        if (!documents.isEmpty()) {
            List<DocumentDto> documentDtos = documents.stream()
                    .map(this::convertDocumentToDto)
                    .collect(Collectors.toList());
            dto.setSupportingDocuments(documentDtos);
        }

        return dto;
    }

    /**
     * Convert DayEntryDocument to DTO
     */
    public DocumentDto convertDocumentToDto(DayEntryDocument doc) {
        DocumentDto dto = new DocumentDto();
        dto.setId(doc.getId());
        dto.setName(doc.getOriginalFilename());
        dto.setType(doc.getMimeType());
        dto.setSize(doc.getFileSize());
        dto.setUploadedAt(doc.getUploadedAt());
        return dto;
    }

    /**
     * Convert WorkingHoursPreset to DTO
     */
    public WorkingHoursPresetDto convertPresetToDto(WorkingHoursPreset preset) {
        WorkingHoursPresetDto dto = new WorkingHoursPresetDto();
        dto.setId(preset.getId());
        dto.setName(preset.getName());
        dto.setStartTime(preset.getStartTime().toString());
        dto.setEndTime(preset.getEndTime().toString());
        dto.setIsDefault(preset.getIsDefault());
        dto.setCreatedAt(preset.getCreatedAt());
        dto.setUpdatedAt(preset.getUpdatedAt());
        return dto;
    }

    /**
     * Convert MonthlyTimesheet to TimesheetHistoryDto
     */
    public TimesheetHistoryDto convertToHistoryDto(MonthlyTimesheet timesheet) {
        TimesheetHistoryDto dto = new TimesheetHistoryDto();
        dto.setTimesheetId(timesheet.getId());
        dto.setYear(timesheet.getYear());
        dto.setMonth(timesheet.getMonth());
        dto.setMonthName(statisticsService.getMonthName(timesheet.getMonth()));
        dto.setStatus(timesheet.getStatus().toString());
        dto.setSubmittedAt(timesheet.getSubmittedAt());

        if (timesheet.getApprovedBy() != null) {
            dto.setApprovedBy(timesheet.getApprovedBy().getFullName());
        }
        dto.setApprovedAt(timesheet.getApprovedAt());
        dto.setApprovalComments(timesheet.getApprovalComments());

        // Calculate summary stats
        List<DayEntry> entries = dayEntryRepository.findByUserIdAndYearAndMonth(
                timesheet.getUser().getId(), timesheet.getYear(), timesheet.getMonth());

        dto.setTotalEntries(entries.size());
        dto.setWorkingDays((int) entries.stream()
                .filter(entry -> entry.getEntryType() == DayEntry.EntryType.working_hours)
                .count());
        dto.setLeaveDays((int) entries.stream()
                .filter(entry -> entry.getEntryType() != DayEntry.EntryType.working_hours)
                .count());

        return dto;
    }

    /**
     * Build complete timesheet response DTO
     */
    public TimesheetResponseDto buildTimesheetResponse(MonthlyTimesheet monthlyTimesheet, List<DayEntry> dayEntries) {
        TimesheetResponseDto response = new TimesheetResponseDto();
        response.setTimesheetId(monthlyTimesheet.getId());
        response.setYear(monthlyTimesheet.getYear());
        response.setMonth(monthlyTimesheet.getMonth());
        response.setMonthName(statisticsService.getMonthName(monthlyTimesheet.getMonth()));
        response.setStatus(monthlyTimesheet.getStatus().toString());
        response.setSubmittedAt(monthlyTimesheet.getSubmittedAt());

        if (monthlyTimesheet.getApprovedBy() != null) {
            response.setApprovedBy(monthlyTimesheet.getApprovedBy().getFullName());
        }
        response.setApprovedAt(monthlyTimesheet.getApprovedAt());
        response.setApprovalComments(monthlyTimesheet.getApprovalComments());

        // Convert day entries to map
        Map<String, DayEntryDto> entriesMap = dayEntries.stream()
                .collect(Collectors.toMap(
                        entry -> entry.getDate().toString(),
                        this::convertToDto
                ));
        response.setEntries(entriesMap);

        // Calculate statistics
        response.setStats(statisticsService.calculateStats(dayEntries));

        response.setCreatedAt(monthlyTimesheet.getCreatedAt());
        response.setUpdatedAt(monthlyTimesheet.getUpdatedAt());

        return response;
    }
}
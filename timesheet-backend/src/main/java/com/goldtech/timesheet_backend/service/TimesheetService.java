// src/main/java/com/goldtech/timesheet_backend/service/TimesheetService.java
package com.goldtech.timesheet_backend.service;

import com.goldtech.timesheet_backend.dto.timesheet.*;
import com.goldtech.timesheet_backend.entity.*;
import com.goldtech.timesheet_backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class TimesheetService {

    private static final Logger logger = LoggerFactory.getLogger(TimesheetService.class);

    @Autowired
    private DayEntryRepository dayEntryRepository;

    @Autowired
    private MonthlyTimesheetRepository monthlyTimesheetRepository;

    @Autowired
    private WorkingHoursPresetRepository workingHoursPresetRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DayEntryDocumentRepository dayEntryDocumentRepository;

    @Autowired
    private DocumentStorageService documentStorageService;

    /**
     * Get timesheet data for a specific month
     */
    public TimesheetResponseDto getTimesheet(Long userId, Integer year, Integer month) {
        logger.debug("Getting timesheet for user {} - {}/{}", userId, year, month);

        // Get or create monthly timesheet record
        MonthlyTimesheet monthlyTimesheet = getOrCreateMonthlyTimesheet(userId, year, month);

        // Get day entries for the month
        List<DayEntry> dayEntries = dayEntryRepository.findByUserIdAndYearAndMonth(userId, year, month);

        // Convert to response DTO
        TimesheetResponseDto response = new TimesheetResponseDto();
        response.setTimesheetId(monthlyTimesheet.getId());
        response.setYear(year);
        response.setMonth(month);
        response.setMonthName(getMonthName(month));
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
        response.setStats(calculateStats(dayEntries));

        response.setCreatedAt(monthlyTimesheet.getCreatedAt());
        response.setUpdatedAt(monthlyTimesheet.getUpdatedAt());

        return response;
    }

    /**
     * Save a single day entry with document support
     */
    public DayEntryDto saveDayEntry(Long userId, SaveEntryRequestDto request) {
        logger.debug("Saving day entry for user {} on {}", userId, request.getDate());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        LocalDate date = LocalDate.parse(request.getDate());

        // Check if entry already exists
        Optional<DayEntry> existingEntry = dayEntryRepository.findByUserIdAndDate(userId, date);

        DayEntry dayEntry;
        boolean isNewEntry = false;

        if (existingEntry.isPresent()) {
            dayEntry = existingEntry.get();
            // Delete existing documents if updating
            documentStorageService.deleteDocuments(dayEntry.getId());
        } else {
            dayEntry = new DayEntry();
            dayEntry.setUser(user);
            dayEntry.setDate(date);
            isNewEntry = true;
        }

        // Update entry fields
        updateDayEntryFromRequest(dayEntry, request);

        // Save the entry first to get the ID
        dayEntry = dayEntryRepository.save(dayEntry);

        // Save supporting documents if provided
        if (request.getSupportingDocuments() != null && !request.getSupportingDocuments().isEmpty()) {
            List<DocumentStorageService.DocumentUploadDto> documents = request.getSupportingDocuments().stream()
                    .map(doc -> new DocumentStorageService.DocumentUploadDto(
                            doc.getName(), doc.getType(), doc.getSize(), doc.getBase64Data()))
                    .collect(Collectors.toList());

            documentStorageService.saveDocuments(dayEntry, documents);
        }

        // Update monthly timesheet status to draft if it was submitted
        updateMonthlyTimesheetToDraft(userId, date.getYear(), date.getMonthValue());

        logger.info("Day entry {} for user {} on {} with {} documents",
                isNewEntry ? "created" : "updated", userId, request.getDate(),
                request.getSupportingDocuments() != null ? request.getSupportingDocuments().size() : 0);

        return convertToDto(dayEntry);
    }

    /**
     * Save multiple day entries with document support
     */
    public List<DayEntryDto> saveBulkEntries(Long userId, List<SaveEntryRequestDto> requests) {
        logger.debug("Saving {} bulk entries for user {}", requests.size(), userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<DayEntry> savedEntries = new ArrayList<>();

        for (SaveEntryRequestDto request : requests) {
            LocalDate date = LocalDate.parse(request.getDate());

            // Check if entry already exists
            Optional<DayEntry> existingEntry = dayEntryRepository.findByUserIdAndDate(userId, date);

            DayEntry dayEntry;
            if (existingEntry.isPresent()) {
                dayEntry = existingEntry.get();
                // Delete existing documents if updating
                documentStorageService.deleteDocuments(dayEntry.getId());
            } else {
                dayEntry = new DayEntry();
                dayEntry.setUser(user);
                dayEntry.setDate(date);
            }

            // Update entry fields
            updateDayEntryFromRequest(dayEntry, request);
            savedEntries.add(dayEntry);
        }

        // Save all entries first
        savedEntries = dayEntryRepository.saveAll(savedEntries);

        // Save documents for entries that have them
        for (int i = 0; i < requests.size(); i++) {
            SaveEntryRequestDto request = requests.get(i);
            DayEntry dayEntry = savedEntries.get(i);

            if (request.getSupportingDocuments() != null && !request.getSupportingDocuments().isEmpty()) {
                List<DocumentStorageService.DocumentUploadDto> documents = request.getSupportingDocuments().stream()
                        .map(doc -> new DocumentStorageService.DocumentUploadDto(
                                doc.getName(), doc.getType(), doc.getSize(), doc.getBase64Data()))
                        .collect(Collectors.toList());

                documentStorageService.saveDocuments(dayEntry, documents);
            }
        }

        // Update monthly timesheet status to draft if needed
        if (!requests.isEmpty()) {
            LocalDate firstDate = LocalDate.parse(requests.get(0).getDate());
            updateMonthlyTimesheetToDraft(userId, firstDate.getYear(), firstDate.getMonthValue());
        }

        int totalDocuments = requests.stream()
                .mapToInt(req -> req.getSupportingDocuments() != null ? req.getSupportingDocuments().size() : 0)
                .sum();

        logger.info("Saved {} bulk entries for user {} with {} total documents",
                savedEntries.size(), userId, totalDocuments);

        return savedEntries.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    /**
     * Submit timesheet for approval
     */
    public TimesheetResponseDto submitTimesheet(Long userId, Integer year, Integer month) {
        logger.debug("Submitting timesheet for user {} - {}/{}", userId, year, month);

        MonthlyTimesheet monthlyTimesheet = monthlyTimesheetRepository
                .findByUserIdAndYearAndMonth(userId, year, month)
                .orElseThrow(() -> new IllegalArgumentException("Timesheet not found"));

        // Validation: Check if timesheet has sufficient entries
        long entryCount = dayEntryRepository.countByUserIdAndYearAndMonth(userId, year, month);
        if (entryCount == 0) {
            throw new IllegalArgumentException("Cannot submit empty timesheet");
        }

        // Update status and submission time
        monthlyTimesheet.setStatus(MonthlyTimesheet.TimesheetStatus.submitted);
        monthlyTimesheet.setSubmittedAt(LocalDateTime.now());

        // Clear previous approval data
        monthlyTimesheet.setApprovedBy(null);
        monthlyTimesheet.setApprovedAt(null);
        monthlyTimesheet.setApprovalComments(null);

        monthlyTimesheet = monthlyTimesheetRepository.save(monthlyTimesheet);

        logger.info("Timesheet submitted for user {} - {}/{}", userId, year, month);
        return getTimesheet(userId, year, month);
    }

    /**
     * Get working hours presets for a user
     */
    public List<WorkingHoursPresetDto> getWorkingHoursPresets(Long userId) {
        List<WorkingHoursPreset> presets = workingHoursPresetRepository.findByUserIdOrderByCreatedAtAsc(userId);
        return presets.stream().map(this::convertPresetToDto).collect(Collectors.toList());
    }

    /**
     * Save working hours preset
     */
    public WorkingHoursPresetDto saveWorkingHoursPreset(Long userId, String name, String startTime, String endTime) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        WorkingHoursPreset preset = new WorkingHoursPreset();
        preset.setUser(user);
        preset.setName(name);
        preset.setStartTime(LocalTime.parse(startTime));
        preset.setEndTime(LocalTime.parse(endTime));
        preset.setIsDefault(false);

        preset = workingHoursPresetRepository.save(preset);
        logger.info("Working hours preset saved for user {}: {}", userId, name);

        return convertPresetToDto(preset);
    }

    /**
     * Delete working hours preset
     */
    public void deleteWorkingHoursPreset(Long userId, Long presetId) {
        workingHoursPresetRepository.deleteByIdAndUserId(presetId, userId);
        logger.info("Working hours preset deleted for user {}: {}", userId, presetId);
    }

    /**
     * Delete a day entry and its documents
     */
    public void deleteDayEntry(Long userId, String date) {
        LocalDate entryDate = LocalDate.parse(date);
        Optional<DayEntry> entry = dayEntryRepository.findByUserIdAndDate(userId, entryDate);

        if (entry.isPresent()) {
            DayEntry dayEntry = entry.get();

            // Delete associated documents
            documentStorageService.deleteDocuments(dayEntry.getId());

            // Delete the entry
            dayEntryRepository.delete(dayEntry);

            // Update monthly timesheet status to draft if it was submitted
            updateMonthlyTimesheetToDraft(userId, entryDate.getYear(), entryDate.getMonthValue());

            logger.info("Day entry and documents deleted for user {} on {}", userId, date);
        }
    }

    /**
     * Get timesheet statistics for a month
     */
    public TimesheetStatsDto getTimesheetStats(Long userId, Integer year, Integer month) {
        List<DayEntry> entries = dayEntryRepository.findByUserIdAndYearAndMonth(userId, year, month);
        return calculateStats(entries);
    }

    // Private helper methods

    private MonthlyTimesheet getOrCreateMonthlyTimesheet(Long userId, Integer year, Integer month) {
        return monthlyTimesheetRepository.findByUserIdAndYearAndMonth(userId, year, month)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("User not found"));

                    MonthlyTimesheet newTimesheet = new MonthlyTimesheet();
                    newTimesheet.setUser(user);
                    newTimesheet.setYear(year);
                    newTimesheet.setMonth(month);
                    newTimesheet.setStatus(MonthlyTimesheet.TimesheetStatus.draft);

                    return monthlyTimesheetRepository.save(newTimesheet);
                });
    }

    private void updateDayEntryFromRequest(DayEntry dayEntry, SaveEntryRequestDto request) {
        // Set entry type
        dayEntry.setEntryType(DayEntry.EntryType.valueOf(request.getType()));

        // Set working hours fields
        if (request.getStartTime() != null && !request.getStartTime().isEmpty()) {
            dayEntry.setStartTime(LocalTime.parse(request.getStartTime()));
        } else {
            dayEntry.setStartTime(null);
        }

        if (request.getEndTime() != null && !request.getEndTime().isEmpty()) {
            dayEntry.setEndTime(LocalTime.parse(request.getEndTime()));
        } else {
            dayEntry.setEndTime(null);
        }

        // Set half day period
        if (request.getHalfDayPeriod() != null && !request.getHalfDayPeriod().isEmpty()) {
            dayEntry.setHalfDayPeriod(DayEntry.HalfDayPeriod.valueOf(request.getHalfDayPeriod()));
        } else {
            dayEntry.setHalfDayPeriod(null);
        }

        // Set date earned (for off_in_lieu)
        if (request.getDateEarned() != null && !request.getDateEarned().isEmpty()) {
            dayEntry.setDateEarned(LocalDate.parse(request.getDateEarned()));
        } else {
            dayEntry.setDateEarned(null);
        }

        // Set document reference fields
        if (request.getPrimaryDocumentDay() != null && !request.getPrimaryDocumentDay().isEmpty()) {
            dayEntry.setPrimaryDocumentDay(LocalDate.parse(request.getPrimaryDocumentDay()));
        }

        if (request.getIsPrimaryDocument() != null) {
            dayEntry.setIsPrimaryDocument(request.getIsPrimaryDocument());
        }

        // Set notes
        dayEntry.setNotes(request.getNotes());
    }

    private void updateMonthlyTimesheetToDraft(Long userId, Integer year, Integer month) {
        monthlyTimesheetRepository.findByUserIdAndYearAndMonth(userId, year, month)
                .ifPresent(timesheet -> {
                    if (timesheet.getStatus() == MonthlyTimesheet.TimesheetStatus.submitted ||
                            timesheet.getStatus() == MonthlyTimesheet.TimesheetStatus.pending) {
                        timesheet.setStatus(MonthlyTimesheet.TimesheetStatus.draft);
                        timesheet.setSubmittedAt(null);
                        monthlyTimesheetRepository.save(timesheet);
                    }
                });
    }

    /**
     * Convert DayEntry to DTO with document information
     */
    private DayEntryDto convertToDto(DayEntry entry) {
        DayEntryDto dto = new DayEntryDto();
        dto.setId(entry.getId());
        dto.setDate(entry.getDate().toString());
        dto.setType(entry.getEntryType().toString());

        if (entry.getStartTime() != null) {
            dto.setStartTime(entry.getStartTime().toString());
        }
        if (entry.getEndTime() != null) {
            dto.setEndTime(entry.getEndTime().toString());
        }
        if (entry.getHalfDayPeriod() != null) {
            dto.setHalfDayPeriod(entry.getHalfDayPeriod().toString());
        }
        if (entry.getDateEarned() != null) {
            dto.setDateEarned(entry.getDateEarned().toString());
        }
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
                    .map(doc -> {
                        DocumentDto docDto = new DocumentDto();
                        docDto.setId(doc.getId());
                        docDto.setName(doc.getOriginalFilename());
                        docDto.setType(doc.getMimeType());
                        docDto.setSize(doc.getFileSize());
                        docDto.setUploadedAt(doc.getUploadedAt());
                        return docDto;
                    })
                    .collect(Collectors.toList());
            dto.setSupportingDocuments(documentDtos);
        }

        return dto;
    }

    private WorkingHoursPresetDto convertPresetToDto(WorkingHoursPreset preset) {
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

    private TimesheetStatsDto calculateStats(List<DayEntry> entries) {
        TimesheetStatsDto stats = new TimesheetStatsDto();

        stats.setTotalEntries(entries.size());

        long workingDays = entries.stream()
                .filter(entry -> entry.getEntryType() == DayEntry.EntryType.working_hours)
                .count();
        stats.setWorkingDays((int) workingDays);

        long leaveDays = entries.stream()
                .filter(entry -> entry.getEntryType() != DayEntry.EntryType.working_hours)
                .count();
        stats.setLeaveDays((int) leaveDays);

        // Calculate total hours for working days
        double totalHours = entries.stream()
                .filter(entry -> entry.getEntryType() == DayEntry.EntryType.working_hours)
                .filter(entry -> entry.getStartTime() != null && entry.getEndTime() != null)
                .mapToDouble(entry -> {
                    LocalTime start = entry.getStartTime();
                    LocalTime end = entry.getEndTime();
                    return java.time.Duration.between(start, end).toMinutes() / 60.0;
                })
                .sum();
        stats.setTotalHours(totalHours);

        // Calculate leave breakdown
        Map<String, Integer> leaveBreakdown = entries.stream()
                .filter(entry -> entry.getEntryType() != DayEntry.EntryType.working_hours)
                .collect(Collectors.groupingBy(
                        entry -> entry.getEntryType().toString(),
                        Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
                ));
        stats.setLeaveBreakdown(leaveBreakdown);

        return stats;
    }

    private String getMonthName(Integer month) {
        return LocalDate.of(2024, month, 1)
                .getMonth()
                .getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }
}
// Updated TimesheetService.java - With submission rules and history
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
     * Get available months for timesheet submission based on business rules
     * Current month + 10 days into next month for previous month
     */
    public List<AvailableMonthDto> getAvailableMonths(Long userId) {
        LocalDate today = LocalDate.now();
        List<AvailableMonthDto> availableMonths = new ArrayList<>();

        // Current month - always available
        AvailableMonthDto currentMonth = new AvailableMonthDto();
        currentMonth.setYear(today.getYear());
        currentMonth.setMonth(today.getMonthValue());
        currentMonth.setMonthName(getMonthName(today.getMonthValue()));
        currentMonth.setIsCurrentMonth(true);

        // Check if current month is submitted
        Optional<MonthlyTimesheet> currentTimesheet = monthlyTimesheetRepository
                .findByUserIdAndYearAndMonth(userId, today.getYear(), today.getMonthValue());
        currentMonth.setIsSubmitted(currentTimesheet.isPresent() &&
                currentTimesheet.get().getStatus() != MonthlyTimesheet.TimesheetStatus.draft);

        availableMonths.add(currentMonth);

        // Previous month - available only if within 10 days of current month
        if (today.getDayOfMonth() <= 10) {
            LocalDate previousMonth = today.minusMonths(1);

            Optional<MonthlyTimesheet> prevTimesheet = monthlyTimesheetRepository
                    .findByUserIdAndYearAndMonth(userId, previousMonth.getYear(), previousMonth.getMonthValue());

            // Only show if not submitted or if rejected (can resubmit)
            boolean canShowPrevious = true;
            if (prevTimesheet.isPresent()) {
                MonthlyTimesheet.TimesheetStatus status = prevTimesheet.get().getStatus();
                canShowPrevious = status == MonthlyTimesheet.TimesheetStatus.draft ||
                        status == MonthlyTimesheet.TimesheetStatus.rejected;
            }

            if (canShowPrevious) {
                AvailableMonthDto prevMonth = new AvailableMonthDto();
                prevMonth.setYear(previousMonth.getYear());
                prevMonth.setMonth(previousMonth.getMonthValue());
                prevMonth.setMonthName(getMonthName(previousMonth.getMonthValue()));
                prevMonth.setIsCurrentMonth(false);
                prevMonth.setIsSubmitted(prevTimesheet.isPresent() &&
                        prevTimesheet.get().getStatus() != MonthlyTimesheet.TimesheetStatus.draft);

                availableMonths.add(0, prevMonth); // Add at beginning
            }
        }

        return availableMonths;
    }

    /**
     * Get timesheet history for a user
     */
    public List<TimesheetHistoryDto> getTimesheetHistory(Long userId) {
        logger.debug("Getting timesheet history for user {}", userId);

        List<MonthlyTimesheet> timesheets = monthlyTimesheetRepository
                .findByUserIdOrderByYearDescMonthDesc(userId);

        // Only return submitted timesheets (exclude drafts)
        List<MonthlyTimesheet> submittedTimesheets = timesheets.stream()
                .filter(ts -> ts.getStatus() != MonthlyTimesheet.TimesheetStatus.draft)
                .collect(Collectors.toList());

        return submittedTimesheets.stream()
                .map(this::convertToHistoryDto)
                .collect(Collectors.toList());
    }

    /**
     * Check if a timesheet can be submitted based on business rules
     */
    public boolean canSubmitTimesheet(Long userId, Integer year, Integer month) {
        LocalDate today = LocalDate.now();
        LocalDate timesheetMonth = LocalDate.of(year, month, 1);

        // Current month - always allowed
        if (timesheetMonth.getYear() == today.getYear() &&
                timesheetMonth.getMonthValue() == today.getMonthValue()) {
            return true;
        }

        // Previous month - only allowed within first 10 days of current month
        if (timesheetMonth.getYear() == today.getYear() &&
                timesheetMonth.getMonthValue() == today.getMonthValue() - 1) {
            return today.getDayOfMonth() <= 10;
        }

        // Previous year December - only allowed within first 10 days of January
        if (timesheetMonth.getYear() == today.getYear() - 1 &&
                timesheetMonth.getMonthValue() == 12 &&
                today.getMonthValue() == 1) {
            return today.getDayOfMonth() <= 10;
        }

        return false; // All other cases not allowed
    }

    /**
     * Check if a timesheet can be resubmitted (after rejection)
     */
    public boolean canResubmitTimesheet(Long userId, Integer year, Integer month) {
        Optional<MonthlyTimesheet> timesheetOpt = monthlyTimesheetRepository
                .findByUserIdAndYearAndMonth(userId, year, month);

        return timesheetOpt.isPresent() &&
                timesheetOpt.get().getStatus() == MonthlyTimesheet.TimesheetStatus.rejected;
    }

    /**
     * Submit timesheet for approval with validation
     */
    public TimesheetResponseDto submitTimesheet(Long userId, Integer year, Integer month) {
        logger.debug("Submitting timesheet for user {} - {}/{}", userId, year, month);

        MonthlyTimesheet monthlyTimesheet = monthlyTimesheetRepository
                .findByUserIdAndYearAndMonth(userId, year, month)
                .orElseThrow(() -> new IllegalArgumentException("Timesheet not found"));

        // Check submission rules
        boolean canSubmit = canSubmitTimesheet(userId, year, month);
        boolean canResubmit = canResubmitTimesheet(userId, year, month);

        if (!canSubmit && !canResubmit) {
            LocalDate today = LocalDate.now();
            if (today.getDayOfMonth() > 10) {
                throw new IllegalArgumentException(
                        "Previous month timesheet can only be submitted within the first 10 days of the current month");
            } else {
                throw new IllegalArgumentException("This timesheet cannot be submitted at this time");
            }
        }

        // Validation: Check if timesheet has sufficient entries
        long entryCount = dayEntryRepository.countByUserIdAndYearAndMonth(userId, year, month);
        if (entryCount == 0) {
            throw new IllegalArgumentException("Cannot submit empty timesheet");
        }

        // Get the employee's supervisor
        User employee = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (employee.getSupervisor() == null) {
            throw new IllegalArgumentException("Cannot submit timesheet: No supervisor assigned to this employee");
        }

        // Update status and submission details
        monthlyTimesheet.setStatus(MonthlyTimesheet.TimesheetStatus.submitted);
        monthlyTimesheet.setSubmittedAt(LocalDateTime.now());

        // Set approved_by to the employee's supervisor when submitting
        monthlyTimesheet.setApprovedBy(employee.getSupervisor());

        // Clear approval timestamp and comments (in case of resubmission)
        monthlyTimesheet.setApprovedAt(null);
        monthlyTimesheet.setApprovalComments(null);

        monthlyTimesheet = monthlyTimesheetRepository.save(monthlyTimesheet);

        logger.info("Timesheet {} for user {} - {}/{}. Assigned to supervisor: {} (ID: {})",
                canResubmit ? "resubmitted" : "submitted",
                userId, year, month,
                employee.getSupervisor().getFullName(),
                employee.getSupervisor().getId());

        return getTimesheet(userId, year, month);
    }

    /**
     * Get timesheet data for a specific month (existing method - no changes)
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
     * Save a single day entry with document support (existing method - no changes)
     */
    public DayEntryDto saveDayEntry(Long userId, SaveEntryRequestDto request) {
        logger.debug("Saving day entry for user {} on {}", userId, request.getDate());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        LocalDate date = LocalDate.parse(request.getDate());

        // Check if timesheet is already submitted/approved (prevent editing)
        Optional<MonthlyTimesheet> monthlyTimesheet = monthlyTimesheetRepository
                .findByUserIdAndYearAndMonth(userId, date.getYear(), date.getMonthValue());

        if (monthlyTimesheet.isPresent() &&
                (monthlyTimesheet.get().getStatus() == MonthlyTimesheet.TimesheetStatus.submitted ||
                        monthlyTimesheet.get().getStatus() == MonthlyTimesheet.TimesheetStatus.pending ||
                        monthlyTimesheet.get().getStatus() == MonthlyTimesheet.TimesheetStatus.approved)) {
            throw new IllegalArgumentException("Cannot edit submitted or approved timesheet");
        }

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
     * Save multiple day entries with document support (existing method - no changes needed)
     */
    public List<DayEntryDto> saveBulkEntries(Long userId, List<SaveEntryRequestDto> requests) {
        logger.debug("Saving {} bulk entries for user {}", requests.size(), userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Check if any of the dates belong to submitted/approved timesheets
        for (SaveEntryRequestDto request : requests) {
            LocalDate date = LocalDate.parse(request.getDate());
            Optional<MonthlyTimesheet> monthlyTimesheet = monthlyTimesheetRepository
                    .findByUserIdAndYearAndMonth(userId, date.getYear(), date.getMonthValue());

            if (monthlyTimesheet.isPresent() &&
                    (monthlyTimesheet.get().getStatus() == MonthlyTimesheet.TimesheetStatus.submitted ||
                            monthlyTimesheet.get().getStatus() == MonthlyTimesheet.TimesheetStatus.pending ||
                            monthlyTimesheet.get().getStatus() == MonthlyTimesheet.TimesheetStatus.approved)) {
                throw new IllegalArgumentException("Cannot edit submitted or approved timesheet for date: " + request.getDate());
            }
        }

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
     * Validate working hours with overnight shift support
     */
    private void validateWorkingHours(LocalTime startTime, LocalTime endTime) {
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

        // Maximum 16 hours per shift
        if (totalMinutes > 16 * 60) {
            throw new IllegalArgumentException("Working hours cannot exceed 16 hours per shift");
        }

        // Minimum 30 minutes
        if (totalMinutes < 30) {
            throw new IllegalArgumentException("Working hours must be at least 30 minutes");
        }
    }

    /**
     * Convert MonthlyTimesheet to TimesheetHistoryDto
     */
    private TimesheetHistoryDto convertToHistoryDto(MonthlyTimesheet timesheet) {
        TimesheetHistoryDto dto = new TimesheetHistoryDto();
        dto.setTimesheetId(timesheet.getId());
        dto.setYear(timesheet.getYear());
        dto.setMonth(timesheet.getMonth());
        dto.setMonthName(getMonthName(timesheet.getMonth()));
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

    // [Keep all existing private helper methods unchanged]
    // getOrCreateMonthlyTimesheet, updateDayEntryFromRequest, updateMonthlyTimesheetToDraft,
    // convertToDto, convertPresetToDto, calculateStats, getMonthName

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

    /**
     * Update the updateDayEntryFromRequest method to include validation
     */
    private void updateDayEntryFromRequest(DayEntry dayEntry, SaveEntryRequestDto request) {
        // Set entry type
        dayEntry.setEntryType(DayEntry.EntryType.valueOf(request.getType()));

        // Set working hours fields with validation
        if (request.getStartTime() != null && !request.getStartTime().isEmpty()) {
            LocalTime startTime = LocalTime.parse(request.getStartTime());
            dayEntry.setStartTime(startTime);

            if (request.getEndTime() != null && !request.getEndTime().isEmpty()) {
                LocalTime endTime = LocalTime.parse(request.getEndTime());
                dayEntry.setEndTime(endTime);

                // Validate working hours if both times are provided and it's a working hours entry
                if (dayEntry.getEntryType() == DayEntry.EntryType.working_hours) {
                    validateWorkingHours(startTime, endTime);
                }
            }
        } else {
            dayEntry.setStartTime(null);
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

    /**
     * Calculate statistics with support for overnight shifts
     */
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

        // Calculate total hours for working days with overnight shift support
        double totalHours = entries.stream()
                .filter(entry -> entry.getEntryType() == DayEntry.EntryType.working_hours)
                .filter(entry -> entry.getStartTime() != null && entry.getEndTime() != null)
                .mapToDouble(entry -> {
                    LocalTime start = entry.getStartTime();
                    LocalTime end = entry.getEndTime();

                    // Calculate duration with overnight shift support
                    LocalDateTime startDateTime = LocalDateTime.of(LocalDate.of(2000, 1, 1), start);
                    LocalDateTime endDateTime = LocalDateTime.of(LocalDate.of(2000, 1, 1), end);

                    // If end time is before or equal to start time, assume next day (overnight shift)
                    if (end.isBefore(start) || end.equals(start)) {
                        endDateTime = endDateTime.plusDays(1);
                    }

                    long minutes = java.time.Duration.between(startDateTime, endDateTime).toMinutes();
                    return minutes / 60.0;
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

    // Keep existing methods: getWorkingHoursPresets, saveWorkingHoursPreset,
    // deleteWorkingHoursPreset, deleteDayEntry, getTimesheetStats...

    public List<WorkingHoursPresetDto> getWorkingHoursPresets(Long userId) {
        List<WorkingHoursPreset> presets = workingHoursPresetRepository.findByUserIdOrderByCreatedAtAsc(userId);
        return presets.stream().map(this::convertPresetToDto).collect(Collectors.toList());
    }

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

    public void deleteWorkingHoursPreset(Long userId, Long presetId) {
        workingHoursPresetRepository.deleteByIdAndUserId(presetId, userId);
        logger.info("Working hours preset deleted for user {}: {}", userId, presetId);
    }

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

    public TimesheetStatsDto getTimesheetStats(Long userId, Integer year, Integer month) {
        List<DayEntry> entries = dayEntryRepository.findByUserIdAndYearAndMonth(userId, year, month);
        return calculateStats(entries);
    }
}
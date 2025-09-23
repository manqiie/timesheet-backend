// Complete Refactored TimesheetService.java - Works with all new services
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class TimesheetService {

    private static final Logger logger = LoggerFactory.getLogger(TimesheetService.class);

    // Repositories
    @Autowired private DayEntryRepository dayEntryRepository;
    @Autowired private MonthlyTimesheetRepository monthlyTimesheetRepository;
    @Autowired private WorkingHoursPresetRepository workingHoursPresetRepository;
    @Autowired private UserRepository userRepository;
    //mapper
    @Autowired private TimesheetMapper timesheetMapper;
    //service
    @Autowired private TimesheetBusinessRulesService businessRulesService;
    @Autowired private TimesheetValidationService validationService;
    @Autowired private TimesheetStatisticsService statisticsService;
    @Autowired private TimesheetEntryService entryService;

    // ========== PUBLIC API METHODS ==========

    /**
     * Get available months for timesheet submission
     */
    public List<AvailableMonthDto> getAvailableMonths(Long userId) {
        return statisticsService.getAvailableMonths(userId);
    }

    /**
     * Get timesheet history for a user
     */
    public List<TimesheetHistoryDto> getTimesheetHistory(Long userId) {
        logger.debug("Getting timesheet history for user {}", userId);

        List<MonthlyTimesheet> timesheets = monthlyTimesheetRepository
                .findByUserIdOrderByYearDescMonthDesc(userId);

        return timesheets.stream()
                .filter(ts -> ts.getStatus() != MonthlyTimesheet.TimesheetStatus.draft)
                .map(timesheetMapper::convertToHistoryDto)
                .collect(Collectors.toList());
    }

    /**
     * Check if timesheet can be submitted
     */
    public boolean canSubmitTimesheet(Long userId, Integer year, Integer month) {
        return businessRulesService.canSubmitTimesheet(userId, year, month);
    }

    /**
     * Check if timesheet can be resubmitted
     */
    public boolean canResubmitTimesheet(Long userId, Integer year, Integer month) {
        return businessRulesService.canResubmitTimesheet(userId, year, month);
    }

    /**
     * Get timesheet data for a specific month
     */
    public TimesheetResponseDto getTimesheet(Long userId, Integer year, Integer month) {
        logger.debug("Getting timesheet for user {} - {}/{}", userId, year, month);

        MonthlyTimesheet monthlyTimesheet = getOrCreateMonthlyTimesheet(userId, year, month);
        List<DayEntry> dayEntries = dayEntryRepository.findByUserIdAndYearAndMonth(userId, year, month);

        return timesheetMapper.buildTimesheetResponse(monthlyTimesheet, dayEntries);
    }

    /**
     * Save a single day entry
     */
    public DayEntryDto saveDayEntry(Long userId, SaveEntryRequestDto request) {
        logger.debug("Saving day entry for user {} on {}", userId, request.getDate());

        // Validate request
        validationService.validateSaveEntryRequest(request);

        User user = getUserById(userId);
        LocalDate date = LocalDate.parse(request.getDate());

        // Check if timesheet can be edited
        validateTimesheetCanBeEdited(userId, date);

        // Get or create entry
        DayEntry dayEntry = entryService.getOrCreateDayEntry(user, date);
        boolean isNewEntry = dayEntry.getId() == null;

        // Delete existing documents if updating
        if (!isNewEntry) {
            entryService.deleteExistingDocuments(dayEntry);
        }

        // Update entry
        entryService.updateDayEntryFromRequest(dayEntry, request);
        dayEntry = dayEntryRepository.save(dayEntry);

        // Save documents
        entryService.saveDocuments(dayEntry, request.getSupportingDocuments());

        // Update timesheet status
        updateMonthlyTimesheetToDraft(userId, date.getYear(), date.getMonthValue());

        logger.info("Day entry {} for user {} on {} with {} documents",
                isNewEntry ? "created" : "updated", userId, request.getDate(),
                request.getSupportingDocuments() != null ? request.getSupportingDocuments().size() : 0);

        return timesheetMapper.convertToDto(dayEntry);
    }

    /**
     * Save multiple day entries
     */
    public List<DayEntryDto> saveBulkEntries(Long userId, List<SaveEntryRequestDto> requests) {
        logger.debug("Saving {} bulk entries for user {}", requests.size(), userId);

        User user = getUserById(userId);
        List<DayEntry> savedEntries = new ArrayList<>();

        // Validate all requests first
        for (SaveEntryRequestDto request : requests) {
            validationService.validateSaveEntryRequest(request);
            LocalDate date = LocalDate.parse(request.getDate());
            validateTimesheetCanBeEdited(userId, date);
        }

        // Process each entry
        for (SaveEntryRequestDto request : requests) {
            LocalDate date = LocalDate.parse(request.getDate());
            DayEntry dayEntry = entryService.getOrCreateDayEntry(user, date);

            if (dayEntry.getId() != null) {
                entryService.deleteExistingDocuments(dayEntry);
            }

            entryService.updateDayEntryFromRequest(dayEntry, request);
            savedEntries.add(dayEntry);
        }

        // Save all entries
        savedEntries = dayEntryRepository.saveAll(savedEntries);

        // Save documents
        for (int i = 0; i < requests.size(); i++) {
            entryService.saveDocuments(savedEntries.get(i), requests.get(i).getSupportingDocuments());
        }

        // Update timesheet status
        if (!requests.isEmpty()) {
            LocalDate firstDate = LocalDate.parse(requests.get(0).getDate());
            updateMonthlyTimesheetToDraft(userId, firstDate.getYear(), firstDate.getMonthValue());
        }

        logger.info("Saved {} bulk entries for user {}", savedEntries.size(), userId);
        return savedEntries.stream()
                .map(timesheetMapper::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Submit timesheet for approval
     */
    public TimesheetResponseDto submitTimesheet(Long userId, Integer year, Integer month) {
        logger.debug("Submitting timesheet for user {} - {}/{}", userId, year, month);

        MonthlyTimesheet monthlyTimesheet = monthlyTimesheetRepository
                .findByUserIdAndYearAndMonth(userId, year, month)
                .orElseThrow(() -> new IllegalArgumentException("Timesheet not found"));

        // Validate submission rules
        boolean canSubmit = businessRulesService.canSubmitTimesheet(userId, year, month);
        boolean canResubmit = businessRulesService.canResubmitTimesheet(userId, year, month);

        if (!canSubmit && !canResubmit) {
            throw new IllegalArgumentException(
                    businessRulesService.getSubmissionDeadlineMessage(year, month));
        }

        // Validate timesheet content
        long entryCount = dayEntryRepository.countByUserIdAndYearAndMonth(userId, year, month);
        businessRulesService.validateTimesheetForSubmission(userId, year, month, entryCount);

        // Get supervisor
        User employee = getUserById(userId);
        if (employee.getSupervisor() == null) {
            throw new IllegalArgumentException("Cannot submit timesheet: No supervisor assigned");
        }

        // Update timesheet status
        monthlyTimesheet.setStatus(MonthlyTimesheet.TimesheetStatus.submitted);
        monthlyTimesheet.setSubmittedAt(LocalDateTime.now());
        monthlyTimesheet.setApprovedBy(employee.getSupervisor());
        monthlyTimesheet.setApprovedAt(null);
        monthlyTimesheet.setApprovalComments(null);

        monthlyTimesheetRepository.save(monthlyTimesheet);

        logger.info("Timesheet {} for user {} - {}/{}. Assigned to supervisor: {}",
                canResubmit ? "resubmitted" : "submitted", userId, year, month,
                employee.getSupervisor().getFullName());

        return getTimesheet(userId, year, month);
    }

    /**
     * Delete a day entry
     */
    public void deleteDayEntry(Long userId, String date) {
        LocalDate entryDate = LocalDate.parse(date);
        validateTimesheetCanBeEdited(userId, entryDate.getYear(), entryDate.getMonthValue());

        entryService.deleteDayEntry(userId, entryDate);
        updateMonthlyTimesheetToDraft(userId, entryDate.getYear(), entryDate.getMonthValue());
    }

    /**
     * Get timesheet statistics
     */
    public TimesheetStatsDto getTimesheetStats(Long userId, Integer year, Integer month) {
        List<DayEntry> entries = dayEntryRepository.findByUserIdAndYearAndMonth(userId, year, month);
        return statisticsService.calculateStats(entries);
    }

    /**
     * Get working hours presets for user
     */
    public List<WorkingHoursPresetDto> getWorkingHoursPresets(Long userId) {
        List<WorkingHoursPreset> presets = workingHoursPresetRepository.findByUserIdOrderByCreatedAtAsc(userId);
        return presets.stream()
                .map(timesheetMapper::convertPresetToDto)
                .collect(Collectors.toList());
    }

    /**
     * Save working hours preset
     */
    public WorkingHoursPresetDto saveWorkingHoursPreset(Long userId, String name, String startTime, String endTime) {
        User user = getUserById(userId);

        WorkingHoursPreset preset = new WorkingHoursPreset();
        preset.setUser(user);
        preset.setName(name);
        preset.setStartTime(LocalTime.parse(startTime));
        preset.setEndTime(LocalTime.parse(endTime));
        preset.setIsDefault(false);

        preset = workingHoursPresetRepository.save(preset);
        logger.info("Working hours preset saved for user {}: {}", userId, name);

        return timesheetMapper.convertPresetToDto(preset);
    }

    /**
     * Delete working hours preset
     */
    public void deleteWorkingHoursPreset(Long userId, Long presetId) {
        workingHoursPresetRepository.deleteByIdAndUserId(presetId, userId);
        logger.info("Working hours preset deleted for user {}: {}", userId, presetId);
    }

    // ========== PRIVATE HELPER METHODS ==========

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private void validateTimesheetCanBeEdited(Long userId, LocalDate date) {
        validateTimesheetCanBeEdited(userId, date.getYear(), date.getMonthValue());
    }

    private void validateTimesheetCanBeEdited(Long userId, Integer year, Integer month) {
        if (!businessRulesService.canEditTimesheet(userId, year, month)) {
            throw new IllegalArgumentException("Cannot edit submitted or approved timesheet");
        }
    }

    private MonthlyTimesheet getOrCreateMonthlyTimesheet(Long userId, Integer year, Integer month) {
        return monthlyTimesheetRepository.findByUserIdAndYearAndMonth(userId, year, month)
                .orElseGet(() -> {
                    User user = getUserById(userId);
                    MonthlyTimesheet newTimesheet = new MonthlyTimesheet();
                    newTimesheet.setUser(user);
                    newTimesheet.setYear(year);
                    newTimesheet.setMonth(month);
                    newTimesheet.setStatus(MonthlyTimesheet.TimesheetStatus.draft);
                    return monthlyTimesheetRepository.save(newTimesheet);
                });
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
}
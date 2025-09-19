// Updated TimesheetController.java - With new endpoints for history and available months
package com.goldtech.timesheet_backend.controller;

import com.goldtech.timesheet_backend.dto.timesheet.*;
import com.goldtech.timesheet_backend.entity.User;
import com.goldtech.timesheet_backend.service.TimesheetService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/timesheets")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"}, allowCredentials = "true")
public class TimesheetController {

    private static final Logger logger = LoggerFactory.getLogger(TimesheetController.class);

    @Autowired
    private TimesheetService timesheetService;

    /**
     * Get available months for timesheet submission based on business rules
     */
    @GetMapping("/available-months")
    public ResponseEntity<Map<String, Object>> getAvailableMonths(Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            logger.debug("Getting available months for user {}", user.getId());

            List<AvailableMonthDto> availableMonths = timesheetService.getAvailableMonths(user.getId());

            return ResponseEntity.ok(createSuccessResponse(availableMonths, "Available months retrieved successfully"));

        } catch (Exception e) {
            logger.error("Error getting available months", e);
            return ResponseEntity.status(500)
                    .body(createErrorResponse("Failed to retrieve available months: " + e.getMessage()));
        }
    }

    /**
     * Get timesheet history for current user
     */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getTimesheetHistory(Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            logger.debug("Getting timesheet history for user {}", user.getId());

            List<TimesheetHistoryDto> history = timesheetService.getTimesheetHistory(user.getId());

            return ResponseEntity.ok(createSuccessResponse(history, "Timesheet history retrieved successfully"));

        } catch (Exception e) {
            logger.error("Error getting timesheet history", e);
            return ResponseEntity.status(500)
                    .body(createErrorResponse("Failed to retrieve timesheet history: " + e.getMessage()));
        }
    }

    /**
     * Check if timesheet can be submitted (for frontend validation)
     */
    @GetMapping("/{year}/{month}/can-submit")
    public ResponseEntity<Map<String, Object>> canSubmitTimesheet(
            @PathVariable Integer year,
            @PathVariable Integer month,
            Authentication authentication
    ) {
        try {
            User user = (User) authentication.getPrincipal();

            boolean canSubmit = timesheetService.canSubmitTimesheet(user.getId(), year, month);
            boolean canResubmit = timesheetService.canResubmitTimesheet(user.getId(), year, month);

            Map<String, Object> result = new HashMap<>();
            result.put("canSubmit", canSubmit);
            result.put("canResubmit", canResubmit);
            result.put("canPerformAction", canSubmit || canResubmit);

            return ResponseEntity.ok(createSuccessResponse(result, "Submission check completed"));

        } catch (Exception e) {
            logger.error("Error checking submission eligibility for {}/{}", year, month, e);
            return ResponseEntity.status(500)
                    .body(createErrorResponse("Failed to check submission eligibility: " + e.getMessage()));
        }
    }

    /**
     * Get timesheet for a specific month
     */
    @GetMapping("/{year}/{month}")
    public ResponseEntity<Map<String, Object>> getTimesheet(
            @PathVariable Integer year,
            @PathVariable Integer month,
            Authentication authentication
    ) {
        try {
            User user = (User) authentication.getPrincipal();
            logger.debug("Getting timesheet for user {} - {}/{}", user.getId(), year, month);

            TimesheetResponseDto timesheet = timesheetService.getTimesheet(user.getId(), year, month);

            return ResponseEntity.ok(createSuccessResponse(timesheet, "Timesheet retrieved successfully"));

        } catch (Exception e) {
            logger.error("Error getting timesheet for {}/{}", year, month, e);
            return ResponseEntity.status(500)
                    .body(createErrorResponse("Failed to retrieve timesheet: " + e.getMessage()));
        }
    }

    /**
     * Save a single day entry
     */
    @PostMapping("/entries")
    public ResponseEntity<Map<String, Object>> saveDayEntry(
            @Valid @RequestBody SaveEntryRequestDto request,
            Authentication authentication
    ) {
        try {
            User user = (User) authentication.getPrincipal();
            logger.debug("Saving day entry for user {} on {}", user.getId(), request.getDate());

            DayEntryDto savedEntry = timesheetService.saveDayEntry(user.getId(), request);

            return ResponseEntity.ok(createSuccessResponse(savedEntry, "Day entry saved successfully"));

        } catch (IllegalArgumentException e) {
            logger.warn("Validation error saving day entry: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error saving day entry", e);
            return ResponseEntity.status(500)
                    .body(createErrorResponse("Failed to save day entry: " + e.getMessage()));
        }
    }

    /**
     * Save multiple day entries (bulk operation)
     */
    @PostMapping("/entries/bulk")
    public ResponseEntity<Map<String, Object>> saveBulkEntries(
            @Valid @RequestBody List<SaveEntryRequestDto> requests,
            Authentication authentication
    ) {
        try {
            User user = (User) authentication.getPrincipal();
            logger.debug("Saving {} bulk entries for user {}", requests.size(), user.getId());

            List<DayEntryDto> savedEntries = timesheetService.saveBulkEntries(user.getId(), requests);

            return ResponseEntity.ok(createSuccessResponse(savedEntries,
                    "Bulk entries saved successfully (" + savedEntries.size() + " entries)"));

        } catch (IllegalArgumentException e) {
            logger.warn("Validation error saving bulk entries: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error saving bulk entries", e);
            return ResponseEntity.status(500)
                    .body(createErrorResponse("Failed to save bulk entries: " + e.getMessage()));
        }
    }

    /**
     * Delete a day entry
     */
    @DeleteMapping("/entries/{date}")
    public ResponseEntity<Map<String, Object>> deleteDayEntry(
            @PathVariable String date,
            Authentication authentication
    ) {
        try {
            User user = (User) authentication.getPrincipal();
            logger.debug("Deleting day entry for user {} on {}", user.getId(), date);

            timesheetService.deleteDayEntry(user.getId(), date);

            return ResponseEntity.ok(createSuccessResponse(null, "Day entry deleted successfully"));

        } catch (IllegalArgumentException e) {
            logger.warn("Validation error deleting day entry: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error deleting day entry for date {}", date, e);
            return ResponseEntity.status(500)
                    .body(createErrorResponse("Failed to delete day entry: " + e.getMessage()));
        }
    }

    /**
     * Submit timesheet for approval
     */
    @PostMapping("/{year}/{month}/submit")
    public ResponseEntity<Map<String, Object>> submitTimesheet(
            @PathVariable Integer year,
            @PathVariable Integer month,
            Authentication authentication
    ) {
        try {
            User user = (User) authentication.getPrincipal();
            logger.debug("Submitting timesheet for user {} - {}/{}", user.getId(), year, month);

            TimesheetResponseDto timesheet = timesheetService.submitTimesheet(user.getId(), year, month);

            return ResponseEntity.ok(createSuccessResponse(timesheet, "Timesheet submitted for approval"));

        } catch (IllegalArgumentException e) {
            logger.warn("Validation error submitting timesheet: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error submitting timesheet for {}/{}", year, month, e);
            return ResponseEntity.status(500)
                    .body(createErrorResponse("Failed to submit timesheet: " + e.getMessage()));
        }
    }

    /**
     * Get timesheet statistics for a month
     */
    @GetMapping("/{year}/{month}/stats")
    public ResponseEntity<Map<String, Object>> getTimesheetStats(
            @PathVariable Integer year,
            @PathVariable Integer month,
            Authentication authentication
    ) {
        try {
            User user = (User) authentication.getPrincipal();
            logger.debug("Getting timesheet stats for user {} - {}/{}", user.getId(), year, month);

            TimesheetStatsDto stats = timesheetService.getTimesheetStats(user.getId(), year, month);

            return ResponseEntity.ok(createSuccessResponse(stats, "Timesheet statistics retrieved successfully"));

        } catch (Exception e) {
            logger.error("Error getting timesheet stats for {}/{}", year, month, e);
            return ResponseEntity.status(500)
                    .body(createErrorResponse("Failed to retrieve timesheet statistics: " + e.getMessage()));
        }
    }

    /**
     * Get working hours presets for current user
     */
    @GetMapping("/working-hours-presets")
    public ResponseEntity<Map<String, Object>> getWorkingHoursPresets(Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            logger.debug("Getting working hours presets for user {}", user.getId());

            List<WorkingHoursPresetDto> presets = timesheetService.getWorkingHoursPresets(user.getId());

            return ResponseEntity.ok(createSuccessResponse(presets, "Working hours presets retrieved successfully"));

        } catch (Exception e) {
            logger.error("Error getting working hours presets", e);
            return ResponseEntity.status(500)
                    .body(createErrorResponse("Failed to retrieve working hours presets: " + e.getMessage()));
        }
    }

    /**
     * Save working hours preset
     */
    @PostMapping("/working-hours-presets")
    public ResponseEntity<Map<String, Object>> saveWorkingHoursPreset(
            @RequestBody Map<String, String> request,
            Authentication authentication
    ) {
        try {
            User user = (User) authentication.getPrincipal();
            String name = request.get("name");
            String startTime = request.get("startTime");
            String endTime = request.get("endTime");

            logger.debug("Saving working hours preset for user {}: {}", user.getId(), name);

            WorkingHoursPresetDto preset = timesheetService.saveWorkingHoursPreset(
                    user.getId(), name, startTime, endTime);

            return ResponseEntity.ok(createSuccessResponse(preset, "Working hours preset saved successfully"));

        } catch (IllegalArgumentException e) {
            logger.warn("Validation error saving working hours preset: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error saving working hours preset", e);
            return ResponseEntity.status(500)
                    .body(createErrorResponse("Failed to save working hours preset: " + e.getMessage()));
        }
    }

    /**
     * Delete working hours preset
     */
    @DeleteMapping("/working-hours-presets/{presetId}")
    public ResponseEntity<Map<String, Object>> deleteWorkingHoursPreset(
            @PathVariable Long presetId,
            Authentication authentication
    ) {
        try {
            User user = (User) authentication.getPrincipal();
            logger.debug("Deleting working hours preset {} for user {}", presetId, user.getId());

            timesheetService.deleteWorkingHoursPreset(user.getId(), presetId);

            return ResponseEntity.ok(createSuccessResponse(null, "Working hours preset deleted successfully"));

        } catch (Exception e) {
            logger.error("Error deleting working hours preset {}", presetId, e);
            return ResponseEntity.status(500)
                    .body(createErrorResponse("Failed to delete working hours preset: " + e.getMessage()));
        }
    }

    // Helper methods for response formatting

    private Map<String, Object> createSuccessResponse(Object data, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("data", data);
        response.put("timestamp", java.time.Instant.now().toString());
        return response;
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("data", null);
        response.put("timestamp", java.time.Instant.now().toString());
        return response;
    }
}
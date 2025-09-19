// src/main/java/com/goldtech/timesheet_backend/controller/TimesheetApprovalController.java
package com.goldtech.timesheet_backend.controller;

import com.goldtech.timesheet_backend.dto.timesheet.TimesheetResponseDto;
import com.goldtech.timesheet_backend.entity.User;
import com.goldtech.timesheet_backend.service.TimesheetApprovalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/timesheets/approval")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"}, allowCredentials = "true")
public class TimesheetApprovalController {

    private static final Logger logger = LoggerFactory.getLogger(TimesheetApprovalController.class);

    @Autowired
    private TimesheetApprovalService approvalService;

    /**
     * Get pending timesheets for approval by supervisor
     */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('SUPERVISOR') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getPendingTimesheets(Authentication authentication) {
        try {
            User supervisor = (User) authentication.getPrincipal();
            logger.debug("Getting pending timesheets for supervisor {}", supervisor.getId());

            List<TimesheetResponseDto> pendingTimesheets = approvalService.getPendingTimesheets(supervisor.getId());

            return ResponseEntity.ok(createSuccessResponse(pendingTimesheets, "Pending timesheets retrieved successfully"));

        } catch (Exception e) {
            logger.error("Error getting pending timesheets", e);
            return ResponseEntity.status(500)
                    .body(createErrorResponse("Failed to retrieve pending timesheets: " + e.getMessage()));
        }
    }

    /**
     * Get all timesheets for approval (pending, approved, rejected)
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('SUPERVISOR') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllTimesheetsForApproval(
            @RequestParam(defaultValue = "all") String status,
            Authentication authentication
    ) {
        try {
            User supervisor = (User) authentication.getPrincipal();
            logger.debug("Getting timesheets for approval by supervisor {} with status {}", supervisor.getId(), status);

            List<TimesheetResponseDto> timesheets = approvalService.getTimesheetsForApproval(supervisor.getId(), status);

            return ResponseEntity.ok(createSuccessResponse(timesheets, "Timesheets retrieved successfully"));

        } catch (Exception e) {
            logger.error("Error getting timesheets for approval", e);
            return ResponseEntity.status(500)
                    .body(createErrorResponse("Failed to retrieve timesheets: " + e.getMessage()));
        }
    }

    /**
     * Approve or reject a timesheet
     */
    @PostMapping("/{timesheetId}/decision")
    @PreAuthorize("hasRole('SUPERVISOR') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> approveOrRejectTimesheet(
            @PathVariable Long timesheetId,
            @RequestBody Map<String, String> request,
            Authentication authentication
    ) {
        try {
            User supervisor = (User) authentication.getPrincipal();
            String decision = request.get("decision"); // "approved" or "rejected"
            String comments = request.get("comments");

            logger.debug("Processing timesheet {} decision: {} by supervisor {}",
                    timesheetId, decision, supervisor.getId());

            // Validate decision
            if (!"approved".equals(decision) && !"rejected".equals(decision)) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Invalid decision. Must be 'approved' or 'rejected'"));
            }

            // Require comments for rejection
            if ("rejected".equals(decision) && (comments == null || comments.trim().isEmpty())) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Comments are required for rejection"));
            }

            TimesheetResponseDto updatedTimesheet = approvalService.processApproval(
                    timesheetId, supervisor.getId(), decision, comments
            );

            String message = "approved".equals(decision) ?
                    "Timesheet approved successfully" :
                    "Timesheet rejected successfully";

            return ResponseEntity.ok(createSuccessResponse(updatedTimesheet, message));

        } catch (IllegalArgumentException e) {
            logger.warn("Validation error processing timesheet approval: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error processing timesheet approval for timesheet {}", timesheetId, e);
            return ResponseEntity.status(500)
                    .body(createErrorResponse("Failed to process approval: " + e.getMessage()));
        }
    }

    /**
     * Get timesheet details for approval review
     */
    @GetMapping("/{timesheetId}/details")
    @PreAuthorize("hasRole('SUPERVISOR') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getTimesheetForApproval(
            @PathVariable Long timesheetId,
            Authentication authentication
    ) {
        try {
            User supervisor = (User) authentication.getPrincipal();
            logger.debug("Getting timesheet {} details for approval by supervisor {}", timesheetId, supervisor.getId());

            TimesheetResponseDto timesheet = approvalService.getTimesheetForApproval(timesheetId, supervisor.getId());

            return ResponseEntity.ok(createSuccessResponse(timesheet, "Timesheet details retrieved successfully"));

        } catch (IllegalArgumentException e) {
            logger.warn("Access denied for timesheet {}: {}", timesheetId, e.getMessage());
            return ResponseEntity.status(403)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error getting timesheet details for approval", e);
            return ResponseEntity.status(500)
                    .body(createErrorResponse("Failed to retrieve timesheet details: " + e.getMessage()));
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
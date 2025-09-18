// DocumentController.java - For testing document uploads
package com.goldtech.timesheet_backend.controller;

import com.goldtech.timesheet_backend.entity.DayEntryDocument;
import com.goldtech.timesheet_backend.repository.DayEntryDocumentRepository;
import com.goldtech.timesheet_backend.service.DocumentStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/documents")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"}, allowCredentials = "true")
public class DocumentController {

    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);

    @Autowired
    private DayEntryDocumentRepository documentRepository;

    @Autowired
    private DocumentStorageService documentStorageService;

    /**
     * Download a document by ID
     */
    @GetMapping("/{documentId}/download")
    public ResponseEntity<?> downloadDocument(
            @PathVariable Long documentId,
            Authentication authentication
    ) {
        try {
            logger.debug("Downloading document: {}", documentId);

            Optional<DayEntryDocument> documentOpt = documentRepository.findById(documentId);
            if (documentOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            DayEntryDocument document = documentOpt.get();

            // Get file content as base64
            String base64Content = documentStorageService.getDocumentAsBase64(documentId);

            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("filename", document.getOriginalFilename());
            response.put("mimeType", document.getMimeType());
            response.put("size", document.getFileSize());
            response.put("content", base64Content);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error downloading document: {}", documentId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to download document: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Get document metadata by ID
     */
    @GetMapping("/{documentId}")
    public ResponseEntity<?> getDocumentInfo(
            @PathVariable Long documentId,
            Authentication authentication
    ) {
        try {
            Optional<DayEntryDocument> documentOpt = documentRepository.findById(documentId);
            if (documentOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            DayEntryDocument document = documentOpt.get();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                    "id", document.getId(),
                    "originalFilename", document.getOriginalFilename(),
                    "mimeType", document.getMimeType(),
                    "fileSize", document.getFileSize(),
                    "uploadedAt", document.getUploadedAt()
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting document info: {}", documentId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to get document info: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * List documents for a day entry
     */
    @GetMapping("/day-entry/{dayEntryId}")
    public ResponseEntity<?> getDocumentsByDayEntry(
            @PathVariable Long dayEntryId,
            Authentication authentication
    ) {
        try {
            List<DayEntryDocument> documents = documentRepository.findByDayEntryId(dayEntryId);

            List<Map<String, Object>> documentList = documents.stream()
                    .map(doc -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", doc.getId());
                        map.put("originalFilename", doc.getOriginalFilename());
                        map.put("mimeType", doc.getMimeType());
                        map.put("fileSize", doc.getFileSize());
                        map.put("uploadedAt", doc.getUploadedAt());
                        return map;
                    })
                    .toList();


            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", documentList);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting documents for day entry: {}", dayEntryId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to get documents: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Test endpoint to check document storage status
     */
    @GetMapping("/test/storage-status")
    public ResponseEntity<?> getStorageStatus() {
        try {
            long totalDocuments = documentRepository.count();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalDocuments", totalDocuments);
            response.put("message", "Document storage is working");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error checking storage status", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Storage check failed: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}
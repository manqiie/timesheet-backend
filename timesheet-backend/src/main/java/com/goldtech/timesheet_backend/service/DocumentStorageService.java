// DocumentStorageService.java - Handle file storage
package com.goldtech.timesheet_backend.service;

import com.goldtech.timesheet_backend.entity.DayEntry;
import com.goldtech.timesheet_backend.entity.DayEntryDocument;
import com.goldtech.timesheet_backend.repository.DayEntryDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DocumentStorageService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentStorageService.class);

    @Autowired
    private DayEntryDocumentRepository documentRepository;

    // Configure upload directory in application.properties
    @Value("${app.upload.dir:${user.home}/timesheet-uploads}")
    private String uploadDir;

    /**
     * Save documents for a day entry
     */
    public void saveDocuments(DayEntry dayEntry, List<DocumentUploadDto> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }

        // Create upload directory if it doesn't exist
        createUploadDirectory();

        for (DocumentUploadDto documentDto : documents) {
            try {
                saveDocument(dayEntry, documentDto);
            } catch (Exception e) {
                logger.error("Failed to save document: {}", documentDto.getName(), e);
                throw new RuntimeException("Failed to save document: " + documentDto.getName(), e);
            }
        }
    }

    /**
     * Save a single document
     */
    private void saveDocument(DayEntry dayEntry, DocumentUploadDto documentDto) throws IOException {
        // Decode base64 content
        byte[] fileContent = Base64.getDecoder().decode(documentDto.getBase64Data());

        // Generate unique filename
        String fileExtension = getFileExtension(documentDto.getName());
        String storedFilename = UUID.randomUUID().toString() + "." + fileExtension;

        // Create file path
        Path filePath = Paths.get(uploadDir, "user_" + dayEntry.getUser().getId(),
                dayEntry.getDate().getYear() + "",
                dayEntry.getDate().getMonthValue() + "",
                storedFilename);

        // Create directories if they don't exist
        Files.createDirectories(filePath.getParent());

        // Write file to disk
        Files.write(filePath, fileContent);

        // Save document metadata to database
        DayEntryDocument document = new DayEntryDocument();
        document.setDayEntry(dayEntry);
        document.setOriginalFilename(documentDto.getName());
        document.setStoredFilename(storedFilename);
        document.setFilePath(filePath.toString());
        document.setMimeType(documentDto.getType());
        document.setFileSize(documentDto.getSize());

        documentRepository.save(document);

        logger.info("Document saved: {} -> {}", documentDto.getName(), storedFilename);
    }

    /**
     * Delete documents for a day entry
     */
    public void deleteDocuments(Long dayEntryId) {
        List<DayEntryDocument> documents = documentRepository.findByDayEntryId(dayEntryId);

        for (DayEntryDocument document : documents) {
            try {
                // Delete physical file
                Path filePath = Paths.get(document.getFilePath());
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    logger.info("Physical file deleted: {}", document.getStoredFilename());
                }
            } catch (IOException e) {
                logger.warn("Failed to delete physical file: {}", document.getStoredFilename(), e);
            }
        }

        // Delete database records
        documentRepository.deleteByDayEntryId(dayEntryId);
        logger.info("Deleted {} documents for day entry {}", documents.size(), dayEntryId);
    }

    /**
     * Get file content as base64 (for download)
     */
    public String getDocumentAsBase64(Long documentId) throws IOException {
        DayEntryDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        Path filePath = Paths.get(document.getFilePath());
        if (!Files.exists(filePath)) {
            throw new RuntimeException("Physical file not found: " + document.getStoredFilename());
        }

        byte[] fileContent = Files.readAllBytes(filePath);
        return Base64.getEncoder().encodeToString(fileContent);
    }

    /**
     * Create upload directory
     */
    private void createUploadDirectory() {
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                logger.info("Created upload directory: {}", uploadDir);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create upload directory: " + uploadDir, e);
        }
    }

    /**
     * Extract file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "unknown";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * DTO for document upload
     */
    public static class DocumentUploadDto {
        private String name;
        private String type;
        private Long size;
        private String base64Data;

        public DocumentUploadDto(String name, String type, Long size, String base64Data) {
            this.name = name;
            this.type = type;
            this.size = size;
            this.base64Data = base64Data;
        }

        // Getters
        public String getName() { return name; }
        public String getType() { return type; }
        public Long getSize() { return size; }
        public String getBase64Data() { return base64Data; }
    }
}
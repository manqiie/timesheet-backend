// 3. Document DTO
// src/main/java/com/goldtech/timesheet_backend/dto/timesheet/DocumentDto.java
package com.goldtech.timesheet_backend.dto.timesheet;

import java.time.LocalDateTime;

public class DocumentDto {
    private Long id;
    private String name; // Original filename
    private String type; // MIME type
    private Long size;   // File size in bytes
    private String url;  // Download URL (optional)
    private LocalDateTime uploadedAt;

    // Constructors
    public DocumentDto() {}

    public DocumentDto(String name, String type, Long size) {
        this.name = name;
        this.type = type;
        this.size = size;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}
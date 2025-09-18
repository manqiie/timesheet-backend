// 3. Day Entry Document Repository
// src/main/java/com/goldtech/timesheet_backend/repository/DayEntryDocumentRepository.java
package com.goldtech.timesheet_backend.repository;

import com.goldtech.timesheet_backend.entity.DayEntryDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DayEntryDocumentRepository extends JpaRepository<DayEntryDocument, Long> {

    // Find documents by day entry
    List<DayEntryDocument> findByDayEntryId(Long dayEntryId);

    // Find documents by day entry and original filename
    List<DayEntryDocument> findByDayEntryIdAndOriginalFilename(Long dayEntryId, String originalFilename);

    // Delete documents by day entry
    void deleteByDayEntryId(Long dayEntryId);
}
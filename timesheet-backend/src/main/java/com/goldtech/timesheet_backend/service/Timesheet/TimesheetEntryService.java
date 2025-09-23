// TimesheetEntryService.java - Handle day entry operations
package com.goldtech.timesheet_backend.service;

import com.goldtech.timesheet_backend.dto.timesheet.SaveEntryRequestDto;
import com.goldtech.timesheet_backend.entity.DayEntry;
import com.goldtech.timesheet_backend.entity.User;
import com.goldtech.timesheet_backend.repository.DayEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TimesheetEntryService {

    private static final Logger logger = LoggerFactory.getLogger(TimesheetEntryService.class);

    @Autowired
    private DayEntryRepository dayEntryRepository;

    @Autowired
    private DocumentStorageService documentStorageService;

    @Autowired
    private TimesheetValidationService validationService;

    /**
     * Get or create a day entry
     */
    public DayEntry getOrCreateDayEntry(User user, LocalDate date) {
        return dayEntryRepository.findByUserIdAndDate(user.getId(), date)
                .orElse(new DayEntry(user, date, DayEntry.EntryType.working_hours));
    }

    /**
     * Update day entry from request DTO
     */
    public void updateDayEntryFromRequest(DayEntry dayEntry, SaveEntryRequestDto request) {
        // Set entry type
        dayEntry.setEntryType(DayEntry.EntryType.valueOf(request.getType()));

        // Set working hours fields
        if (request.getStartTime() != null && !request.getStartTime().isEmpty()) {
            LocalTime startTime = LocalTime.parse(request.getStartTime());
            dayEntry.setStartTime(startTime);

            if (request.getEndTime() != null && !request.getEndTime().isEmpty()) {
                LocalTime endTime = LocalTime.parse(request.getEndTime());
                dayEntry.setEndTime(endTime);

                // Validation is already done in ValidationService
                if (dayEntry.getEntryType() == DayEntry.EntryType.working_hours) {
                    validationService.validateWorkingHours(startTime, endTime);
                }
            }
        } else {
            dayEntry.setStartTime(null);
            dayEntry.setEndTime(null);
        }

        // Set other fields
        setOptionalField(request.getHalfDayPeriod(),
                value -> dayEntry.setHalfDayPeriod(DayEntry.HalfDayPeriod.valueOf(value)));

        setOptionalDateField(request.getDateEarned(), dayEntry::setDateEarned);
        setOptionalDateField(request.getPrimaryDocumentDay(), dayEntry::setPrimaryDocumentDay);

        if (request.getIsPrimaryDocument() != null) {
            dayEntry.setIsPrimaryDocument(request.getIsPrimaryDocument());
        }

        dayEntry.setNotes(request.getNotes());
    }

    /**
     * Save documents for a day entry
     */
    public void saveDocuments(DayEntry dayEntry, List<SaveEntryRequestDto.SupportingDocumentDto> documents) {
        if (documents != null && !documents.isEmpty()) {
            List<DocumentStorageService.DocumentUploadDto> uploadDtos = documents.stream()
                    .map(doc -> new DocumentStorageService.DocumentUploadDto(
                            doc.getName(), doc.getType(), doc.getSize(), doc.getBase64Data()))
                    .collect(Collectors.toList());

            documentStorageService.saveDocuments(dayEntry, uploadDtos);
        }
    }

    /**
     * Delete day entry and its documents
     */
    public void deleteDayEntry(Long userId, LocalDate date) {
        Optional<DayEntry> entry = dayEntryRepository.findByUserIdAndDate(userId, date);
        if (entry.isPresent()) {
            DayEntry dayEntry = entry.get();
            documentStorageService.deleteDocuments(dayEntry.getId());
            dayEntryRepository.delete(dayEntry);
            logger.info("Day entry deleted for user {} on {}", userId, date);
        }
    }

    /**
     * Delete existing documents for an entry (for updates)
     */
    public void deleteExistingDocuments(DayEntry dayEntry) {
        if (dayEntry.getId() != null) {
            documentStorageService.deleteDocuments(dayEntry.getId());
        }
    }

    // Helper methods

    private void setOptionalField(String value, java.util.function.Consumer<String> setter) {
        if (value != null && !value.isEmpty()) {
            setter.accept(value);
        }
    }

    private void setOptionalDateField(String value, java.util.function.Consumer<LocalDate> setter) {
        if (value != null && !value.isEmpty()) {
            setter.accept(LocalDate.parse(value));
        }
    }
}
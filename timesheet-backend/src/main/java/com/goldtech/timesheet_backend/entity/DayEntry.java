// 2. Day Entry Entity
// src/main/java/com/goldtech/timesheet_backend/entity/DayEntry.java
package com.goldtech.timesheet_backend.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "day_entries")
public class DayEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false)
    private EntryType entryType;

    // Working Hours fields
    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    // Leave fields
    @Enumerated(EnumType.STRING)
    @Column(name = "half_day_period")
    private HalfDayPeriod halfDayPeriod;

    @Column(name = "date_earned") // For off_in_lieu only
    private LocalDate dateEarned;

    // Document reference fields
    @Column(name = "primary_document_day")
    private LocalDate primaryDocumentDay;

    @Column(name = "is_primary_document")
    private Boolean isPrimaryDocument = false;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "dayEntry", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DayEntryDocument> documents = new ArrayList<>();

    // Enums
    public enum EntryType {
        working_hours,
        annual_leave,
        annual_leave_halfday,
        medical_leave,
        off_in_lieu,
        childcare_leave,
        childcare_leave_halfday,
        shared_parental_leave,
        nopay_leave,
        nopay_leave_halfday,
        hospitalization_leave,
        reservist,
        paternity_leave,
        compassionate_leave,
        maternity_leave,
        day_off
    }

    public enum HalfDayPeriod {
        AM, PM
    }

    // Constructors
    public DayEntry() {}

    public DayEntry(User user, LocalDate date, EntryType entryType) {
        this.user = user;
        this.date = date;
        this.entryType = entryType;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public EntryType getEntryType() { return entryType; }
    public void setEntryType(EntryType entryType) { this.entryType = entryType; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public HalfDayPeriod getHalfDayPeriod() { return halfDayPeriod; }
    public void setHalfDayPeriod(HalfDayPeriod halfDayPeriod) { this.halfDayPeriod = halfDayPeriod; }

    public LocalDate getDateEarned() { return dateEarned; }
    public void setDateEarned(LocalDate dateEarned) { this.dateEarned = dateEarned; }

    public LocalDate getPrimaryDocumentDay() { return primaryDocumentDay; }
    public void setPrimaryDocumentDay(LocalDate primaryDocumentDay) { this.primaryDocumentDay = primaryDocumentDay; }

    public Boolean getIsPrimaryDocument() { return isPrimaryDocument; }
    public void setIsPrimaryDocument(Boolean isPrimaryDocument) { this.isPrimaryDocument = isPrimaryDocument; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<DayEntryDocument> getDocuments() { return documents; }
    public void setDocuments(List<DayEntryDocument> documents) { this.documents = documents; }
}
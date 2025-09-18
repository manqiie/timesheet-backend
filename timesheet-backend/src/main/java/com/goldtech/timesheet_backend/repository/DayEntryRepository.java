// 2. Day Entry Repository
// src/main/java/com/goldtech/timesheet_backend/repository/DayEntryRepository.java
package com.goldtech.timesheet_backend.repository;

import com.goldtech.timesheet_backend.entity.DayEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DayEntryRepository extends JpaRepository<DayEntry, Long> {

    // Find by user and date
    Optional<DayEntry> findByUserIdAndDate(Long userId, LocalDate date);

    // Find entries for a month
    @Query("SELECT de FROM DayEntry de WHERE de.user.id = :userId AND " +
            "YEAR(de.date) = :year AND MONTH(de.date) = :month " +
            "ORDER BY de.date ASC")
    List<DayEntry> findByUserIdAndYearAndMonth(@Param("userId") Long userId,
                                               @Param("year") int year,
                                               @Param("month") int month);

    // Find entries between dates
    @Query("SELECT de FROM DayEntry de WHERE de.user.id = :userId AND " +
            "de.date BETWEEN :startDate AND :endDate ORDER BY de.date ASC")
    List<DayEntry> findByUserIdAndDateBetween(@Param("userId") Long userId,
                                              @Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);

    // Check if user has entries for a month
    @Query("SELECT COUNT(de) > 0 FROM DayEntry de WHERE de.user.id = :userId AND " +
            "YEAR(de.date) = :year AND MONTH(de.date) = :month")
    boolean existsByUserIdAndYearAndMonth(@Param("userId") Long userId,
                                          @Param("year") int year,
                                          @Param("month") int month);

    // Count entries for a month
    @Query("SELECT COUNT(de) FROM DayEntry de WHERE de.user.id = :userId AND " +
            "YEAR(de.date) = :year AND MONTH(de.date) = :month")
    long countByUserIdAndYearAndMonth(@Param("userId") Long userId,
                                      @Param("year") int year,
                                      @Param("month") int month);

    // Find working days for a month
    @Query("SELECT de FROM DayEntry de WHERE de.user.id = :userId AND " +
            "YEAR(de.date) = :year AND MONTH(de.date) = :month AND " +
            "de.entryType = 'working_hours' ORDER BY de.date ASC")
    List<DayEntry> findWorkingDaysByUserIdAndYearAndMonth(@Param("userId") Long userId,
                                                          @Param("year") int year,
                                                          @Param("month") int month);

    // Find leave days for a month
    @Query("SELECT de FROM DayEntry de WHERE de.user.id = :userId AND " +
            "YEAR(de.date) = :year AND MONTH(de.date) = :month AND " +
            "de.entryType != 'working_hours' ORDER BY de.date ASC")
    List<DayEntry> findLeaveDaysByUserIdAndYearAndMonth(@Param("userId") Long userId,
                                                        @Param("year") int year,
                                                        @Param("month") int month);

    // Delete entries for a month
    void deleteByUserIdAndDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

    // Find entries that reference a primary document day
    List<DayEntry> findByUserIdAndPrimaryDocumentDay(Long userId, LocalDate primaryDocumentDay);
}
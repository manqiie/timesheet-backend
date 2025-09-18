// 1. Working Hours Preset Repository
// src/main/java/com/goldtech/timesheet_backend/repository/WorkingHoursPresetRepository.java
package com.goldtech.timesheet_backend.repository;

import com.goldtech.timesheet_backend.entity.WorkingHoursPreset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkingHoursPresetRepository extends JpaRepository<WorkingHoursPreset, Long> {

    // Find by user ID
    List<WorkingHoursPreset> findByUserIdOrderByCreatedAtAsc(Long userId);

    // Find default preset for user
    Optional<WorkingHoursPreset> findByUserIdAndIsDefaultTrue(Long userId);

    // Check if user has any presets
    boolean existsByUserId(Long userId);

    // Delete by user ID and preset ID
    void deleteByIdAndUserId(Long id, Long userId);

    // Count presets for user
    long countByUserId(Long userId);
}
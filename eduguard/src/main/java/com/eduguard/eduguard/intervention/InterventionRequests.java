package com.eduguard.eduguard.intervention;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

record CreateCaseRequest(
        @NotNull @Positive Long studentId,
        @NotBlank @Size(max = 150) String title,
        @NotBlank @Size(max = 2000) String description,
        @NotNull CaseCategory category,
        @NotNull CasePriority priority,
        @Size(max = 100) String assignedStaffId,
        @FutureOrPresent LocalDate followUpDate,
        @Size(max = 100) String actorId) {}

record UpdateCaseRequest(
        @Size(max = 150) String title,
        @Size(max = 2000) String description,
        CaseCategory category,
        CasePriority priority,
        @Size(max = 100) String assignedStaffId,
        @FutureOrPresent LocalDate followUpDate,
        CaseStatus status,
        @Size(max = 2000) String resolutionSummary,
        @Size(max = 100) String actorId) {}

record CaseResponse(
        Long caseId, Long studentId, String title, String description,
        CaseCategory category, CasePriority priority, CaseStatus status,
        String assignedStaffId, LocalDate followUpDate, String resolutionSummary,
        java.time.LocalDateTime createdAt, java.time.LocalDateTime updatedAt) {}

record ActivityResponse(
        Long activityId, Long caseId, String activityType, String description,
        CaseStatus previousStatus, CaseStatus newStatus, String actorId,
        java.time.LocalDateTime createdAt) {}

record PageResponse<T>(
        java.util.List<T> content, int page, int size,
        long totalElements, int totalPages) {}

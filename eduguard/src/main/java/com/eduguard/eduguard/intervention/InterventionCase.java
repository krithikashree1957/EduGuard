package com.eduguard.eduguard.intervention;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "intervention_cases")
public class InterventionCase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "case_id")
    private Long caseId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CaseCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CasePriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CaseStatus status;

    @Column(name = "assigned_staff_id", length = 100)
    private String assignedStaffId;

    @Column(name = "follow_up_date")
    private LocalDate followUpDate;

    @Column(name = "resolution_summary", length = 2000)
    private String resolutionSummary;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected InterventionCase() {}

    InterventionCase(Long studentId, String title, String description,
                     CaseCategory category, CasePriority priority,
                     String assignedStaffId, LocalDate followUpDate) {
        this.studentId = studentId;
        this.title = title;
        this.description = description;
        this.category = category;
        this.priority = priority;
        this.status = CaseStatus.OPEN;
        this.assignedStaffId = assignedStaffId;
        this.followUpDate = followUpDate;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getCaseId() { return caseId; }
    public Long getStudentId() { return studentId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public CaseCategory getCategory() { return category; }
    public CasePriority getPriority() { return priority; }
    public CaseStatus getStatus() { return status; }
    public String getAssignedStaffId() { return assignedStaffId; }
    public LocalDate getFollowUpDate() { return followUpDate; }
    public String getResolutionSummary() { return resolutionSummary; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    void update(String title, String description, CaseCategory category,
                CasePriority priority, String assignedStaffId,
                LocalDate followUpDate, CaseStatus status,
                String resolutionSummary) {
        if (title != null) this.title = title;
        if (description != null) this.description = description;
        if (category != null) this.category = category;
        if (priority != null) this.priority = priority;
        if (assignedStaffId != null) this.assignedStaffId = assignedStaffId;
        if (followUpDate != null) this.followUpDate = followUpDate;
        if (status != null) this.status = status;
        if (resolutionSummary != null) this.resolutionSummary = resolutionSummary;
    }
}

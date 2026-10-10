package com.eduguard.eduguard.intervention;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "intervention_case_activities")
public class CaseActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activity_id")
    private Long activityId;
    @Column(name = "case_id", nullable = false)
    private Long caseId;
    @Column(name = "activity_type", nullable = false, length = 40)
    private String activityType;
    @Column(nullable = false, length = 500)
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 20)
    private CaseStatus previousStatus;
    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 20)
    private CaseStatus newStatus;
    @Column(name = "actor_id", length = 100)
    private String actorId;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected CaseActivity() {}

    CaseActivity(Long caseId, String activityType, String description,
                 CaseStatus previousStatus, CaseStatus newStatus,
                 String actorId) {
        this.caseId = caseId;
        this.activityType = activityType;
        this.description = description;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.actorId = actorId;
    }

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }

    public Long getActivityId() { return activityId; }
    public Long getCaseId() { return caseId; }
    public String getActivityType() { return activityType; }
    public String getDescription() { return description; }
    public CaseStatus getPreviousStatus() { return previousStatus; }
    public CaseStatus getNewStatus() { return newStatus; }
    public String getActorId() { return actorId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

package com.eduguard.eduguard.support;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_support_barriers")
public class SupportBarrier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "barrier_id")
    private Long barrierId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "barrier_type", nullable = false, length = 50)
    private String barrierType;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "severity", nullable = false, length = 20)
    private String severity;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected SupportBarrier() {
    }

    public SupportBarrier(Long studentId, String barrierType, String description,
                          String severity) {
        this.studentId = studentId;
        this.barrierType = barrierType;
        this.description = description;
        this.severity = severity;
    }

    @PrePersist
    void setCreatedAt() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getBarrierId() {
        return barrierId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public String getBarrierType() {
        return barrierType;
    }

    public String getDescription() {
        return description;
    }

    public String getSeverity() {
        return severity;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

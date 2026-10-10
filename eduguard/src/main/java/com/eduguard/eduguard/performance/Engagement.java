package com.eduguard.eduguard.performance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "engagement")
public class Engagement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "engagement_id")
    private Long engagementId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "participation_score", nullable = false)
    private BigDecimal participationScore;

    @Column(name = "assignment_completion_rate", nullable = false)
    private BigDecimal assignmentCompletionRate;

    public Engagement() {
    }

    public Long getEngagementId() {
        return engagementId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public BigDecimal getParticipationScore() {
        return participationScore;
    }

    public BigDecimal getAssignmentCompletionRate() {
        return assignmentCompletionRate;
    }
}
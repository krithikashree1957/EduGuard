package com.eduguard.eduguard.performance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "assessments")
public class Assessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assessment_id")
    private Long assessmentId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "assessment_name", nullable = false)
    private String assessmentName;

    @Column(name = "marks_obtained", nullable = false)
    private BigDecimal marksObtained;

    @Column(name = "maximum_marks", nullable = false)
    private BigDecimal maximumMarks;

    @Column(name = "assessment_date")
    private LocalDate assessmentDate;

    public Assessment() {
    }

    public Long getAssessmentId() {
        return assessmentId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public String getAssessmentName() {
        return assessmentName;
    }

    public BigDecimal getMarksObtained() {
        return marksObtained;
    }

    public BigDecimal getMaximumMarks() {
        return maximumMarks;
    }

    public LocalDate getAssessmentDate() {
        return assessmentDate;
    }

    public double getPercentage() {
        if (maximumMarks == null || maximumMarks.signum() == 0) {
            return 0.0;
        }

        return marksObtained
                .multiply(BigDecimal.valueOf(100))
                .divide(maximumMarks, 2, java.math.RoundingMode.HALF_UP)
                .doubleValue();
    }
}
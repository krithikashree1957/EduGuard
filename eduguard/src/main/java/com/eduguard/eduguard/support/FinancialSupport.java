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
@Table(name = "student_financial_support")
public class FinancialSupport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "financial_support_id")
    private Long financialSupportId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "support_type", nullable = false, length = 50)
    private String supportType;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected FinancialSupport() {
    }

    public FinancialSupport(Long studentId, String supportType, String status,
                            String notes) {
        this.studentId = studentId;
        this.supportType = supportType;
        this.status = status;
        this.notes = notes;
    }

    @PrePersist
    void setCreatedAt() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getFinancialSupportId() {
        return financialSupportId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public String getSupportType() {
        return supportType;
    }

    public String getStatus() {
        return status;
    }

    public String getNotes() {
        return notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

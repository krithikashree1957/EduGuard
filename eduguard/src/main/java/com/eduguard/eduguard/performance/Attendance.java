package com.eduguard.eduguard.performance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "attendance")
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attendance_id")
    private Long attendanceId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "total_classes", nullable = false)
    private Integer totalClasses;

    @Column(name = "attended_classes", nullable = false)
    private Integer attendedClasses;

    public Attendance() {
    }

    public Long getAttendanceId() {
        return attendanceId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public Integer getTotalClasses() {
        return totalClasses;
    }

    public Integer getAttendedClasses() {
        return attendedClasses;
    }

    public double getAttendancePercentage() {
        if (totalClasses == null || totalClasses == 0) {
            return 0.0;
        }

        return (attendedClasses * 100.0) / totalClasses;
    }
}
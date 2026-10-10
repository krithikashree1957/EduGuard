package com.eduguard.eduguard.performance;

import com.eduguard.eduguard.student.Student;
import com.eduguard.eduguard.student.StudentRepository;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/students")
public class StudentPerformanceController {

    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;
    private final AssessmentRepository assessmentRepository;
    private final EngagementRepository engagementRepository;

    public StudentPerformanceController(
            StudentRepository studentRepository,
            AttendanceRepository attendanceRepository,
            AssessmentRepository assessmentRepository,
            EngagementRepository engagementRepository) {

        this.studentRepository = studentRepository;
        this.attendanceRepository = attendanceRepository;
        this.assessmentRepository = assessmentRepository;
        this.engagementRepository = engagementRepository;
    }

    @GetMapping("/{studentId}/performance")
    public Map<String, Object> getStudentPerformance(
            @PathVariable Long studentId) {

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Student not found"));

        Optional<Attendance> attendance =
                attendanceRepository.findByStudentId(studentId);

        List<Assessment> assessments =
                assessmentRepository.findByStudentId(studentId);

        Optional<Engagement> engagement =
                engagementRepository.findByStudentId(studentId);

        Map<String, Object> result = new java.util.LinkedHashMap<>();

        result.put("studentId", student.getStudentId());
        result.put("studentCode", student.getStudentCode());
        result.put("className", student.getClassName());
        result.put("term", student.getTerm());

        if (attendance.isPresent()) {
            Attendance record = attendance.get();

            result.put("totalClasses", record.getTotalClasses());
            result.put("attendedClasses", record.getAttendedClasses());
            result.put("attendancePercentage",
                    Math.round(record.getAttendancePercentage() * 100.0) / 100.0);
        } else {
            result.put("attendance", "No attendance data available");
        }

        List<Map<String, Object>> assessmentResults = assessments.stream()
                .map(assessment -> {
                    Map<String, Object> item = new java.util.LinkedHashMap<>();

                    item.put("assessmentName", assessment.getAssessmentName());
                    item.put("marksObtained", assessment.getMarksObtained());
                    item.put("maximumMarks", assessment.getMaximumMarks());
                    item.put("percentage", assessment.getPercentage());
                    item.put("assessmentDate", assessment.getAssessmentDate());

                    return item;
                })
                .toList();

        result.put("assessments", assessmentResults);

        if (engagement.isPresent()) {
            Engagement record = engagement.get();

            result.put("participationScore", record.getParticipationScore());
            result.put("assignmentCompletionRate",
                    record.getAssignmentCompletionRate());
        } else {
            result.put("engagement", "No engagement data available");
        }

        return result;
    }
}
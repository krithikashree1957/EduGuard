package com.eduguard.eduguard.performance;

import com.eduguard.eduguard.student.Student;
import com.eduguard.eduguard.student.StudentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StudentPerformanceControllerTest {

    private final StudentRepository students = mock(StudentRepository.class);
    private final AttendanceRepository attendance = mock(AttendanceRepository.class);
    private final AssessmentRepository assessments = mock(AssessmentRepository.class);
    private final EngagementRepository engagement = mock(EngagementRepository.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(
            new StudentPerformanceController(students, attendance, assessments, engagement))
            .build();

    @Test
    void returnsStableAssessmentShapeWhenOptionalDataIsMissing() throws Exception {
        Student student = mock(Student.class);
        when(student.getStudentId()).thenReturn(42L);
        when(student.getStudentCode()).thenReturn("STU042");
        when(student.getClassName()).thenReturn("CSE-A");
        when(student.getTerm()).thenReturn("2026-T1");
        when(students.findById(42L)).thenReturn(Optional.of(student));
        when(attendance.findByStudentId(42L)).thenReturn(Optional.empty());
        when(assessments.findByStudentId(42L)).thenReturn(List.of());
        when(engagement.findByStudentId(42L)).thenReturn(Optional.empty());

        mvc.perform(get("/api/students/42/performance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(42))
                .andExpect(jsonPath("$.studentCode").value("STU042"))
                .andExpect(jsonPath("$.attendance").value("No attendance data available"))
                .andExpect(jsonPath("$.assessments").isArray())
                .andExpect(jsonPath("$.assessments").isEmpty())
                .andExpect(jsonPath("$.engagement").value("No engagement data available"));
    }

    @Test
    void returnsNotFoundForUnknownStudent() throws Exception {
        when(students.findById(404L)).thenReturn(Optional.empty());

        mvc.perform(get("/api/students/404/performance"))
                .andExpect(status().isNotFound());
    }
}

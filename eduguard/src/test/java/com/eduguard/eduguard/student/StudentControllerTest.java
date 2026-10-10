package com.eduguard.eduguard.student;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StudentControllerTest {

    private final StudentRepository students = mock(StudentRepository.class);
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new StudentController(students))
            .build();

    @Test
    void returnsStudentRecordsAndSupportedFilters() throws Exception {
        Student student = mock(Student.class);
        when(student.getStudentId()).thenReturn(42L);
        when(student.getStudentCode()).thenReturn("STU042");
        when(student.getClassName()).thenReturn("CSE-A");
        when(student.getTerm()).thenReturn("2026-T1");
        when(students.findByClassNameAndTerm("CSE-A", "2026-T1"))
                .thenReturn(List.of(student));

        mvc.perform(get("/api/students")
                        .param("className", "CSE-A")
                        .param("term", "2026-T1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentId").value(42))
                .andExpect(jsonPath("$[0].studentCode").value("STU042"))
                .andExpect(jsonPath("$[0].className").value("CSE-A"))
                .andExpect(jsonPath("$[0].term").value("2026-T1"));

        verify(students).findByClassNameAndTerm("CSE-A", "2026-T1");
    }
}

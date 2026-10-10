package com.eduguard.eduguard.student;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentRepository studentRepository;

    public StudentController(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @GetMapping
    public List<Student> getStudents(
            @RequestParam(required = false) String className,
            @RequestParam(required = false) String term) {

        if (className != null && term != null) {
            return studentRepository.findByClassNameAndTerm(className, term);
        }

        if (className != null) {
            return studentRepository.findByClassName(className);
        }

        if (term != null) {
            return studentRepository.findByTerm(term);
        }

        return studentRepository.findAll();
    }
}
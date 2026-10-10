package com.eduguard.eduguard.student;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    List<Student> findByClassName(String className);

    List<Student> findByTerm(String term);

    List<Student> findByClassNameAndTerm(String className, String term);
}
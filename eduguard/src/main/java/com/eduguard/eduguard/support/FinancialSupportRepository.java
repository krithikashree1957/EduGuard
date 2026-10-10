package com.eduguard.eduguard.support;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FinancialSupportRepository
        extends JpaRepository<FinancialSupport, Long> {

    List<FinancialSupport> findByStudentIdOrderByCreatedAtDesc(Long studentId);
}

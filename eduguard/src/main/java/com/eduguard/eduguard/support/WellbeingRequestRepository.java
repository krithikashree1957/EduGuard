package com.eduguard.eduguard.support;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WellbeingRequestRepository
        extends JpaRepository<WellbeingRequest, Long> {

    List<WellbeingRequest> findByStudentIdOrderByCreatedAtDesc(Long studentId);
}

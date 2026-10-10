package com.eduguard.eduguard.performance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EngagementRepository
        extends JpaRepository<Engagement, Long> {

    Optional<Engagement> findByStudentId(Long studentId);
}
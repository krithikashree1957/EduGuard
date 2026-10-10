package com.eduguard.eduguard.support;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupportBarrierRepository
        extends JpaRepository<SupportBarrier, Long> {

    List<SupportBarrier> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    boolean existsByStudentIdAndBarrierTypeAndActiveTrue(
            Long studentId, String barrierType);
}

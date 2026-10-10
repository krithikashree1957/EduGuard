package com.eduguard.eduguard.intervention;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

interface CaseActivityRepository extends JpaRepository<CaseActivity, Long> {
    List<CaseActivity> findByCaseIdOrderByCreatedAtAscActivityIdAsc(Long caseId);
}

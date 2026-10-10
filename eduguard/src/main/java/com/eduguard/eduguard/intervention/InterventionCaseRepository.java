package com.eduguard.eduguard.intervention;

import org.springframework.data.jpa.repository.*;
import java.time.LocalDate;
import java.util.List;

interface InterventionCaseRepository extends JpaRepository<InterventionCase, Long>,
        JpaSpecificationExecutor<InterventionCase> {
    List<InterventionCase> findByStudentIdOrderByUpdatedAtDescCaseIdDesc(Long studentId);
    List<InterventionCase> findByFollowUpDateAndStatusNotOrderByFollowUpDateAscCaseIdAsc(
            LocalDate date, CaseStatus status);
    List<InterventionCase> findByFollowUpDateBeforeAndStatusNotOrderByFollowUpDateAscCaseIdAsc(
            LocalDate date, CaseStatus status);
}

package com.eduguard.eduguard.intervention;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class InterventionCaseRepositoryTest {
    @Autowired
    private InterventionCaseRepository cases;

    @Test
    void listsAndPagesPersistedCasesInUpdatedAtAndCaseIdOrder() {
        InterventionCase older = new InterventionCase(1L, "Earlier support", "Description",
                CaseCategory.ACADEMIC, CasePriority.MEDIUM, null, LocalDate.of(2026, 10, 20));
        InterventionCase newer = new InterventionCase(2L, "Later support", "Description",
                CaseCategory.WELLBEING, CasePriority.HIGH, null, LocalDate.of(2026, 10, 22));
        cases.save(older);
        cases.save(newer);
        cases.flush();

        var result = cases.findAll((Specification<InterventionCase>) null,
                PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "updatedAt", "caseId")));

        assertEquals(2, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(newer.getCaseId(), result.getContent().getFirst().getCaseId());
    }
}

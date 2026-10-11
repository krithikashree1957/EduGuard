package com.eduguard.eduguard.intervention;

import com.eduguard.eduguard.student.StudentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InterventionServiceTest {
    private final StudentRepository students = mock(StudentRepository.class);
    private final InterventionCaseRepository cases = mock(InterventionCaseRepository.class);
    private final CaseActivityRepository activities = mock(CaseActivityRepository.class);
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-10-10T00:00:00Z"), ZoneOffset.UTC);
    private final InterventionService service =
            new InterventionService(students, cases, activities, clock);

    @Test
    void rejectsCreationForUnknownStudent() {
        when(students.existsById(99L)).thenReturn(false);
        CreateCaseRequest request = request(99L);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class, () -> service.create(99L, request));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verifyNoInteractions(cases, activities);
    }

    @Test
    void createsCaseAndActivityTogether() {
        when(students.existsById(1L)).thenReturn(true);
        InterventionCase saved = mock(InterventionCase.class);
        when(saved.getCaseId()).thenReturn(7L);
        when(saved.getStatus()).thenReturn(CaseStatus.OPEN);
        when(cases.save(any())).thenReturn(saved);

        CaseResponse result = service.create(1L, request(1L));

        assertEquals(7L, result.caseId());
        verify(cases).save(any(InterventionCase.class));
        verify(activities).save(any(CaseActivity.class));
    }

    @Test
    void searchesWithoutOptionalFilters() {
        Pageable pageable = PageRequest.of(0, 20,
                Sort.by(Sort.Direction.DESC, "updatedAt", "caseId"));
        when(cases.findAll(any(org.springframework.data.jpa.domain.Specification.class),
                any(Pageable.class))).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        assertEquals(0, service.search(null, null, null, null, null, pageable)
                .getTotalElements());

        verify(cases).findAll(any(org.springframework.data.jpa.domain.Specification.class),
                eq(pageable));
    }

    @Test
    void searchesWithCombinedOptionalFilters() {
        Pageable pageable = PageRequest.of(1, 5,
                Sort.by(Sort.Direction.DESC, "updatedAt", "caseId"));
        when(cases.findAll(any(org.springframework.data.jpa.domain.Specification.class),
                any(Pageable.class))).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        service.search(17L, CaseStatus.OPEN, CasePriority.HIGH,
                CaseCategory.ACADEMIC, "staff-1", pageable);

        verify(cases).findAll(any(org.springframework.data.jpa.domain.Specification.class),
                eq(pageable));
    }

    @Test
    void rejectsInvalidTransitionAndClosingWithoutResolution() {
        InterventionCase current = mock(InterventionCase.class);
        when(current.getStatus()).thenReturn(CaseStatus.CLOSED);
        when(cases.findById(3L)).thenReturn(java.util.Optional.of(current));

        ResponseStatusException transition = assertThrows(
                ResponseStatusException.class,
                () -> service.update(3L, new UpdateCaseRequest(
                        null, null, null, null, null, null,
                        CaseStatus.OPEN, null, null)));
        assertEquals(HttpStatus.BAD_REQUEST, transition.getStatusCode());

        when(current.getStatus()).thenReturn(CaseStatus.OPEN);
        ResponseStatusException resolution = assertThrows(
                ResponseStatusException.class,
                () -> service.update(3L, new UpdateCaseRequest(
                        null, null, null, null, null, null,
                        CaseStatus.CLOSED, null, null)));
        assertEquals(HttpStatus.BAD_REQUEST, resolution.getStatusCode());
    }

    @Test
    void dueUsesTodayAndOverdueExcludesClosedCases() {
        InterventionCase due = mock(InterventionCase.class);
        InterventionCase overdue = mock(InterventionCase.class);
        when(cases.findByFollowUpDateAndStatusNotOrderByFollowUpDateAscCaseIdAsc(
                LocalDate.of(2026, 10, 10), CaseStatus.CLOSED))
                .thenReturn(List.of(due));
        when(cases.findByFollowUpDateBeforeAndStatusNotOrderByFollowUpDateAscCaseIdAsc(
                LocalDate.of(2026, 10, 10), CaseStatus.CLOSED))
                .thenReturn(List.of(overdue));
        when(due.getCaseId()).thenReturn(1L);
        when(overdue.getCaseId()).thenReturn(2L);

        assertEquals(1, service.due(false).size());
        assertEquals(2L, service.due(true).get(0).caseId());
    }

    private static CreateCaseRequest request(Long studentId) {
        return new CreateCaseRequest(studentId, "Study plan", "Needs support",
                CaseCategory.ACADEMIC, CasePriority.MEDIUM, "staff-1",
                LocalDate.of(2026, 10, 11), "actor-1");
    }
}

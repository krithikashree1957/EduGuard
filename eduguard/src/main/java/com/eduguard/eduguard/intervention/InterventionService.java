package com.eduguard.eduguard.intervention;

import com.eduguard.eduguard.student.StudentRepository;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
public class InterventionService {
    private final StudentRepository students;
    private final InterventionCaseRepository cases;
    private final CaseActivityRepository activities;
    private final Clock clock;

    public InterventionService(StudentRepository students,
                               InterventionCaseRepository cases,
                               CaseActivityRepository activities,
                               Clock clock) {
        this.students = students;
        this.cases = cases;
        this.activities = activities;
        this.clock = clock;
    }

    @Transactional
    public CaseResponse create(Long studentId, CreateCaseRequest request) {
        requireStudent(studentId);
        InterventionCase saved = cases.save(new InterventionCase(
                studentId, request.title(), request.description(),
                request.category(), request.priority(),
                request.assignedStaffId(), request.followUpDate()));
        activities.save(new CaseActivity(saved.getCaseId(), "CREATED",
                "Case created", null, CaseStatus.OPEN, request.actorId()));
        return response(saved);
    }

    @Transactional(readOnly = true)
    public Page<CaseResponse> search(Long studentId, CaseStatus status,
                                     CasePriority priority, CaseCategory category,
                                     String assignedStaffId, Pageable pageable) {
        Specification<InterventionCase> specification =
                (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
        if (studentId != null) specification = specification.and((root, q, cb) ->
                cb.equal(root.get("studentId"), studentId));
        if (status != null) specification = specification.and((root, q, cb) ->
                cb.equal(root.get("status"), status));
        if (priority != null) specification = specification.and((root, q, cb) ->
                cb.equal(root.get("priority"), priority));
        if (category != null) specification = specification.and((root, q, cb) ->
                cb.equal(root.get("category"), category));
        if (assignedStaffId != null) specification = specification.and((root, q, cb) ->
                cb.equal(root.get("assignedStaffId"), assignedStaffId));
        return cases.findAll(specification, pageable).map(this::response);
    }

    @Transactional(readOnly = true)
    public CaseResponse get(Long caseId) {
        return response(findCase(caseId));
    }

    @Transactional
    public CaseResponse update(Long caseId, UpdateCaseRequest request) {
        InterventionCase current = findCase(caseId);
        CaseStatus previousStatus = current.getStatus();
        if (request.title() != null && request.title().isBlank()) {
            throw badRequest("Title must not be blank");
        }
        if (request.description() != null && request.description().isBlank()) {
            throw badRequest("Description must not be blank");
        }
        if (request.status() != null && request.status() != previousStatus) {
            validateTransition(previousStatus, request.status());
            if (request.status() == CaseStatus.CLOSED
                    && isBlank(request.resolutionSummary())
                    && isBlank(current.getResolutionSummary())) {
                throw badRequest("A resolution summary is required before closing a case");
            }
        }
        if (request.resolutionSummary() != null
                && request.resolutionSummary().isBlank()) {
            throw badRequest("Resolution summary must not be blank");
        }
        current.update(request.title(), request.description(), request.category(),
                request.priority(), request.assignedStaffId(), request.followUpDate(),
                request.status(), request.resolutionSummary());
        InterventionCase saved = cases.save(current);
        if (request.status() != null && request.status() != previousStatus) {
            activities.save(new CaseActivity(caseId, "STATUS_CHANGED",
                    "Status changed from " + previousStatus + " to " + request.status(),
                    previousStatus, request.status(), request.actorId()));
        } else {
            activities.save(new CaseActivity(caseId, "UPDATED",
                    "Case details updated", previousStatus, previousStatus,
                    request.actorId()));
        }
        return response(saved);
    }

    @Transactional(readOnly = true)
    public List<ActivityResponse> activities(Long caseId) {
        findCase(caseId);
        return this.activities.findByCaseIdOrderByCreatedAtAscActivityIdAsc(caseId)
                .stream().map(this::activityResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<CaseResponse> due(boolean overdue) {
        LocalDate today = LocalDate.now(clock);
        List<InterventionCase> result = overdue
                ? cases.findByFollowUpDateBeforeAndStatusNotOrderByFollowUpDateAscCaseIdAsc(
                    today, CaseStatus.CLOSED)
                : cases.findByFollowUpDateAndStatusNotOrderByFollowUpDateAscCaseIdAsc(
                    today, CaseStatus.CLOSED);
        return result.stream().map(this::response).toList();
    }

    private void requireStudent(Long id) {
        if (!students.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found");
        }
    }

    private InterventionCase findCase(Long id) {
        return cases.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Case not found"));
    }

    private void validateTransition(CaseStatus from, CaseStatus to) {
        boolean valid = (from == CaseStatus.OPEN && to == CaseStatus.IN_PROGRESS)
                || (from == CaseStatus.IN_PROGRESS && to == CaseStatus.ON_HOLD)
                || (from == CaseStatus.ON_HOLD && to == CaseStatus.IN_PROGRESS)
                || (to == CaseStatus.CLOSED && from != CaseStatus.CLOSED);
        if (!valid) throw badRequest("Invalid case status transition");
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private CaseResponse response(InterventionCase c) {
        return new CaseResponse(c.getCaseId(), c.getStudentId(), c.getTitle(),
                c.getDescription(), c.getCategory(), c.getPriority(), c.getStatus(),
                c.getAssignedStaffId(), c.getFollowUpDate(), c.getResolutionSummary(),
                c.getCreatedAt(), c.getUpdatedAt());
    }

    private ActivityResponse activityResponse(CaseActivity a) {
        return new ActivityResponse(a.getActivityId(), a.getCaseId(),
                a.getActivityType(), a.getDescription(), a.getPreviousStatus(),
                a.getNewStatus(), a.getActorId(), a.getCreatedAt());
    }
}

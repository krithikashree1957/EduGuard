package com.eduguard.eduguard.intervention;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/interventions")
public class InterventionController {
    private final InterventionService service;

    public InterventionController(InterventionService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CaseResponse create(@Valid @RequestBody CreateCaseRequest request) {
        return service.create(request.studentId(), request);
    }

    @GetMapping
    public PageResponse<CaseResponse> search(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) CaseStatus status,
            @RequestParam(required = false) CasePriority priority,
            @RequestParam(required = false) CaseCategory category,
            @RequestParam(required = false) String assignedStaffId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return pageResponse(service.search(studentId, status, priority, category,
                assignedStaffId, PageRequest.of(page, size,
                        Sort.by(Sort.Direction.DESC, "updatedAt", "caseId"))));
    }

    @GetMapping("/follow-ups/due")
    public List<CaseResponse> due() { return service.due(false); }

    @GetMapping("/follow-ups/overdue")
    public List<CaseResponse> overdue() { return service.due(true); }

    @GetMapping("/student/{studentId}")
    public PageResponse<CaseResponse> byStudent(@PathVariable @Positive Long studentId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return pageResponse(service.search(studentId, null, null, null, null,
                PageRequest.of(page, size,
                        Sort.by(Sort.Direction.DESC, "updatedAt", "caseId"))));
    }

    @GetMapping("/{caseId}")
    public CaseResponse get(@PathVariable @Positive Long caseId) {
        return service.get(caseId);
    }

    @PatchMapping("/{caseId}")
    public CaseResponse update(@PathVariable @Positive Long caseId,
                               @Valid @RequestBody UpdateCaseRequest request) {
        return service.update(caseId, request);
    }

    @GetMapping("/{caseId}/activities")
    public List<ActivityResponse> activities(@PathVariable @Positive Long caseId) {
        return service.activities(caseId);
    }

    private PageResponse<CaseResponse> pageResponse(Page<CaseResponse> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(),
                page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}

package com.eduguard.eduguard.support;

import com.eduguard.eduguard.student.StudentRepository;
import com.eduguard.eduguard.support.SupportRequests.BarrierRequest;
import com.eduguard.eduguard.support.SupportRequests.BarrierResponse;
import com.eduguard.eduguard.support.SupportRequests.FinancialSupportRequest;
import com.eduguard.eduguard.support.SupportRequests.FinancialSupportResponse;
import com.eduguard.eduguard.support.SupportRequests.WellbeingRequestPayload;
import com.eduguard.eduguard.support.SupportRequests.WellbeingResponse;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class StudentSupportService {

    private final StudentRepository studentRepository;
    private final SupportBarrierRepository barrierRepository;
    private final FinancialSupportRepository financialRepository;
    private final WellbeingRequestRepository wellbeingRepository;

    public StudentSupportService(
            StudentRepository studentRepository,
            SupportBarrierRepository barrierRepository,
            FinancialSupportRepository financialRepository,
            WellbeingRequestRepository wellbeingRepository) {
        this.studentRepository = studentRepository;
        this.barrierRepository = barrierRepository;
        this.financialRepository = financialRepository;
        this.wellbeingRepository = wellbeingRepository;
    }

    @Transactional(readOnly = true)
    public List<BarrierResponse> getBarriers(Long studentId) {
        requireStudent(studentId);
        return barrierRepository.findByStudentIdOrderByCreatedAtDesc(studentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public BarrierResponse addBarrier(Long studentId, BarrierRequest request) {
        requireStudent(studentId);
        if (barrierRepository.existsByStudentIdAndBarrierTypeAndActiveTrue(
                studentId, request.barrierType())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An active barrier of this type already exists");
        }
        try {
            return toResponse(barrierRepository.saveAndFlush(new SupportBarrier(
                    studentId, request.barrierType(), request.description(),
                    request.severity())));
        } catch (DataIntegrityViolationException exception) {
            if (isActiveBarrierUniqueViolation(exception)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "An active barrier of this type already exists");
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<FinancialSupportResponse> getFinancialSupport(Long studentId) {
        requireStudent(studentId);
        return financialRepository.findByStudentIdOrderByCreatedAtDesc(studentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public FinancialSupportResponse addFinancialSupport(
            Long studentId, FinancialSupportRequest request) {
        requireStudent(studentId);
        return toResponse(financialRepository.save(new FinancialSupport(
                studentId, request.supportType(), request.status(),
                request.notes())));
    }

    @Transactional(readOnly = true)
    public List<WellbeingResponse> getWellbeingRequests(Long studentId) {
        requireStudent(studentId);
        return wellbeingRepository.findByStudentIdOrderByCreatedAtDesc(studentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public WellbeingResponse addWellbeingRequest(
            Long studentId, WellbeingRequestPayload request) {
        requireStudent(studentId);
        return toResponse(wellbeingRepository.save(new WellbeingRequest(
                studentId, request.supportType(), request.status(),
                request.notes())));
    }

    private void requireStudent(Long studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Student not found");
        }
    }

    private boolean isActiveBarrierUniqueViolation(
            DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation
                    && "uq_active_student_support_barrier".equals(
                            violation.getConstraintName())) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private BarrierResponse toResponse(SupportBarrier barrier) {
        return new BarrierResponse(
                barrier.getBarrierId(), barrier.getStudentId(),
                barrier.getBarrierType(), barrier.getDescription(),
                barrier.getSeverity(), barrier.isActive(),
                barrier.getCreatedAt());
    }

    private FinancialSupportResponse toResponse(FinancialSupport support) {
        return new FinancialSupportResponse(
                support.getFinancialSupportId(), support.getStudentId(),
                support.getSupportType(), support.getStatus(),
                support.getNotes(), support.getCreatedAt());
    }

    private WellbeingResponse toResponse(WellbeingRequest request) {
        return new WellbeingResponse(
                request.getWellbeingRequestId(), request.getStudentId(),
                request.getSupportType(), request.getStatus(),
                request.getNotes(), request.getCreatedAt());
    }
}

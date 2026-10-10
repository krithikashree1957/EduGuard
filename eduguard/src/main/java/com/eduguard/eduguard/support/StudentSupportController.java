package com.eduguard.eduguard.support;

import com.eduguard.eduguard.support.SupportRequests.BarrierRequest;
import com.eduguard.eduguard.support.SupportRequests.BarrierResponse;
import com.eduguard.eduguard.support.SupportRequests.FinancialSupportRequest;
import com.eduguard.eduguard.support.SupportRequests.FinancialSupportResponse;
import com.eduguard.eduguard.support.SupportRequests.WellbeingRequestPayload;
import com.eduguard.eduguard.support.SupportRequests.WellbeingResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/students/{studentId}/support")
public class StudentSupportController {

    private final StudentSupportService supportService;

    public StudentSupportController(StudentSupportService supportService) {
        this.supportService = supportService;
    }

    @GetMapping("/barriers")
    public List<BarrierResponse> getBarriers(
            @PathVariable @Positive Long studentId) {
        return supportService.getBarriers(studentId);
    }

    @PostMapping("/barriers")
    @ResponseStatus(HttpStatus.CREATED)
    public BarrierResponse addBarrier(
            @PathVariable @Positive Long studentId,
            @Valid @RequestBody BarrierRequest request) {
        return supportService.addBarrier(studentId, request);
    }

    @GetMapping("/financial")
    public List<FinancialSupportResponse> getFinancialSupport(
            @PathVariable @Positive Long studentId) {
        return supportService.getFinancialSupport(studentId);
    }

    @PostMapping("/financial")
    @ResponseStatus(HttpStatus.CREATED)
    public FinancialSupportResponse addFinancialSupport(
            @PathVariable @Positive Long studentId,
            @Valid @RequestBody FinancialSupportRequest request) {
        return supportService.addFinancialSupport(studentId, request);
    }

    @GetMapping("/wellbeing")
    public List<WellbeingResponse> getWellbeingRequests(
            @PathVariable @Positive Long studentId) {
        return supportService.getWellbeingRequests(studentId);
    }

    @PostMapping("/wellbeing")
    @ResponseStatus(HttpStatus.CREATED)
    public WellbeingResponse addWellbeingRequest(
            @PathVariable @Positive Long studentId,
            @Valid @RequestBody WellbeingRequestPayload request) {
        return supportService.addWellbeingRequest(studentId, request);
    }
}

package com.eduguard.eduguard.support;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class SupportRequests {

    private SupportRequests() {
    }

    public record BarrierRequest(
            @NotBlank @Size(max = 50) String barrierType,
            @Size(max = 500) String description,
            @NotBlank @Pattern(regexp = "LOW|MEDIUM|HIGH") String severity) {
    }

    public record FinancialSupportRequest(
            @NotBlank @Size(max = 50) String supportType,
            @NotBlank @Pattern(regexp = "REQUESTED|IN_REVIEW|APPROVED|DECLINED|CLOSED")
            String status,
            @Size(max = 500) String notes) {
    }

    public record WellbeingRequestPayload(
            @NotBlank @Size(max = 50) String supportType,
            @NotBlank @Pattern(regexp = "REQUESTED|IN_REVIEW|RESOLVED|CLOSED")
            String status,
            @Size(max = 500) String notes) {
    }

    public record BarrierResponse(
            Long barrierId,
            Long studentId,
            String barrierType,
            String description,
            String severity,
            boolean active,
            LocalDateTime createdAt) {
    }

    public record FinancialSupportResponse(
            Long financialSupportId,
            Long studentId,
            String supportType,
            String status,
            String notes,
            LocalDateTime createdAt) {
    }

    public record WellbeingResponse(
            Long wellbeingRequestId,
            Long studentId,
            String supportType,
            String status,
            String notes,
            LocalDateTime createdAt) {
    }
}

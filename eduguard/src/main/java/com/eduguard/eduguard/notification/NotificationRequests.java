package com.eduguard.eduguard.notification;

import jakarta.validation.constraints.*;
import java.util.Map;

record NotificationRequest(
        @NotBlank @Size(max = 80) String eventType,
        @NotBlank @Size(max = 150) String recipientReference,
        @NotNull NotificationChannel channel,
        @NotBlank @Size(max = 80) String templateId,
        @NotNull @Size(max = 20) Map<String, String> parameters,
        @Size(max = 200) String idempotencyKey) {}

record NotificationResponse(Long notificationId, String recipientReference,
        NotificationChannel channel, String eventType, String templateId,
        String subject, String message, NotificationStatus status,
        java.time.LocalDateTime createdAt, java.time.LocalDateTime lastAttemptAt,
        java.time.LocalDateTime deliveredAt, int attemptCount, String errorSummary,
        String idempotencyKey, String deliveryMode, boolean read) {}

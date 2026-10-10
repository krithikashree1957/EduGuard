package com.eduguard.eduguard.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
public class NotificationService {
    private final NotificationRepository notifications;
    private final NotificationAttemptRepository attempts;
    private final NotificationTemplateService templates;
    private final NotificationDelivery delivery;
    private final int maxAttempts;

    public NotificationService(NotificationRepository notifications,
                               NotificationAttemptRepository attempts,
                               NotificationTemplateService templates,
                               NotificationDelivery delivery,
                               @Value("${eduguard.notifications.max-attempts:3}") int maxAttempts) {
        this.notifications = notifications; this.attempts = attempts;
        this.templates = templates; this.delivery = delivery;
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    @Transactional
    public NotificationResponse request(NotificationRequest request) {
        if (request.idempotencyKey() != null) {
            var existing = notifications.findByIdempotencyKey(request.idempotencyKey());
            if (existing.isPresent()) return response(existing.get());
        }
        RenderedTemplate rendered;
        try {
            rendered = templates.render(request.templateId(), request.eventType(),
                    request.parameters());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid notification template or parameters");
        }
        Notification notification = new Notification(request.recipientReference(),
                request.channel(), request.eventType(), request.templateId(),
                rendered.subject(), rendered.message(), request.idempotencyKey());
        try {
            notification = notifications.save(notification);
            DeliveryResult result = delivery.deliver(notification);
            if (result.success()) {
                notification.sent(result.mode());
                attempts.save(new NotificationAttempt(notification.getNotificationId(),
                        "SENT", null));
            } else {
                notification.failed(result.safeError(), result.retryable(), maxAttempts);
                attempts.save(new NotificationAttempt(notification.getNotificationId(),
                        "FAILED", result.safeError()));
            }
            return response(notifications.save(notification));
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Notification idempotency key already exists");
        }
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(String recipient) {
        if (recipient == null || recipient.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "recipientReference is required");
        }
        return notifications.findByRecipientReferenceOrderByCreatedAtDescNotificationIdDesc(
                recipient).stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public NotificationResponse get(Long id) {
        return response(notifications.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Notification not found")));
    }

    @Transactional
    public NotificationResponse markRead(Long id) {
        Notification notification = notifications.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Notification not found"));
        if (notification.getChannel() != NotificationChannel.IN_APP) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only in-app notifications can be marked as read");
        }
        notification.markRead();
        return response(notifications.save(notification));
    }

    private NotificationResponse response(Notification n) {
        return new NotificationResponse(n.getNotificationId(), n.getRecipientReference(),
                n.getChannel(), n.getEventType(), n.getTemplateId(), n.getSubject(),
                n.getMessage(), n.getStatus(), n.getCreatedAt(), n.getLastAttemptAt(),
                n.getDeliveredAt(), n.getAttemptCount(), n.getErrorSummary(),
                n.getIdempotencyKey(), n.getDeliveryMode(), n.getReadAt() != null);
    }
}

package com.eduguard.eduguard.notification;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications",
        uniqueConstraints = @UniqueConstraint(name = "uq_notification_idempotency",
                columnNames = "idempotency_key"))
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id") private Long notificationId;
    @Column(name = "recipient_reference", nullable = false, length = 150)
    private String recipientReference;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private NotificationChannel channel;
    @Column(name = "event_type", nullable = false, length = 80) private String eventType;
    @Column(name = "template_id", nullable = false, length = 80) private String templateId;
    @Column(length = 200) private String subject;
    @Column(nullable = false, length = 4000) private String message;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private NotificationStatus status;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "last_attempt_at") private LocalDateTime lastAttemptAt;
    @Column(name = "delivered_at") private LocalDateTime deliveredAt;
    @Column(name = "attempt_count", nullable = false) private int attemptCount;
    @Column(name = "error_summary", length = 500) private String errorSummary;
    @Column(name = "idempotency_key", length = 200) private String idempotencyKey;
    @Column(name = "delivery_mode", nullable = false, length = 20) private String deliveryMode;
    @Column(name = "read_at") private LocalDateTime readAt;

    protected Notification() {}
    Notification(String recipient, NotificationChannel channel, String eventType,
                 String templateId, String subject, String message, String key) {
        this.recipientReference = recipient; this.channel = channel;
        this.eventType = eventType; this.templateId = templateId;
        this.subject = subject; this.message = message; this.idempotencyKey = key;
        this.status = NotificationStatus.PENDING; this.deliveryMode = "NOT_ATTEMPTED";
    }
    @PrePersist void create() { createdAt = LocalDateTime.now(); }
    public Long getNotificationId() { return notificationId; }
    public String getRecipientReference() { return recipientReference; }
    public NotificationChannel getChannel() { return channel; }
    public String getEventType() { return eventType; }
    public String getTemplateId() { return templateId; }
    public String getSubject() { return subject; }
    public String getMessage() { return message; }
    public NotificationStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastAttemptAt() { return lastAttemptAt; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public int getAttemptCount() { return attemptCount; }
    public String getErrorSummary() { return errorSummary; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getDeliveryMode() { return deliveryMode; }
    public LocalDateTime getReadAt() { return readAt; }
    void sent(String mode) { status = NotificationStatus.SENT; deliveryMode = mode;
        deliveredAt = LocalDateTime.now(); errorSummary = null; }
    void failed(String error, boolean retryable, int max) {
        attemptCount++; lastAttemptAt = LocalDateTime.now();
        errorSummary = error; status = retryable && attemptCount < max
                ? NotificationStatus.RETRYING : NotificationStatus.FAILED;
    }
    void attempted() { attemptCount++; lastAttemptAt = LocalDateTime.now(); }
    void markRead() { readAt = LocalDateTime.now(); }
}

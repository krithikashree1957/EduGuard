package com.eduguard.eduguard.notification;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_attempts")
public class NotificationAttempt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attempt_id") private Long attemptId;
    @Column(name = "notification_id", nullable = false) private Long notificationId;
    @Column(nullable = false, length = 20) private String outcome;
    @Column(name = "error_summary", length = 500) private String errorSummary;
    @Column(name = "attempted_at", nullable = false) private LocalDateTime attemptedAt;
    protected NotificationAttempt() {}
    NotificationAttempt(Long id, String outcome, String error) {
        notificationId = id; this.outcome = outcome; errorSummary = error;
    }
    @PrePersist void create() { attemptedAt = LocalDateTime.now(); }
}

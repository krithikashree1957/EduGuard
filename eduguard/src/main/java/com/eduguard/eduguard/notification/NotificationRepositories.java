package com.eduguard.eduguard.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

interface NotificationRepository extends JpaRepository<Notification, Long> {
    Optional<Notification> findByIdempotencyKey(String key);
    List<Notification> findByRecipientReferenceOrderByCreatedAtDescNotificationIdDesc(
            String recipientReference);
}

interface NotificationAttemptRepository extends JpaRepository<NotificationAttempt, Long> {}

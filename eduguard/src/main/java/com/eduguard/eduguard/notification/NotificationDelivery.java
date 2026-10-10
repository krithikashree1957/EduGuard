package com.eduguard.eduguard.notification;

interface NotificationDelivery {
    DeliveryResult deliver(Notification notification);
}

record DeliveryResult(boolean success, boolean retryable, String mode,
                      String safeError) {}

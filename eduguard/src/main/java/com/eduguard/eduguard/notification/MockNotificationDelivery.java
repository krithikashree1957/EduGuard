package com.eduguard.eduguard.notification;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "eduguard.notifications.email-mode",
        havingValue = "mock", matchIfMissing = true)
class MockNotificationDelivery implements NotificationDelivery {
    public DeliveryResult deliver(Notification notification) {
        return new DeliveryResult(true, false,
                notification.getChannel() == NotificationChannel.EMAIL
                        ? "MOCK_SIMULATED" : "IN_APP", null);
    }
}

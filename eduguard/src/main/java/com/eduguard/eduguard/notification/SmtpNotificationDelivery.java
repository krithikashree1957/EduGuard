package com.eduguard.eduguard.notification;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

@Component
@ConditionalOnProperty(name = "eduguard.notifications.email-mode", havingValue = "smtp")
class SmtpNotificationDelivery implements NotificationDelivery {
    private final JavaMailSender sender;
    private final String from;
    SmtpNotificationDelivery(JavaMailSender sender,
                             @Value("${eduguard.notifications.sender}") String from) {
        this.sender = sender; this.from = from;
    }
    public DeliveryResult deliver(Notification notification) {
        if (notification.getChannel() != NotificationChannel.EMAIL) {
            return new DeliveryResult(true, false, "IN_APP", null);
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(notification.getRecipientReference());
            message.setSubject(notification.getSubject());
            message.setText(notification.getMessage());
            sender.send(message);
            return new DeliveryResult(true, false, "SMTP", null);
        } catch (RuntimeException exception) {
            return new DeliveryResult(false, true, "SMTP", "Email delivery failed");
        }
    }
}

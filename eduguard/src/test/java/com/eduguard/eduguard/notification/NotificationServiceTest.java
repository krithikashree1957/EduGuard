package com.eduguard.eduguard.notification;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationServiceTest {
    private final NotificationRepository repository = mock(NotificationRepository.class);
    private final NotificationAttemptRepository attempts = mock(NotificationAttemptRepository.class);
    private final NotificationTemplateService templates = new NotificationTemplateService();
    private final NotificationDelivery delivery = mock(NotificationDelivery.class);
    private final NotificationService service = new NotificationService(
            repository, attempts, templates, delivery, 3);

    @Test
    void acceptsMockInAppNotificationAndRendersSafeTemplate() {
        when(repository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(delivery.deliver(any())).thenReturn(new DeliveryResult(true, false, "IN_APP", null));

        NotificationResponse response = service.request(new NotificationRequest(
                "INTERVENTION_ASSIGNED", "staff-1", NotificationChannel.IN_APP,
                "intervention-assigned", Map.of("caseReference", "CASE-1"), "key-1"));

        assertEquals(NotificationStatus.SENT, response.status());
        assertEquals("IN_APP", response.deliveryMode());
        assertTrue(response.message().contains("CASE-1"));
        verify(attempts).save(any(NotificationAttempt.class));
    }

    @Test
    void returnsExistingNotificationForRepeatedIdempotencyKey() {
        Notification existing = mock(Notification.class);
        when(repository.findByIdempotencyKey("key-1")).thenReturn(Optional.of(existing));
        when(existing.getStatus()).thenReturn(NotificationStatus.SENT);
        when(existing.getChannel()).thenReturn(NotificationChannel.IN_APP);

        NotificationResponse response = service.request(new NotificationRequest(
                "INTERVENTION_ASSIGNED", "staff-1", NotificationChannel.IN_APP,
                "intervention-assigned",
                Map.of("caseReference", "CASE-1"), "key-1"));
        assertEquals(NotificationStatus.SENT, response.status());
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsMissingTemplateParametersAndWrongReadChannel() {
        ResponseStatusException invalid = assertThrows(ResponseStatusException.class,
                () -> service.request(new NotificationRequest(
                        "INTERVENTION_ASSIGNED", "staff-1", NotificationChannel.IN_APP,
                        "intervention-assigned",
                        Map.of(), null)));
        assertEquals(HttpStatus.BAD_REQUEST, invalid.getStatusCode());

        Notification email = mock(Notification.class);
        when(repository.findById(2L)).thenReturn(Optional.of(email));
        when(email.getChannel()).thenReturn(NotificationChannel.EMAIL);
        ResponseStatusException readError = assertThrows(ResponseStatusException.class,
                () -> service.markRead(2L));
        assertEquals(HttpStatus.BAD_REQUEST, readError.getStatusCode());
    }
}

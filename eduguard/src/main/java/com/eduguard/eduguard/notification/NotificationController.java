package com.eduguard.eduguard.notification;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService service;
    public NotificationController(NotificationService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NotificationResponse request(@Valid @RequestBody NotificationRequest request) {
        return service.request(request);
    }

    @GetMapping
    public List<NotificationResponse> list(@RequestParam String recipientReference) {
        return service.list(recipientReference);
    }

    @GetMapping("/{notificationId}")
    public NotificationResponse get(@PathVariable Long notificationId) {
        return service.get(notificationId);
    }

    @PatchMapping("/{notificationId}/read")
    public NotificationResponse markRead(@PathVariable Long notificationId) {
        return service.markRead(notificationId);
    }
}

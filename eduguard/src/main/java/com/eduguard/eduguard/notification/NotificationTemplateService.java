package com.eduguard.eduguard.notification;

import org.springframework.stereotype.Component;
import java.util.*;

@Component
class NotificationTemplateService {
    private record Template(String event, String subject, String body,
                            Set<String> required) {}
    private final Map<String, Template> templates = Map.of(
            "intervention-assigned", new Template("INTERVENTION_ASSIGNED",
                    "Intervention assigned", "A support case {{caseReference}} was assigned to you.",
                    Set.of("caseReference")),
            "follow-up-due", new Template("FOLLOW_UP_DUE",
                    "Follow-up due", "Follow-up {{caseReference}} is due.",
                    Set.of("caseReference")),
            "intervention-status-updated", new Template("INTERVENTION_STATUS_UPDATED",
                    "Intervention status updated", "Support case {{caseReference}} is now {{status}}.",
                    Set.of("caseReference", "status")),
            "student-support-information", new Template("STUDENT_SUPPORT_INFORMATION",
                    "Student support information", "A student-support update is available.",
                    Set.of()));

    RenderedTemplate render(String id, String event, Map<String, String> parameters) {
        Template template = templates.get(id);
        if (template == null || !template.event().equals(event)) {
            throw new IllegalArgumentException("Unsupported notification template");
        }
        if (!parameters.keySet().containsAll(template.required())) {
            throw new IllegalArgumentException("Required notification parameters are missing");
        }
        String body = template.body();
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null
                    || entry.getValue().length() > 200) {
                throw new IllegalArgumentException("Invalid notification parameter");
            }
            body = body.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return new RenderedTemplate(template.subject(), body);
    }
}

record RenderedTemplate(String subject, String message) {}

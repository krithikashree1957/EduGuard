package com.eduguard.eduguard.notification;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationControllerTest {
    private final NotificationService service = mock(NotificationService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(
            new NotificationController(service)).build();

    @Test
    void validatesNotificationRequest() throws Exception {
        mvc.perform(post("/api/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"recipientReference":"","channel":"IN_APP",
                         "eventType":"INTERVENTION_ASSIGNED",
                         "templateId":"intervention-assigned","parameters":{}}
                        """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}

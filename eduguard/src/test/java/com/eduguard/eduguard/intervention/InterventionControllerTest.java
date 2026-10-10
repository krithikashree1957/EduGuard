package com.eduguard.eduguard.intervention;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class InterventionControllerTest {
    private final InterventionService service = mock(InterventionService.class);
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new InterventionController(service)).build();

    @Test
    void validatesCreateRequest() throws Exception {
        mvc.perform(post("/api/interventions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId":0,"title":"","description":"",
                                 "category":"INVALID","priority":"LOW"}
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void exposesCreateAndActivityRoutes() throws Exception {
        CaseResponse response = new CaseResponse(8L, 1L, "Study plan",
                "Needs support", CaseCategory.ACADEMIC, CasePriority.MEDIUM,
                CaseStatus.OPEN, null, null, null, null, null);
        when(service.create(anyLong(), any())).thenReturn(response);

        mvc.perform(post("/api/interventions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId":1,"title":"Study plan",
                                 "description":"Needs support",
                                 "category":"ACADEMIC","priority":"MEDIUM"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.caseId").value(8))
                .andExpect(jsonPath("$.studentId").value(1));

        mvc.perform(get("/api/interventions/8/activities"))
                .andExpect(status().isOk());
        verify(service).activities(8L);
    }

    @Test
    void supportsFilteredPagedSearch() throws Exception {
        when(service.search(any(), any(), any(), any(), any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(
                        java.util.List.of(), org.springframework.data.domain.PageRequest.of(0, 10), 0));

        mvc.perform(get("/api/interventions")
                        .param("status", "OPEN")
                        .param("priority", "HIGH")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
        verify(service).search(eq(null), eq(CaseStatus.OPEN),
                eq(CasePriority.HIGH), eq(null), eq(null), any());
    }
}

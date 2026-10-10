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

    @Test
    void listsSavedCasesAsPagedJsonAndSupportsCaseRoutes() throws Exception {
        CaseResponse first = caseResponse(8L, "STU-008");
        CaseResponse second = caseResponse(7L, "STU-007");
        when(service.search(isNull(), isNull(), isNull(), isNull(), isNull(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(
                        java.util.List.of(first, second),
                        org.springframework.data.domain.PageRequest.of(0, 20), 2));
        when(service.get(8L)).thenReturn(first);
        when(service.update(eq(8L), any())).thenReturn(first);
        when(service.activities(8L)).thenReturn(java.util.List.of(
                new ActivityResponse(1L, 8L, "CREATED", "Case created",
                        null, CaseStatus.OPEN, "staff-1", null)));

        mvc.perform(get("/api/interventions").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].studentId").value(8))
                .andExpect(jsonPath("$.content[0].category").value("ACADEMIC"))
                .andExpect(jsonPath("$.content[0].priority").value("HIGH"))
                .andExpect(jsonPath("$.content[0].status").value("OPEN"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(2));

        mvc.perform(get("/api/interventions/8"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.caseId").value(8));
        mvc.perform(patch("/api/interventions/8")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Updated details\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/interventions/8/activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].activityType").value("CREATED"));
    }

    private static CaseResponse caseResponse(Long id, String studentCode) {
        return new CaseResponse(id, id, "Study plan for " + studentCode,
                "Needs support", CaseCategory.ACADEMIC, CasePriority.HIGH,
                CaseStatus.OPEN, "staff-1", java.time.LocalDate.of(2026, 10, 20),
                null, java.time.LocalDateTime.parse("2026-10-10T10:00:00"),
                java.time.LocalDateTime.parse("2026-10-10T10:00:00"));
    }
}

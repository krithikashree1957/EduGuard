package com.eduguard.eduguard.support;

import com.eduguard.eduguard.support.SupportRequests.BarrierResponse;
import com.eduguard.eduguard.support.SupportRequests.FinancialSupportResponse;
import com.eduguard.eduguard.support.SupportRequests.WellbeingResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StudentSupportControllerTest {

    private final StudentSupportService supportService =
            mock(StudentSupportService.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new StudentSupportController(supportService))
            .build();

    @Test
    void createsBarrierWithCreatedStatus() throws Exception {
        when(supportService.addBarrier(eq(1L), any()))
                .thenReturn(new BarrierResponse(
                        10L, 1L, "COMMUTING", "Long commute",
                        "MEDIUM", true, null));

        mockMvc.perform(post("/api/students/1/support/barriers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"barrierType":"COMMUTING",
                                 "description":"Long commute",
                                 "severity":"MEDIUM"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.barrierId").value(10))
                .andExpect(jsonPath("$.studentId").value(1))
                .andExpect(jsonPath("$.barrierType").value("COMMUTING"));

        when(supportService.getBarriers(1L)).thenReturn(List.of(
                new BarrierResponse(10L, 1L, "COMMUTING", "Long commute",
                        "MEDIUM", true, null)));
        mockMvc.perform(get("/api/students/1/support/barriers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].barrierId").value(10))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    void rejectsInvalidFinancialSupportStatus() throws Exception {
        mockMvc.perform(post("/api/students/1/support/financial")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"supportType":"FEE_DIFFICULTY",
                                 "status":"UNKNOWN",
                                 "notes":"Needs review"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsInvalidBarrierPayload() throws Exception {
        mockMvc.perform(post("/api/students/1/support/barriers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"barrierType":"",
                                 "description":"Invalid",
                                 "severity":"UNKNOWN"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsSeparateSupportCollections() throws Exception {
        when(supportService.getFinancialSupport(1L))
                .thenReturn(List.of(new FinancialSupportResponse(
                        2L, 1L, "SCHOLARSHIP", "REQUESTED", null, null)));
        when(supportService.getWellbeingRequests(1L))
                .thenReturn(List.of(new WellbeingResponse(
                        3L, 1L, "CHECK_IN", "REQUESTED", null, null)));

        mockMvc.perform(get("/api/students/1/support/financial"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].supportType").value("SCHOLARSHIP"));

        mockMvc.perform(get("/api/students/1/support/wellbeing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].supportType").value("CHECK_IN"));
    }

    @Test
    void createsAndRetrievesFinancialAndWellbeingSupport() throws Exception {
        when(supportService.addFinancialSupport(eq(1L), any()))
                .thenReturn(new FinancialSupportResponse(
                        4L, 1L, "FEE_DIFFICULTY", "REQUESTED",
                        "Needs review", null));
        when(supportService.getFinancialSupport(1L))
                .thenReturn(List.of(new FinancialSupportResponse(
                        4L, 1L, "FEE_DIFFICULTY", "REQUESTED",
                        "Needs review", null)));
        when(supportService.addWellbeingRequest(eq(1L), any()))
                .thenReturn(new WellbeingResponse(
                        5L, 1L, "CHECK_IN", "REQUESTED", "Please follow up", null));
        when(supportService.getWellbeingRequests(1L))
                .thenReturn(List.of(new WellbeingResponse(
                        5L, 1L, "CHECK_IN", "REQUESTED", "Please follow up", null)));

        mockMvc.perform(post("/api/students/1/support/financial")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"supportType":"FEE_DIFFICULTY",
                                 "status":"REQUESTED",
                                 "notes":"Needs review"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.financialSupportId").value(4))
                .andExpect(jsonPath("$.status").value("REQUESTED"));
        mockMvc.perform(get("/api/students/1/support/financial"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].supportType").value("FEE_DIFFICULTY"));

        mockMvc.perform(post("/api/students/1/support/wellbeing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"supportType":"CHECK_IN",
                                 "status":"REQUESTED",
                                 "notes":"Please follow up"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.wellbeingRequestId").value(5))
                .andExpect(jsonPath("$.status").value("REQUESTED"));
        mockMvc.perform(get("/api/students/1/support/wellbeing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].supportType").value("CHECK_IN"));
    }
}

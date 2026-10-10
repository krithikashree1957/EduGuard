package com.eduguard.risk_recommendation_service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RiskAssessmentHttpIntegrationTest {

    private MockRestServiceServer studentService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        studentService = MockRestServiceServer.bindTo(builder).build();
        StudentPerformanceClient client =
                new StudentPerformanceClient(builder, "http://student-data.test");
        mvc = MockMvcBuilders.standaloneSetup(new RiskAssessmentController(
                client, new RiskAssessmentCalculator(), new RecommendationGenerator()))
                .build();
    }

    @Test
    void returnsStructuredRiskAssessmentFromStudentPerformanceApi() throws Exception {
        studentService.expect(requestTo(
                        "http://student-data.test/api/students/42/performance"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "studentId": 42,
                          "attendancePercentage": 80,
                          "assessments": [{"percentage": 70}],
                          "participationScore": 60,
                          "assignmentCompletionRate": 90
                        }
                        """, MediaType.APPLICATION_JSON));

        mvc.perform(get("/api/risk/students/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(42))
                .andExpect(jsonPath("$.riskScore").value(24.5))
                .andExpect(jsonPath("$.riskTier").value("LOW"))
                .andExpect(jsonPath("$.assessmentStatus").value("COMPLETE"))
                .andExpect(jsonPath("$.availableIndicatorCount").value(4))
                .andExpect(jsonPath("$.indicators").isArray())
                .andExpect(jsonPath("$.recommendedInterventions").isArray());

        studentService.verify();
    }

    @Test
    void reportsUnprocessableWhenStudentServiceHasNoUsablePerformanceData()
            throws Exception {
        studentService.expect(requestTo(
                        "http://student-data.test/api/students/42/performance"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "studentId": 42,
                          "attendance": "No attendance data available",
                          "assessments": [],
                          "engagement": "No engagement data available"
                        }
                        """, MediaType.APPLICATION_JSON));

        mvc.perform(get("/api/risk/students/42"))
                .andExpect(status().is(HttpStatus.UNPROCESSABLE_ENTITY.value()));

        studentService.verify();
    }
}

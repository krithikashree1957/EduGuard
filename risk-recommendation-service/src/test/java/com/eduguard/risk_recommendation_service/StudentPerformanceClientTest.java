package com.eduguard.risk_recommendation_service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class StudentPerformanceClientTest {

    private MockRestServiceServer server;
    private StudentPerformanceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new StudentPerformanceClient(builder, "http://student-data.test");
    }

    @Test
    void requestsPerformanceFromConfiguredStudentServiceAndMapsResponse() {
        server.expect(requestTo(
                        "http://student-data.test/api/students/42/performance"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "attendancePercentage": 80,
                          "assessments": [{"percentage": 70}],
                          "participationScore": 60,
                          "assignmentCompletionRate": 90
                        }
                        """, MediaType.APPLICATION_JSON));

        PerformanceSnapshot snapshot = client.fetch(42L);

        assertEquals(80.0, snapshot.attendance());
        assertEquals(70.0, snapshot.assessmentAverage());
        assertEquals(60.0, snapshot.participation());
        assertEquals(90.0, snapshot.assignmentCompletion());
        server.verify();
    }

    @Test
    void mapsUnknownStudentWithoutExposingUpstreamResponse() {
        server.expect(requestTo(
                        "http://student-data.test/api/students/404/performance"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"detail\":\"private upstream error\"}"));

        ResponseStatusException failure = assertThrows(ResponseStatusException.class,
                () -> client.fetch(404L));

        assertEquals(HttpStatus.NOT_FOUND, failure.getStatusCode());
        assertEquals("Student not found", failure.getReason());
        server.verify();
    }

    @Test
    void mapsStudentServiceFailuresToSafeBadGatewayResponse() {
        server.expect(requestTo(
                        "http://student-data.test/api/students/42/performance"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.TEXT_PLAIN)
                        .body("private upstream error"));

        ResponseStatusException failure = assertThrows(ResponseStatusException.class,
                () -> client.fetch(42L));

        assertEquals(HttpStatus.BAD_GATEWAY, failure.getStatusCode());
        assertEquals("Student service response was unavailable or malformed",
                failure.getReason());
        server.verify();
    }
}

package com.eduguard.risk_recommendation_service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Component
class StudentPerformanceClient {

    private final RestClient studentService;

    StudentPerformanceClient(
            RestClient.Builder builder,
            @Value("${student-service.base-url:http://localhost:8080}") String baseUrl) {
        this.studentService = builder.baseUrl(baseUrl).build();
    }

    PerformanceSnapshot fetch(Long studentId) {
        try {
            Map<?, ?> data = studentService.get()
                    .uri("/api/students/{id}/performance", studentId)
                    .retrieve()
                    .body(Map.class);
            if (data == null) {
                throw unavailable();
            }
            return PerformanceSnapshot.from(data);
        } catch (HttpClientErrorException.NotFound exception) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Student not found");
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw unavailable();
        }
    }

    private ResponseStatusException unavailable() {
        return new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "Student service response was unavailable or malformed");
    }
}

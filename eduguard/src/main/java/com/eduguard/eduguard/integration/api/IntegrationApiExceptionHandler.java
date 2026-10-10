package com.eduguard.eduguard.integration.api;

import com.eduguard.eduguard.integration.adapter.IntegrationCallException;
import com.eduguard.eduguard.integration.adapter.IntegrationFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = IntegrationResourceController.class)
public class IntegrationApiExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(IntegrationApiExceptionHandler.class);

    @ExceptionHandler(InvalidResourceQueryException.class)
    public ResponseEntity<ApiErrorResponse> invalidQuery() {
        return error(HttpStatus.BAD_REQUEST, "INVALID_QUERY", "The resource query parameters are invalid.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> invalidParameterType() {
        return error(HttpStatus.BAD_REQUEST, "INVALID_QUERY", "The resource query parameters are invalid.");
    }

    @ExceptionHandler(IntegrationCallException.class)
    public ResponseEntity<ApiErrorResponse> integrationFailure(IntegrationCallException exception) {
        return switch (exception.getFailure()) {
            case DISABLED -> error(HttpStatus.SERVICE_UNAVAILABLE,
                    "INTEGRATION_DISABLED", "This integration is currently disabled.");
            case TIMEOUT -> error(HttpStatus.GATEWAY_TIMEOUT,
                    "UPSTREAM_TIMEOUT", "The resource provider did not respond in time.");
            case UNAVAILABLE -> error(HttpStatus.BAD_GATEWAY,
                    "UPSTREAM_UNAVAILABLE", "The resource provider is currently unavailable.");
            case MALFORMED_RESPONSE -> error(HttpStatus.BAD_GATEWAY,
                    "MALFORMED_UPSTREAM_RESPONSE", "The resource provider returned an invalid response.");
        };
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> unexpectedFailure(Exception exception) {
        LOGGER.error("Integration request failed unexpectedly (type={})",
                exception.getClass().getSimpleName());
        return error(HttpStatus.INTERNAL_SERVER_ERROR,
                "INTEGRATION_ERROR", "The integration request could not be completed.");
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(new ApiError(code, message)));
    }
}

package com.eduguard.eduguard.integration.adapter;

public class IntegrationCallException extends RuntimeException {
    private final IntegrationFailure failure;

    public IntegrationCallException(IntegrationFailure failure) {
        super(failure.name());
        this.failure = failure;
    }

    public IntegrationFailure getFailure() {
        return failure;
    }
}

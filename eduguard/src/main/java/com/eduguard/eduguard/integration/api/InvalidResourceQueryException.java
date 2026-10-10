package com.eduguard.eduguard.integration.api;

public class InvalidResourceQueryException extends RuntimeException {
    public InvalidResourceQueryException() {
        super("Invalid resource query");
    }
}

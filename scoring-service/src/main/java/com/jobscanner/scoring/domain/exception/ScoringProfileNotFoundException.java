package com.jobscanner.scoring.domain.exception;

public class ScoringProfileNotFoundException extends RuntimeException {
    public ScoringProfileNotFoundException() {
        super("No scoring profile found for the current tenant. Please create one first.");
    }
}

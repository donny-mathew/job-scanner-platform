package com.jobscanner.scan.domain.exception;

import java.util.UUID;

public class SearchConfigNotFoundException extends RuntimeException {
    public SearchConfigNotFoundException(UUID id) {
        super("Search config not found: " + id);
    }
}

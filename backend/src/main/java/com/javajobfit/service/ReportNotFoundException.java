package com.javajobfit.service;

public class ReportNotFoundException extends RuntimeException {
    public ReportNotFoundException(Long id) {
        super("Report not found: " + id);
    }
}

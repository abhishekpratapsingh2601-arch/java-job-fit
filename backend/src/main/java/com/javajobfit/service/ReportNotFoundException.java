package com.javajobfit.service;

public class ReportNotFoundException extends RuntimeException {
    public ReportNotFoundException(Long id) {
        super("Report not found.");
    }

    public ReportNotFoundException(String reference) {
        super("Report not found.");
    }
}

package com.javajobfit.api.dto;

import javax.validation.constraints.NotBlank;

public class FeedbackRequest {
    private Long reportId;
    private String email;

    @NotBlank
    private String message;

    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

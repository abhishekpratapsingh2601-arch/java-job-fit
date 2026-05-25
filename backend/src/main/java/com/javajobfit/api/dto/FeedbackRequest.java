package com.javajobfit.api.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Email;
import javax.validation.constraints.Size;

public class FeedbackRequest {
    private Long reportId;

    @Email
    @Size(max = 254)
    private String email;

    @NotBlank
    @Size(max = 2000)
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

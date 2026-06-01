package com.javajobfit.api.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Email;
import javax.validation.constraints.Size;

public class FeedbackRequest {
    private String reportId;

    private String publicId;

    @Email(message = "Please enter a valid email address.")
    @Size(max = 254)
    private String email;

    @NotBlank(message = "Feedback message is required.")
    @Size(max = 2000, message = "Feedback message must be 2000 characters or less.")
    private String message;

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
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

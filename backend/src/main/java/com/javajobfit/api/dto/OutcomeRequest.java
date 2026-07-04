package com.javajobfit.api.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class OutcomeRequest {
    @Size(max = 64, message = "Report public ID is too long.")
    @Pattern(
            regexp = "^$|^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
            message = "Report public ID must be a valid UUID.")
    private String reportPublicId;

    @NotBlank(message = "Outcome type is required.")
    @Pattern(
            regexp = "^(applied|recruiter_reply|interview|no_response|still_applying|useful|not_useful)$",
            message = "Outcome type is not supported.")
    private String outcomeType;

    @Min(value = 1, message = "Usefulness rating must be between 1 and 5.")
    @Max(value = 5, message = "Usefulness rating must be between 1 and 5.")
    private Integer usefulnessRating;

    private Boolean gotRecruiterReply;

    private Boolean gotInterview;

    private Boolean appliedWithThisResume;

    @Size(max = 1000, message = "Message is too long.")
    private String message;

    @Email(message = "Email must be valid.")
    @Size(max = 255, message = "Email is too long.")
    private String email;

    public String getReportPublicId() {
        return reportPublicId;
    }

    public void setReportPublicId(String reportPublicId) {
        this.reportPublicId = reportPublicId;
    }

    public String getOutcomeType() {
        return outcomeType;
    }

    public void setOutcomeType(String outcomeType) {
        this.outcomeType = outcomeType;
    }

    public Integer getUsefulnessRating() {
        return usefulnessRating;
    }

    public void setUsefulnessRating(Integer usefulnessRating) {
        this.usefulnessRating = usefulnessRating;
    }

    public Boolean getGotRecruiterReply() {
        return gotRecruiterReply;
    }

    public void setGotRecruiterReply(Boolean gotRecruiterReply) {
        this.gotRecruiterReply = gotRecruiterReply;
    }

    public Boolean getGotInterview() {
        return gotInterview;
    }

    public void setGotInterview(Boolean gotInterview) {
        this.gotInterview = gotInterview;
    }

    public Boolean getAppliedWithThisResume() {
        return appliedWithThisResume;
    }

    public void setAppliedWithThisResume(Boolean appliedWithThisResume) {
        this.appliedWithThisResume = appliedWithThisResume;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}

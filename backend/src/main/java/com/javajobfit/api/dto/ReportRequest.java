package com.javajobfit.api.dto;

import javax.validation.constraints.NotBlank;

public class ReportRequest {
    @NotBlank
    private String resumeText;

    @NotBlank
    private String jobDescription;

    @NotBlank
    private String experienceLevel;

    public String getResumeText() {
        return resumeText;
    }

    public void setResumeText(String resumeText) {
        this.resumeText = resumeText;
    }

    public String getJobDescription() {
        return jobDescription;
    }

    public void setJobDescription(String jobDescription) {
        this.jobDescription = jobDescription;
    }

    public String getExperienceLevel() {
        return experienceLevel;
    }

    public void setExperienceLevel(String experienceLevel) {
        this.experienceLevel = experienceLevel;
    }
}

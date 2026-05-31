package com.javajobfit.api.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class ReportRequest {
    @NotBlank(message = "Resume is required.")
    @Size(min = 40, max = 12000, message = "Please paste a longer resume for a useful scan.")
    private String resumeText;

    @NotBlank(message = "Job description is required.")
    @Size(min = 40, max = 12000, message = "Please paste a longer job description for a useful scan.")
    private String jobDescription;

    @NotBlank(message = "Experience level is required.")
    @Pattern(regexp = "fresher|oneToThree|threeToFive|fiveToEight|senior|threeToSix|sixPlus",
            message = "Please select a supported experience level.")
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

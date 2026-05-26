package com.javajobfit.api.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class ReportRequest {
    @NotBlank
    @Size(min = 40, max = 12000)
    private String resumeText;

    @NotBlank
    @Size(min = 40, max = 12000)
    private String jobDescription;

    @NotBlank
    @Pattern(regexp = "fresher|oneToThree|threeToFive|fiveToEight|senior|threeToSix|sixPlus")
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

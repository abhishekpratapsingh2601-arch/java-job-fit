package com.javajobfit.domain;

import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.PrePersist;
import javax.persistence.Table;

@Entity
@Table(name = "reports")
public class Report {
    private static final String RAW_INPUT_NOT_STORED = "[not stored for privacy]";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Column(name = "resume_text", nullable = false)
    private String resumeTextRedaction = RAW_INPUT_NOT_STORED;

    @Lob
    @Column(name = "job_description", nullable = false)
    private String jobDescriptionRedaction = RAW_INPUT_NOT_STORED;

    @Column(nullable = false)
    private String experienceLevel;

    @Column(nullable = false)
    private int score;

    @Lob
    private String scoreSummary;

    @Lob
    private String matchedSkills;

    @Lob
    private String missingKeywords;

    @Lob
    private String topFixes;

    @Lob
    private String bulletSuggestions;

    @Lob
    private String interviewQuestions;

    @Lob
    private String prepPlan;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void markRawInputsNotStored() {
        resumeTextRedaction = RAW_INPUT_NOT_STORED;
        jobDescriptionRedaction = RAW_INPUT_NOT_STORED;
    }

    public String getResumeTextRedaction() {
        return resumeTextRedaction;
    }

    public String getJobDescriptionRedaction() {
        return jobDescriptionRedaction;
    }

    public String getExperienceLevel() {
        return experienceLevel;
    }

    public void setExperienceLevel(String experienceLevel) {
        this.experienceLevel = experienceLevel;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getScoreSummary() {
        return scoreSummary;
    }

    public void setScoreSummary(String scoreSummary) {
        this.scoreSummary = scoreSummary;
    }

    public String getMatchedSkills() {
        return matchedSkills;
    }

    public void setMatchedSkills(String matchedSkills) {
        this.matchedSkills = matchedSkills;
    }

    public String getMissingKeywords() {
        return missingKeywords;
    }

    public void setMissingKeywords(String missingKeywords) {
        this.missingKeywords = missingKeywords;
    }

    public String getTopFixes() {
        return topFixes;
    }

    public void setTopFixes(String topFixes) {
        this.topFixes = topFixes;
    }

    public String getBulletSuggestions() {
        return bulletSuggestions;
    }

    public void setBulletSuggestions(String bulletSuggestions) {
        this.bulletSuggestions = bulletSuggestions;
    }

    public String getInterviewQuestions() {
        return interviewQuestions;
    }

    public void setInterviewQuestions(String interviewQuestions) {
        this.interviewQuestions = interviewQuestions;
    }

    public String getPrepPlan() {
        return prepPlan;
    }

    public void setPrepPlan(String prepPlan) {
        this.prepPlan = prepPlan;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

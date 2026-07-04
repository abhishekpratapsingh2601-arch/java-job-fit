package com.javajobfit.domain;

import java.time.Instant;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;

@Entity
@Table(name = "reports")
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, columnDefinition = "UUID")
    private UUID publicId;

    @Column(nullable = false)
    private String experienceLevel;

    @Column(nullable = false)
    private int score;

    @Column(columnDefinition = "TEXT")
    private String scoreSummary;

    @Column(name = "matched_strengths", columnDefinition = "TEXT")
    private String matchedSkills;

    @Column(columnDefinition = "TEXT")
    private String missingKeywords;

    @Column(columnDefinition = "TEXT")
    private String topFixes;

    @Column(columnDefinition = "TEXT")
    private String bulletSuggestions;

    @Column(columnDefinition = "TEXT")
    private String interviewQuestions;

    @Column(columnDefinition = "TEXT")
    private String prepPlan;

    @Column(columnDefinition = "TEXT")
    private String scoreBreakdown;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public void setPublicId(UUID publicId) {
        this.publicId = publicId;
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

    public String getScoreBreakdown() {
        return scoreBreakdown;
    }

    public void setScoreBreakdown(String scoreBreakdown) {
        this.scoreBreakdown = scoreBreakdown;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

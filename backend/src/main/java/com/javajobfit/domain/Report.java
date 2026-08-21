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

    @Column(name = "resume_summary", columnDefinition = "TEXT")
    private String resumeSummary;

    @Column(name = "cover_letter", columnDefinition = "TEXT")
    private String coverLetter;

    @Column(name = "keyword_placements", columnDefinition = "TEXT")
    private String keywordPlacements;

    @Column(name = "linkedin_headline", columnDefinition = "TEXT")
    private String linkedinHeadline;

    @Column(name = "linkedin_about", columnDefinition = "TEXT")
    private String linkedinAbout;

    @Column(nullable = false)
    private boolean paid;

    @Column(name = "paid_at")
    private Instant paidAt;

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

    public String getResumeSummary() {
        return resumeSummary;
    }

    public void setResumeSummary(String resumeSummary) {
        this.resumeSummary = resumeSummary;
    }

    public String getCoverLetter() {
        return coverLetter;
    }

    public void setCoverLetter(String coverLetter) {
        this.coverLetter = coverLetter;
    }

    public String getKeywordPlacements() {
        return keywordPlacements;
    }

    public void setKeywordPlacements(String keywordPlacements) {
        this.keywordPlacements = keywordPlacements;
    }

    public String getLinkedinHeadline() {
        return linkedinHeadline;
    }

    public void setLinkedinHeadline(String linkedinHeadline) {
        this.linkedinHeadline = linkedinHeadline;
    }

    public String getLinkedinAbout() {
        return linkedinAbout;
    }

    public void setLinkedinAbout(String linkedinAbout) {
        this.linkedinAbout = linkedinAbout;
    }

    public boolean isPaid() {
        return paid;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    /**
     * Unlocks the full report. Deliberately the only way to set {@code paid}, so a report can
     * never be marked paid without recording when — and so every call site is easy to audit.
     * Must only be called after a payment provider webhook has been signature-verified.
     */
    public void markPaid() {
        this.paid = true;
        this.paidAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

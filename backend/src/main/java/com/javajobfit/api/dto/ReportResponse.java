package com.javajobfit.api.dto;

import java.time.Instant;
import java.util.List;

import com.javajobfit.service.ScoreBreakdown;

public class ReportResponse {
    private String id;
    private String reportId;
    private String publicId;
    private int score;
    private int atsScore;
    private String scoreSummary;
    private List<String> matchedSkills;
    private List<String> matchedStrengths;
    private List<String> missingKeywords;
    private List<String> topFixes;
    private List<String> bulletSuggestions;
    private List<String> bulletUpgrades;
    private List<String> interviewQuestions;
    private List<String> prepPlan;
    private ScoreBreakdown scoreBreakdown;
    private int mustHaveScore;
    private int preferredScore;
    private int keywordScore;
    private int evidenceScore;
    private int seniorityScore;
    private int impactScore;
    private int readabilityScore;
    private boolean freePreview;
    private boolean premiumAvailable;
    private List<String> premiumLockedSections;
    private String experienceLevel;
    private Instant createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getAtsScore() {
        return atsScore;
    }

    public void setAtsScore(int atsScore) {
        this.atsScore = atsScore;
    }

    public String getScoreSummary() {
        return scoreSummary;
    }

    public void setScoreSummary(String scoreSummary) {
        this.scoreSummary = scoreSummary;
    }

    public List<String> getMatchedSkills() {
        return matchedSkills;
    }

    public void setMatchedSkills(List<String> matchedSkills) {
        this.matchedSkills = matchedSkills;
    }

    public List<String> getMatchedStrengths() {
        return matchedStrengths;
    }

    public void setMatchedStrengths(List<String> matchedStrengths) {
        this.matchedStrengths = matchedStrengths;
    }

    public List<String> getMissingKeywords() {
        return missingKeywords;
    }

    public void setMissingKeywords(List<String> missingKeywords) {
        this.missingKeywords = missingKeywords;
    }

    public List<String> getTopFixes() {
        return topFixes;
    }

    public void setTopFixes(List<String> topFixes) {
        this.topFixes = topFixes;
    }

    public List<String> getBulletSuggestions() {
        return bulletSuggestions;
    }

    public void setBulletSuggestions(List<String> bulletSuggestions) {
        this.bulletSuggestions = bulletSuggestions;
    }

    public List<String> getBulletUpgrades() {
        return bulletUpgrades;
    }

    public void setBulletUpgrades(List<String> bulletUpgrades) {
        this.bulletUpgrades = bulletUpgrades;
    }

    public List<String> getInterviewQuestions() {
        return interviewQuestions;
    }

    public void setInterviewQuestions(List<String> interviewQuestions) {
        this.interviewQuestions = interviewQuestions;
    }

    public List<String> getPrepPlan() {
        return prepPlan;
    }

    public void setPrepPlan(List<String> prepPlan) {
        this.prepPlan = prepPlan;
    }

    public ScoreBreakdown getScoreBreakdown() {
        return scoreBreakdown;
    }

    public void setScoreBreakdown(ScoreBreakdown scoreBreakdown) {
        this.scoreBreakdown = scoreBreakdown;
    }

    public int getMustHaveScore() {
        return mustHaveScore;
    }

    public void setMustHaveScore(int mustHaveScore) {
        this.mustHaveScore = mustHaveScore;
    }

    public int getPreferredScore() {
        return preferredScore;
    }

    public void setPreferredScore(int preferredScore) {
        this.preferredScore = preferredScore;
    }

    public int getKeywordScore() {
        return keywordScore;
    }

    public void setKeywordScore(int keywordScore) {
        this.keywordScore = keywordScore;
    }

    public int getEvidenceScore() {
        return evidenceScore;
    }

    public void setEvidenceScore(int evidenceScore) {
        this.evidenceScore = evidenceScore;
    }

    public int getSeniorityScore() {
        return seniorityScore;
    }

    public void setSeniorityScore(int seniorityScore) {
        this.seniorityScore = seniorityScore;
    }

    public int getImpactScore() {
        return impactScore;
    }

    public void setImpactScore(int impactScore) {
        this.impactScore = impactScore;
    }

    public int getReadabilityScore() {
        return readabilityScore;
    }

    public void setReadabilityScore(int readabilityScore) {
        this.readabilityScore = readabilityScore;
    }

    public boolean isFreePreview() {
        return freePreview;
    }

    public void setFreePreview(boolean freePreview) {
        this.freePreview = freePreview;
    }

    public boolean isPremiumAvailable() {
        return premiumAvailable;
    }

    public void setPremiumAvailable(boolean premiumAvailable) {
        this.premiumAvailable = premiumAvailable;
    }

    public List<String> getPremiumLockedSections() {
        return premiumLockedSections;
    }

    public void setPremiumLockedSections(List<String> premiumLockedSections) {
        this.premiumLockedSections = premiumLockedSections;
    }

    public String getExperienceLevel() {
        return experienceLevel;
    }

    public void setExperienceLevel(String experienceLevel) {
        this.experienceLevel = experienceLevel;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

package com.javajobfit.service;

public class ScoreBreakdown {
    private int mustHaveScore;
    private int preferredScore;
    private int keywordScore;
    private int evidenceScore;
    private int seniorityScore;
    private int impactScore;
    private int readabilityScore;

    public ScoreBreakdown() {
    }

    public ScoreBreakdown(
            int mustHaveScore,
            int preferredScore,
            int keywordScore,
            int evidenceScore,
            int seniorityScore,
            int impactScore,
            int readabilityScore) {
        this.mustHaveScore = mustHaveScore;
        this.preferredScore = preferredScore;
        this.keywordScore = keywordScore;
        this.evidenceScore = evidenceScore;
        this.seniorityScore = seniorityScore;
        this.impactScore = impactScore;
        this.readabilityScore = readabilityScore;
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
}

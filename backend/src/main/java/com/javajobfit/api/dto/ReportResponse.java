package com.javajobfit.api.dto;

import java.time.Instant;
import java.util.List;

public class ReportResponse {
    private Long id;
    private int score;
    private List<String> matchedSkills;
    private List<String> missingKeywords;
    private List<String> bulletSuggestions;
    private List<String> interviewQuestions;
    private List<String> prepPlan;
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public List<String> getMatchedSkills() {
        return matchedSkills;
    }

    public void setMatchedSkills(List<String> matchedSkills) {
        this.matchedSkills = matchedSkills;
    }

    public List<String> getMissingKeywords() {
        return missingKeywords;
    }

    public void setMissingKeywords(List<String> missingKeywords) {
        this.missingKeywords = missingKeywords;
    }

    public List<String> getBulletSuggestions() {
        return bulletSuggestions;
    }

    public void setBulletSuggestions(List<String> bulletSuggestions) {
        this.bulletSuggestions = bulletSuggestions;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

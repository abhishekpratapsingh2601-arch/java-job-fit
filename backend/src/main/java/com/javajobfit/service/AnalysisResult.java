package com.javajobfit.service;

import java.util.List;

public class AnalysisResult {
    private final int score;
    private final List<String> matchedSkills;
    private final List<String> missingKeywords;
    private final List<String> bulletSuggestions;
    private final List<String> interviewQuestions;
    private final List<String> prepPlan;

    public AnalysisResult(
            int score,
            List<String> matchedSkills,
            List<String> missingKeywords,
            List<String> bulletSuggestions,
            List<String> interviewQuestions,
            List<String> prepPlan) {
        this.score = score;
        this.matchedSkills = matchedSkills;
        this.missingKeywords = missingKeywords;
        this.bulletSuggestions = bulletSuggestions;
        this.interviewQuestions = interviewQuestions;
        this.prepPlan = prepPlan;
    }

    public int getScore() {
        return score;
    }

    public List<String> getMatchedSkills() {
        return matchedSkills;
    }

    public List<String> getMissingKeywords() {
        return missingKeywords;
    }

    public List<String> getBulletSuggestions() {
        return bulletSuggestions;
    }

    public List<String> getInterviewQuestions() {
        return interviewQuestions;
    }

    public List<String> getPrepPlan() {
        return prepPlan;
    }
}

package com.javajobfit.service;

import java.util.List;

public class AnalysisResult {
    private final int score;
    private final String scoreSummary;
    private final List<String> matchedSkills;
    private final List<String> missingKeywords;
    private final List<String> topFixes;
    private final List<String> bulletSuggestions;
    private final List<String> interviewQuestions;
    private final List<String> prepPlan;
    private final ScoreBreakdown scoreBreakdown;

    public AnalysisResult(
            int score,
            String scoreSummary,
            List<String> matchedSkills,
            List<String> missingKeywords,
            List<String> topFixes,
            List<String> bulletSuggestions,
            List<String> interviewQuestions,
            List<String> prepPlan,
            ScoreBreakdown scoreBreakdown) {
        this.score = score;
        this.scoreSummary = scoreSummary;
        this.matchedSkills = matchedSkills;
        this.missingKeywords = missingKeywords;
        this.topFixes = topFixes;
        this.bulletSuggestions = bulletSuggestions;
        this.interviewQuestions = interviewQuestions;
        this.prepPlan = prepPlan;
        this.scoreBreakdown = scoreBreakdown;
    }

    public int getScore() {
        return score;
    }

    public String getScoreSummary() {
        return scoreSummary;
    }

    public List<String> getMatchedSkills() {
        return matchedSkills;
    }

    public List<String> getMissingKeywords() {
        return missingKeywords;
    }

    public List<String> getTopFixes() {
        return topFixes;
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

    public ScoreBreakdown getScoreBreakdown() {
        return scoreBreakdown;
    }
}

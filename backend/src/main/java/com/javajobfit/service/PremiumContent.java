package com.javajobfit.service;

import java.util.Collections;
import java.util.List;

/**
 * The sections a paid report unlocks, grouped so they can be stored, gated, and audited as one
 * unit rather than as five loose fields.
 *
 * <p>Every value here is generated from derived signals only — skill labels from the analyzer's
 * own taxonomy plus the experience level — never from the user's resume or job-description text.
 * That is what keeps the "never store raw input" rule intact while still selling real content.
 */
public class PremiumContent {
    private final String resumeSummary;
    private final String coverLetter;
    private final List<String> keywordPlacements;
    private final String linkedinHeadline;
    private final String linkedinAbout;

    public PremiumContent(
            String resumeSummary,
            String coverLetter,
            List<String> keywordPlacements,
            String linkedinHeadline,
            String linkedinAbout) {
        this.resumeSummary = resumeSummary;
        this.coverLetter = coverLetter;
        this.keywordPlacements = keywordPlacements == null ? Collections.emptyList() : keywordPlacements;
        this.linkedinHeadline = linkedinHeadline;
        this.linkedinAbout = linkedinAbout;
    }

    public static PremiumContent empty() {
        return new PremiumContent("", "", Collections.emptyList(), "", "");
    }

    public String getResumeSummary() {
        return resumeSummary;
    }

    public String getCoverLetter() {
        return coverLetter;
    }

    public List<String> getKeywordPlacements() {
        return keywordPlacements;
    }

    public String getLinkedinHeadline() {
        return linkedinHeadline;
    }

    public String getLinkedinAbout() {
        return linkedinAbout;
    }
}

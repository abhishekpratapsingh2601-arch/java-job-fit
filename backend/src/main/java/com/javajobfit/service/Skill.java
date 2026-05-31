package com.javajobfit.service;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class Skill {
    private final String label;
    private final List<String> terms;
    private final List<Pattern> termPatterns;

    public Skill(String label, String... terms) {
        this.label = label;
        this.terms = Arrays.asList(terms);
        this.termPatterns = Arrays.stream(terms)
                .map(Pattern::quote)
                .map(term -> Pattern.compile("(^|[^a-z0-9+#])" + term + "([^a-z0-9+#]|$)"))
                .collect(java.util.stream.Collectors.toList());
    }

    public String getLabel() {
        return label;
    }

    public boolean isPresentIn(String text) {
        return termPatterns.stream().anyMatch(pattern -> pattern.matcher(text).find());
    }

    /**
     * Stemmed forms of each term (single-word terms as a unigram stem, two-word terms as a bigram
     * stem). Used to dedupe generic keyword requirements against curated skills so the same
     * evidence is never scored twice.
     */
    public Set<String> stemmedTerms() {
        Set<String> stems = new LinkedHashSet<>();
        for (String term : terms) {
            String[] words = term.split("\\s+");
            StringBuilder joined = new StringBuilder();
            for (int i = 0; i < words.length; i++) {
                if (i > 0) {
                    joined.append(' ');
                }
                joined.append(AnalysisService.stem(words[i]));
            }
            stems.add(joined.toString());
        }
        return stems;
    }
}

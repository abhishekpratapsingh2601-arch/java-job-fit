package com.javajobfit.service;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class Skill {
    private final String label;
    private final List<Pattern> termPatterns;

    public Skill(String label, String... terms) {
        this.label = label;
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
}

package com.javajobfit.service;

import java.util.Arrays;
import java.util.List;

public class Skill {
    private final String label;
    private final List<String> terms;

    public Skill(String label, String... terms) {
        this.label = label;
        this.terms = Arrays.asList(terms);
    }

    public String getLabel() {
        return label;
    }

    public boolean isPresentIn(String text) {
        return terms.stream().anyMatch(text::contains);
    }
}

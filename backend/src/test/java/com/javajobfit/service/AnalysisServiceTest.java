package com.javajobfit.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class AnalysisServiceTest {
    private final AnalysisService service = new AnalysisService();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void scoresSharedFixturesForParityWithBrowserEngine() throws Exception {
        List<Map<String, String>> fixtures;
        try (InputStream in = getClass().getResourceAsStream("/scoring-fixtures.json")) {
            fixtures = mapper.readValue(in, mapper.getTypeFactory()
                    .constructCollectionType(List.class,
                            mapper.getTypeFactory().constructMapType(Map.class, String.class, String.class)));
        }

        // Expected scores produced by the JS engine (app.js analyzeLocally) on the same fixtures.
        // The two engines must agree within a couple of points or the fallback would mislead users.
        Map<String, Integer> jsScores = Map.of(
                "strong_match", 82,
                "partial_match", 29,
                "weak_match", 5,
                "stemming_and_boundaries", 71);

        for (Map<String, String> f : fixtures) {
            AnalysisResult result = service.analyze(f.get("resume"), f.get("job"), f.get("experience"));
            int expected = jsScores.get(f.get("name"));
            assertThat(result.getScore())
                    .as("score parity for fixture '%s' (java=%d, js=%d)", f.get("name"), result.getScore(), expected)
                    .isBetween(expected - 3, expected + 3);
        }
    }

    @Test
    void strongMatchScoresHigherThanWeakMatch() {
        String jd = "Required: strong Core Java, Spring Boot, REST APIs, Hibernate, SQL, JUnit, Docker, microservices.";
        String strong = "Java Spring Boot REST APIs Hibernate SQL JUnit Docker microservices backend engineer.";
        String weak = "Marketing specialist with SEO and content writing skills.";
        assertThat(service.analyze(strong, jd, "oneToThree").getScore())
                .isGreaterThan(service.analyze(weak, jd, "oneToThree").getScore());
    }

    @Test
    void missingRequiredSkillHurtsMoreThanMissingNiceToHave() {
        // Resume covers Java/Spring/REST/SQL/Docker but NOT Kubernetes. The only gap is Kubernetes.
        String resume = "Java Spring Boot REST APIs SQL Docker backend developer.";
        // Same covered set in both; only the WEIGHT of the missing Kubernetes differs.
        String kubernetesRequired = "Required: Java, Spring Boot, REST APIs, SQL, Docker, and Kubernetes are required.";
        String kubernetesNiceToHave = "Required: Java, Spring Boot, REST APIs, SQL, Docker. Kubernetes is a plus.";
        int missRequired = service.analyze(resume, kubernetesRequired, "oneToThree").getScore();
        int missPreferred = service.analyze(resume, kubernetesNiceToHave, "oneToThree").getScore();
        assertThat(missRequired).isLessThan(missPreferred);
    }

    @Test
    void doesNotMatchKeywordAsSubstringOfAnotherWord() {
        // "api" must NOT be considered covered just because the resume contains "rapid".
        String jd = "Required: api design and delivery.";
        String resumeWithRapid = "I work in a rapid agile setting on delivery.";
        String resumeWithApi = "I build api endpoints and own api design.";
        int withoutApi = service.analyze(resumeWithRapid, jd, "oneToThree").getScore();
        int withApi = service.analyze(resumeWithApi, jd, "oneToThree").getScore();
        assertThat(withApi).isGreaterThan(withoutApi);
    }

    @Test
    void stemmingUnifiesTenseAndPluralVariants() {
        String jd = "Required: testing, deploying, and algorithms.";
        // Resume uses different tenses/singulars: tested, deployed, algorithm.
        String withVariants = "I tested code, deployed builds, and implemented an algorithm.";
        // A resume with NONE of those concepts (even as variants) must score lower, proving the
        // variants are actually credited by the stemmer rather than ignored.
        String withNone = "I write marketing copy and manage social media campaigns.";
        int variantScore = service.analyze(withVariants, jd, "oneToThree").getScore();
        int noneScore = service.analyze(withNone, jd, "oneToThree").getScore();
        assertThat(variantScore).isGreaterThan(noneScore);
    }

    @Test
    void scoreSummaryCountMatchesDisplayedMissingCount() {
        String jd = "Required: Java, Spring Boot, REST, SQL, Kafka, Docker, Kubernetes, AWS, microservices, Mockito.";
        String resume = "Java Spring Boot REST SQL backend developer with some testing.";
        AnalysisResult result = service.analyze(resume, jd, "oneToThree");
        if (result.getScore() >= 60 && result.getScore() < 80) {
            int shown = Math.min(result.getMissingKeywords().size(), AnalysisService.FREE_MISSING_LIMIT);
            assertThat(result.getScoreSummary()).contains("missing " + shown + " important");
        }
    }

    @Test
    void emptyJobDescriptionYieldsZeroScore() {
        assertThat(service.analyze("Java Spring Boot developer", "   ", "oneToThree").getScore()).isZero();
    }

    @Test
    void privateCanaryMarkersDoNotBecomeGeneratedKeywords() {
        AnalysisResult result = service.analyze(
                "DO_NOT_STORE_BETA_CANARY_20260602_RESUME Java developer with Spring Boot, REST API, SQL, JUnit, and microservices experience.",
                "DO_NOT_STORE_BETA_CANARY_20260602_JD Looking for a Java Spring Boot backend developer with REST APIs, SQL, testing, microservices, and Kafka.",
                "oneToThree");

        List<String> generated = new java.util.ArrayList<>();
        generated.addAll(result.getMatchedSkills());
        generated.addAll(result.getMissingKeywords());
        generated.addAll(result.getTopFixes());
        generated.addAll(result.getBulletSuggestions());
        generated.addAll(result.getInterviewQuestions());
        generated.addAll(result.getPrepPlan());
        generated.add(result.getScoreSummary());
        String generatedOutput = String.join(" ", generated).toLowerCase();

        assertThat(generatedOutput)
                .doesNotContain("do_not_store", "beta_canary", "canary", "20260602", "resume marker", "jd marker");
        assertThat(generatedOutput).contains("java", "spring boot", "rest", "sql");
    }
}

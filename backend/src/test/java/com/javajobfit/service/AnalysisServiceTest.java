package com.javajobfit.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class AnalysisServiceTest {
    private final AnalysisService service = new AnalysisService();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void strongMatchScoresHigherThanWeakMatch() {
        String jd = "Required: strong Core Java, Spring Boot, REST APIs, Hibernate, SQL, JUnit, Docker, microservices.";
        String strong = "Experience: Built Java Spring Boot REST APIs with Hibernate, SQL, JUnit, Docker and microservices.";
        String weak = "Marketing specialist with SEO and content writing skills.";

        assertThat(service.analyze(strong, jd, "oneToThree").getScore())
                .isGreaterThan(service.analyze(weak, jd, "oneToThree").getScore());
    }

    @Test
    void missingRequiredSkillHurtsMoreThanMissingNiceToHave() {
        String resume = "Summary: Java backend developer with hands-on Spring Boot delivery. "
                + "Experience: Built Java Spring Boot REST APIs for internal operations, added validation and error handling, "
                + "worked with SQL queries and Docker-based local services, used Git reviews, and supported release fixes across sprints.";
        String kubernetesRequired = "Required: Java, Spring Boot, REST APIs, SQL, Docker, and Kubernetes are required.";
        String kubernetesNiceToHave = "Required: Java, Spring Boot, REST APIs, SQL, Docker. Kubernetes is a plus.";

        int missRequired = service.analyze(resume, kubernetesRequired, "oneToThree").getScore();
        int missPreferred = service.analyze(resume, kubernetesNiceToHave, "oneToThree").getScore();

        assertThat(missRequired).isLessThan(missPreferred);
    }

    @Test
    void aliasMatchingRecognizesCommonJavaTerms() {
        String resume = "Experience: Built SpringBoot RESTful services with Postgres persistence and Git workflows.";
        String jd = "Required: Spring Boot, REST APIs, PostgreSQL, and Git.";

        AnalysisResult result = service.analyze(resume, jd, "oneToThree");

        assertThat(result.getMatchedSkills())
                .anyMatch(item -> item.contains("Spring Boot"))
                .anyMatch(item -> item.contains("REST APIs"))
                .anyMatch(item -> item.contains("SQL"))
                .anyMatch(item -> item.contains("Git"));
    }

    @Test
    void doesNotMatchKeywordAsSubstringOfAnotherWord() {
        String jd = "Required: REST API design and Git.";
        String resumeWithNoise = "Experience: Worked in a rapid digital team on delivery.";
        String resumeWithApiGit = "Experience: Built REST API endpoints and used Git for code reviews.";

        int withoutApiGit = service.analyze(resumeWithNoise, jd, "oneToThree").getScore();
        int withApiGit = service.analyze(resumeWithApiGit, jd, "oneToThree").getScore();

        assertThat(withApiGit).isGreaterThan(withoutApiGit);
        assertThat(service.analyze(resumeWithNoise, jd, "oneToThree").getMissingKeywords())
                .anyMatch(item -> item.contains("REST APIs"))
                .anyMatch(item -> item.contains("Git"));
    }

    @Test
    void experienceEvidenceScoresHigherThanSkillsOnlyEvidence() {
        String jd = "Required: Java, Spring Boot, REST APIs, SQL, JUnit, Docker.";
        String skillsOnly = "Summary: Java developer profile. Skills: Java, Spring Boot, REST APIs, SQL, JUnit, Mockito, Docker, "
                + "Maven, Git, backend development, troubleshooting, validation, deployment, monitoring, Agile, Jira, services, "
                + "repositories, controllers, DTOs, exception handling, and database concepts.";
        String withEvidence = "Skills: Java, Spring Boot, REST APIs, SQL, JUnit, Docker.\n"
                + "Experience: Built Java Spring Boot REST APIs for onboarding flows, wrote JUnit tests for service logic, "
                + "used SQL queries for reporting, Dockerized local services, handled validation, debugged production defects, "
                + "collaborated in code reviews, documented API behavior, and supported release verification across Agile sprints.";

        AnalysisResult weak = service.analyze(skillsOnly, jd, "oneToThree");
        AnalysisResult strong = service.analyze(withEvidence, jd, "oneToThree");

        assertThat(strong.getScore()).isGreaterThan(weak.getScore());
        assertThat(strong.getScoreBreakdown().getEvidenceScore())
                .isGreaterThan(weak.getScoreBreakdown().getEvidenceScore());
    }

    @Test
    void impactMetricsRequireActualNumbersNotAdjectives() {
        String jd = "Required: Java, Spring Boot, REST APIs, SQL.";
        String base = "Skills: Java, Spring Boot, REST APIs, SQL.\nExperience: ";
        String adjectivesOnly = base
                + "- Built Java Spring Boot REST APIs and improved performance.\n"
                + "- Optimized SQL queries and improved throughput for services.";
        String realNumbers = base
                + "- Built Java Spring Boot REST APIs and reduced p95 latency from 800ms to 220ms.\n"
                + "- Optimized SQL queries, cutting report generation time by 40%.";

        AnalysisResult vague = service.analyze(adjectivesOnly, jd, "oneToThree");
        AnalysisResult measured = service.analyze(realNumbers, jd, "oneToThree");

        assertThat(vague.getScoreBreakdown().getImpactScore())
                .as("adjectives without digits must not earn impact points")
                .isZero();
        assertThat(measured.getScoreBreakdown().getImpactScore()).isGreaterThan(0);
    }

    @Test
    void warnsWhenNoExperienceOrProjectsSectionIsDetected() {
        String jd = "Required: Java, Spring Boot, REST APIs, SQL, JUnit.";
        String sectionless = "Java backend developer familiar with Spring Boot and REST APIs. "
                + "Worked with SQL databases and JUnit tests across several internal tools and release cycles. "
                + "Comfortable with Git, Docker, debugging, and Agile collaboration in distributed teams.";
        String sectioned = "Experience: Built Java Spring Boot REST APIs with SQL persistence and JUnit tests.";

        assertThat(service.analyze(sectionless, jd, "oneToThree").getTopFixes())
                .anyMatch(fix -> fix.contains("could not find an Experience or Projects section"));
        assertThat(service.analyze(sectioned, jd, "oneToThree").getTopFixes())
                .noneMatch(fix -> fix.contains("could not find an Experience or Projects section"));
    }

    @Test
    void scoreCapsProtectAgainstWeakOrMisleadingInputs() {
        assertThat(service.analyze("Java Spring Boot", "Required: Java Spring Boot REST APIs SQL testing.", "fresher").getScore())
                .isLessThanOrEqualTo(45);

        assertThat(service.analyze("Python Flask developer with SQL.",
                "Required: Java, Spring Boot, REST APIs, SQL.", "oneToThree").getScore())
                .isLessThanOrEqualTo(55);

        String stuffing = "Skills: Java Java Java Java Spring Boot Spring Boot REST API REST API SQL SQL Docker Docker Git Git.";
        assertThat(service.analyze(stuffing,
                "Required: Java, Spring Boot, REST APIs, SQL, Docker, Git.", "oneToThree").getScore())
                .isLessThanOrEqualTo(70);
    }

    @Test
    void seniorityFitRewardsArchitectureEvidenceForSeniorRoles() {
        String jd = "Required: senior Java engineer with Spring Boot, microservices, system design, architecture, mentoring, Kafka.";
        String junior = "Summary: Java developer with backend support experience. Experience: Built Java Spring Boot REST APIs, "
                + "fixed bugs, wrote unit tests, updated SQL queries, supported production tickets, joined Agile ceremonies, "
                + "and collaborated with senior engineers on deployments and code reviews.";
        String senior = "Summary: Senior Java backend engineer. Experience: Led Java Spring Boot microservices architecture, "
                + "owned system design reviews, designed Kafka event flows, mentored engineers, improved observability, "
                + "guided production readiness, and drove cross-team backend architecture decisions.";

        assertThat(service.analyze(senior, jd, "senior").getScore())
                .isGreaterThan(service.analyze(junior, jd, "senior").getScore());
    }

    @Test
    void privateCanaryMarkersDoNotBecomeGeneratedOutput() {
        AnalysisResult result = service.analyze(
                "DO_NOT_STORE_BETA_CANARY_20260602_RESUME Java developer with Spring Boot, REST API, SQL, JUnit, and microservices experience.",
                "DO_NOT_STORE_BETA_CANARY_20260602_JD Looking for a Java Spring Boot backend developer with REST APIs, SQL, testing, microservices, and Kafka.",
                "oneToThree");

        String generatedOutput = generatedOutput(result);

        assertThat(generatedOutput)
                .doesNotContain("do_not_store", "beta_canary", "canary", "20260602", "resume marker", "jd marker");
        assertThat(generatedOutput).contains("java", "spring boot", "rest", "sql");
    }

    @Test
    void genericBetaCopyDoesNotBecomeMissingKeywords() {
        AnalysisResult result = service.analyze(
                "Summary: Java developer with Spring Boot, REST APIs, SQL, Git, and JUnit project experience.",
                "Required: Java, Spring Boot, REST APIs, SQL, Git, JUnit. Projects become available during beta and candidates may apply later.",
                "oneToThree");

        String generatedOutput = generatedOutput(result);

        assertThat(generatedOutput)
                .doesNotContain("become available", "projects become", "projects become available");
    }

    @Test
    void benchmarkCasesStayWithinExpectedRanges() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/ats-benchmark-cases.json")) {
            JsonNode cases = mapper.readTree(in);
            for (JsonNode testCase : cases) {
                AnalysisResult result = service.analyze(
                        testCase.get("resume").asText(),
                        testCase.get("jobDescription").asText(),
                        testCase.get("experienceLevel").asText());

                assertThat(result.getScore())
                        .as(testCase.get("name").asText())
                        .isBetween(testCase.get("expectedScoreMin").asInt(), testCase.get("expectedScoreMax").asInt());

                String output = generatedOutput(result);
                assertContainsAll(output, testCase.withArray("mustIncludeTerms"));
                assertContainsNone(output, testCase.withArray("mustNotIncludeTerms"));
            }
        }
    }

    private static void assertContainsAll(String output, JsonNode terms) {
        for (JsonNode term : terms) {
            assertThat(output).contains(term.asText().toLowerCase());
        }
    }

    private static void assertContainsNone(String output, JsonNode terms) {
        for (JsonNode term : terms) {
            assertThat(output).doesNotContain(term.asText().toLowerCase());
        }
    }

    private static String generatedOutput(AnalysisResult result) {
        List<String> generated = new ArrayList<>();
        generated.addAll(result.getMatchedSkills());
        generated.addAll(result.getMissingKeywords());
        generated.addAll(result.getTopFixes());
        generated.addAll(result.getBulletSuggestions());
        generated.addAll(result.getInterviewQuestions());
        generated.addAll(result.getPrepPlan());
        generated.add(result.getScoreSummary());
        return String.join(" ", generated).toLowerCase();
    }
}

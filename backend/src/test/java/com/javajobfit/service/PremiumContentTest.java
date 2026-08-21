package com.javajobfit.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PremiumContentTest {
    private final AnalysisService service = new AnalysisService();

    private static final String RESUME = "Summary: Java backend developer.\n"
            + "Skills: Java, Spring Boot, REST APIs, SQL, PostgreSQL, JUnit, Mockito, Docker, Git.\n"
            + "Experience: Built Java Spring Boot REST APIs for onboarding with DTO validation and centralized "
            + "exception handling, tuned PostgreSQL queries cutting p99 latency by 40%, wrote JUnit and Mockito "
            + "service tests, containerized services with Docker, and reviewed pull requests across Agile sprints.";
    private static final String JD = "Required: Java 17, Spring Boot, REST APIs, PostgreSQL, Apache Kafka, "
            + "Kubernetes, AWS, microservices, CI/CD, JUnit. Redis and GraphQL are a plus. You will design "
            + "distributed systems, mentor developers, and own services end to end.";

    private AnalysisResult analyze() {
        return service.analyze(RESUME, JD, "threeToFive");
    }

    @Test
    void deliversTenPlusBulletRewritesAsAdvertised() {
        // The locked-section list promises "10+ tailored resume bullet rewrites".
        assertThat(analyze().getBulletSuggestions()).hasSizeGreaterThanOrEqualTo(10);
    }

    @Test
    void bulletRewritesAreSkillSpecificNotBoilerplate() {
        var bullets = analyze().getBulletSuggestions();

        // Skills this JD actually names should each get their own rewrite.
        assertThat(bullets).anyMatch(b -> b.contains("Kafka"));
        assertThat(bullets).anyMatch(b -> b.contains("Kubernetes"));
        assertThat(bullets).anyMatch(b -> b.contains("PostgreSQL") || b.contains("SQL"));
        assertThat(bullets).doesNotHaveDuplicates();
    }

    @Test
    void bulletRewritesUseTheCorrectArticleBeforeSkillNames() {
        // "add a AWS bullet" is the same article bug that shipped once in missing-keyword advice.
        var bullets = analyze().getBulletSuggestions();

        assertThat(bullets).noneMatch(b -> b.contains("add a AWS"));
        assertThat(bullets).noneMatch(b -> b.contains("add an Kafka"));
        assertThat(bullets).anyMatch(b -> b.contains("add an AWS"));
    }

    @Test
    void deliversAFullInterviewQuestionSet() {
        assertThat(analyze().getInterviewQuestions()).hasSizeGreaterThanOrEqualTo(8);
    }

    @Test
    void keywordPlacementGuideSaysWhereEachKeywordBelongs() {
        var placements = analyze().getPremiumContent().getKeywordPlacements();

        assertThat(placements).isNotEmpty();
        assertThat(placements).allMatch(p -> p.contains("->") || p.contains("placement"));
        assertThat(placements).anyMatch(p -> p.contains("Experience") || p.contains("Project") || p.contains("Skills"));
    }

    @Test
    void resumeSummaryIsTailoredToLevelAndMatchedSkills() {
        String summary = analyze().getPremiumContent().getResumeSummary();

        assertThat(summary).isNotBlank();
        assertThat(summary).contains("Java backend engineer");
        assertThat(summary).containsAnyOf("Spring Boot", "REST APIs", "Java");
    }

    @Test
    void coverLetterIsADraftWithPlaceholdersAndNeverInventsHistory() {
        String letter = analyze().getPremiumContent().getCoverLetter();

        assertThat(letter).isNotBlank();
        assertThat(letter).contains("[Company]").contains("[Your Name]");
        // Placeholders, not fabricated achievements: the user fills in real numbers.
        assertThat(letter).contains("[one measurable result]");
        assertThat(letter).contains("Dear [Hiring Manager]");
    }

    @Test
    void linkedinRewriteCoversHeadlineAndAbout() {
        PremiumContent premium = analyze().getPremiumContent();

        assertThat(premium.getLinkedinHeadline()).isNotBlank().contains("|");
        assertThat(premium.getLinkedinAbout()).isNotBlank().contains("[email]");
    }

    @Test
    void premiumSectionsNeverEchoTheUsersOwnSentences() {
        // The privacy rule: premium content is templated from skill labels, so a distinctive
        // phrase that exists only in the resume must not appear anywhere in generated output.
        String resume = RESUME + " My mother's maiden name is Wolverhampton and I live at 14 Baker Street.";
        AnalysisResult result = service.analyze(resume, JD, "threeToFive");
        PremiumContent premium = result.getPremiumContent();

        String all = String.join(" ",
                premium.getResumeSummary(),
                premium.getCoverLetter(),
                premium.getLinkedinHeadline(),
                premium.getLinkedinAbout(),
                String.join(" ", premium.getKeywordPlacements()));

        assertThat(all).doesNotContain("Wolverhampton");
        assertThat(all).doesNotContain("Baker Street");
        assertThat(all).doesNotContain("maiden name");
    }

    @Test
    void emptyJobDescriptionYieldsEmptyPremiumContentNotNulls() {
        PremiumContent premium = service.analyze(RESUME, "", "threeToFive").getPremiumContent();

        assertThat(premium.getResumeSummary()).isNotNull();
        assertThat(premium.getCoverLetter()).isNotNull();
        assertThat(premium.getKeywordPlacements()).isNotNull();
        assertThat(premium.getLinkedinHeadline()).isNotNull();
        assertThat(premium.getLinkedinAbout()).isNotNull();
    }
}

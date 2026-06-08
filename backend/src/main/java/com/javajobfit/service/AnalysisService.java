package com.javajobfit.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

/**
 * ATS-style resume/JD match scoring.
 *
 * <p>The score is a <b>weighted coverage</b> figure, not a token-frequency heuristic. Each
 * requirement detected in the job description (a curated skill or a salient term/phrase) is given
 * a weight based on how strongly the JD asks for it (required &gt; neutral &gt; nice-to-have).
 * The score is the share of total requirement weight that the resume actually covers. Matching is
 * word-boundary + lightly stemmed on both sides, so {@code test/testing/tested} unify and there
 * are no cross-word substring false positives (e.g. "api" inside "rapid").
 *
 * <p>This logic is mirrored byte-for-byte in {@code app.js} {@code analyzeLocally()} so the browser
 * fallback and the backend agree within a point or two.
 */
@Service
public class AnalysisService {
    /** Free-preview cap for the missing list; the score summary cites at most this many. */
    static final int FREE_MISSING_LIMIT = 5;

    // Requirement weights.
    private static final double W_SKILL_REQUIRED = 3.0;
    private static final double W_SKILL_NEUTRAL = 2.0;
    private static final double W_SKILL_PREFERRED = 1.0;
    private static final double W_KEYWORD_REQUIRED = 1.5;
    private static final double W_KEYWORD_NEUTRAL = 1.0;
    private static final double W_KEYWORD_PREFERRED = 0.5;

    private static final int MAX_KEYWORD_UNIGRAMS = 12;
    private static final int MAX_KEYWORD_BIGRAMS = 6;
    private static final int MAX_KEYWORD_REQUIREMENTS = 10;

    private static final List<Skill> SKILLS = Arrays.asList(
            new Skill("Java", "java", "core java", "jdk"),
            new Skill("Kotlin", "kotlin"),
            new Skill("Spring Boot", "spring boot", "springboot"),
            new Skill("Spring MVC", "spring mvc", "spring web"),
            new Skill("Spring Security", "spring security", "oauth", "jwt"),
            new Skill("Reactive/WebFlux", "webflux", "reactive", "project reactor"),
            new Skill("REST APIs", "rest", "rest api", "restful"),
            new Skill("GraphQL", "graphql"),
            new Skill("gRPC", "grpc"),
            new Skill("Microservices", "microservice", "microservices"),
            new Skill("Hibernate/JPA", "hibernate", "jpa", "spring data"),
            new Skill("SQL", "sql", "mysql", "postgres", "postgresql", "oracle"),
            new Skill("NoSQL", "mongodb", "redis", "dynamodb", "cassandra"),
            new Skill("Elasticsearch", "elasticsearch", "elk"),
            new Skill("Kafka", "kafka", "event streaming"),
            new Skill("RabbitMQ", "rabbitmq", "message queue"),
            new Skill("Docker", "docker", "container"),
            new Skill("Kubernetes", "kubernetes", "k8s"),
            new Skill("AWS", "aws", "ec2", "s3", "lambda", "cloudwatch"),
            new Skill("Azure", "azure"),
            new Skill("GCP", "gcp", "google cloud"),
            new Skill("Terraform/IaC", "terraform", "iac"),
            new Skill("CI/CD", "ci/cd", "jenkins", "github actions", "gitlab ci"),
            new Skill("JUnit", "junit", "unit testing"),
            new Skill("Mockito", "mockito"),
            new Skill("Maven/Gradle", "maven", "gradle"),
            new Skill("Git", "git", "github", "gitlab", "bitbucket"),
            new Skill("Design Patterns", "design pattern", "design patterns"),
            new Skill("DSA", "data structure", "data structures", "algorithm", "algorithms", "dsa"),
            new Skill("System Design", "system design", "scalable", "distributed"),
            new Skill("Agile", "agile", "scrum", "jira"),
            new Skill("Observability", "logging", "monitoring", "prometheus", "grafana"));

    private static final Set<String> STOP_WORDS = new LinkedHashSet<>(Arrays.asList(
            "and", "the", "for", "with", "you", "are", "will", "this", "that", "from", "have",
            "has", "had", "our", "your", "job", "role", "roles", "work", "working", "team", "teams",
            "experience", "experiences", "candidate", "candidates", "developer", "developers",
            "engineer", "engineers", "software", "good", "great", "strong", "using", "use", "used",
            "build", "building", "built", "ability", "able", "year", "years", "month", "months",
            "looking", "join", "joining", "responsibilities", "responsibility", "requirement",
            "requirements", "required", "require", "must", "plus", "etc", "including", "include",
            "includes", "such", "who", "what", "when", "where", "which", "how", "why", "they",
            "them", "their", "its", "into", "onto", "over", "under", "about", "across", "within",
            "would", "should", "could", "can", "may", "might", "well", "also", "more", "most",
            "some", "any", "all", "one", "two", "three", "new", "help", "helping", "make", "making",
            "get", "getting", "want", "need", "needs", "needed", "like", "via", "per", "out", "off",
            "but", "not", "yet", "than", "then", "too", "very", "just", "now", "day", "days",
            "knowledge", "understanding", "familiarity", "hands", "based", "level", "part", "full",
            "time", "company", "companies", "product", "products", "service", "services", "system",
            "systems", "application", "applications", "skill", "skills", "tool", "tools",
            "technology", "technologies", "environment", "environments", "opportunity", "people",
            "world", "every", "across", "ensure", "deliver", "delivering", "support", "supporting",
            "canary", "resume", "resumes", "marker", "markers"));

    // Sentence-level cues used to weight requirements as required vs nice-to-have.
    private static final List<String> REQUIRED_CUES = Arrays.asList(
            "required", "require", "must", "must-have", "must have", "strong", "proficient",
            "proficiency", "expertise", "expert", "essential", "essentials", "minimum", "at least",
            "solid", "deep", "advanced", "mandatory", "extensive", "hands-on", "hands on");
    private static final List<String> PREFERRED_CUES = Arrays.asList(
            "plus", "nice to have", "nice-to-have", "preferred", "prefer", "bonus", "good to have",
            "advantage", "desirable", "desired", "ideally", "optional", "familiarity", "exposure",
            "a plus", "would be", "is a plus", "are a plus");

    public AnalysisResult analyze(String resumeText, String jobDescription, String experienceLevel) {
        String resume = normalize(resumeText);
        String job = normalize(jobDescription);
        List<String> segments = segments(jobDescription);

        Set<String> resumeUnigrams = stemmedUnigrams(resume);
        Set<String> resumeBigrams = stemmedBigrams(segments(resumeText));

        // ---- Skill requirements ----
        List<Requirement> skillReqs = new ArrayList<>();
        Set<String> skillTermStems = new LinkedHashSet<>();
        for (Skill skill : SKILLS) {
            for (String stem : skill.stemmedTerms()) {
                skillTermStems.add(stem);
            }
            if (skill.isPresentIn(job)) {
                Weight weight = classify(segments, seg -> skill.isPresentIn(seg));
                boolean covered = skill.isPresentIn(resume);
                skillReqs.add(new Requirement(skill.getLabel(), skillWeight(weight), covered));
            }
        }

        // ---- Keyword requirements (deduped against skills) ----
        List<Requirement> keywordReqs = keywordRequirements(
                job, segments, resumeUnigrams, resumeBigrams, skillTermStems);

        // ---- Weighted-coverage score ----
        double totalWeight = 0;
        double coveredWeight = 0;
        for (Requirement r : skillReqs) {
            totalWeight += r.weight;
            if (r.covered) {
                coveredWeight += r.weight;
            }
        }
        for (Requirement r : keywordReqs) {
            totalWeight += r.weight;
            if (r.covered) {
                coveredWeight += r.weight;
            }
        }
        int score;
        if (totalWeight <= 0) {
            score = 0;
        } else {
            int raw = (int) Math.round((coveredWeight / totalWeight) * 100);
            score = Math.max(5, Math.min(99, raw));
        }

        // ---- Display lists ----
        List<String> matched = skillReqs.stream()
                .filter(r -> r.covered)
                .map(r -> r.label)
                .collect(Collectors.toList());
        List<String> missingSkills = skillReqs.stream()
                .filter(r -> !r.covered)
                .map(r -> r.label)
                .collect(Collectors.toList());

        List<String> missingKeywords = new ArrayList<>(missingSkills);
        Set<String> missingKeys = missingKeywords.stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (Requirement r : keywordReqs) {
            if (missingKeywords.size() >= 8) {
                break;
            }
            if (!r.covered && missingKeys.add(r.label.toLowerCase(Locale.ROOT))) {
                missingKeywords.add(r.label);
            }
        }

        int shownMissing = Math.min(missingKeywords.size(), FREE_MISSING_LIMIT);

        return new AnalysisResult(
                score,
                buildScoreSummary(score, matched, shownMissing),
                matched.isEmpty()
                        ? Arrays.asList("No major Java job keywords matched yet. Add truthful skills, projects, and tools from your actual experience.")
                        : matched,
                missingKeywords.isEmpty()
                        ? Arrays.asList("No obvious gaps from this job description. Focus on proof, numbers, and interview storytelling.")
                        : missingKeywords,
                buildTopFixes(missingKeywords, matched, experienceLevel),
                buildBullets(matched, missingSkills, experienceLevel),
                buildQuestions(missingSkills, matched, experienceLevel),
                buildPlan(missingSkills, matched, experienceLevel));
    }

    // ---------------------------------------------------------------------
    // Text processing
    // ---------------------------------------------------------------------

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    /** Lowercased sentence/line segments, used to detect required vs nice-to-have context. */
    private List<String> segments(String text) {
        if (text == null) {
            return new ArrayList<>();
        }
        String[] parts = text.toLowerCase(Locale.ROOT).split("[\\n\\.;:!?]+");
        List<String> out = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.replaceAll("\\s+", " ").trim();
            if (!trimmed.isEmpty()) {
                out.add(trimmed);
            }
        }
        return out;
    }

    /** Content tokens in order: length &gt; 2, not a stop word, not purely numeric, edges trimmed. */
    private List<String> contentTokens(String text) {
        List<String> tokens = new ArrayList<>();
        String cleaned = removePrivateMarkers(text);
        for (String raw : cleaned.replaceAll("[^a-z0-9+#./-]", " ").split("\\s+")) {
            String word = trimEdges(raw);
            if (word.length() > 2 && !STOP_WORDS.contains(word) && !word.matches("[0-9]+")) {
                tokens.add(word);
            }
        }
        return tokens;
    }

    private String removePrivateMarkers(String text) {
        return text.replaceAll("(do[_-]?not[_-]?store|beta[_-]?canary|canary)[a-z0-9_-]*", " ");
    }

    /** Strip leading/trailing separator punctuation while keeping internal ones (ci/cd, node.js). */
    private String trimEdges(String word) {
        int start = 0;
        int end = word.length();
        while (start < end && isEdgeChar(word.charAt(start))) {
            start++;
        }
        while (end > start && isEdgeChar(word.charAt(end - 1))) {
            end--;
        }
        return word.substring(start, end);
    }

    private boolean isEdgeChar(char c) {
        return c == '.' || c == '/' || c == '-';
    }

    private Set<String> stemmedUnigrams(String normalized) {
        Set<String> set = new LinkedHashSet<>();
        for (String token : contentTokens(normalized)) {
            set.add(stem(token));
        }
        return set;
    }

    /** Bigrams built WITHIN each segment so pairs never cross a sentence boundary. */
    private Set<String> stemmedBigrams(List<String> segments) {
        Set<String> set = new LinkedHashSet<>();
        for (String segment : segments) {
            List<String> tokens = contentTokens(segment);
            for (int i = 0; i + 1 < tokens.size(); i++) {
                set.add(stem(tokens.get(i)) + " " + stem(tokens.get(i + 1)));
            }
        }
        return set;
    }

    /**
     * Light, deterministic suffix stemmer. Linguistic precision is not the goal; the only
     * requirement is that it produces the SAME stem for the resume side and the JD side (and that
     * the JS port matches it exactly).
     */
    static String stem(String word) {
        String w = word;
        if (w.length() <= 3) {
            return w;
        }
        if (w.endsWith("ing") && w.length() - 3 >= 3) {
            w = w.substring(0, w.length() - 3);
        } else if (w.endsWith("ed") && w.length() - 2 >= 3) {
            w = w.substring(0, w.length() - 2);
        }
        if (w.endsWith("ies") && w.length() > 4) {
            w = w.substring(0, w.length() - 3) + "y";
        }
        if (w.length() > 3 && w.endsWith("s") && !w.endsWith("ss")) {
            w = w.substring(0, w.length() - 1);
        }
        return w;
    }

    // ---------------------------------------------------------------------
    // Keyword requirement extraction
    // ---------------------------------------------------------------------

    private List<Requirement> keywordRequirements(
            String job,
            List<String> segments,
            Set<String> resumeUnigrams,
            Set<String> resumeBigrams,
            Set<String> skillTermStems) {
        List<String> tokens = contentTokens(job);

        // Frequency by stem, keeping the first original token as the display label.
        Map<String, Integer> unigramFreq = new LinkedHashMap<>();
        Map<String, String> unigramLabel = new LinkedHashMap<>();
        for (String token : tokens) {
            String s = stem(token);
            unigramFreq.merge(s, 1, Integer::sum);
            unigramLabel.putIfAbsent(s, token);
        }

        Map<String, Integer> bigramFreq = new LinkedHashMap<>();
        Map<String, String> bigramLabel = new LinkedHashMap<>();
        for (String segment : segments) {
            List<String> segTokens = contentTokens(segment);
            for (int i = 0; i + 1 < segTokens.size(); i++) {
                String s = stem(segTokens.get(i)) + " " + stem(segTokens.get(i + 1));
                bigramFreq.merge(s, 1, Integer::sum);
                bigramLabel.putIfAbsent(s, segTokens.get(i) + " " + segTokens.get(i + 1));
            }
        }

        List<String> topUnigrams = topByFrequency(unigramFreq, MAX_KEYWORD_UNIGRAMS);
        List<String> topBigrams = topByFrequency(bigramFreq, MAX_KEYWORD_BIGRAMS);

        List<Requirement> reqs = new ArrayList<>();
        Set<String> usedStems = new LinkedHashSet<>(skillTermStems);

        for (String stem : topBigrams) {
            if (reqs.size() >= MAX_KEYWORD_REQUIREMENTS) {
                break;
            }
            if (!usedStems.add(stem)) {
                continue;
            }
            String label = bigramLabel.get(stem);
            boolean covered = resumeBigrams.contains(stem);
            Weight weight = classify(segments, seg -> seg.contains(label));
            reqs.add(new Requirement(label, keywordWeight(weight), covered));
        }

        for (String stem : topUnigrams) {
            if (reqs.size() >= MAX_KEYWORD_REQUIREMENTS) {
                break;
            }
            if (!usedStems.add(stem)) {
                continue;
            }
            String label = unigramLabel.get(stem);
            boolean covered = resumeUnigrams.contains(stem);
            Weight weight = classify(segments, seg -> seg.contains(label));
            reqs.add(new Requirement(label, keywordWeight(weight), covered));
        }
        return reqs;
    }

    private List<String> topByFrequency(Map<String, Integer> freq, int limit) {
        return freq.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue)
                        .reversed()
                        .thenComparing(Map.Entry::getKey))
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    // ---------------------------------------------------------------------
    // Required / nice-to-have classification
    // ---------------------------------------------------------------------

    private interface SegmentMatcher {
        boolean matches(String segment);
    }

    private Weight classify(List<String> segments, SegmentMatcher matcher) {
        boolean required = false;
        boolean preferred = false;
        for (String seg : segments) {
            if (!matcher.matches(seg)) {
                continue;
            }
            if (containsCue(seg, REQUIRED_CUES)) {
                required = true;
            }
            if (containsCue(seg, PREFERRED_CUES)) {
                preferred = true;
            }
        }
        if (required) {
            return Weight.REQUIRED;
        }
        if (preferred) {
            return Weight.PREFERRED;
        }
        return Weight.NEUTRAL;
    }

    private boolean containsCue(String segment, List<String> cues) {
        for (String cue : cues) {
            if (segment.contains(cue)) {
                return true;
            }
        }
        return false;
    }

    private double skillWeight(Weight weight) {
        switch (weight) {
            case REQUIRED:
                return W_SKILL_REQUIRED;
            case PREFERRED:
                return W_SKILL_PREFERRED;
            default:
                return W_SKILL_NEUTRAL;
        }
    }

    private double keywordWeight(Weight weight) {
        switch (weight) {
            case REQUIRED:
                return W_KEYWORD_REQUIRED;
            case PREFERRED:
                return W_KEYWORD_PREFERRED;
            default:
                return W_KEYWORD_NEUTRAL;
        }
    }

    private enum Weight {
        REQUIRED,
        NEUTRAL,
        PREFERRED
    }

    private static final class Requirement {
        private final String label;
        private final double weight;
        private final boolean covered;

        private Requirement(String label, double weight, boolean covered) {
            this.label = label;
            this.weight = weight;
            this.covered = covered;
        }
    }

    // ---------------------------------------------------------------------
    // Copy builders (advice text; unchanged behaviour)
    // ---------------------------------------------------------------------

    private String buildScoreSummary(int score, List<String> matched, int shownMissing) {
        if (score >= 80) {
            return "Your resume is a strong fit for this Java role. Polish the remaining keyword gaps before applying.";
        }
        if (score >= 60) {
            String noun = shownMissing == 1 ? "keyword" : "keywords";
            return "Your resume matches this Java role, but you are missing " + shownMissing
                    + " important " + noun + ". Fix the top gaps before applying.";
        }
        if (matched.isEmpty()) {
            return "Your resume needs clearer Java and Spring Boot evidence for this role. Add truthful project and skill proof before applying.";
        }
        return "Your resume has some useful Java signals, but it needs stronger alignment with this job description before applying.";
    }

    private List<String> buildTopFixes(List<String> missingKeywords, List<String> matched, String experienceLevel) {
        List<String> fixes = new ArrayList<>();
        if (!missingKeywords.isEmpty()) {
            fixes.add("Add truthful resume evidence for " + String.join(", ", missingKeywords.stream().limit(3).collect(Collectors.toList())) + ".");
        }
        fixes.add("Rewrite your summary for a " + copyFor(experienceLevel).role + " role using the exact Java/Spring keywords you can defend.");
        fixes.add("Turn one project or work item into a measurable backend impact bullet with Java, Spring Boot, database, and testing details.");
        if (matched.isEmpty()) {
            fixes.add("Move your strongest Java projects and tools into the top half of the resume so they are easy to scan.");
        } else {
            fixes.add("Keep your strongest matched skills visible near the top: " + String.join(", ", matched.stream().limit(3).collect(Collectors.toList())) + ".");
        }
        return fixes.stream().limit(3).collect(Collectors.toList());
    }

    private List<String> buildBullets(List<String> matched, List<String> missing, String experienceLevel) {
        ExperienceCopy copy = copyFor(experienceLevel);
        List<String> bullets = new ArrayList<>(copy.bullets);
        if (!matched.isEmpty()) {
            bullets.add(0, "Position yourself as a " + copy.role + " by highlighting hands-on work with "
                    + String.join(", ", matched.stream().limit(4).collect(Collectors.toList())) + ".");
        }
        if (!missing.isEmpty()) {
            bullets.add("Add truthful project or learning evidence for "
                    + String.join(", ", missing.stream().limit(3).collect(Collectors.toList()))
                    + " if you have used them. Avoid keyword stuffing without proof.");
        }
        bullets.add("Rewrite weak bullets with this pattern: action verb + Java/Spring skill + measurable business or technical result.");
        return bullets;
    }

    private List<String> buildQuestions(List<String> missing, List<String> matched, String experienceLevel) {
        List<String> focus = new ArrayList<>();
        focus.addAll(missing);
        focus.addAll(matched);
        List<String> questions = new ArrayList<>(Arrays.asList(
                "Explain how Spring Boot auto-configuration works and when you would override it.",
                "How would you design a REST API for high traffic, validation, error handling, and versioning?",
                "What happens internally when a Java HashMap handles collisions?",
                "How do you write testable service-layer code using JUnit and Mockito?",
                "How would you debug a slow API in production?"));

        if (focus.contains("Microservices")) {
            questions.add(0, "How would you split a monolith into microservices without breaking existing users?");
        }
        if (focus.contains("Kafka")) {
            questions.add(0, "How do Kafka consumer groups, offsets, partitions, and retries work in a backend service?");
        }
        if ("sixPlus".equals(experienceLevel) || "senior".equals(experienceLevel)) {
            questions.add(0, "Describe a backend architecture decision you led, including tradeoffs and production impact.");
        }
        return questions.stream().limit(7).collect(Collectors.toList());
    }

    private List<String> buildPlan(List<String> missing, List<String> matched, String experienceLevel) {
        ExperienceCopy copy = copyFor(experienceLevel);
        String priority = missing.isEmpty()
                ? "Spring Boot, REST APIs, SQL, testing"
                : String.join(", ", missing.stream().limit(4).collect(Collectors.toList()));
        String strengths = matched.isEmpty()
                ? "your strongest Java skills"
                : String.join(", ", matched.stream().limit(3).collect(Collectors.toList()));

        return Arrays.asList(
                "Day 1: Rewrite resume summary for the target " + copy.role + " role and add exact matching Java keywords you can honestly defend.",
                "Day 2: Review core Java, OOP, collections, exceptions, streams, and concurrency basics.",
                "Day 3: Build or polish one Spring Boot REST API story using controller, service, repository, DTO, validation, and error handling.",
                "Day 4: Study priority gaps from this JD: " + priority + ".",
                "Day 5: Practice SQL, JPA/Hibernate mappings, transactions, indexes, and common performance problems.",
                "Day 6: Prepare testing, debugging, CI/CD, Git, and deployment examples from your own experience.",
                "Day 7: Mock interview day. Practice " + strengths + " plus one system design question.");
    }

    private ExperienceCopy copyFor(String experienceLevel) {
        switch (experienceLevel) {
            case "fresher":
                return new ExperienceCopy("entry-level Java developer", Arrays.asList(
                        "Built Java and Spring Boot projects with REST APIs, layered architecture, validation, and database integration.",
                        "Practiced DSA, OOP, collections, exception handling, and SQL through hands-on projects and coding problems."));
            case "threeToFive":
                return new ExperienceCopy("Java backend engineer", Arrays.asList(
                        "Delivered Java and Spring Boot services with REST APIs, persistence, validation, testing, and release support.",
                        "Improved backend reliability through debugging, SQL tuning, code reviews, and production-ready error handling."));
            case "fiveToEight":
                return new ExperienceCopy("senior Java backend engineer", Arrays.asList(
                        "Owned Spring Boot service design, database performance, API contracts, security, and CI/CD delivery.",
                        "Reduced production risk through observability, test strategy, incident debugging, and cross-team backend delivery."));
            case "senior":
                return new ExperienceCopy("senior Java technical leader", Arrays.asList(
                        "Led scalable Java backend design across services, owning tradeoffs, performance, reliability, and delivery standards.",
                        "Mentored engineers, improved architecture quality, and partnered with product teams on backend roadmap execution."));
            case "threeToSix":
                return new ExperienceCopy("mid-level Java backend engineer", Arrays.asList(
                        "Designed and maintained Spring Boot microservices with database optimization, security, and CI/CD delivery.",
                        "Reduced defects and deployment risk through test coverage, code reviews, observability, and clear API contracts."));
            case "sixPlus":
                return new ExperienceCopy("senior Java backend engineer", Arrays.asList(
                        "Led design of scalable Java microservices, owning architecture decisions, performance tuning, and production readiness.",
                        "Mentored engineers, improved engineering standards, and drove delivery across cross-functional backend initiatives."));
            default:
                return new ExperienceCopy("junior Java developer", Arrays.asList(
                        "Delivered Spring Boot REST APIs with JPA repositories, validation, exception handling, and unit tests.",
                        "Improved API reliability by debugging production issues, writing JUnit/Mockito tests, and collaborating in Agile sprints."));
        }
    }

    private static class ExperienceCopy {
        private final String role;
        private final List<String> bullets;

        private ExperienceCopy(String role, List<String> bullets) {
            this.role = role;
            this.bullets = bullets;
        }
    }
}

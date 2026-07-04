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
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class AnalysisService {
    static final int FREE_MISSING_LIMIT = 5;

    private static final Pattern PRIVATE_MARKER = Pattern.compile(
            "(do[_-]?not[_-]?store|beta[_-]?canary|canary|resume[_-]?marker|jd[_-]?marker)[a-z0-9_-]*",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern METRIC_PATTERN = Pattern.compile(
            "(\\d+\\s*(%|ms|s|sec|secs|seconds|users|requests|rps|qps|minutes|hours|days)|latency|uptime|cost|defects|scale|performance|throughput|reduced|improved|optimized)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern YEARS_PATTERN = Pattern.compile("(\\d+)\\s*(?:\\+|to|-)?\\s*(\\d+)?\\s*\\+?\\s*years?");

    private static final List<String> REQUIRED_CUES = Arrays.asList(
            "required", "must", "mandatory", "strong", "expertise", "proficient", "hands-on",
            "hands on", "minimum", "essential", "solid", "deep", "advanced", "at least");
    private static final List<String> PREFERRED_CUES = Arrays.asList(
            "preferred", "nice to have", "nice-to-have", "bonus", "plus", "good to have",
            "exposure", "familiarity", "desirable", "desired", "optional", "advantage");
    private static final List<String> RESPONSIBILITY_CUES = Arrays.asList(
            "build", "design", "develop", "maintain", "optimize", "troubleshoot", "deploy",
            "collaborate", "own", "debug", "support", "review", "monitor");
    private static final List<String> ACTION_VERBS = Arrays.asList(
            "built", "build", "designed", "developed", "implemented", "optimized", "improved",
            "reduced", "migrated", "maintained", "debugged", "tested", "deployed", "owned",
            "led", "created", "integrated", "documented", "supported", "reviewed");
    private static final List<String> CONTEXT_WORDS = Arrays.asList(
            "api", "service", "workflow", "query", "database", "test", "deployment", "customer",
            "production", "latency", "validation", "exception", "transaction", "consumer",
            "endpoint", "pipeline", "dashboard", "monitoring");

    private static final Set<String> STOP_WORDS = new LinkedHashSet<>(Arrays.asList(
            "and", "the", "for", "with", "you", "are", "will", "this", "that", "from", "have",
            "has", "had", "our", "your", "job", "role", "roles", "work", "working", "team",
            "teams", "experience", "experiences", "candidate", "candidates", "developer",
            "developers", "engineer", "engineers", "software", "good", "great", "strong",
            "using", "use", "used", "ability", "able", "year", "years", "month", "months",
            "looking", "join", "joining", "project", "projects", "available", "availability",
            "become", "becomes", "became", "get", "getting", "want", "needs", "needed", "core",
            "responsibility", "responsibilities", "requirement", "requirements", "required",
            "require", "must", "plus", "etc", "including", "include", "includes", "such", "who",
            "what", "when", "where", "which", "how", "why", "they", "them", "their", "its", "into",
            "onto", "over", "under", "about", "across", "within", "would", "should", "could",
            "can", "may", "might", "well", "also", "more", "most", "some", "any", "all", "one",
            "two", "three", "new", "help", "helping", "make", "making", "need", "like", "via",
            "per", "out", "off", "but", "not", "yet", "than", "then", "too", "very", "just",
            "now", "day", "days", "company", "companies", "product", "products", "service",
            "services", "system", "systems", "application", "applications", "skill", "skills",
            "tool", "tools", "technology", "technologies", "environment", "environments",
            "opportunity", "people", "world", "every", "ensure", "deliver", "delivering",
            "support", "supporting", "knowledge", "understanding", "familiarity", "hands",
            "based", "level", "part", "full", "time", "resume", "resumes", "marker", "markers"));

    private static final List<SkillSpec> SKILLS = Arrays.asList(
            skill("Java", "Core Java", 1.0, "java", "core java", "jdk"),
            skill("OOP", "Core Java", 0.75, "oop", "object oriented", "object-oriented"),
            skill("Collections", "Core Java", 0.7, "collections", "collection framework"),
            skill("Streams", "Core Java", 0.7, "streams", "stream api", "java streams"),
            skill("Concurrency", "Core Java", 0.8, "concurrency", "multithreading", "multi threading", "threads"),
            skill("JVM", "Core Java", 0.65, "jvm", "memory management", "garbage collection"),
            skill("Exceptions", "Core Java", 0.55, "exceptions", "exception handling"),
            skill("Spring Boot", "Spring", 1.0, "spring boot", "springboot"),
            skill("Spring MVC", "Spring", 0.85, "spring mvc", "spring web"),
            skill("Spring Security", "Spring", 0.85, "spring security", "oauth", "jwt"),
            skill("Spring Data JPA", "Spring", 0.85, "spring data jpa", "spring data"),
            skill("Spring Cloud", "Spring", 0.65, "spring cloud"),
            skill("WebFlux", "Spring", 0.55, "webflux", "reactive", "project reactor"),
            skill("REST APIs", "APIs", 1.0, "rest api", "rest apis", "restful", "restful services", "rest"),
            skill("GraphQL", "APIs", 0.55, "graphql"),
            skill("gRPC", "APIs", 0.55, "grpc", "gRPC"),
            skill("OpenAPI/Swagger", "APIs", 0.55, "openapi", "swagger"),
            skill("API versioning", "APIs", 0.45, "api versioning", "versioned api"),
            skill("SQL", "Database", 1.0, "sql"),
            skill("MySQL", "Database", 0.65, "mysql"),
            skill("PostgreSQL", "Database", 0.75, "postgresql", "postgres"),
            skill("Oracle", "Database", 0.55, "oracle"),
            skill("Hibernate/JPA", "Database", 0.95, "hibernate", "jpa", "spring data jpa"),
            skill("JDBC", "Database", 0.45, "jdbc"),
            skill("Transactions", "Database", 0.65, "transactions", "transaction management"),
            skill("Indexing", "Database", 0.55, "indexing", "indexes"),
            skill("Query optimization", "Database", 0.7, "query optimization", "query tuning"),
            skill("MongoDB", "Database", 0.5, "mongodb", "mongo"),
            skill("Redis", "Database", 0.5, "redis"),
            skill("JUnit", "Testing", 0.85, "junit", "unit testing"),
            skill("Mockito", "Testing", 0.75, "mockito"),
            skill("Integration testing", "Testing", 0.7, "integration testing", "integration tests"),
            skill("Testcontainers", "Testing", 0.55, "testcontainers"),
            skill("TDD", "Testing", 0.45, "tdd", "test driven"),
            skill("Kafka", "Messaging", 0.85, "kafka", "apache kafka"),
            skill("RabbitMQ", "Messaging", 0.55, "rabbitmq"),
            skill("JMS", "Messaging", 0.45, "jms"),
            skill("Event-driven architecture", "Messaging", 0.65, "event driven", "event-driven architecture"),
            skill("Docker", "DevOps/Cloud", 0.8, "docker", "containerized", "container"),
            skill("Kubernetes", "DevOps/Cloud", 0.75, "kubernetes", "k8s"),
            skill("AWS", "DevOps/Cloud", 0.75, "aws", "ec2", "s3", "lambda", "cloudwatch"),
            skill("Azure", "DevOps/Cloud", 0.55, "azure"),
            skill("GCP", "DevOps/Cloud", 0.55, "gcp", "google cloud"),
            skill("CI/CD", "DevOps/Cloud", 0.8, "ci/cd", "jenkins", "github actions", "gitlab ci"),
            skill("Maven", "DevOps/Cloud", 0.55, "maven"),
            skill("Gradle", "DevOps/Cloud", 0.55, "gradle"),
            skill("Microservices", "Architecture", 0.9, "microservice", "microservices"),
            skill("System design", "Architecture", 0.75, "system design"),
            skill("Distributed systems", "Architecture", 0.75, "distributed systems", "distributed"),
            skill("Design patterns", "Architecture", 0.6, "design patterns", "design pattern"),
            skill("Scalability", "Architecture", 0.6, "scalability", "scalable"),
            skill("Observability", "Architecture", 0.65, "observability", "logging", "monitoring"),
            skill("Prometheus", "Architecture", 0.5, "prometheus"),
            skill("Grafana", "Architecture", 0.5, "grafana"),
            skill("Git", "Tools/Process", 0.65, "git", "github", "gitlab", "bitbucket"),
            skill("Agile/Scrum", "Tools/Process", 0.55, "agile", "scrum", "jira"),
            skill("Code review", "Tools/Process", 0.55, "code review", "pull request"),
            skill("Production support", "Tools/Process", 0.65, "production support", "prod support"),
            skill("Debugging", "Tools/Process", 0.65, "debugging", "troubleshooting", "root cause"));

    private static SkillSpec skill(String label, String category, double importance, String... aliases) {
        return new SkillSpec(label, category, importance, Arrays.asList(aliases));
    }

    public AnalysisResult analyze(String resumeText, String jobDescription, String experienceLevel) {
        String resume = safeText(resumeText);
        String job = safeText(jobDescription);
        String normalizedResume = normalize(resume);
        String normalizedJob = normalize(job);
        if (normalizedJob.isBlank()) {
            return emptyResult();
        }

        ResumeProfile profile = parseResume(resume);
        List<String> jobSegments = segments(job);
        List<Requirement> skillRequirements = extractSkillRequirements(normalizedJob, jobSegments, profile);
        List<KeywordRequirement> keywordRequirements = extractKeywordRequirements(job, profile, skillRequirements);

        ScoreBreakdown breakdown = new ScoreBreakdown(
                component(skillRequirements, RequirementType.REQUIRED, 30, true),
                preferredScore(skillRequirements),
                keywordScore(keywordRequirements),
                evidenceScore(skillRequirements),
                seniorityScore(job, experienceLevel),
                impactScore(profile),
                readabilityScore(profile));

        int rawScore = breakdown.getMustHaveScore()
                + breakdown.getPreferredScore()
                + breakdown.getKeywordScore()
                + breakdown.getEvidenceScore()
                + breakdown.getSeniorityScore()
                + breakdown.getImpactScore()
                + breakdown.getReadabilityScore();
        int score = applyScoreCaps(rawScore, profile, skillRequirements, normalizedJob, normalizedResume);

        List<Requirement> matchedReqs = skillRequirements.stream()
                .filter(Requirement::isCovered)
                .sorted(Comparator.comparing((Requirement r) -> r.evidenceLevel).reversed()
                        .thenComparing(r -> -r.weight))
                .collect(Collectors.toList());
        List<Requirement> missingReqs = skillRequirements.stream()
                .filter(r -> !r.isCovered())
                .sorted(Comparator.comparing((Requirement r) -> r.type).thenComparing(r -> -r.weight))
                .collect(Collectors.toList());

        List<String> matched = matchedReqs.stream()
                .map(r -> r.label + evidenceSuffix(r.evidenceLevel))
                .limit(8)
                .collect(Collectors.toList());
        List<String> missing = missingKeywords(missingReqs, keywordRequirements);
        List<String> topFixes = buildTopFixes(missingReqs, keywordRequirements, profile, experienceLevel);
        List<String> bullets = buildBullets(matchedReqs, missingReqs, experienceLevel);
        List<String> questions = buildQuestions(missingReqs, matchedReqs, experienceLevel);
        List<String> plan = buildPlan(missingReqs, matchedReqs, experienceLevel);

        return new AnalysisResult(
                score,
                buildScoreSummary(score, matchedReqs, missingReqs, skillRequirements, profile),
                matched.isEmpty()
                        ? Arrays.asList("No major Java job keywords matched yet. Add truthful Java/backend evidence from projects or experience.")
                        : sanitizeList(matched),
                missing.isEmpty()
                        ? Arrays.asList("No obvious keyword gaps from this job description. Focus on proof, metrics, and interview stories.")
                        : sanitizeList(missing),
                sanitizeList(topFixes),
                sanitizeList(bullets),
                sanitizeList(questions),
                sanitizeList(plan),
                breakdown);
    }

    private AnalysisResult emptyResult() {
        ScoreBreakdown breakdown = new ScoreBreakdown(0, 0, 0, 0, 0, 0, 0);
        return new AnalysisResult(
                0,
                "Paste a target Java job description to calculate an ATS-style score.",
                Arrays.asList("No job description keywords were provided."),
                Arrays.asList("Paste the target Java/Spring Boot job description."),
                Arrays.asList("Paste a complete Java/Spring Boot job description before analyzing."),
                Arrays.asList("Add truthful Java backend project or work evidence once a target JD is available."),
                Arrays.asList("Which Java/Spring Boot topics does the target job require?"),
                Arrays.asList("Day 1: Paste a target job description and rerun the scan."),
                breakdown);
    }

    private List<Requirement> extractSkillRequirements(String normalizedJob, List<String> jobSegments, ResumeProfile profile) {
        List<Requirement> requirements = new ArrayList<>();
        for (SkillSpec skill : SKILLS) {
            if (!skill.isPresentIn(normalizedJob)) {
                continue;
            }
            RequirementType type = classify(jobSegments, segment -> skill.isPresentIn(segment));
            EvidenceLevel evidenceLevel = profile.evidenceFor(skill);
            requirements.add(new Requirement(
                    skill.label,
                    skill.category,
                    type,
                    skill.importance * type.weight,
                    evidenceLevel));
        }
        return dedupeByLabel(requirements);
    }

    private List<Requirement> dedupeByLabel(List<Requirement> requirements) {
        Map<String, Requirement> byLabel = new LinkedHashMap<>();
        for (Requirement requirement : requirements) {
            Requirement existing = byLabel.get(requirement.label);
            if (existing == null || requirement.weight > existing.weight || requirement.evidenceLevel.ordinal() > existing.evidenceLevel.ordinal()) {
                byLabel.put(requirement.label, requirement);
            }
        }
        return new ArrayList<>(byLabel.values());
    }

    private List<KeywordRequirement> extractKeywordRequirements(
            String jobDescription,
            ResumeProfile profile,
            List<Requirement> skillRequirements) {
        Set<String> skillStems = skillRequirements.stream()
                .flatMap(requirement -> requirement.label.toLowerCase(Locale.ROOT).contains("/")
                        ? Arrays.stream(requirement.label.toLowerCase(Locale.ROOT).split("[^a-z0-9+#]+")).map(AnalysisService::stem)
                        : Arrays.stream(requirement.label.toLowerCase(Locale.ROOT).split("[^a-z0-9+#]+")).map(AnalysisService::stem))
                .filter(stem -> stem.length() > 1)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<String> jobSegments = segments(jobDescription);
        Map<String, Integer> phraseFreq = new LinkedHashMap<>();
        Map<String, String> phraseLabel = new LinkedHashMap<>();
        Map<String, Integer> tokenFreq = new LinkedHashMap<>();
        Map<String, String> tokenLabel = new LinkedHashMap<>();

        for (String segment : jobSegments) {
            List<String> tokens = contentTokens(segment);
            for (String token : tokens) {
                String stem = stem(token);
                tokenFreq.merge(stem, 1, Integer::sum);
                tokenLabel.putIfAbsent(stem, token);
            }
            for (int n = 2; n <= 3; n++) {
                for (int i = 0; i + n <= tokens.size(); i++) {
                    String phrase = String.join(" ", tokens.subList(i, i + n));
                    String phraseStem = tokens.subList(i, i + n).stream().map(AnalysisService::stem).collect(Collectors.joining(" "));
                    phraseFreq.merge(phraseStem, 1, Integer::sum);
                    phraseLabel.putIfAbsent(phraseStem, phrase);
                }
            }
        }

        List<KeywordRequirement> output = new ArrayList<>();
        Set<String> used = new LinkedHashSet<>(skillStems);
        for (String key : topByFrequency(phraseFreq, 8)) {
            if (output.size() >= 10 || phraseMostlySkill(key, skillStems) || !used.add(key)) {
                continue;
            }
            String label = phraseLabel.get(key);
            RequirementType type = classify(jobSegments, segment -> segment.contains(label));
            output.add(new KeywordRequirement(label, type, profile.containsPhrase(label)));
        }
        for (String key : topByFrequency(tokenFreq, 8)) {
            if (output.size() >= 12 || skillStems.contains(key) || !used.add(key)) {
                continue;
            }
            String label = tokenLabel.get(key);
            RequirementType type = classify(jobSegments, segment -> segment.contains(label));
            output.add(new KeywordRequirement(label, type, profile.containsStem(key)));
        }
        return output;
    }

    private boolean phraseMostlySkill(String phraseStem, Set<String> skillStems) {
        List<String> stems = Arrays.stream(phraseStem.split("\\s+"))
                .filter(stem -> !stem.isBlank())
                .collect(Collectors.toList());
        if (stems.size() < 2) {
            return false;
        }
        long skillCount = stems.stream().filter(skillStems::contains).count();
        return skillCount >= Math.max(2, stems.size() - 1);
    }

    private int component(List<Requirement> requirements, RequirementType type, int max, boolean fallbackWhenMissing) {
        List<Requirement> scoped = requirements.stream()
                .filter(r -> r.type == type)
                .collect(Collectors.toList());
        if (scoped.isEmpty() && fallbackWhenMissing) {
            scoped = requirements.stream()
                    .filter(r -> r.type == RequirementType.RESPONSIBILITY || r.type == RequirementType.NEUTRAL)
                    .collect(Collectors.toList());
        }
        if (scoped.isEmpty()) {
            return fallbackWhenMissing ? max - 3 : max;
        }
        double total = scoped.stream().mapToDouble(r -> r.weight).sum();
        double covered = scoped.stream()
                .filter(Requirement::isCovered)
                .mapToDouble(r -> r.weight)
                .sum();
        return clamp((int) Math.round((covered / total) * max), 0, max);
    }

    private int keywordScore(List<KeywordRequirement> requirements) {
        if (requirements.isEmpty()) {
            return 12;
        }
        double total = 0;
        double covered = 0;
        for (KeywordRequirement requirement : requirements) {
            total += requirement.type.keywordWeight;
            if (requirement.covered) {
                covered += requirement.type.keywordWeight;
            }
        }
        return clamp((int) Math.round((covered / total) * 15), 0, 15);
    }

    private int preferredScore(List<Requirement> requirements) {
        List<Requirement> scoped = requirements.stream()
                .filter(r -> r.type == RequirementType.PREFERRED)
                .collect(Collectors.toList());
        if (scoped.isEmpty()) {
            return 10;
        }
        double total = scoped.stream().mapToDouble(r -> r.weight).sum();
        double covered = scoped.stream().filter(Requirement::isCovered).mapToDouble(r -> r.weight).sum();
        return 7 + clamp((int) Math.round((covered / Math.max(total, 0.1)) * 3), 0, 3);
    }

    private int evidenceScore(List<Requirement> requirements) {
        List<Requirement> covered = requirements.stream().filter(Requirement::isCovered).collect(Collectors.toList());
        if (covered.isEmpty()) {
            return 0;
        }
        double score = 0;
        for (Requirement requirement : covered) {
            switch (requirement.evidenceLevel) {
                case STRONG:
                    score += 1.0;
                    break;
                case MEDIUM:
                    score += 0.62;
                    break;
                case WEAK:
                    score += 0.35;
                    break;
                default:
                    break;
            }
        }
        return clamp((int) Math.round((score / covered.size()) * 20), 0, 20);
    }

    private int seniorityScore(String jobDescription, String experienceLevel) {
        YearRange jdRange = detectYears(jobDescription);
        if (!jdRange.present) {
            return 9;
        }
        YearRange selected = selectedRange(experienceLevel);
        if (selected.max < jdRange.min) {
            return selected.max + 1 < jdRange.min ? 3 : 5;
        }
        if (selected.min > jdRange.max + 2) {
            return 8;
        }
        return 10;
    }

    private int impactScore(ResumeProfile profile) {
        int metricLines = 0;
        for (String line : profile.experienceProjectLines()) {
            if (METRIC_PATTERN.matcher(line).find()) {
                metricLines++;
            }
        }
        return clamp(metricLines * 2, 0, 5);
    }

    private int readabilityScore(ResumeProfile profile) {
        int score = 0;
        int length = profile.original.length();
        if (length >= 500) {
            score += 3;
        } else if (length >= 280) {
            score += 2;
        }
        if (profile.hasSection("skills")) {
            score += 1;
        }
        if (profile.hasSection("experience") || profile.hasSection("work experience")) {
            score += 2;
        }
        if (profile.hasSection("projects")) {
            score += 1;
        }
        if (profile.bulletCount >= 3) {
            score += 2;
        } else if (profile.bulletCount > 0) {
            score += 1;
        }
        if (!profile.hasWeirdSymbolNoise()) {
            score += 1;
        }
        if (profile.keywordStuffed) {
            score -= 3;
        }
        return clamp(score, 0, 10);
    }

    private int applyScoreCaps(
            int score,
            ResumeProfile profile,
            List<Requirement> requirements,
            String normalizedJob,
            String normalizedResume) {
        int capped = clamp(score, 0, 99);
        if (profile.original.length() < 160) {
            capped = Math.min(capped, 45);
        }
        if (containsJavaRequirement(normalizedJob) && !containsJavaRequirement(normalizedResume)) {
            capped = Math.min(capped, 55);
        }
        if (containsSpringBootRequirement(normalizedJob) && !containsSpringBootRequirement(normalizedResume)) {
            capped = Math.min(capped, 65);
        }
        long missingRequiredBackend = requirements.stream()
                .filter(r -> r.type == RequirementType.REQUIRED)
                .filter(r -> !r.isCovered())
                .filter(r -> !"Tools/Process".equals(r.category))
                .count();
        if (missingRequiredBackend >= 2) {
            capped = Math.min(capped, 75);
        }
        List<Requirement> covered = requirements.stream().filter(Requirement::isCovered).collect(Collectors.toList());
        long weakOnly = covered.stream().filter(r -> r.evidenceLevel == EvidenceLevel.WEAK).count();
        if (!covered.isEmpty() && weakOnly > covered.size() / 2) {
            capped = Math.min(capped, 80);
        }
        if (profile.keywordStuffed) {
            capped = Math.min(capped, 70);
        }
        return capped;
    }

    private boolean containsJavaRequirement(String text) {
        return Pattern.compile("(^|[^a-z0-9])java([^a-z0-9]|$)").matcher(text).find()
                || text.contains("core java");
    }

    private boolean containsSpringBootRequirement(String text) {
        return text.contains("spring boot") || text.contains("springboot");
    }

    private List<String> missingKeywords(List<Requirement> missingReqs, List<KeywordRequirement> keywordReqs) {
        List<String> output = new ArrayList<>();
        for (Requirement requirement : missingReqs) {
            if (output.size() >= 8) {
                break;
            }
            output.add(requirement.label + " — add truthful proof in an " + placementFor(requirement));
        }
        for (KeywordRequirement keyword : keywordReqs) {
            if (output.size() >= 8) {
                break;
            }
            if (!keyword.covered && output.stream().noneMatch(value -> value.toLowerCase(Locale.ROOT).contains(keyword.label.toLowerCase(Locale.ROOT)))) {
                output.add(keyword.label + " — mention in a summary or project bullet if true");
            }
        }
        return output;
    }

    private String placementFor(Requirement requirement) {
        if (requirement.type == RequirementType.REQUIRED || requirement.type == RequirementType.RESPONSIBILITY) {
            return "Experience or Projects bullet";
        }
        if ("Core Java".equals(requirement.category) || "Tools/Process".equals(requirement.category)) {
            return "Skills section plus one proof bullet";
        }
        return "Project bullet";
    }

    private List<String> buildTopFixes(
            List<Requirement> missingReqs,
            List<KeywordRequirement> keywordReqs,
            ResumeProfile profile,
            String experienceLevel) {
        List<String> fixes = new ArrayList<>();
        missingReqs.stream()
                .filter(r -> r.type == RequirementType.REQUIRED)
                .findFirst()
                .ifPresent(r -> fixes.add("Add " + r.label + " project evidence in your Experience or Projects section, not only the Skills list."));
        if (missingReqs.stream().anyMatch(r -> r.label.toLowerCase(Locale.ROOT).contains("rest"))) {
            fixes.add("Add one REST API bullet with endpoint design, validation, error handling, and database persistence.");
        }
        if (missingReqs.stream().anyMatch(r -> r.label.toLowerCase(Locale.ROOT).contains("junit") || r.label.toLowerCase(Locale.ROOT).contains("mockito"))) {
            fixes.add("If true, mention JUnit/Mockito testing coverage for one service-layer flow.");
        }
        if (missingReqs.stream().anyMatch(r -> r.category.equals("Database"))) {
            fixes.add("Add SQL/JPA proof with query optimization, transactions, schema design, or repository work.");
        }
        if (profile.impactScoreHint() == 0) {
            fixes.add("Add one measurable impact bullet with %, latency, defects, users, requests, cost, or performance context.");
        }
        if (fixes.isEmpty()) {
            fixes.add("Keep your strongest Java/Spring evidence visible near the top and connect it to the target job.");
        }
        fixes.add("Rewrite your summary for a " + copyFor(experienceLevel).role + " role using only Java/backend skills you can defend.");
        fixes.add("Move the most relevant Java, Spring Boot, database, testing, and deployment proof into the top half of the resume.");
        fixes.add("Replace generic responsibility wording with action, technology, and result: built, optimized, tested, deployed, or debugged.");
        return fixes.stream().distinct().limit(3).collect(Collectors.toList());
    }

    private List<String> buildBullets(List<Requirement> matched, List<Requirement> missing, String experienceLevel) {
        List<String> bullets = new ArrayList<>();
        String role = copyFor(experienceLevel).role;
        List<String> strong = matched.stream()
                .filter(r -> r.evidenceLevel == EvidenceLevel.STRONG)
                .map(r -> r.label)
                .limit(3)
                .collect(Collectors.toList());
        if (!strong.isEmpty()) {
            bullets.add("Built Java backend features using " + String.join(", ", strong)
                    + " with clear ownership, test coverage, and production-ready error handling.");
        }
        Requirement firstMissing = missing.isEmpty() ? null : missing.get(0);
        if (firstMissing != null) {
            bullets.add("If true, add a " + firstMissing.label + " bullet for a " + role
                    + " role: describe the API/service, context, testing, and measurable result.");
        }
        bullets.add("Built Spring Boot REST APIs for customer onboarding with DTO validation, centralized exception handling, JPA persistence, and JUnit/Mockito service tests.");
        return bullets;
    }

    private List<String> buildQuestions(List<Requirement> missing, List<Requirement> matched, String experienceLevel) {
        List<String> questions = new ArrayList<>();
        for (Requirement requirement : missing.stream().filter(r -> r.type == RequirementType.REQUIRED).limit(2).collect(Collectors.toList())) {
            questions.add("The JD emphasizes " + requirement.label + ". Explain one real project where you used it, or how you would learn and apply it safely.");
        }
        if (matched.stream().anyMatch(r -> r.label.contains("Spring Boot")) || missing.stream().anyMatch(r -> r.label.contains("Spring Boot"))) {
            questions.add("Explain how Spring Boot auto-configuration works and when you would override it.");
        }
        if (matched.stream().anyMatch(r -> r.label.contains("REST")) || missing.stream().anyMatch(r -> r.label.contains("REST"))) {
            questions.add("How would you design a REST API with validation, error handling, pagination, and versioning?");
        }
        if (matched.stream().anyMatch(r -> r.label.contains("Kafka")) || missing.stream().anyMatch(r -> r.label.contains("Kafka"))) {
            questions.add("How do Kafka consumer groups, offsets, partitions, retries, and dead-letter topics work?");
        }
        if ("senior".equals(experienceLevel) || "fiveToEight".equals(experienceLevel)) {
            questions.add("Describe a backend architecture decision you led, including tradeoffs and production impact.");
        }
        questions.add("How would you debug a slow Java API in production without exposing user data?");
        return questions.stream().distinct().limit(7).collect(Collectors.toList());
    }

    private List<String> buildPlan(List<Requirement> missing, List<Requirement> matched, String experienceLevel) {
        String priority = missing.isEmpty()
                ? "Spring Boot, REST APIs, SQL/JPA, testing, and debugging stories"
                : missing.stream().limit(4).map(r -> r.label).collect(Collectors.joining(", "));
        String strengths = matched.isEmpty()
                ? "your strongest Java/backend projects"
                : matched.stream().limit(3).map(r -> r.label).collect(Collectors.joining(", "));
        return Arrays.asList(
                "Day 1: Fix resume gaps for the must-have skills first: " + priority + ".",
                "Day 2: Add one Experience or Projects bullet with action, Java/Spring context, and measurable impact.",
                "Day 3: Prepare interview stories around matched strengths: " + strengths + ".",
                "Day 4: Review Spring Boot REST API design, validation, exception handling, and DTO patterns.",
                "Day 5: Review SQL/JPA transactions, query tuning, indexes, and common persistence pitfalls.",
                "Day 6: Practice testing, debugging, CI/CD, Git, Docker, and production support questions.",
                "Day 7: Run a mock interview using the JD stack and rewrite any weak resume bullets before applying.");
    }

    private String buildScoreSummary(
            int score,
            List<Requirement> matched,
            List<Requirement> missing,
            List<Requirement> allRequirements,
            ResumeProfile profile) {
        String strongest = matched.stream()
                .collect(Collectors.groupingBy(r -> r.category, LinkedHashMap::new, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Java/backend fundamentals");
        String weakest = missing.stream()
                .collect(Collectors.groupingBy(r -> r.category, LinkedHashMap::new, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("evidence depth");
        String missingRequired = missing.stream()
                .filter(r -> r.type == RequirementType.REQUIRED)
                .map(r -> r.label)
                .findFirst()
                .orElse(missing.stream().map(r -> r.label).findFirst().orElse("no major must-have skill"));
        String evidenceNote = profile.mostEvidenceWeak(allRequirements)
                ? "Most matched skills appear in the Skills section, so add Experience or Project proof."
                : "Your strongest matches have usable project or experience evidence.";

        if (score >= 80) {
            return "Strong Java/backend fit. Strongest area: " + strongest + ". Weakest area: " + weakest
                    + ". Top gap: " + missingRequired + ". " + evidenceNote;
        }
        if (score >= 60) {
            return "Moderate Java/backend fit. Strongest area: " + strongest + ". Weakest area: " + weakest
                    + ". Top missing required skill: " + missingRequired + ". " + evidenceNote;
        }
        return "Low-to-moderate Java/backend fit for this JD. Strongest area: " + strongest
                + ". Weakest area: " + weakest + ". Top missing required skill: " + missingRequired
                + ". Add truthful Experience or Projects evidence before applying.";
    }

    private String evidenceSuffix(EvidenceLevel level) {
        switch (level) {
            case STRONG:
                return " (project/experience evidence)";
            case MEDIUM:
                return " (summary evidence)";
            case WEAK:
                return " (skills-only evidence)";
            default:
                return "";
        }
    }

    private RequirementType classify(List<String> segments, Predicate<String> matcher) {
        boolean required = false;
        boolean preferred = false;
        boolean responsibility = false;
        for (String segment : segments) {
            if (!matcher.test(segment)) {
                continue;
            }
            required |= containsCue(segment, REQUIRED_CUES);
            preferred |= containsCue(segment, PREFERRED_CUES);
            responsibility |= containsCue(segment, RESPONSIBILITY_CUES);
        }
        if (required) {
            return RequirementType.REQUIRED;
        }
        if (preferred) {
            return RequirementType.PREFERRED;
        }
        if (responsibility) {
            return RequirementType.RESPONSIBILITY;
        }
        return RequirementType.NEUTRAL;
    }

    private boolean containsCue(String text, List<String> cues) {
        for (String cue : cues) {
            if (text.contains(cue)) {
                return true;
            }
        }
        return false;
    }

    private ResumeProfile parseResume(String resumeText) {
        return new ResumeProfile(resumeText);
    }

    private String safeText(String text) {
        return PRIVATE_MARKER.matcher(text == null ? "" : text).replaceAll(" ");
    }

    private String normalize(String text) {
        return safeText(text).toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private List<String> segments(String text) {
        String[] parts = safeText(text).toLowerCase(Locale.ROOT).split("[\\n\\.;:!?]+");
        List<String> out = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.replaceAll("\\s+", " ").trim();
            if (!trimmed.isEmpty()) {
                out.add(trimmed);
            }
        }
        return out;
    }

    private List<String> contentTokens(String text) {
        List<String> tokens = new ArrayList<>();
        for (String raw : normalize(text).replaceAll("[^a-z0-9+#./-]", " ").split("\\s+")) {
            String token = trimEdges(raw);
            if (token.length() > 2 && !STOP_WORDS.contains(token) && !token.matches("[0-9]+")) {
                tokens.add(token);
            }
        }
        return tokens;
    }

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

    static String stem(String word) {
        String w = word == null ? "" : word.toLowerCase(Locale.ROOT);
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

    private List<String> topByFrequency(Map<String, Integer> freq, int limit) {
        return freq.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue)
                        .reversed()
                        .thenComparing(Map.Entry::getKey))
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private List<String> sanitizeList(List<String> values) {
        return values.stream()
                .map(value -> PRIVATE_MARKER.matcher(value).replaceAll("").replaceAll("\\s+", " ").trim())
                .filter(value -> !value.isBlank())
                .collect(Collectors.toList());
    }

    private YearRange detectYears(String text) {
        java.util.regex.Matcher matcher = YEARS_PATTERN.matcher(text.toLowerCase(Locale.ROOT));
        if (!matcher.find()) {
            return new YearRange(0, 99, false);
        }
        int min = Integer.parseInt(matcher.group(1));
        int max = matcher.group(2) == null ? min + 2 : Integer.parseInt(matcher.group(2));
        if (text.substring(matcher.start(), matcher.end()).contains("+")) {
            max = 99;
        }
        return new YearRange(min, max, true);
    }

    private YearRange selectedRange(String experienceLevel) {
        switch (experienceLevel == null ? "" : experienceLevel) {
            case "fresher":
                return new YearRange(0, 1, true);
            case "threeToFive":
                return new YearRange(3, 5, true);
            case "fiveToEight":
                return new YearRange(5, 8, true);
            case "senior":
            case "sixPlus":
                return new YearRange(6, 99, true);
            case "threeToSix":
                return new YearRange(3, 6, true);
            default:
                return new YearRange(1, 3, true);
        }
    }

    private ExperienceCopy copyFor(String experienceLevel) {
        switch (experienceLevel == null ? "" : experienceLevel) {
            case "fresher":
                return new ExperienceCopy("entry-level Java developer");
            case "threeToFive":
                return new ExperienceCopy("Java backend engineer");
            case "fiveToEight":
                return new ExperienceCopy("senior Java backend engineer");
            case "senior":
            case "sixPlus":
                return new ExperienceCopy("senior Java technical leader");
            default:
                return new ExperienceCopy("junior Java developer");
        }
    }

    private enum RequirementType {
        REQUIRED(1.0, 1.0),
        PREFERRED(0.55, 0.55),
        RESPONSIBILITY(0.78, 0.8),
        NEUTRAL(0.68, 0.7);

        private final double weight;
        private final double keywordWeight;

        RequirementType(double weight, double keywordWeight) {
            this.weight = weight;
            this.keywordWeight = keywordWeight;
        }
    }

    private enum EvidenceLevel {
        NONE,
        WEAK,
        MEDIUM,
        STRONG
    }

    private static final class Requirement {
        private final String label;
        private final String category;
        private final RequirementType type;
        private final double weight;
        private final EvidenceLevel evidenceLevel;

        private Requirement(String label, String category, RequirementType type, double weight, EvidenceLevel evidenceLevel) {
            this.label = label;
            this.category = category;
            this.type = type;
            this.weight = weight;
            this.evidenceLevel = evidenceLevel;
        }

        private boolean isCovered() {
            return evidenceLevel != EvidenceLevel.NONE;
        }
    }

    private static final class KeywordRequirement {
        private final String label;
        private final RequirementType type;
        private final boolean covered;

        private KeywordRequirement(String label, RequirementType type, boolean covered) {
            this.label = label;
            this.type = type;
            this.covered = covered;
        }
    }

    private static final class SkillSpec {
        private final String label;
        private final String category;
        private final double importance;
        private final List<String> aliases;
        private final List<Pattern> patterns;

        private SkillSpec(String label, String category, double importance, List<String> aliases) {
            this.label = label;
            this.category = category;
            this.importance = importance;
            this.aliases = aliases;
            this.patterns = aliases.stream()
                    .map(alias -> Pattern.compile("(^|[^a-z0-9+#])" + Pattern.quote(alias.toLowerCase(Locale.ROOT)) + "([^a-z0-9+#]|$)"))
                    .collect(Collectors.toList());
        }

        private boolean isPresentIn(String text) {
            String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT);
            return patterns.stream().anyMatch(pattern -> pattern.matcher(normalized).find());
        }
    }

    private final class ResumeProfile {
        private final String original;
        private final String normalized;
        private final Map<String, List<String>> sections = new LinkedHashMap<>();
        private final int bulletCount;
        private final boolean keywordStuffed;

        private ResumeProfile(String original) {
            this.original = safeText(original);
            this.normalized = normalize(original);
            parseSections();
            this.bulletCount = (int) Arrays.stream(this.original.split("\\n"))
                    .filter(line -> line.trim().matches("^[-*•].+"))
                    .count();
            this.keywordStuffed = detectKeywordStuffing();
        }

        private void parseSections() {
            String current = "summary";
            sections.put(current, new ArrayList<>());
            String sectionReadyText = original.replaceAll(
                    "(?i)\\b(Summary|Profile|Technical Skills|Skills|Skill Set|Technologies|Experience|Work Experience|Professional Experience|Employment|Projects|Project Experience|Personal Projects|Education|Academics|Certifications|Certification)\\s*:",
                    "\n$1:");
            for (String rawLine : sectionReadyText.split("\\n")) {
                String line = rawLine.trim();
                if (line.isBlank()) {
                    continue;
                }
                String detected = sectionName(line);
                if (detected != null) {
                    current = detected;
                    sections.putIfAbsent(current, new ArrayList<>());
                } else {
                    InlineSection inlineSection = inlineSection(line);
                    if (inlineSection != null) {
                        current = inlineSection.name;
                        sections.putIfAbsent(current, new ArrayList<>());
                        if (!inlineSection.content.isBlank()) {
                            sections.get(current).add(inlineSection.content);
                        }
                    } else {
                        sections.computeIfAbsent(current, key -> new ArrayList<>()).add(line);
                    }
                }
            }
        }

        private String sectionName(String line) {
            String normalizedLine = line.toLowerCase(Locale.ROOT).replace(":", "").trim();
            if (normalizedLine.matches("(professional )?summary|profile")) {
                return "summary";
            }
            if (normalizedLine.matches("technical skills|skills|skill set|technologies")) {
                return "skills";
            }
            if (normalizedLine.matches("experience|work experience|professional experience|employment")) {
                return "experience";
            }
            if (normalizedLine.matches("projects|project experience|personal projects")) {
                return "projects";
            }
            if (normalizedLine.matches("education|academics")) {
                return "education";
            }
            if (normalizedLine.matches("certifications|certification")) {
                return "certifications";
            }
            return null;
        }

        private InlineSection inlineSection(String line) {
            int colon = line.indexOf(':');
            if (colon < 0) {
                return null;
            }
            String maybeHeader = sectionName(line.substring(0, colon));
            if (maybeHeader == null) {
                return null;
            }
            return new InlineSection(maybeHeader, line.substring(colon + 1).trim());
        }

        private EvidenceLevel evidenceFor(SkillSpec skill) {
            if (appearsInLines(skill, experienceProjectLines(), true)) {
                return EvidenceLevel.STRONG;
            }
            if (appearsInLines(skill, sections.getOrDefault("summary", new ArrayList<>()), false)) {
                return EvidenceLevel.MEDIUM;
            }
            if (appearsInLines(skill, sections.getOrDefault("skills", new ArrayList<>()), false)
                    || skill.isPresentIn(normalized)) {
                return skill.isPresentIn(String.join(" ", sections.getOrDefault("skills", new ArrayList<>())))
                        ? EvidenceLevel.WEAK
                        : EvidenceLevel.MEDIUM;
            }
            return EvidenceLevel.NONE;
        }

        private boolean appearsInLines(SkillSpec skill, List<String> lines, boolean requireEvidenceContext) {
            for (String line : lines) {
                String normalizedLine = normalize(line);
                if (!skill.isPresentIn(normalizedLine)) {
                    continue;
                }
                if (!requireEvidenceContext || isEvidenceLine(normalizedLine)) {
                    return true;
                }
            }
            return false;
        }

        private boolean isEvidenceLine(String line) {
            boolean action = ACTION_VERBS.stream().anyMatch(line::contains);
            boolean context = CONTEXT_WORDS.stream().anyMatch(line::contains) || METRIC_PATTERN.matcher(line).find();
            return action && context;
        }

        private List<String> experienceProjectLines() {
            List<String> lines = new ArrayList<>();
            lines.addAll(sections.getOrDefault("experience", new ArrayList<>()));
            lines.addAll(sections.getOrDefault("work experience", new ArrayList<>()));
            lines.addAll(sections.getOrDefault("projects", new ArrayList<>()));
            return lines;
        }

        private boolean containsPhrase(String phrase) {
            return normalized.contains(phrase.toLowerCase(Locale.ROOT));
        }

        private boolean containsStem(String wantedStem) {
            return contentTokens(normalized).stream().map(AnalysisService::stem).anyMatch(wantedStem::equals);
        }

        private boolean hasSection(String section) {
            return sections.containsKey(section) && !sections.get(section).isEmpty();
        }

        private int impactScoreHint() {
            return (int) experienceProjectLines().stream().filter(line -> METRIC_PATTERN.matcher(line).find()).count();
        }

        private boolean hasWeirdSymbolNoise() {
            if (original.isBlank()) {
                return false;
            }
            long weird = original.chars()
                    .filter(ch -> !(Character.isLetterOrDigit(ch) || Character.isWhitespace(ch)
                            || ".,;:!?()[]{}+-*/#@%&'\"_|•".indexOf(ch) >= 0))
                    .count();
            return weird > Math.max(20, original.length() / 20);
        }

        private boolean detectKeywordStuffing() {
            List<String> tokens = contentTokens(normalized);
            if (tokens.isEmpty()) {
                return false;
            }
            Map<String, Long> counts = tokens.stream().collect(Collectors.groupingBy(AnalysisService::stem, Collectors.counting()));
            boolean repeatedDump = counts.values().stream().anyMatch(count -> count >= 9);
            String skillsText = String.join(" ", sections.getOrDefault("skills", new ArrayList<>()));
            long commaCount = skillsText.chars().filter(ch -> ch == ',').count();
            return repeatedDump || (commaCount >= 18 && bulletCount < 2);
        }

        private boolean mostEvidenceWeak(List<Requirement> requirements) {
            List<Requirement> covered = requirements.stream().filter(Requirement::isCovered).collect(Collectors.toList());
            if (covered.isEmpty()) {
                return false;
            }
            long weak = covered.stream().filter(r -> r.evidenceLevel == EvidenceLevel.WEAK).count();
            return weak > covered.size() / 2;
        }
    }

    private static final class YearRange {
        private final int min;
        private final int max;
        private final boolean present;

        private YearRange(int min, int max, boolean present) {
            this.min = min;
            this.max = max;
            this.present = present;
        }
    }

    private static final class InlineSection {
        private final String name;
        private final String content;

        private InlineSection(String name, String content) {
            this.name = name;
            this.content = content;
        }
    }

    private static final class ExperienceCopy {
        private final String role;

        private ExperienceCopy(String role) {
            this.role = role;
        }
    }
}

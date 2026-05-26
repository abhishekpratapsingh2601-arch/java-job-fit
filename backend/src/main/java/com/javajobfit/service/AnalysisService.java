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

@Service
public class AnalysisService {
    private static final List<Skill> SKILLS = Arrays.asList(
            new Skill("Java", "java", "core java", "jdk"),
            new Skill("Spring Boot", "spring boot", "springboot"),
            new Skill("Spring MVC", "spring mvc", "spring web"),
            new Skill("Spring Security", "spring security", "oauth", "jwt"),
            new Skill("REST APIs", "rest", "rest api", "restful"),
            new Skill("Microservices", "microservice", "microservices"),
            new Skill("Hibernate/JPA", "hibernate", "jpa", "spring data"),
            new Skill("SQL", "sql", "mysql", "postgres", "postgresql", "oracle"),
            new Skill("NoSQL", "mongodb", "redis", "dynamodb", "cassandra"),
            new Skill("Kafka", "kafka", "event streaming"),
            new Skill("RabbitMQ", "rabbitmq", "message queue"),
            new Skill("Docker", "docker", "container"),
            new Skill("Kubernetes", "kubernetes", "k8s"),
            new Skill("AWS", "aws", "ec2", "s3", "lambda", "cloudwatch"),
            new Skill("Azure", "azure"),
            new Skill("CI/CD", "ci/cd", "jenkins", "github actions", "gitlab ci"),
            new Skill("JUnit", "junit", "unit testing"),
            new Skill("Mockito", "mockito"),
            new Skill("Maven/Gradle", "maven", "gradle"),
            new Skill("Git", "git", "github", "gitlab", "bitbucket"),
            new Skill("Design Patterns", "design pattern", "design patterns"),
            new Skill("DSA", "data structure", "algorithm", "dsa"),
            new Skill("System Design", "system design", "scalable", "distributed"),
            new Skill("Agile", "agile", "scrum", "jira"),
            new Skill("Observability", "logging", "monitoring", "prometheus", "grafana"));

    private static final Set<String> STOP_WORDS = new LinkedHashSet<>(Arrays.asList(
            "and", "the", "for", "with", "you", "are", "will", "this", "that", "from", "have",
            "has", "our", "your", "job", "role", "work", "team", "experience", "candidate",
            "developer", "engineer", "software", "good", "strong", "using", "build"));

    public AnalysisResult analyze(String resumeText, String jobDescription, String experienceLevel) {
        String resume = normalize(resumeText);
        String job = normalize(jobDescription);

        List<Skill> relevantSkills = SKILLS.stream()
                .filter(skill -> skill.isPresentIn(job))
                .collect(Collectors.toList());
        List<String> matched = relevantSkills.stream()
                .filter(skill -> skill.isPresentIn(resume))
                .map(Skill::getLabel)
                .collect(Collectors.toList());
        List<String> missing = relevantSkills.stream()
                .filter(skill -> !skill.isPresentIn(resume))
                .map(Skill::getLabel)
                .collect(Collectors.toList());
        List<String> jobKeywords = extractKeywords(jobDescription);
        List<String> keywordMatches = jobKeywords.stream()
                .filter(resume::contains)
                .collect(Collectors.toList());

        int skillScore = relevantSkills.isEmpty()
                ? 35
                : Math.round(((float) matched.size() / relevantSkills.size()) * 70);
        int keywordScore = jobKeywords.isEmpty()
                ? 15
                : Math.round(((float) keywordMatches.size() / jobKeywords.size()) * 30);
        int score = Math.max(18, Math.min(96, skillScore + keywordScore));

        List<String> missingKeywords = new ArrayList<>(missing);
        Set<String> missingKeywordKeys = missingKeywords.stream()
                .map(keyword -> keyword.toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        jobKeywords.stream()
                .filter(keyword -> !resume.contains(keyword))
                .filter(keyword -> !missingKeywordKeys.contains(keyword.toLowerCase(Locale.ROOT)))
                .limit(Math.max(0, 8 - missingKeywords.size()))
                .forEach(keyword -> {
                    missingKeywords.add(keyword);
                    missingKeywordKeys.add(keyword.toLowerCase(Locale.ROOT));
                });

        return new AnalysisResult(
                score,
                buildScoreSummary(score, matched, missingKeywords),
                matched.isEmpty()
                        ? Arrays.asList("No major Java job keywords matched yet. Add truthful skills, projects, and tools from your actual experience.")
                        : matched,
                missingKeywords.isEmpty()
                        ? Arrays.asList("No obvious gaps from this job description. Focus on proof, numbers, and interview storytelling.")
                        : missingKeywords,
                buildTopFixes(missingKeywords, matched, experienceLevel),
                buildBullets(matched, missing, experienceLevel),
                buildQuestions(missing, matched, experienceLevel),
                buildPlan(missing, matched, experienceLevel));
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private List<String> extractKeywords(String text) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String word : normalize(text).replaceAll("[^a-z0-9+#./\\s-]", " ").split("\\s+")) {
            if (word.length() > 2 && !STOP_WORDS.contains(word)) {
                counts.put(word, counts.getOrDefault(word, 0) + 1);
            }
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(12)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
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

    private String buildScoreSummary(int score, List<String> matched, List<String> missingKeywords) {
        int missingCount = missingKeywords.size();
        if (score >= 80) {
            return "Your resume is a strong fit for this Java role. Polish the remaining keyword gaps before applying.";
        }
        if (score >= 60) {
            return "Your resume matches this Java role, but you are missing " + missingCount
                    + " important keywords. Fix the top gaps before applying.";
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
        if ("sixPlus".equals(experienceLevel)) {
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

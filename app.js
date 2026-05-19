const form = document.querySelector("#fit-form");
const resumeInput = document.querySelector("#resume");
const jobInput = document.querySelector("#job");
const experienceInput = document.querySelector("#experience");
const emptyState = document.querySelector("#empty-state");
const results = document.querySelector("#results");
const scoreNode = document.querySelector("#score");
const scoreRing = document.querySelector("#score-ring");
const sampleButton = document.querySelector("#sample-button");
const clearButton = document.querySelector("#clear-button");
const printButton = document.querySelector("#print-button");
const copyBulletsButton = document.querySelector("#copy-bullets");
const copyQuestionsButton = document.querySelector("#copy-questions");
const copyPlanButton = document.querySelector("#copy-plan");
const matchedList = document.querySelector("#matched-list");
const missingList = document.querySelector("#missing-list");
const bulletList = document.querySelector("#bullet-list");
const questionList = document.querySelector("#question-list");
const planList = document.querySelector("#plan-list");

let latestReport = null;

const sampleResume = `Rahul Sharma
Java Backend Developer | 2.5 years experience

Skills: Java, Spring Boot, REST APIs, Hibernate, JPA, MySQL, JUnit, Mockito, Maven, Git, Docker, Agile

Experience:
- Built Spring Boot REST APIs for customer onboarding with validation, exception handling, and MySQL persistence.
- Improved API response time by optimizing SQL queries and reducing duplicate service calls.
- Wrote JUnit and Mockito tests for service layer flows and participated in code reviews.
- Used Docker for local development and collaborated using Git, Jira, and Agile sprint rituals.

Projects:
- Expense Tracker API: Java, Spring Boot, Spring Data JPA, MySQL, JWT authentication.
- Inventory Service: REST APIs, layered architecture, unit tests, and Swagger documentation.`;

const sampleJob = `We are hiring a Java Developer with 2-4 years of experience.

Required skills:
- Strong Core Java and Spring Boot
- REST API design and microservices
- Hibernate, JPA, and SQL databases
- JUnit, Mockito, Git, Maven or Gradle
- Docker and CI/CD exposure
- Kafka experience is a plus
- Good debugging, Agile collaboration, and production support mindset`;

const skillBank = [
  { label: "Java", terms: ["java", "core java", "jdk"] },
  { label: "Spring Boot", terms: ["spring boot", "springboot"] },
  { label: "Spring MVC", terms: ["spring mvc", "spring web"] },
  { label: "Spring Security", terms: ["spring security", "oauth", "jwt"] },
  { label: "REST APIs", terms: ["rest", "rest api", "restful", "api"] },
  { label: "Microservices", terms: ["microservice", "microservices"] },
  { label: "Hibernate/JPA", terms: ["hibernate", "jpa", "spring data"] },
  { label: "SQL", terms: ["sql", "mysql", "postgres", "postgresql", "oracle"] },
  { label: "NoSQL", terms: ["mongodb", "redis", "dynamodb", "cassandra"] },
  { label: "Kafka", terms: ["kafka", "event streaming"] },
  { label: "RabbitMQ", terms: ["rabbitmq", "message queue"] },
  { label: "Docker", terms: ["docker", "container"] },
  { label: "Kubernetes", terms: ["kubernetes", "k8s"] },
  { label: "AWS", terms: ["aws", "ec2", "s3", "lambda", "cloudwatch"] },
  { label: "Azure", terms: ["azure"] },
  { label: "CI/CD", terms: ["ci/cd", "jenkins", "github actions", "gitlab ci"] },
  { label: "JUnit", terms: ["junit", "unit testing"] },
  { label: "Mockito", terms: ["mockito"] },
  { label: "Maven/Gradle", terms: ["maven", "gradle"] },
  { label: "Git", terms: ["git", "github", "gitlab", "bitbucket"] },
  { label: "Design Patterns", terms: ["design pattern", "design patterns"] },
  { label: "DSA", terms: ["data structure", "algorithm", "dsa"] },
  { label: "System Design", terms: ["system design", "scalable", "distributed"] },
  { label: "Agile", terms: ["agile", "scrum", "jira"] },
  { label: "Observability", terms: ["logging", "monitoring", "prometheus", "grafana"] },
];

const experienceGuidance = {
  fresher: {
    role: "entry-level Java developer",
    bullets: [
      "Built Java and Spring Boot projects with REST APIs, layered architecture, validation, and database integration.",
      "Practiced DSA, OOP, collections, exception handling, and SQL through hands-on projects and coding problems.",
    ],
  },
  oneToThree: {
    role: "junior Java developer",
    bullets: [
      "Delivered Spring Boot REST APIs with JPA repositories, validation, exception handling, and unit tests.",
      "Improved API reliability by debugging production issues, writing JUnit/Mockito tests, and collaborating in Agile sprints.",
    ],
  },
  threeToSix: {
    role: "mid-level Java backend engineer",
    bullets: [
      "Designed and maintained Spring Boot microservices with database optimization, security, and CI/CD delivery.",
      "Reduced defects and deployment risk through test coverage, code reviews, observability, and clear API contracts.",
    ],
  },
  sixPlus: {
    role: "senior Java backend engineer",
    bullets: [
      "Led design of scalable Java microservices, owning architecture decisions, performance tuning, and production readiness.",
      "Mentored engineers, improved engineering standards, and drove delivery across cross-functional backend initiatives.",
    ],
  },
};

const genericStopWords = new Set([
  "and",
  "the",
  "for",
  "with",
  "you",
  "are",
  "will",
  "this",
  "that",
  "from",
  "have",
  "has",
  "our",
  "your",
  "job",
  "role",
  "work",
  "team",
  "experience",
  "candidate",
  "developer",
  "engineer",
  "software",
  "good",
  "strong",
  "using",
  "build",
]);

function normalize(text) {
  return text.toLowerCase().replace(/\s+/g, " ").trim();
}

function hasAny(text, terms) {
  return terms.some((term) => text.includes(term));
}

function extractKeywords(text) {
  const words = normalize(text)
    .replace(/[^a-z0-9+#./\s-]/g, " ")
    .split(/\s+/)
    .filter((word) => word.length > 2 && !genericStopWords.has(word));

  const counts = new Map();
  words.forEach((word) => counts.set(word, (counts.get(word) || 0) + 1));

  return [...counts.entries()]
    .sort((a, b) => b[1] - a[1])
    .slice(0, 12)
    .map(([word]) => word);
}

function analyze(resumeText, jobText, experience) {
  const resume = normalize(resumeText);
  const job = normalize(jobText);
  const relevantSkills = skillBank.filter((skill) => hasAny(job, skill.terms));
  const matched = relevantSkills.filter((skill) => hasAny(resume, skill.terms));
  const missing = relevantSkills.filter((skill) => !hasAny(resume, skill.terms));
  const jobKeywords = extractKeywords(jobText);
  const keywordMatches = jobKeywords.filter((keyword) => resume.includes(keyword));
  const skillScore = relevantSkills.length
    ? Math.round((matched.length / relevantSkills.length) * 70)
    : 35;
  const keywordScore = jobKeywords.length
    ? Math.round((keywordMatches.length / jobKeywords.length) * 30)
    : 15;
  const score = Math.max(18, Math.min(96, skillScore + keywordScore));

  return {
    score,
    matched,
    missing,
    keywords: jobKeywords.filter((keyword) => !resume.includes(keyword)).slice(0, 8),
    bullets: buildBullets(matched, missing, experience),
    questions: buildQuestions(missing, matched, experience),
    plan: buildPlan(missing, matched, experience),
  };
}

function buildBullets(matched, missing, experience) {
  const guidance = experienceGuidance[experience];
  const matchedLabels = matched.slice(0, 4).map((skill) => skill.label);
  const missingLabels = missing.slice(0, 3).map((skill) => skill.label);
  const bullets = [...guidance.bullets];

  if (matchedLabels.length) {
    bullets.unshift(
      `Position yourself as a ${guidance.role} by highlighting hands-on work with ${matchedLabels.join(", ")}.`
    );
  }

  if (missingLabels.length) {
    bullets.push(
      `Add truthful project or learning evidence for ${missingLabels.join(", ")} if you have used them. Avoid keyword stuffing without proof.`
    );
  }

  bullets.push(
    "Rewrite weak bullets with this pattern: action verb + Java/Spring skill + measurable business or technical result."
  );

  return bullets;
}

function buildQuestions(missing, matched, experience) {
  const focus = [...missing, ...matched].slice(0, 5).map((skill) => skill.label);
  const questions = [
    "Explain how Spring Boot auto-configuration works and when you would override it.",
    "How would you design a REST API for high traffic, validation, error handling, and versioning?",
    "What happens internally when a Java HashMap handles collisions?",
    "How do you write testable service-layer code using JUnit and Mockito?",
    "How would you debug a slow API in production?",
  ];

  if (focus.includes("Microservices")) {
    questions.unshift("How would you split a monolith into microservices without breaking existing users?");
  }

  if (focus.includes("Kafka")) {
    questions.unshift("How do Kafka consumer groups, offsets, partitions, and retries work in a backend service?");
  }

  if (experience === "sixPlus") {
    questions.unshift("Describe a backend architecture decision you led, including tradeoffs and production impact.");
  }

  return questions.slice(0, 7);
}

function buildPlan(missing, matched, experience) {
  const missingLabels = missing.slice(0, 4).map((skill) => skill.label);
  const matchedLabels = matched.slice(0, 3).map((skill) => skill.label);
  const priority = missingLabels.length ? missingLabels.join(", ") : "Spring Boot, REST APIs, SQL, testing";

  return [
    `Day 1: Rewrite resume summary for the target ${experienceGuidance[experience].role} role and add exact matching Java keywords you can honestly defend.`,
    `Day 2: Review core Java, OOP, collections, exceptions, streams, and concurrency basics.`,
    `Day 3: Build or polish one Spring Boot REST API story using controller, service, repository, DTO, validation, and error handling.`,
    `Day 4: Study priority gaps from this JD: ${priority}.`,
    "Day 5: Practice SQL, JPA/Hibernate mappings, transactions, indexes, and common performance problems.",
    "Day 6: Prepare testing, debugging, CI/CD, Git, and deployment examples from your own experience.",
    `Day 7: Mock interview day. Practice ${matchedLabels.length ? matchedLabels.join(", ") : "your strongest Java skills"} plus one system design question.`,
  ];
}

function renderList(node, items) {
  node.innerHTML = "";
  items.forEach((item) => {
    const li = document.createElement("li");
    li.textContent = item;
    node.appendChild(li);
  });
}

function renderResults(report) {
  latestReport = report;
  scoreNode.textContent = report.score;
  scoreRing.style.background = `conic-gradient(var(--accent) ${report.score * 3.6}deg, #e1d8c7 0deg)`;

  renderList(
    matchedList,
    report.matched.length
      ? report.matched.map((skill) => skill.label)
      : ["No major Java job keywords matched yet. Add truthful skills, projects, and tools from your actual experience."]
  );

  renderList(
    missingList,
    [
      ...report.missing.slice(0, 8).map((skill) => skill.label),
      ...report.keywords.slice(0, Math.max(0, 8 - report.missing.length)),
    ].length
      ? [
          ...report.missing.slice(0, 8).map((skill) => skill.label),
          ...report.keywords.slice(0, Math.max(0, 8 - report.missing.length)),
        ]
      : ["No obvious gaps from this job description. Focus on proof, numbers, and interview storytelling."],
  );

  renderList(bulletList, report.bullets);
  renderList(questionList, report.questions, true);
  renderList(planList, report.plan, true);

  emptyState.hidden = true;
  results.hidden = false;
}

function formatItems(title, items) {
  return `${title}\n${items.map((item, index) => `${index + 1}. ${item}`).join("\n")}`;
}

async function copyText(button, text) {
  const original = button.textContent;
  await navigator.clipboard.writeText(text);
  button.textContent = "Copied";
  setTimeout(() => {
    button.textContent = original;
  }, 1200);
}

form.addEventListener("submit", (event) => {
  event.preventDefault();
  const report = analyze(resumeInput.value, jobInput.value, experienceInput.value);
  renderResults(report);
});

sampleButton.addEventListener("click", () => {
  resumeInput.value = sampleResume;
  jobInput.value = sampleJob;
  experienceInput.value = "oneToThree";
  renderResults(analyze(sampleResume, sampleJob, "oneToThree"));
});

clearButton.addEventListener("click", () => {
  form.reset();
  latestReport = null;
  results.hidden = true;
  emptyState.hidden = false;
});

printButton.addEventListener("click", () => {
  window.print();
});

copyBulletsButton.addEventListener("click", () => {
  if (!latestReport) return;
  copyText(copyBulletsButton, formatItems("Resume bullet upgrades", latestReport.bullets));
});

copyQuestionsButton.addEventListener("click", () => {
  if (!latestReport) return;
  copyText(copyQuestionsButton, formatItems("Java interview questions", latestReport.questions));
});

copyPlanButton.addEventListener("click", () => {
  if (!latestReport) return;
  copyText(copyPlanButton, formatItems("7-day prep plan", latestReport.plan));
});

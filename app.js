const form = document.querySelector("#fit-form");
const resumeInput = document.querySelector("#resume");
const jobInput = document.querySelector("#job");
const experienceInput = document.querySelector("#experience");
const emptyState = document.querySelector("#empty-state");
const scanProgress = document.querySelector("#scan-progress");
const progressValue = document.querySelector("#progress-value");
const progressStatus = document.querySelector("#progress-status");
const progressBar = document.querySelector("#progress-bar");
const progressSteps = Array.from(document.querySelectorAll(".progress-steps li"));
const results = document.querySelector("#results");
const scoreNode = document.querySelector("#score");
const scoreSummary = document.querySelector("#score-summary");
const scoreRing = document.querySelector("#score-ring");
const analyzeButton = document.querySelector("#analyze-button");
const sampleButton = document.querySelector("#sample-button");
const clearButton = document.querySelector("#clear-button");
const printButton = document.querySelector("#print-button");
const resumeError = document.querySelector("#resume-error");
const jobError = document.querySelector("#job-error");
const formError = document.querySelector("#form-error");
const fixList = document.querySelector("#fix-list");
const matchedList = document.querySelector("#matched-list");
const missingList = document.querySelector("#missing-list");
const bulletList = document.querySelector("#bullet-list");
const questionList = document.querySelector("#question-list");
const planList = document.querySelector("#plan-list");
const lockedGrid = document.querySelector("#locked-grid");
const copyBulletsButton = document.querySelector("#copy-bullets");
const copyQuestionsButton = document.querySelector("#copy-questions");
const copyPlanButton = document.querySelector("#copy-plan");
const leadForm = document.querySelector("#lead-form");
const leadEmail = document.querySelector("#lead-email");
const leadCountry = document.querySelector("#lead-country");
const leadConsent = document.querySelector("#lead-consent");
const leadStatus = document.querySelector("#lead-status");
const feedbackForm = document.querySelector("#feedback-form");
const feedbackEmail = document.querySelector("#feedback-email");
const feedbackMessage = document.querySelector("#feedback-message");
const feedbackStatus = document.querySelector("#feedback-status");
const premiumModal = document.querySelector("#premium-modal");
const premiumClose = document.querySelector("#premium-close");
const joinEarlyAccess = document.querySelector("#join-early-access");
const notifyPro = document.querySelector("#notify-pro");

let latestReport = null;
let latestReportSaved = false;
let resumePasteTracked = false;
let jdPasteTracked = false;
let progressTimer = null;
let progressMessageTimer = null;
let progressPercent = 0;
const apiBase = (window.JAVAJOBFIT_API_BASE || "").replace(/\/$/, "");
const defaultAnalyzeLabel = "Analyze my Java resume";
const backendTimeoutMs = 18000;

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

// Mirrors AnalysisService.SKILLS on the backend. Keep both lists in sync.
const skillBank = [
  { label: "Java", terms: ["java", "core java", "jdk"] },
  { label: "Kotlin", terms: ["kotlin"] },
  { label: "Spring Boot", terms: ["spring boot", "springboot"] },
  { label: "Spring MVC", terms: ["spring mvc", "spring web"] },
  { label: "Spring Security", terms: ["spring security", "oauth", "jwt"] },
  { label: "Reactive/WebFlux", terms: ["webflux", "reactive", "project reactor"] },
  { label: "REST APIs", terms: ["rest", "rest api", "restful"] },
  { label: "GraphQL", terms: ["graphql"] },
  { label: "gRPC", terms: ["grpc"] },
  { label: "Microservices", terms: ["microservice", "microservices"] },
  { label: "Hibernate/JPA", terms: ["hibernate", "jpa", "spring data"] },
  { label: "SQL", terms: ["sql", "mysql", "postgres", "postgresql", "oracle"] },
  { label: "NoSQL", terms: ["mongodb", "redis", "dynamodb", "cassandra"] },
  { label: "Elasticsearch", terms: ["elasticsearch", "elk"] },
  { label: "Kafka", terms: ["kafka", "event streaming"] },
  { label: "RabbitMQ", terms: ["rabbitmq", "message queue"] },
  { label: "Docker", terms: ["docker", "container"] },
  { label: "Kubernetes", terms: ["kubernetes", "k8s"] },
  { label: "AWS", terms: ["aws", "ec2", "s3", "lambda", "cloudwatch"] },
  { label: "Azure", terms: ["azure"] },
  { label: "GCP", terms: ["gcp", "google cloud"] },
  { label: "Terraform/IaC", terms: ["terraform", "iac"] },
  { label: "CI/CD", terms: ["ci/cd", "jenkins", "github actions", "gitlab ci"] },
  { label: "JUnit", terms: ["junit", "unit testing"] },
  { label: "Mockito", terms: ["mockito"] },
  { label: "Maven/Gradle", terms: ["maven", "gradle"] },
  { label: "Git", terms: ["git", "github", "gitlab", "bitbucket"] },
  { label: "Design Patterns", terms: ["design pattern", "design patterns"] },
  { label: "DSA", terms: ["data structure", "data structures", "algorithm", "algorithms", "dsa"] },
  { label: "System Design", terms: ["system design", "scalable", "distributed"] },
  { label: "Agile", terms: ["agile", "scrum", "jira"] },
  { label: "Observability", terms: ["logging", "monitoring", "prometheus", "grafana"] },
];

// Requirement weights — mirror AnalysisService.
const SCORING = {
  skill: { required: 3.0, neutral: 2.0, preferred: 1.0 },
  keyword: { required: 1.5, neutral: 1.0, preferred: 0.5 },
  maxUnigrams: 12,
  maxBigrams: 6,
  maxKeywordRequirements: 10,
  freeMissingLimit: 5,
};

const requiredCues = [
  "required", "require", "must", "must-have", "must have", "strong", "proficient",
  "proficiency", "expertise", "expert", "essential", "essentials", "minimum", "at least",
  "solid", "deep", "advanced", "mandatory", "extensive", "hands-on", "hands on",
];
const preferredCues = [
  "plus", "nice to have", "nice-to-have", "preferred", "prefer", "bonus", "good to have",
  "advantage", "desirable", "desired", "ideally", "optional", "familiarity", "exposure",
  "a plus", "would be", "is a plus", "are a plus",
];

const experienceGuidance = {
  fresher: "entry-level Java developer",
  oneToThree: "junior Java developer",
  threeToFive: "Java backend engineer",
  fiveToEight: "senior Java backend engineer",
  senior: "senior Java technical leader",
  threeToSix: "mid-level Java backend engineer",
  sixPlus: "senior Java backend engineer",
};

// Mirrors AnalysisService.STOP_WORDS on the backend.
const genericStopWords = new Set([
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
  "world", "every", "ensure", "deliver", "delivering", "support", "supporting",
]);

function trackEvent(name, payload = {}) {
  const event = {
    name,
    payload: {
      ...payload,
      hasReport: Boolean(latestReport),
      timestamp: new Date().toISOString(),
    },
  };

  if (window.JAVAJOBFIT_TRACK_EVENT) {
    window.JAVAJOBFIT_TRACK_EVENT(event.name, event.payload);
    return;
  }

  if (location.hostname === "localhost" || location.hostname === "127.0.0.1") {
    console.info("[JavaJobFit event]", event.name, event.payload);
  }
}

function normalize(text) {
  return (text || "").toLowerCase().replace(/\s+/g, " ").trim();
}

function escapeRegex(text) {
  return text.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

function hasAny(text, terms) {
  return terms.some((term) => new RegExp(`(^|[^a-z0-9+#])${escapeRegex(term)}([^a-z0-9+#]|$)`).test(text));
}

// Light, deterministic suffix stemmer. Must match AnalysisService.stem() exactly.
function stem(word) {
  let w = word;
  if (w.length <= 3) return w;
  if (w.endsWith("ing") && w.length - 3 >= 3) {
    w = w.slice(0, -3);
  } else if (w.endsWith("ed") && w.length - 2 >= 3) {
    w = w.slice(0, -2);
  }
  if (w.endsWith("ies") && w.length > 4) {
    w = w.slice(0, -3) + "y";
  }
  if (w.length > 3 && w.endsWith("s") && !w.endsWith("ss")) {
    w = w.slice(0, -1);
  }
  return w;
}

function segments(text) {
  return normalize(text)
    .split(/[\n.;:!?]+/)
    .map((part) => part.replace(/\s+/g, " ").trim())
    .filter(Boolean);
}

function trimEdges(word) {
  return word.replace(/^[./-]+/, "").replace(/[./-]+$/, "");
}

function contentTokens(text) {
  return text
    .replace(/[^a-z0-9+#./-]/g, " ")
    .split(/\s+/)
    .map(trimEdges)
    .filter((word) => word.length > 2 && !genericStopWords.has(word) && !/^[0-9]+$/.test(word));
}

function stemmedUnigrams(normalized) {
  return new Set(contentTokens(normalized).map(stem));
}

// Bigrams built WITHIN each segment so pairs never cross a sentence boundary.
function stemmedBigrams(segs) {
  const set = new Set();
  segs.forEach((segment) => {
    const tokens = contentTokens(segment);
    for (let i = 0; i + 1 < tokens.length; i += 1) {
      set.add(`${stem(tokens[i])} ${stem(tokens[i + 1])}`);
    }
  });
  return set;
}

function skillStemmedTerms(skill) {
  return skill.terms.map((term) => term.split(/\s+/).map(stem).join(" "));
}

function containsCue(segment, cues) {
  return cues.some((cue) => segment.includes(cue));
}

// Returns "required" | "preferred" | "neutral" for a requirement, based on JD sentence context.
function classify(segs, matcher) {
  let required = false;
  let preferred = false;
  segs.forEach((seg) => {
    if (!matcher(seg)) return;
    if (containsCue(seg, requiredCues)) required = true;
    if (containsCue(seg, preferredCues)) preferred = true;
  });
  if (required) return "required";
  if (preferred) return "preferred";
  return "neutral";
}

function topByFrequency(freq, limit) {
  return [...freq.entries()]
    .sort((a, b) => b[1] - a[1] || (a[0] < b[0] ? -1 : a[0] > b[0] ? 1 : 0))
    .slice(0, limit)
    .map(([key]) => key);
}

function keywordRequirements(job, segs, resumeUnigrams, resumeBigrams, skillStems) {
  const tokens = contentTokens(job);

  const unigramFreq = new Map();
  const unigramLabel = new Map();
  tokens.forEach((token) => {
    const s = stem(token);
    unigramFreq.set(s, (unigramFreq.get(s) || 0) + 1);
    if (!unigramLabel.has(s)) unigramLabel.set(s, token);
  });

  const bigramFreq = new Map();
  const bigramLabel = new Map();
  segs.forEach((segment) => {
    const segTokens = contentTokens(segment);
    for (let i = 0; i + 1 < segTokens.length; i += 1) {
      const s = `${stem(segTokens[i])} ${stem(segTokens[i + 1])}`;
      bigramFreq.set(s, (bigramFreq.get(s) || 0) + 1);
      if (!bigramLabel.has(s)) bigramLabel.set(s, `${segTokens[i]} ${segTokens[i + 1]}`);
    }
  });

  const topUnigrams = topByFrequency(unigramFreq, SCORING.maxUnigrams);
  const topBigrams = topByFrequency(bigramFreq, SCORING.maxBigrams);

  const reqs = [];
  const used = new Set(skillStems);

  topBigrams.forEach((s) => {
    if (reqs.length >= SCORING.maxKeywordRequirements || used.has(s)) return;
    used.add(s);
    const label = bigramLabel.get(s);
    const weight = SCORING.keyword[classify(segs, (seg) => seg.includes(label))];
    reqs.push({ label, weight, covered: resumeBigrams.has(s) });
  });

  topUnigrams.forEach((s) => {
    if (reqs.length >= SCORING.maxKeywordRequirements || used.has(s)) return;
    used.add(s);
    const label = unigramLabel.get(s);
    const weight = SCORING.keyword[classify(segs, (seg) => seg.includes(label))];
    reqs.push({ label, weight, covered: resumeUnigrams.has(s) });
  });

  return reqs;
}

function analyzeLocally(resumeText, jobText, experience) {
  const resume = normalize(resumeText);
  const job = normalize(jobText);
  const segs = segments(jobText);
  const resumeUnigrams = stemmedUnigrams(resume);
  const resumeBigrams = stemmedBigrams(segments(resumeText));

  const skillStems = new Set();
  const skillReqs = [];
  skillBank.forEach((skill) => {
    skillStemmedTerms(skill).forEach((s) => skillStems.add(s));
    if (hasAny(job, skill.terms)) {
      const weight = SCORING.skill[classify(segs, (seg) => hasAny(seg, skill.terms))];
      skillReqs.push({ label: skill.label, weight, covered: hasAny(resume, skill.terms) });
    }
  });

  const keywordReqs = keywordRequirements(job, segs, resumeUnigrams, resumeBigrams, skillStems);

  let totalWeight = 0;
  let coveredWeight = 0;
  [...skillReqs, ...keywordReqs].forEach((r) => {
    totalWeight += r.weight;
    if (r.covered) coveredWeight += r.weight;
  });
  const score = totalWeight <= 0
    ? 0
    : Math.max(5, Math.min(99, Math.round((coveredWeight / totalWeight) * 100)));

  const matched = skillReqs.filter((r) => r.covered).map((r) => r.label);
  const missingSkills = skillReqs.filter((r) => !r.covered).map((r) => r.label);

  const missingKeywords = [...missingSkills];
  const missingKeys = new Set(missingKeywords.map((value) => value.toLowerCase()));
  keywordReqs.forEach((r) => {
    if (missingKeywords.length >= 8 || r.covered) return;
    const key = r.label.toLowerCase();
    if (!missingKeys.has(key)) {
      missingKeys.add(key);
      missingKeywords.push(r.label);
    }
  });

  const shownMissing = Math.min(missingKeywords.length, SCORING.freeMissingLimit);
  const role = experienceGuidance[experience] || experienceGuidance.oneToThree;
  const topFixes = [
    missingKeywords.length
      ? `Add truthful resume evidence for ${missingKeywords.slice(0, 3).join(", ")}.`
      : "Keep the strongest Java proof visible near the top of your resume.",
    `Rewrite your summary for a ${role} role using Java/Spring keywords you can defend.`,
    "Turn one project or work item into a measurable backend impact bullet.",
  ];

  return normalizeReport({
    id: null,
    saved: false,
    score,
    scoreSummary: buildScoreSummary(score, matched, shownMissing),
    matchedSkills: matched,
    missingKeywords,
    topFixes,
    bulletSuggestions: [
      matched.length
        ? `Position yourself as a ${role} by highlighting hands-on work with ${matched.slice(0, 3).join(", ")}.`
        : `Add a project bullet that proves Java, Spring Boot, REST APIs, SQL, and testing experience for a ${role} role.`,
    ],
    interviewQuestions: [
      "Explain how Spring Boot auto-configuration works and when you would override it.",
      "How would you design a REST API for high traffic, validation, error handling, and versioning?",
      "How do you write testable service-layer code using JUnit and Mockito?",
    ],
    prepPlan: [
      `Day 1: Rewrite resume summary for the target ${role} role and add exact Java keywords you can honestly defend.`,
      "Day 2: Review core Java, OOP, collections, exceptions, streams, and concurrency basics.",
    ],
    premiumAvailable: true,
    premiumLockedSections: defaultLockedSections(),
  });
}

function buildScoreSummary(score, matched, shownMissing) {
  if (score >= 80) return "Your resume is a strong fit for this Java role. Polish the remaining keyword gaps before applying.";
  if (score >= 60) {
    const noun = shownMissing === 1 ? "keyword" : "keywords";
    return `Your resume matches this Java role, but you are missing ${shownMissing} important ${noun}. Fix the top gaps before applying.`;
  }
  if (!matched.length) {
    return "Your resume needs clearer Java and Spring Boot evidence for this role. Add truthful project and skill proof before applying.";
  }
  return "Your resume has some useful Java signals, but it needs stronger alignment with this job description before applying.";
}

function defaultLockedSections() {
  return [
    "Full keyword analysis",
    "10+ resume bullet upgrades",
    "Keyword placement suggestions",
    "Tailored Java/Spring Boot resume summary",
    "Full Java interview question set",
    "Full 7-day prep plan",
    "Cover letter draft",
    "LinkedIn headline/About rewrite",
    "Export full PDF/DOCX report",
  ];
}

function normalizeReport(report) {
  const score = report.atsScore ?? report.score ?? 0;
  const matched = report.matchedStrengths || report.matchedSkills || report.matched || [];
  const missing = report.missingKeywords || report.missing || [];
  const bullets = report.bulletUpgrades || report.bulletSuggestions || report.bullets || [];
  const questions = report.interviewQuestions || report.questions || [];
  const plan = report.prepPlan || report.plan || [];

  return {
    id: report.publicId || report.reportId || report.id || null,
    saved: Boolean(report.publicId || report.reportId || report.id),
    score,
    scoreSummary: report.scoreSummary || buildScoreSummary(score, matched, Math.min(missing.length, 5)),
    matched: matched.slice(0, 3),
    missing: missing.slice(0, 5),
    topFixes: (report.topFixes || []).slice(0, 3),
    bullets: bullets.slice(0, 1),
    questions: questions.slice(0, 3),
    plan: plan.slice(0, 2),
    premiumAvailable: report.premiumAvailable !== false,
    premiumLockedSections: report.premiumLockedSections || defaultLockedSections(),
    experienceLevel: report.experienceLevel || experienceInput.value,
    notice: report.notice || "",
  };
}

function renderList(node, items) {
  node.innerHTML = "";
  items.forEach((item) => {
    const li = document.createElement("li");
    li.textContent = item;
    node.appendChild(li);
  });
}

function renderLockedSections(sections) {
  lockedGrid.innerHTML = "";
  sections.forEach((section) => {
    const card = document.createElement("article");
    card.className = "locked-card";
    const badge = document.createElement("span");
    const heading = document.createElement("h3");
    const text = document.createElement("p");
    const button = document.createElement("button");

    badge.className = "lock-badge";
    badge.textContent = "Locked";
    heading.textContent = section;
    text.textContent = "Unlock full Java resume optimization to access this section.";
    button.type = "button";
    button.className = "secondary-action premium-cta";
    button.textContent = "Join early access";

    card.append(badge, heading, text, button);
    lockedGrid.appendChild(card);
  });
}

function clearRenderedResults() {
  [fixList, matchedList, missingList, bulletList, questionList, planList].forEach((node) => {
    node.innerHTML = "";
  });
  lockedGrid.innerHTML = "";
  scoreNode.textContent = "--";
  scoreSummary.textContent = "";
  scoreRing.style.background = "conic-gradient(var(--accent) 0deg, #e1d8c7 0deg)";
}

function updateProgress(percent) {
  progressPercent = Math.max(0, Math.min(100, Math.round(percent)));
  progressValue.textContent = `${progressPercent}%`;
  progressBar.style.width = `${progressPercent}%`;

  const activeStep = progressPercent < 28 ? 0 : progressPercent < 58 ? 1 : progressPercent < 88 ? 2 : 3;
  progressSteps.forEach((step, index) => {
    step.classList.toggle("active", index <= activeStep);
  });

  if (progressPercent < 28) {
    progressStatus.textContent = "Reading resume signals...";
  } else if (progressPercent < 58) {
    progressStatus.textContent = "Comparing Java and Spring Boot keywords...";
  } else if (progressPercent < 88) {
    progressStatus.textContent = "Building your free report preview...";
  } else if (progressPercent < 100) {
    progressStatus.textContent = "Finalizing recommendations...";
  } else {
    progressStatus.textContent = "Report ready.";
  }
}

function startScanProgress() {
  clearInterval(progressTimer);
  clearTimeout(progressMessageTimer);
  updateProgress(0);
  emptyState.hidden = true;
  results.hidden = true;
  scanProgress.hidden = false;

  progressTimer = setInterval(() => {
    const remaining = 94 - progressPercent;
    const increment = Math.max(1, Math.ceil(remaining * 0.08));
    updateProgress(progressPercent + increment);
    if (progressPercent >= 94) {
      clearInterval(progressTimer);
      progressTimer = null;
    }
  }, 180);

  progressMessageTimer = setTimeout(() => {
    if (!scanProgress.hidden && progressPercent >= 80) {
      progressStatus.textContent = "Waking the backend. Preparing a browser preview if it takes too long...";
    }
  }, 7000);
}

function stopScanProgress() {
  clearInterval(progressTimer);
  clearTimeout(progressMessageTimer);
  progressTimer = null;
  progressMessageTimer = null;
  scanProgress.hidden = true;
}

function finishScanProgress() {
  clearInterval(progressTimer);
  clearTimeout(progressMessageTimer);
  progressTimer = null;
  progressMessageTimer = null;
  updateProgress(100);
  return new Promise((resolve) => setTimeout(resolve, 360));
}

function renderResults(report) {
  latestReport = normalizeReport(report);
  latestReportSaved = Boolean(latestReport.saved && latestReport.id);

  scoreNode.textContent = latestReport.score;
  scoreSummary.textContent = latestReport.scoreSummary;
  scoreRing.style.background = `conic-gradient(var(--accent) ${latestReport.score * 3.6}deg, #e1d8c7 0deg)`;

  renderList(fixList, latestReport.topFixes.length ? latestReport.topFixes : ["Add clearer Java/Spring proof before applying."]);
  renderList(matchedList, latestReport.matched.length ? latestReport.matched : ["No major Java job keywords matched yet."]);
  renderList(missingList, latestReport.missing.length ? latestReport.missing : ["No obvious gaps from this job description."]);
  renderList(bulletList, latestReport.bullets);
  renderList(questionList, latestReport.questions);
  renderList(planList, latestReport.plan);
  renderLockedSections(latestReport.premiumLockedSections);

  feedbackStatus.textContent = latestReportSaved
    ? ""
    : latestReport.notice || "Feedback can still be sent, but it may not attach to a saved report.";
  emptyState.hidden = true;
  scanProgress.hidden = true;
  results.hidden = false;
}

function validateInputs() {
  const resume = resumeInput.value.trim();
  const job = jobInput.value.trim();
  let valid = true;

  resumeError.textContent = "";
  jobError.textContent = "";
  formError.textContent = "";

  if (!resume) {
    resumeError.textContent = "Resume is required.";
    valid = false;
  } else if (resume.length < 40) {
    resumeError.textContent = "Please paste a longer resume for a useful scan.";
    valid = false;
  }

  if (!job) {
    jobError.textContent = "Job description is required.";
    valid = false;
  } else if (job.length < 40) {
    jobError.textContent = "Please paste a longer job description for a useful scan.";
    valid = false;
  }

  if (!valid && (!resume || !job)) {
    formError.textContent = "Please paste both your resume and the target job description.";
  }

  return valid;
}

async function createBackendReport() {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), backendTimeoutMs);

  const response = await fetch(`${apiBase}/api/reports`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    signal: controller.signal,
    body: JSON.stringify({
      resumeText: resumeInput.value,
      jobDescription: jobInput.value,
      experienceLevel: experienceInput.value,
    }),
  }).finally(() => clearTimeout(timeoutId));

  if (!response.ok) {
    let message = "Something went wrong while generating your report. Please try again.";
    if (response.status === 400) {
      message = "Please paste both your resume and the target job description.";
    } else if (response.status >= 500) {
      message = "JavaJobFit is temporarily unavailable. Please try again in a minute.";
    }
    const error = new Error(message);
    error.status = response.status;
    throw error;
  }

  return normalizeReport(await response.json());
}

async function buildReport() {
  if (!apiBase) {
    return analyzeLocally(resumeInput.value, jobInput.value, experienceInput.value);
  }
  try {
    return await createBackendReport();
  } catch (error) {
    if (error.name === "AbortError" || error.status >= 500 || error instanceof TypeError) {
      const report = analyzeLocally(resumeInput.value, jobInput.value, experienceInput.value);
      report.saved = false;
      report.id = null;
      report.notice =
        "The backend is taking longer than usual, so this free preview was generated in your browser. Try again in a minute to save the report.";
      return report;
    }
    throw error;
  }
}

function setLoading(isLoading) {
  analyzeButton.disabled = isLoading;
  analyzeButton.textContent = isLoading ? "Analyzing your Java resume..." : defaultAnalyzeLabel;
}

function resetScanState() {
  latestReport = null;
  latestReportSaved = false;
  formError.textContent = "";
  feedbackStatus.textContent = "";
  leadStatus.textContent = "";
  stopScanProgress();
  clearRenderedResults();
  results.hidden = true;
  emptyState.hidden = false;
}

function formatItems(title, items) {
  return `${title}\n${items.map((item, index) => `${index + 1}. ${item}`).join("\n")}`;
}

async function copyText(button, title, items) {
  if (!latestReport || !items.length) return;
  const original = button.textContent;
  try {
    const text = formatItems(title, items);
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(text);
    } else {
      const textarea = document.createElement("textarea");
      textarea.value = text;
      textarea.setAttribute("readonly", "");
      textarea.style.position = "absolute";
      textarea.style.left = "-9999px";
      document.body.appendChild(textarea);
      textarea.select();
      document.execCommand("copy");
      document.body.removeChild(textarea);
    }
    trackEvent("copy_clicked", { title });
    button.textContent = "Copied";
  } catch (error) {
    console.warn("Copy failed", error);
    button.textContent = "Copy failed";
  } finally {
    setTimeout(() => {
      button.textContent = original;
    }, 1200);
  }
}

function openPremiumModal() {
  trackEvent("premium_cta_clicked", { publicId: latestReport?.id || null });
  premiumModal.hidden = false;
}

function closePremiumModal() {
  premiumModal.hidden = true;
}

form.addEventListener("submit", async (event) => {
  event.preventDefault();
  trackEvent("analyze_clicked", { experienceLevel: experienceInput.value });
  if (!validateInputs()) {
    trackEvent("scan_failed", { reason: "invalid_input" });
    return;
  }

  setLoading(true);
  startScanProgress();
  try {
    const report = await buildReport();
    await finishScanProgress();
    renderResults(report);
    trackEvent("scan_completed", { publicId: latestReport?.id || null, score: latestReport.score });
  } catch (error) {
    console.warn("Scan failed", error);
    stopScanProgress();
    emptyState.hidden = false;
    formError.textContent = error.message || "Something went wrong while generating your report. Please try again.";
    trackEvent("scan_failed", { reason: error.status || "unknown" });
  } finally {
    setLoading(false);
  }
});

sampleButton.addEventListener("click", () => {
  resumeInput.value = sampleResume;
  jobInput.value = sampleJob;
  experienceInput.value = "oneToThree";
  resumeError.textContent = "";
  jobError.textContent = "";
  feedbackForm.reset();
  leadForm.reset();
  resetScanState();
  trackEvent("sample_loaded");
});

clearButton.addEventListener("click", () => {
  form.reset();
  feedbackForm.reset();
  leadForm.reset();
  resumePasteTracked = false;
  jdPasteTracked = false;
  resumeError.textContent = "";
  jobError.textContent = "";
  resetScanState();
});

resumeInput.addEventListener("input", () => {
  if (!resumePasteTracked && resumeInput.value.trim().length > 30) {
    resumePasteTracked = true;
    trackEvent("resume_pasted");
  }
});

jobInput.addEventListener("input", () => {
  if (!jdPasteTracked && jobInput.value.trim().length > 30) {
    jdPasteTracked = true;
    trackEvent("jd_pasted");
  }
});

printButton.addEventListener("click", () => {
  trackEvent("pdf_export_clicked", { publicId: latestReport?.id || null, freePreview: true });
  window.print();
});

leadForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  leadStatus.textContent = "";

  if (!leadEmail.value.trim()) {
    leadStatus.textContent = "Please enter your email.";
    return;
  }

  if (!apiBase) {
    leadStatus.textContent = "Saved locally for this session. Backend lead capture is not configured.";
    trackEvent("email_submitted", { localOnly: true });
    return;
  }

  try {
    const response = await fetch(`${apiBase}/api/leads`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        email: leadEmail.value,
        experienceLevel: experienceInput.value,
        country: leadCountry.value,
        publicId: latestReport?.id || null,
        consent: leadConsent.checked,
      }),
    });

    if (!response.ok) throw new Error(`Lead request failed (${response.status})`);
    leadStatus.textContent = "Saved. We'll notify you when full report unlock is available.";
    trackEvent("email_submitted", { publicId: latestReport?.id || null });
  } catch (error) {
    console.warn("Lead capture failed", error);
    leadStatus.textContent = "Email could not be saved. Please try again later.";
  }
});

feedbackForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  feedbackStatus.textContent = "";

  if (!apiBase) {
    feedbackStatus.textContent = "Backend is not configured yet. Feedback will work after API deployment.";
    return;
  }

  try {
    const response = await fetch(`${apiBase}/api/feedback`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        publicId: latestReport?.id || null,
        email: feedbackEmail.value,
        message: feedbackMessage.value,
      }),
    });

    if (!response.ok) throw new Error(`Feedback request failed (${response.status})`);
    feedbackMessage.value = "";
    feedbackStatus.textContent = "Thanks. Feedback saved.";
    trackEvent("feedback_submitted", { publicId: latestReport?.id || null });
  } catch (error) {
    console.warn("Feedback failed", error);
    feedbackStatus.textContent = "Feedback could not be saved. Please try again later.";
  }
});

document.addEventListener("click", (event) => {
  if (event.target.closest(".premium-cta")) {
    openPremiumModal();
  }
});

premiumClose.addEventListener("click", closePremiumModal);
notifyPro.addEventListener("click", closePremiumModal);
joinEarlyAccess.addEventListener("click", () => {
  closePremiumModal();
  leadEmail.focus();
});

premiumModal.addEventListener("click", (event) => {
  if (event.target === premiumModal) closePremiumModal();
});

copyBulletsButton.addEventListener("click", () => {
  copyText(copyBulletsButton, "Resume bullet upgrades", latestReport?.bullets || []);
});

copyQuestionsButton.addEventListener("click", () => {
  copyText(copyQuestionsButton, "Java interview questions", latestReport?.questions || []);
});

copyPlanButton.addEventListener("click", () => {
  copyText(copyPlanButton, "7-day prep plan preview", latestReport?.plan || []);
});

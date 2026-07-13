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
const resumeFileInput = document.querySelector("#resume-file");
const uploadStatus = document.querySelector("#upload-status");
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
const scoreBreakdownNode = document.querySelector("#score-breakdown");
const lockedGrid = document.querySelector("#locked-grid");
const copyBulletsButton = document.querySelector("#copy-bullets");
const copyQuestionsButton = document.querySelector("#copy-questions");
const copyPlanButton = document.querySelector("#copy-plan");
const leadForm = document.querySelector("#lead-form");
const leadEmail = document.querySelector("#lead-email");
const leadCountry = document.querySelector("#lead-country");
const leadConsent = document.querySelector("#lead-consent");
const leadStatus = document.querySelector("#lead-status");
const leadSubmitButton = leadForm?.querySelector('button[type="submit"]');
const feedbackForm = document.querySelector("#feedback-form");
const feedbackEmail = document.querySelector("#feedback-email");
const feedbackMessage = document.querySelector("#feedback-message");
const feedbackStatus = document.querySelector("#feedback-status");
const feedbackSubmitButton = feedbackForm?.querySelector('button[type="submit"]');
const usefulnessStatus = document.querySelector("#usefulness-status");
const usefulYes = document.querySelector("#useful-yes");
const usefulNo = document.querySelector("#useful-no");
const premiumModal = document.querySelector("#premium-modal");
const premiumClose = document.querySelector("#premium-close");
const joinEarlyAccess = document.querySelector("#join-early-access");
const notifyPro = document.querySelector("#notify-pro");

let latestReport = null;
let latestReportSaved = false;
// Auto-upgrade state: when a scan falls back to the browser engine (backend cold/slow),
// we retry the backend in the background and replace the preview with the real saved report.
let scanGeneration = 0;
let upgradeTimers = [];
let upgradeInFlight = false;
const UPGRADE_DELAYS_MS = [20000, 45000, 90000];
let resumePasteTracked = false;
let jdPasteTracked = false;
let progressTimer = null;
let progressMessageTimer = null;
let progressPercent = 0;
const apiBase = (window.JAVAJOBFIT_API_BASE || "").replace(/\/$/, "");
const defaultAnalyzeLabel = "Analyze my Java resume";
const backendTimeoutMs = 18000;
const uploadTimeoutMs = 120000; // cold Render free instance can take ~100s to wake
let backendWarmed = false;

// Wake the free-tier backend early so the first scan/upload does not eat a ~100s cold start.
// Fire-and-forget on load, and again the first time the user touches the upload control.
function warmBackend() {
  if (!apiBase || backendWarmed || window.location.protocol === "file:") return;
  backendWarmed = true;
  fetch(`${apiBase}/api/health`, { method: "GET", keepalive: true }).catch(() => {
    backendWarmed = false; // allow a later retry if the wake ping failed
  });
}
const urlParams = new URLSearchParams(window.location.search);
const pageSource = urlParams.get("source") || urlParams.get("ref") || "";
const utmSource = urlParams.get("utm_source") || "";
const utmMedium = urlParams.get("utm_medium") || "";
const utmCampaign = urlParams.get("utm_campaign") || "";

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
  "project", "projects", "available", "availability", "become", "becomes", "became",
  "knowledge", "understanding", "familiarity", "hands", "based", "level", "part", "full",
  "time", "company", "companies", "product", "products", "service", "services", "system",
  "systems", "application", "applications", "skill", "skills", "tool", "tools",
  "technology", "technologies", "environment", "environments", "opportunity", "people",
  "world", "every", "ensure", "deliver", "delivering", "support", "supporting",
  "canary", "resume", "resumes", "marker", "markers",
]);

function trackEvent(name, payload = {}) {
  const safePayload = {
    pagePath: window.location.pathname,
    reportPublicId: payload.publicId || payload.reportPublicId || null,
    experienceLevel: payload.experienceLevel || latestReport?.experienceLevel || null,
    country: payload.country || null,
    source: payload.source || pageSource || "frontend",
    utmSource,
    utmMedium,
    utmCampaign,
    timestamp: new Date().toISOString(),
    viewport: `${window.innerWidth}x${window.innerHeight}`,
    reason: payload.reason || null,
    score: typeof payload.score === "number" ? payload.score : null,
    label: payload.label || payload.title || null,
    localOnly: Boolean(payload.localOnly),
  };
  const event = {
    name,
    payload: safePayload,
  };

  if (window.JAVAJOBFIT_TRACK_EVENT) {
    window.JAVAJOBFIT_TRACK_EVENT(event.name, event.payload);
  }

  if (location.hostname === "localhost" || location.hostname === "127.0.0.1") {
    console.info("[JavaJobFit event]", event.name, event.payload);
  }

  if (!apiBase) {
    return;
  }

  const body = JSON.stringify({
    eventName: event.name,
    pagePath: event.payload.pagePath,
    reportPublicId: event.payload.reportPublicId,
    experienceLevel: event.payload.experienceLevel,
    country: event.payload.country,
    source: event.payload.source,
    utmSource: event.payload.utmSource,
    utmMedium: event.payload.utmMedium,
    utmCampaign: event.payload.utmCampaign,
  });

  if (navigator.sendBeacon) {
    navigator.sendBeacon(`${apiBase}/api/events`, new Blob([body], { type: "application/json" }));
    return;
  }

  fetch(`${apiBase}/api/events`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body,
    keepalive: true,
  }).catch(() => {
    // Analytics should never interrupt the resume scan.
  });
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

function removePrivateMarkers(text) {
  return text.replace(/(do[_-]?not[_-]?store|beta[_-]?canary|canary)[a-z0-9_-]*/g, " ");
}

function contentTokens(text) {
  return removePrivateMarkers(text)
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
    "10+ Java resume bullet upgrades",
    "Keyword placement suggestions",
    "Full Java/Spring Boot interview questions",
    "Full 7-day prep plan",
    "Cover letter draft",
    "LinkedIn headline/About rewrite",
    "Export full report",
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
    // Only the backend engine produces a real 7-component breakdown. The browser fallback
    // engine computes a simpler preview score, so we pass null and hide the breakdown card
    // instead of rendering misleading zeros next to a non-zero headline score.
    scoreBreakdown: report.scoreBreakdown || null,
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

function renderLockedSections() {
  lockedGrid.innerHTML = "";
  lockedGrid.hidden = true;
}

function renderScoreBreakdown(breakdown) {
  if (!scoreBreakdownNode) return;
  const card = scoreBreakdownNode.closest(".score-breakdown-card");
  scoreBreakdownNode.innerHTML = "";

  if (!breakdown) {
    // Browser-fallback preview: no real component breakdown exists, so hide the card
    // rather than showing zeros that contradict the headline score.
    if (card) card.hidden = true;
    return;
  }
  if (card) card.hidden = false;

  const rows = [
    ["Must-have skills", breakdown.mustHaveScore, 30],
    ["Preferred skills", breakdown.preferredScore, 10],
    ["Keyword alignment", breakdown.keywordScore, 15],
    ["Evidence quality", breakdown.evidenceScore, 20],
    ["Seniority fit", breakdown.seniorityScore, 10],
    ["Impact metrics", breakdown.impactScore, 5],
    ["Readability", breakdown.readabilityScore, 10],
  ];

  rows.forEach(([label, value, max]) => {
    const item = document.createElement("div");
    item.className = "breakdown-item";
    const safeValue = Math.max(0, Math.min(max, Math.round(Number(value) || 0)));
    item.innerHTML = `<span>${label}</span><strong>${safeValue}/${max}</strong>`;
    scoreBreakdownNode.appendChild(item);
  });
}

function clearRenderedResults() {
  [fixList, matchedList, missingList, bulletList, questionList, planList].forEach((node) => {
    node.innerHTML = "";
  });
  lockedGrid.innerHTML = "";
  if (scoreBreakdownNode) scoreBreakdownNode.innerHTML = "";
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
  renderScoreBreakdown(latestReport.scoreBreakdown);
  renderLockedSections(latestReport.premiumLockedSections);

  feedbackStatus.textContent = latestReportSaved
    ? ""
    : latestReport.notice || "Feedback can still be sent, but it may not attach to a saved report.";
  emptyState.hidden = true;
  scanProgress.hidden = true;
  results.hidden = false;
}

function scrollToResults() {
  const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  requestAnimationFrame(() => {
    results.scrollIntoView({
      behavior: reduceMotion ? "auto" : "smooth",
      block: "start",
    });
  });
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

async function createBackendReport(resumeText, jobDescription, experienceLevel) {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), backendTimeoutMs);

  const response = await fetch(`${apiBase}/api/reports`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    signal: controller.signal,
    body: JSON.stringify({
      resumeText,
      jobDescription,
      experienceLevel,
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
  const resumeText = resumeInput.value;
  const jobText = jobInput.value;
  const experienceLevel = experienceInput.value;
  if (!apiBase) {
    return analyzeLocally(resumeText, jobText, experienceLevel);
  }
  try {
    return await createBackendReport(resumeText, jobText, experienceLevel);
  } catch (error) {
    if (error.name === "AbortError" || error.status >= 500 || error instanceof TypeError) {
      const report = analyzeLocally(resumeText, jobText, experienceLevel);
      report.saved = false;
      report.id = null;
      report.notice =
        "The backend is waking up, so this free preview was generated in your browser. We will keep retrying in the background and update your score automatically once the full engine responds.";
      scheduleBackendUpgrade(scanGeneration, resumeText, jobText, experienceLevel);
      return report;
    }
    throw error;
  }
}

function cancelBackendUpgrade() {
  upgradeTimers.forEach(clearTimeout);
  upgradeTimers = [];
}

function scheduleBackendUpgrade(generation, resumeText, jobText, experienceLevel) {
  cancelBackendUpgrade();
  UPGRADE_DELAYS_MS.forEach((delay) => {
    upgradeTimers.push(setTimeout(() => {
      attemptBackendUpgrade(generation, resumeText, jobText, experienceLevel);
    }, delay));
  });
}

async function attemptBackendUpgrade(generation, resumeText, jobText, experienceLevel) {
  if (!apiBase || generation !== scanGeneration || latestReportSaved || upgradeInFlight) {
    return;
  }
  upgradeInFlight = true;
  try {
    // Cheap health probe first: it warms the dyno and avoids firing the heavy scan
    // endpoint while the backend is still asleep.
    const healthController = new AbortController();
    const healthTimer = setTimeout(() => healthController.abort(), 5000);
    const health = await fetch(`${apiBase}/api/health`, { signal: healthController.signal })
      .finally(() => clearTimeout(healthTimer));
    if (!health.ok) {
      return;
    }

    const report = await createBackendReport(resumeText, jobText, experienceLevel);
    if (generation !== scanGeneration) {
      return; // user started a new scan or cleared while we were retrying
    }
    cancelBackendUpgrade();
    renderResults(report);
    feedbackStatus.textContent =
      "Backend is awake. Your score was recalculated with the full engine and the report is now saved.";
    trackEvent("fallback_upgraded", {
      publicId: latestReport?.id || null,
      score: latestReport?.score ?? null,
    });
  } catch (error) {
    // Stay on the browser preview; a later scheduled attempt may still succeed.
  } finally {
    upgradeInFlight = false;
  }
}

function setLoading(isLoading) {
  analyzeButton.disabled = isLoading;
  analyzeButton.textContent = isLoading ? "Analyzing your Java resume..." : defaultAnalyzeLabel;
}

function resetScanState() {
  scanGeneration += 1;
  cancelBackendUpgrade();
  latestReport = null;
  latestReportSaved = false;
  formError.textContent = "";
  feedbackStatus.textContent = "";
  leadStatus.textContent = "";
  usefulnessStatus.textContent = "";
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

async function extractResumeText(file) {
  if (!file) return;
  const allowedExtensions = [".pdf", ".doc", ".docx", ".txt"];
  const lowerName = file.name.toLowerCase();
  const isAllowed = allowedExtensions.some((extension) => lowerName.endsWith(extension));
  const maxBytes = 5 * 1024 * 1024;

  if (!isAllowed) {
    uploadStatus.textContent = "Could not read this file. Please paste your resume text instead.";
    resumeFileInput.value = "";
    return;
  }

  if (file.size > maxBytes) {
    uploadStatus.textContent = "This file is too large for beta upload. Please paste your resume text instead.";
    resumeFileInput.value = "";
    return;
  }

  if (window.location.protocol === "file:") {
    uploadStatus.textContent = "Upload works from the local server URL. Open http://127.0.0.1:4173/ or paste your resume text instead.";
    resumeFileInput.value = "";
    return;
  }

  if (!apiBase) {
    uploadStatus.textContent = "PDF/DOCX upload needs the backend. Paste text for beta.";
    resumeFileInput.value = "";
    return;
  }

  warmBackend();
  uploadStatus.textContent = "Reading your resume file through JavaJobFit API...";

  // The backend is on a free tier that sleeps and can take ~100s to cold-start, during which
  // it may return fast 5xx/network errors. Keep retrying long enough to outlast a full cold
  // boot (up to ~2 min of waiting), with a live "waking up" status so it self-heals instead
  // of failing on the first attempt.
  const maxAttempts = 6;
  const retryDelaysMs = [5000, 10000, 20000, 30000, 30000]; // waits between attempts (~95s total)
  for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), uploadTimeoutMs);
    const retryWithStatus = async () => {
      const waitMs = retryDelaysMs[attempt - 1] || 30000;
      uploadStatus.textContent = `Server is waking up (this can take up to a minute). Retrying automatically... (${attempt}/${maxAttempts})`;
      await new Promise((resolve) => setTimeout(resolve, waitMs));
    };
    try {
      const formData = new FormData();
      formData.append("file", file);
      const response = await fetch(`${apiBase}/api/resume/extract`, {
        method: "POST",
        body: formData,
        signal: controller.signal,
      }).finally(() => clearTimeout(timeoutId));

      if (response.status === 404) {
        uploadStatus.textContent = "Resume upload is temporarily unavailable. Please paste your resume text instead.";
        trackEvent("scan_failed", { reason: "resume_extract_endpoint_missing" });
        resumeFileInput.value = "";
        return;
      }

      const payload = await response.json().catch(() => ({}));

      // 5xx during a cold start / spin-up is transient — keep retrying until the boot finishes.
      if (response.status >= 500 && attempt < maxAttempts) {
        await retryWithStatus();
        continue;
      }

      if (!response.ok || !payload.text) {
        throw new Error(payload.error || "Could not read this file.");
      }

      resumeInput.value = payload.text;
      resumeError.textContent = "";
      resumePasteTracked = true;
      uploadStatus.textContent = "Resume text extracted. Please review it before analyzing.";
      trackEvent("resume_uploaded", {
        label: lowerName.endsWith(".txt") ? "txt" : "document",
        reason: "text_extracted",
      });
      resumeFileInput.value = "";
      return;
    } catch (error) {
      const wokeUpRetryable = error.name === "AbortError" || error instanceof TypeError;
      if (wokeUpRetryable && attempt < maxAttempts) {
        await retryWithStatus();
        continue;
      }
      console.warn("Resume extraction failed", error);
      uploadStatus.textContent =
        "Could not read this file yet — the server may be waking up. Wait a minute and try again, or paste your resume text.";
      trackEvent("scan_failed", { reason: "resume_extract_failed" });
      resumeFileInput.value = "";
      return;
    }
  }
}

async function submitUsefulnessFeedback(answer) {
  usefulnessStatus.textContent = "";

  if (!apiBase) {
    usefulnessStatus.textContent = "Thanks. Your answer is noted for this session.";
    trackEvent("feedback_submitted", { label: `usefulness_${answer}`, localOnly: true });
    return;
  }

  [usefulYes, usefulNo].forEach((button) => {
    button.disabled = true;
  });

  try {
    const response = await fetch(`${apiBase}/api/outcomes`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        reportPublicId: latestReport?.id || null,
        outcomeType: answer === "yes" ? "useful" : "not_useful",
        usefulnessRating: answer === "yes" ? 5 : 2,
        message: `Usefulness prompt: ${answer}. Score: ${latestReport?.score ?? "unknown"}.`,
      }),
    });

    if (!response.ok) throw new Error(`Usefulness feedback failed (${response.status})`);
    usefulnessStatus.textContent = "Thanks. This helps us improve the beta.";
    trackEvent("feedback_submitted", { publicId: latestReport?.id || null, label: `usefulness_${answer}` });
  } catch (error) {
    console.warn("Usefulness feedback failed", error);
    usefulnessStatus.textContent = "Could not save that answer. You can still use the feedback box below.";
  } finally {
    [usefulYes, usefulNo].forEach((button) => {
      button.disabled = false;
    });
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

  scanGeneration += 1;
  cancelBackendUpgrade();
  setLoading(true);
  startScanProgress();
  try {
    const report = await buildReport();
    await finishScanProgress();
    renderResults(report);
    scrollToResults();
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
  if (uploadStatus) {
    uploadStatus.textContent = "Paste text remains the fastest fallback. Uploaded files are only used to extract text and are not stored.";
  }
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
  if (uploadStatus) {
    uploadStatus.textContent = "Paste text remains the fastest fallback. Uploaded files are only used to extract text and are not stored.";
  }
  resetScanState();
});

resumeFileInput?.addEventListener("change", () => {
  extractResumeText(resumeFileInput.files?.[0]);
});

// Wake the backend as soon as the user shows intent, so it is warm by upload/scan time.
resumeFileInput?.addEventListener("focus", warmBackend);
resumeInput.addEventListener("focus", warmBackend);
jobInput.addEventListener("focus", warmBackend);

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
  trackEvent("export_report_clicked", { publicId: latestReport?.id || null, freePreview: true });
  window.print();
});

leadForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  leadStatus.textContent = "";

  if (!leadEmail.value.trim()) {
    leadStatus.textContent = "Please enter your email.";
    return;
  }

  if (!leadConsent.checked) {
    leadStatus.textContent = "Please agree to receive JavaJobFit product updates before saving your email.";
    return;
  }

  if (!apiBase) {
    leadStatus.textContent = "Saved locally for this session. Backend lead capture is not configured.";
    trackEvent("email_submitted", { localOnly: true });
    return;
  }

  const originalLabel = leadSubmitButton?.textContent || "";
  if (leadSubmitButton) {
    leadSubmitButton.disabled = true;
    leadSubmitButton.textContent = "Saving...";
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
    trackEvent("email_submitted", { publicId: latestReport?.id || null, country: leadCountry.value });
  } catch (error) {
    console.warn("Lead capture failed", error);
    leadStatus.textContent = "Email could not be saved. Please try again later.";
  } finally {
    if (leadSubmitButton) {
      leadSubmitButton.disabled = false;
      leadSubmitButton.textContent = originalLabel;
    }
  }
});

feedbackForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  feedbackStatus.textContent = "";

  if (!apiBase) {
    feedbackStatus.textContent = "Backend is not configured yet. Feedback will work after API deployment.";
    return;
  }

  const originalLabel = feedbackSubmitButton?.textContent || "";
  if (feedbackSubmitButton) {
    feedbackSubmitButton.disabled = true;
    feedbackSubmitButton.textContent = "Sending...";
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
  } finally {
    if (feedbackSubmitButton) {
      feedbackSubmitButton.disabled = false;
      feedbackSubmitButton.textContent = originalLabel;
    }
  }
});

usefulYes.addEventListener("click", () => {
  submitUsefulnessFeedback("yes");
});

usefulNo.addEventListener("click", () => {
  submitUsefulnessFeedback("no");
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

trackEvent("page_view", { experienceLevel: experienceInput.value });

const pricingSection = document.querySelector(".pricing-section");
if (pricingSection && "IntersectionObserver" in window) {
  const pricingObserver = new IntersectionObserver((entries) => {
    if (entries.some((entry) => entry.isIntersecting)) {
      trackEvent("pricing_viewed");
      pricingObserver.disconnect();
    }
  }, { threshold: 0.35 });
  pricingObserver.observe(pricingSection);
}

// Warm the free-tier backend on initial load so the first scan/upload avoids a cold start.
warmBackend();

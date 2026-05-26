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

const skillBank = [
  { label: "Java", terms: ["java", "core java", "jdk"] },
  { label: "Spring Boot", terms: ["spring boot", "springboot"] },
  { label: "Spring MVC", terms: ["spring mvc", "spring web"] },
  { label: "Spring Security", terms: ["spring security", "oauth", "jwt"] },
  { label: "REST APIs", terms: ["rest", "rest api", "restful"] },
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
  fresher: "entry-level Java developer",
  oneToThree: "junior Java developer",
  threeToFive: "Java backend engineer",
  fiveToEight: "senior Java backend engineer",
  senior: "senior Java technical leader",
  threeToSix: "mid-level Java backend engineer",
  sixPlus: "senior Java backend engineer",
};

const genericStopWords = new Set([
  "and", "the", "for", "with", "you", "are", "will", "this", "that", "from", "have",
  "has", "our", "your", "job", "role", "work", "team", "experience", "candidate",
  "developer", "engineer", "software", "good", "strong", "using", "build",
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

function analyzeLocally(resumeText, jobText, experience) {
  const resume = normalize(resumeText);
  const job = normalize(jobText);
  const relevantSkills = skillBank.filter((skill) => hasAny(job, skill.terms));
  const matched = relevantSkills.filter((skill) => hasAny(resume, skill.terms)).map((skill) => skill.label);
  const missing = relevantSkills.filter((skill) => !hasAny(resume, skill.terms)).map((skill) => skill.label);
  const jobKeywords = extractKeywords(jobText);
  const keywordMatches = jobKeywords.filter((keyword) => resume.includes(keyword));
  const skillScore = relevantSkills.length ? Math.round((matched.length / relevantSkills.length) * 70) : 35;
  const keywordScore = jobKeywords.length ? Math.round((keywordMatches.length / jobKeywords.length) * 30) : 15;
  const score = Math.max(18, Math.min(96, skillScore + keywordScore));
  const missingKeywords = [...missing, ...jobKeywords.filter((keyword) => !resume.includes(keyword))].slice(0, 8);
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
    scoreSummary: buildScoreSummary(score, matched, missingKeywords),
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

function buildScoreSummary(score, matched, missingKeywords) {
  if (score >= 80) return "Your resume is a strong fit for this Java role. Polish the remaining keyword gaps before applying.";
  if (score >= 60) {
    return `Your resume matches this Java role, but you are missing ${missingKeywords.length} important keywords. Fix the top gaps before applying.`;
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
    id: report.reportId || report.id || null,
    saved: Boolean(report.reportId || report.id),
    score,
    scoreSummary: report.scoreSummary || buildScoreSummary(score, matched, missing),
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
  trackEvent("premium_cta_clicked", { reportId: latestReport?.id || null });
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
    trackEvent("scan_completed", { reportId: latestReport?.id || null, score: latestReport.score });
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
  trackEvent("pdf_export_clicked", { reportId: latestReport?.id || null, freePreview: true });
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
        reportId: latestReport?.id || null,
        consent: leadConsent.checked,
        source: "scan_result",
      }),
    });

    if (!response.ok) throw new Error(`Lead request failed (${response.status})`);
    leadStatus.textContent = "Saved. We'll notify you when full report unlock is available.";
    trackEvent("email_submitted", { reportId: latestReport?.id || null });
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
        reportId: latestReport?.id || null,
        email: feedbackEmail.value,
        message: feedbackMessage.value,
      }),
    });

    if (!response.ok) throw new Error(`Feedback request failed (${response.status})`);
    feedbackMessage.value = "";
    feedbackStatus.textContent = "Thanks. Feedback saved.";
    trackEvent("feedback_submitted", { reportId: latestReport?.id || null });
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

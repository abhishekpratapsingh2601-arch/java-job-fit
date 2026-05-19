# Project Isolation Rule

JavaJobFit must stay separate from every existing project, GitLab account,
GitHub account, Maven project, dependency cache, and local repository on this
machine.

## Rules

- Work only inside this repository folder.
- Do not edit global git config.
- Do not connect this repo to any GitLab remote.
- Do not modify other local repositories.
- Keep Maven dependencies inside `backend/.m2`.
- Keep backend build output inside `backend/target`.
- Keep local backend database files inside `backend/data`.
- Use only JavaJobFit-prefixed Docker resources.
- Do not run Docker builds, containers, or compose commands without explicit approval.
- Do not store secrets, tokens, or credentials in committed files.

## Maven Isolation

The backend uses:

```text
backend/.mvn/maven.config
backend/.mvn/settings.xml
```

with:

```text
-Dmaven.repo.local=.m2
```

So Maven commands run from `backend/` use:

```text
/Users/abhsingh/Documents/Codex/2026-05-19/hello-i-am-a-java-sde/backend/.m2
```

instead of the shared:

```text
/Users/abhsingh/.m2
```

The project-local `settings.xml` also avoids inherited machine-level Maven
mirrors so this project does not depend on another company's or another
project's Maven setup.

## Docker Isolation

Docker uses a system-level daemon, so image layers and some metadata are shared
by Docker itself. To keep JavaJobFit separate in practice:

- Use `docker-compose.yml` from this repository only.
- Use the Compose project name `javajobfit`.
- Use JavaJobFit-specific containers:

```text
javajobfit-backend
javajobfit-postgres
```

- Use JavaJobFit-specific volume:

```text
javajobfit-postgres-data
```

- Do not use anonymous volumes for this project.
- Do not prune Docker images, containers, networks, or volumes from this laptop
  unless explicitly approved, because pruning can affect other projects.
- Prefer local Maven commands for backend development unless Docker is needed
  for deployment parity.

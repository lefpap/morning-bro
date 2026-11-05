# AGENTS.md

Guidance for AI coding agents working on Morning Bro.

## Project

A run-once Spring Boot app (Java 27, Maven wrapper) that posts a daily "good morning" and per-location weather summary to Discord, run on GitHub Actions and triggered daily by a cron-job.org job.

- Build and test: `./mvnw test`
- Local dry run (logs the message, posts nothing): `MORNING_BRO_DRY_RUN=true ./mvnw spring-boot:run`

## Docs

Read the relevant doc before changing code, and update it when behavior changes.

- [README.md](README.md): setup, configuration, running and operating it.
- [docs/product.md](docs/product.md): what it does and the message rules.
- [docs/architecture.md](docs/architecture.md): packages, failure handling, scheduling, testing and design decisions.
- [docs/operations.md](docs/operations.md): the daily cron-job.org trigger, its GitHub token, and day-to-day `gh` commands.

## Git and GitHub

- Use the `gh` CLI for all GitHub operations: repos, branches on the remote, pull requests, workflow runs, and secrets.
- Commit only when the user asks or a plan step says to.

### Commit messages

Format: `<type>(#NN): <summary>`

- `type` is one of: `feat`, `fix`, `ref`, `docs`, `chore`.
- `#NN` is the two-digit number of the plan task the commit belongs to, e.g. `feat(#01): validated morning-bro configuration`.
- Commits that don't belong to a plan task drop the scope: `chore: bump Spring Boot`.
- Summary: imperative or descriptive, lowercase start, no trailing period.

### Pull requests

- Open PRs with `gh pr create`.
- Draft the PR description with the `/pr` skill, and don't write it freehand.

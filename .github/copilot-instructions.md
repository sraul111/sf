# Copilot instructions — ServiceForge

Read `AGENTS.md` at the repo root first — it is the tool-agnostic entry point every tool in this repo shares. This file only adds Copilot-specific pointers.

## The pipeline

Any "develop this feature: `<intent>`" request must go through the three-step pipeline in `pipeline/orchestration.md` (BA → Developer → Tester). Do not jump straight to writing code for a new feature.

Any "implement the fix described in `<rule-file>`" request (a rule file under `pipeline/*/rules/*.md`) routes instead to the standalone `bug-fix-agent`, defined in `pipeline/orchestration.md`'s "separate entry point" section — it writes its own code fix and its own tests and does not call the Developer or Tester agents.

Any "design the architecture for `<source-file>`" or equivalent ADR/planning request routes to `adr-agent` and then `adr-review-agent`. The source file may have any filename; identify it by the path supplied by the user. Stop after the review for human approval; do not proceed directly into implementation.

## Role definitions

Canonical role definitions live under `.claude/agents/*.md`. GitHub Copilot workspace agents live under `.github/agents/`; the ADR wrappers delegate to the canonical role files so their behavior does not drift.

## Skills

Load `.claude/skills/build-code-skill/SKILL.md` before writing backend or frontend code — it has the package layout, DI style, REST conventions, and Angular conventions this repo expects. `.claude/skills/spec-generation-skill/SKILL.md` and `.claude/skills/migration-safety-skill/SKILL.md` apply to the BA role and to data-model changes respectively.

## Conventions to always respect

- No real database — mock/in-memory data only (`MockDataStore` on the backend).
- Constructor injection only on the backend; no HTTP calls inside Angular components, always through a service.
- Every feature has exactly one spec file under `pipeline/features/`, never merged with another feature's spec.

# The "develop this feature" pipeline

This is the single, tool-agnostic description of how any feature in this repo gets built. It exists so the same workflow can be followed whether you're driving it from Claude Code, GitHub Copilot, Codex, or reading it yourself. Every tool-specific file in this repo (`CLAUDE.md`, `.github/copilot-instructions.md`, `AGENTS.md`) points here.

## The trigger

Anyone — human or agent — can kick this off with a single instruction:

> **"Develop this feature: `<intent>`"**

That one line is the only input. Everything else below is how it gets turned into a shipped feature.

## Run reports

Every recognized pipeline execution produces a run report under `pipeline/runs/`. The orchestrator creates or updates this report automatically after delegated work completes, or when the pipeline stops at a human checkpoint or safe-recovery condition. Agents do not require the user to write the report manually.

Use a descriptive Markdown filename such as `YYYY-MM-DD-feature-<slug>.md`, `YYYY-MM-DD-adr-<slug>.md`, or `YYYY-MM-DD-bug-fix-<slug>.md`. Each report records:

- pipeline type, trigger, date, and final status (`PASS`, `FAIL`, or `BLOCKED`)
- subagents run and their execution order
- files changed or produced
- commands run and their results, when applicable
- review verdicts, assumptions, blockers, and required human decisions

This convention applies to feature development, architecture and ADR review, and diagnosed bug-fix pipelines. It does not apply to general questions, read-only exploration, or prompts that do not clearly trigger a named pipeline.

### Memory operation reporting

When a recognized pipeline performs a memory read or write, record the operation in the same run report under a `Memory Operations` section. Do not create a separate report for each operation. For each operation, record:

- operation (`read` or `write`), result status, and timestamp
- the exact namespace used, without recording memory contents
- for reads, the number of validated records returned and the approximate token count; note when FIFO trimming removed records
- for writes, the source task, type tag, and whether a same-namespace read-back verification succeeded
- rejected writes, failed reads, exit codes, and the redacted error category

Run reports must not contain secrets, PII, raw prompts, raw logs, access tokens, or unredacted telemetry. Memory content may be summarized only as a redacted outcome; the report should never reproduce the payload.

## The three steps

### 1 — BA (turns intent into a spec)

- **Role definition:** `.claude/agents/ba-agent.md`
- **Skill it loads:** `.claude/skills/spec-generation-skill/SKILL.md`
- **What it does:** takes the raw intent, asks what it needs to (scope, dependencies on prior features, definition of done), and commits a new file at `pipeline/features/feature-N-<slug>.md` following the same template as `pipeline/features/feature-1-technician-availability.md`.
- **What it must check first:** every existing file under `pipeline/features/` and `pipeline/decisions/`, so the new spec correctly names what it depends on instead of guessing.
- **Output:** one committed spec file. Nothing gets built yet.

### 2 — Developer (turns the spec into code)

- **Role definition:** `.claude/agents/developer-agent.md`
- **Skills it loads:** `.claude/skills/build-code-skill/SKILL.md` (stack conventions) and, if the change touches the data model, `.claude/skills/migration-safety-skill/SKILL.md`.
- **Rules it must follow:** everything under `pipeline/**/rules/*.md` that exists at the time, including rules located in feature-specific directories.
- **What it does:** implements the spec from step 1, in the backend and/or frontend as the spec requires, without expanding scope beyond what the spec says.
- **Output:** working code, committed.

### 3 — Tester (checks the code against the spec)

- **Role definition:** `.claude/agents/tester-agent.md`
- **What it does:** writes and runs tests against the Definition of Done in the spec from step 1 — not against what the code happens to do, but against what it was supposed to do.
- **Output:** test results. If they fail, the loop returns to step 2, not step 1 — the spec doesn't change because the code didn't meet it.

## Architecture and planning pipeline

Architecture work is a separate, reusable path between an approved requirements artifact and implementation. Trigger it with:

> **"Design the architecture for `<source-file>`"**

The source file contains the requirements but may have any filename; routing uses the path supplied by the caller, not a filename pattern. Equivalent requests such as "create an ADR" or "create an implementation plan" follow the same route when they name a source file.

### 1 — ADR author (turns requirements into a design)

- **Role definition:** `.claude/agents/adr-agent.md`
- **What it does:** discovers the repository's constraints, evaluates credible options, and writes a traceable architecture decision plus a dependency-ordered implementation plan.
- **Output:** `architecture-adr.md` and `implementation-plan.md` beside the source file.

### 2 — ADR reviewer (challenges the design for ServiceForge)

- **Role definition:** `.claude/agents/adr-review-agent.md`
- **What it does:** checks the proposed design against ServiceForge's Java 17, Spring Boot 3, Angular, in-memory data model, active rules, prior decisions, and current implementation boundaries.
- **Output:** `adr-review.md` beside the ADR, with an `Approved` or `Changes required` verdict and evidence-backed findings.

The orchestrator must run these agents in order: `[requirements] -> adr-agent -> adr-review-agent -> [human approval]`. It must stop after review for a human decision and must not silently proceed into implementation. The generic ADR author is reusable; ServiceForge-specific constraints belong in the review agent.

## A separate entry point — fixing an already-diagnosed bug

Not every change starts from feature development or architecture design. When a bug has already been diagnosed and committed as a rule file under `pipeline/*/rules/*.md` (constraint, root cause, verification criteria — see `pipeline/feature2/rules/no-overlap-booking.md` for the shape), the trigger is instead:

> **"Implement the fix described in `<rule-file-path>`"**

- **Role definition:** `.claude/agents/bug-fix-agent.md`
- **Skill it loads:** `.claude/skills/bug-fix-skill/SKILL.md` (and `build-code-skill` / `migration-safety-skill` as needed)
- **What it does:** this agent is self-contained — it writes the code fix *and* writes and runs its own tests against the rule's Verification criteria. It does not call, and is not called by, the BA/Developer/Tester pipeline above.
- **Output:** a verified code fix, its own tests, and an updated "Artifacts this rule touches" section on the rule file.

The orchestrator also records the pipeline execution in the applicable `pipeline/runs/` report.

## Safe recovery

If any step produces something that doesn't match its input (a spec with no Definition of Done, code that doesn't match the spec, tests that can't run), stop and surface that to a human rather than proceeding on a guess. This pipeline has no silent fallback.

## Human checkpoints

A human should look at the output of step 1 (the spec) before step 2 starts — specs are cheap to correct, code is not. Everything else can run unattended unless something trips the safe-recovery condition above.

## Running this in each tool

- **Claude Code:** the three role files under `.claude/agents/` are real Claude Code subagents (they have the frontmatter Claude Code expects). Say "develop this feature: `<intent>`" and Claude Code should route through them in order, per this document.
- **GitHub Copilot:** invoke the workspace agents under `.github/agents/`. The orchestrator routes feature, architecture, and bug-fix requests to the corresponding agents defined above.
- **Codex / any `AGENTS.md`-reading CLI agent:** `AGENTS.md` already points here. Give the same "develop this feature" instruction; the agent should read the three role files under `.claude/agents/` as plain markdown (the YAML header is harmless to ignore) and follow the same three steps.
- **Anyone without an agent tool:** the three role files are readable specs. Do the three steps yourself, in order.

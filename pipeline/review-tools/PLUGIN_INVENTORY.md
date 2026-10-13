# Plugin Inventory — ServiceForge Review Tools (`pipeline/review-tools/`)

## Overview

The `review-tools` plugin provides an end-to-end, reusable pre-merge review and pull request creation toolset for autonomous agents and developer workflows. It encapsulates:
1. Automated draft Pull Request generation with standardized change summaries and review findings.
2. A strictly read-only Pre-Merge Review Agent inspecting remote diffs via GitHub MCP.
3. The canonical 5-area Pre-Merge Code Review Checklist rule and reusable agent instructions.

---

## Directory Structure

```
pipeline/review-tools/
├── PLUGIN_INVENTORY.md                             # Complete manifest and component inventory (this file)
├── agents/
│   ├── review-agent.md                             # Canonical Pre-Merge Review Agent definition (Claude Code format)
│   └── review-agent.agent.md                       # GitHub Copilot workspace agent definition
├── instructions/
│   └── review-checklist.instructions.md           # Workspace-level instruction enforcing pre-merge reviews
├── prompts/
│   └── review-agent.prompt.md                      # Reusable review prompt wrapper for Copilot
├── rules/
│   └── review-checklist-rule.md                    # Canonical 5-area pre-merge review checklist rule
└── skills/
    └── create-pr/
        └── SKILL.md                                # Reusable Create Draft Pull Request skill
```

---

## Component Inventory

| Component Name | Type | Relative Path | Purpose & Capabilities | Tools Used |
|---|---|---|---|---|
| `create-pr` | Skill | `skills/create-pr/SKILL.md` | Guides agent to confirm source/target branches, summarize changes and review findings, and invoke `create_pull_request` with `draft: true`. | `create_pull_request`, `gh pr create` |
| `review-agent` (Canonical) | Agent | `agents/review-agent.md` | Inspects remote pull request diff via GitHub MCP; evaluates code against 5 checklist areas; submits pending reviews with inline line comments. Strictly read-only on code. | `Read`, `Grep`, `Glob`, `mcp__github__pull_request_read`, `mcp__github__get_file_contents`, `mcp__github__create_pull_request_review` |
| `review-agent.agent.md` | Agent Wrapper | `agents/review-agent.agent.md` | GitHub Copilot agent interface mapping to the canonical `review-agent.md`. | `read`, `search` |
| `review-agent.prompt.md` | Prompt | `prompts/review-agent.prompt.md` | GitHub Copilot prompt wrapper executing pre-merge reviews against remote diffs. | `read`, `search` |
| `review-checklist-rule` | Rule | `rules/review-checklist-rule.md` | Canonical 5-area checklist rule: Acceptance Criteria, Quality, OWASP Top 10 Security, Rule Compliance, and Design Alignment & Risk Gates. | N/A (Standard Markdown Rule) |
| `review-checklist.instructions` | Instruction | `instructions/review-checklist.instructions.md` | Workspace instruction enforcing that `review-agent` completes the checklist before shipping code. | N/A (Instruction Wrapper) |

---

## Component Details

### 1. `create-pr` Skill
- **Path**: `skills/create-pr/SKILL.md`
- **Description**: Instructs the agent to create a draft PR on GitHub. It enforces:
  - Branch confirmation (`head` and `base`).
  - Structured PR body with Jira linkage, change summary, review checklist outcome, and release gates.
  - Setting `"draft": true`.
  - Post-creation verification.

### 2. `review-agent` (Canonical Definition)
- **Path**: `agents/review-agent.md`
- **Description**: Defines the independent code review agent.
  - Enforces loading `review-checklist-rule.md` explicitly by path.
  - Uses GitHub MCP to retrieve remote PR metadata (`get`), changed files (`get_files`), and unified diff (`get_diff`) instead of reviewing local working tree files.
  - Disallows `Edit`, `Write`, `MultiEdit`, `NotebookEdit`, `Push` (strictly read-only on repository source code).
  - Submits pending reviews (`PENDING` state) with line-anchored comments on GitHub.

### 3. Copilot Wrappers (`review-agent.agent.md` & `review-agent.prompt.md`)
- **Paths**: `agents/review-agent.agent.md`, `prompts/review-agent.prompt.md`
- **Description**: Standard interface definitions enabling Copilot to invoke the canonical review agent.

### 4. Canonical Pre-Merge Checklist Rule (`review-checklist-rule.md`)
- **Path**: `rules/review-checklist-rule.md`
- **Description**: Contains the complete 5-area review specification:
  1. **Acceptance Criteria Verification**: Criterion-to-code mapping, end-to-end behavior, edge cases & rejection behavior.
  2. **Quality Audit**: Correctness, maintainability, validation/test quality, operational & documentation impact.
  3. **OWASP Top 10 Security Review**: Ten-category mapping, security controls & abuse paths, runtime claim verification.
  4. **Compliance With Committed Rules**: Workspace rule inventory, safety guardrails, domain constraints.
  5. **Design Alignment and Risk Mitigation**: Architecture/ADR alignment, risk assessment mitigations, release gates & human sign-offs.

---

## Verification Audit

A strict manual audit of the plugin directory `pipeline/review-tools/` was performed to verify safety, portability, and hygiene:

- [x] **No Application Source Code**: The plugin directory contains **zero** Java (`.java`), TypeScript (`.ts`), JavaScript (`.js`), Python application code (`.py`), or HTML/CSS files.
- [x] **No Feature Evidence**: The plugin directory contains **zero** test output logs (`.log`), test run outputs, execution reports, test databases, or telemetry JSON records.
- [x] **No `.env` Files**: The plugin directory contains **no** `.env`, `.env.*`, or local environment configuration files.
- [x] **No Secrets or Credentials**: The plugin directory contains **zero** GitHub Personal Access Tokens (PATs), API keys, private keys, passwords, or authentication hashes. All references to tokens are abstract descriptions of parameters.
- [x] **Pure Reusable Tooling**: All files are markdown (`.md`) specification, rule, skill, and instruction files designed to be imported into any repository or pipeline.

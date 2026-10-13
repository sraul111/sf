---
name: create-pr
description: Creates a draft pull request on GitHub for Feature 2 changes and C2 review outcome by explicitly invoking the create_pull_request tool with draft=true.
---

# Create Pull Request Skill — Feature 2 and C2 Review

## Purpose

This skill guides the agent to create a draft Pull Request on GitHub for Feature 2 (Parts Reservation and MCP Server Adapter), incorporating the implementation summary and C2 pre-merge code review outcome.

## Instructions for the Agent

When executing this skill, the agent **MUST** perform the following steps in order:

### 1. Confirm Source and Target Branches

Before opening the pull request, confirm the source (head) and target (base) branches:
- **Source branch (`head`)**: Identify the current working branch containing the Feature 2 commits (e.g., `c3`). Verify using `git branch --show-current` or `git status`. Ensure all changes are committed and pushed to the remote (`origin/<source-branch>`).
- **Target branch (`base`)**: Confirm the target branch into which changes will be merged (typically `main`).
- Confirm the remote repository details: Owner is `sraul111`, repository is `sf` (derived from git remote `origin`).

### 2. Summarize Feature 2 Changes

Compose a comprehensive summary of Feature 2 changes to include in the PR body:
- **In-Memory Parts Catalog (`MockDataStore`)**: Seeded repository with exactly 4 parts identified by SKU: `CMP-CLUTCH` (3), `FLT-16` (10), `BRK-20A` (5), and `VLV-BALL-075` (3).
- **Domain Models & DTOs**: Added `Part` model (`id`, `sku`, `name`, `totalQuantity`) and reservation DTO structures.
- **REST Endpoints (`PartController`, `PartService`)**:
  - `GET /api/parts` — list all parts with stock details.
  - `GET /api/parts/{sku}` — fetch part by SKU.
  - `POST /api/parts/{sku}/reservations` — reserve part quantity against a job.
- **Python MCP Server Adapter (`pipeline/feature2/mcp_server/server.py`)**:
  - Line-delimited JSON stdio protocol adapter communicating with the Spring Boot backend.
  - Strict input validation for SKUs and quantities (`test_server.py`).
- **Test Suite & Verification**: Added `Feature2PartsReservationTest`, `MockDataStoreFeature3Test`, and MCP unit tests validating baseline behavior.
- **Documentation & Packaging**: Added `CHANGELOG.md` (`v0.1.0`), `onboarding.md`, `pull-request-template.md`, and `test-plan.md`.

### 3. Summarize C2 Review Outcome

Incorporate the C2 pre-merge review findings from `review-agent` against `.claude/rules/review-checklist-rule.md`:
- **1. Acceptance Criteria & Quality Audit**: Baseline `v0.1.0` satisfies core seed data and basic reservation endpoints. Full windowed reservations (45-min hold), returns REST API, and frontend parts grid remain documented target behavior for future iterations.
- **2. OWASP Top 10 Security Review**: Assessed against OWASP Top 10:2025. No SQL/command injection (in-memory mock store), no sensitive secrets committed, strict input validation enforced on MCP server arguments.
- **3. Committed Rule Compliance**: Fully compliant with `pipeline/feature2/rules/job-notes-injection-guardrail.md` and `no-overlap-booking.md`.
- **4. Risk Assessment & Release Gates**: Per `pipeline/feature2/assessments/risk-assessment-report.md`, automated writes or notes-enabled auto-approval remain **NO-GO for production** until authoritative server-side validation and audit logging are implemented.
- **5. Final Review Verdict**: **Ship as Draft Development Build (`v0.1.0`)**. Draft PR is required to track ongoing development without triggering premature production release.

### 4. Invoke the `create_pull_request` Tool

The agent must explicitly invoke the `create_pull_request` tool with the following parameters:

- **`owner`**: `"sraul111"`
- **`repo`**: `"sf"`
- **`title`**: `"Feature 2: Parts Reservation and MCP Server Adapter (v0.1.0)"`
- **`head`**: Confirmed source branch (e.g., `"c3"`)
- **`base`**: Confirmed target branch (e.g., `"main"`)
- **`draft`**: `true` (MUST be set to `true` to ensure the pull request is created as a draft)
- **`body`**: The PR description formatted with Markdown, including:
  - Jira Linkage (`Closes PROJ-123`)
  - Description: Summary of Feature 2 changes (backend, MCP server, tests, docs)
  - C2 Review Outcome: 5-area checklist results, security review, and risk assessment status
  - Release Gates: Explicit callout of the production NO-GO status for notes-enabled reservation

#### Tool Invocation Example

```json
{
  "owner": "sraul111",
  "repo": "sf",
  "title": "Feature 2: Parts Reservation and MCP Server Adapter (v0.1.0)",
  "head": "c3",
  "base": "main",
  "draft": true,
  "body": "## Jira Linkage\nCloses PROJ-123\n\n## Description\n### Feature 2 Changes\n- Seeded in-memory catalog with 4 parts (CMP-CLUTCH, FLT-16, BRK-20A, VLV-BALL-075)\n- Part model, PartService, and PartController endpoints (/api/parts, /api/parts/{sku}, /api/parts/{sku}/reservations)\n- Python stdio MCP server adapter with strict argument validation\n- Integration tests (Feature2PartsReservationTest, test_server.py)\n- Documentation: CHANGELOG.md (v0.1.0), onboarding.md, test-plan.md, pull-request-template.md\n\n## C2 Review Outcome\n- 5-Area Pre-Merge Checklist: Pass for v0.1.0 development milestone\n- OWASP Top 10:2025: No injection or credential exposure; strict MCP input validation\n- Guardrails: Job-notes injection guardrail and no-overlap booking verified\n- Risk Assessment: Production notes-enabled reservations and auto-approval remain NO-GO\n- Verdict: Approved for merge as Draft Development Build\n\n## Release Gates\nDraft PR opened with draft: true. Production release blocked pending completion of remaining target requirements."
}
```

*Note: In CLI environments where the GitHub MCP server tool is executed via command line, the equivalent command is:*
```bash
gh pr create --repo sraul111/sf --base main --head c3 --draft --title "Feature 2: Parts Reservation and MCP Server Adapter (v0.1.0)" --body "..."
```

### 5. Verify Pull Request Creation

After invoking the tool, verify that the pull request was successfully created on GitHub:
- Check the return output of the tool call or query `gh pr list --state all` / `gh pr view <number>` to verify the PR URL, number, and draft status.

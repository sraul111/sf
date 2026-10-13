---
name: create-pr
description: Creates a draft pull request on GitHub for proposed changes and pre-merge review outcomes by explicitly invoking the create_pull_request tool with draft=true.
---

# Create Pull Request Skill

## Purpose

This skill guides the agent to create a draft Pull Request on GitHub for proposed branch changes, incorporating the implementation summary and C2 pre-merge code review outcome.

## Instructions for the Agent

When executing this skill, the agent **MUST** perform the following steps in order:

### 1. Confirm Source and Target Branches

Before opening the pull request, confirm the source (head) and target (base) branches:
- **Source branch (`head`)**: Identify the current working branch containing the changes (e.g., `git branch --show-current` or `git status`). Ensure all changes are committed and pushed to the remote (`origin/<source-branch>`).
- **Target branch (`base`)**: Confirm the target branch into which changes will be merged (typically `main`).
- Confirm the remote repository details: Owner and repository name (derived from git remote `origin`).

### 2. Summarize Changes

Compose a comprehensive summary of proposed changes to include in the PR body:
- **Scope & Architecture**: Identify modified components, models, endpoints, services, tools, or configuration.
- **Key Modules & Endpoints**: Enumerate key endpoints, models, DTOs, or adapter tools added or modified.
- **Testing & Verification**: Enumerate unit tests, contract tests, and manual verification performed.
- **Documentation & Packaging**: Note updates to changelogs, onboarding guides, and schemas.

### 3. Summarize Pre-Merge Review Outcome

Incorporate the pre-merge review findings from `review-agent` evaluated against the 5-area review checklist:
- **1. Acceptance Criteria & Quality Audit**: Assess baseline requirements, contract conformity, and outstanding target behaviors.
- **2. OWASP Top 10 Security Review**: Document findings across OWASP Top 10 categories (injection prevention, secret handling, input validation, access controls).
- **3. Committed Rule Compliance**: Document verification against repository guardrails and rules.
- **4. Risk Assessment & Release Gates**: Detail production readiness, residual risks, and release conditions.
- **5. Final Review Verdict**: State whether the change is approved for merge as a draft development build or ready for production.

### 4. Invoke the `create_pull_request` Tool

The agent must explicitly invoke the `create_pull_request` tool (or CLI equivalent `gh pr create`) with the following parameters:

- **`owner`**: Remote repository owner (e.g., `"sraul111"`)
- **`repo`**: Remote repository name (e.g., `"sf"`)
- **`title`**: Descriptive PR title (e.g., `"<Feature Name>: <Short Description> (<version>)"`)
- **`head`**: Confirmed source branch
- **`base`**: Confirmed target branch
- **`draft`**: `true` (MUST be set to `true` to ensure the pull request is created as a draft)
- **`body`**: The PR description formatted with Markdown, including:
  - Jira / Issue Linkage
  - Description of changes
  - Pre-Merge Review Outcome (5-area checklist results, security review, and risk assessment status)
  - Release Gates and deployment conditions

### 5. Verify Pull Request Creation

After invoking the tool, verify that the pull request was successfully created on GitHub:
- Check the return output of the tool call or query `gh pr list --state all` / `gh pr view <number>` to verify the PR URL, number, and draft status.

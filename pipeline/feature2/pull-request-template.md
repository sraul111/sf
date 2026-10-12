# Pull Request: <concise change title>

> Complete every required section before requesting review. Replace all placeholders, attach evidence, and do not mark a gate complete without verification.

## Jira Linkage

**Simulated Jira issue:** Closes PROJ-123

Replace `PROJ-123` with the applicable Jira key. This repository example is simulated; verify the actual issue exists and is linked before merge.

## CODEOWNERS Callouts

Request review from the applicable owners. These are example team handles; confirm they match the repository's configured GitHub teams/CODEOWNERS before relying on them.

- [ ] `@platform-team` reviewed MCP server, backend/API, deployment, or platform configuration changes.
- [ ] `@security-team` reviewed security guardrails, authorization, trust boundaries, secrets, or security-sensitive behavior.
- [ ] Other required owner(s): <team or handle and scope, or N/A with reason>

## Description

### What changed

<Describe the implementation and user-visible/API behavior. Link key files or commits.>

### Why

<Problem solved and the applicable requirement, decision, or Jira context.>

### Testing performed

| Check | Command or procedure | Result / evidence |
|---|---|---|
| Focused tests | <command> | <pass/fail and link to run> |
| Full relevant test suite | <command> | <pass/fail and link to run> |
| Build, lint, or type-check | <command> | <pass/fail and link to run> |
| Integration / UI / manual verification | <procedure> | <observed result or N/A with reason> |

List failed, skipped, or unavailable checks and explain the impact. Do not describe an unrun check as passing.

### Dependencies and configuration

- Runtime/build dependencies or version changes: <list, or none>
- API/schema/configuration changes: <list, or none>
- Required secrets, environment variables, or infrastructure changes: <list, or none>
- Backward-compatibility or migration considerations: <details, or none>

### Risks and mitigations

- Risks introduced or changed: <list>
- Mitigations and monitoring: <details>
- Risk assessment attached/updated: <path and evidence>
- Open risks, decisions, or release blockers: <list; write `None` only when verified>

### Rollback plan

<Exact rollback steps, including how to disable/revert the change, restore compatible configuration/data if applicable, validate service health, and identify the rollback owner. For in-memory data loss or non-reversible changes, state the impact and recovery path.>

## Pre-Merge Checklist

- [ ] Required tests pass; CI evidence is linked, and all failures/skips are explained.
- [ ] Security review is complete for applicable changes; findings are resolved or explicitly dispositioned by the authorized reviewer.
- [ ] The current risk assessment is attached or linked, reviewed, and reflects this change; applicable release gates and human approvals are satisfied.
- [ ] Applicable CODEOWNERS and domain-owner approvals are recorded.
- [ ] API/configuration and documentation changes are reviewed for consistency.
- [ ] Rollback steps and operational owner are confirmed.

Do not merge with unchecked required items, unresolved blocking findings, missing risk evidence, or an applicable NO-GO decision.

## Deployment and Release

1. Deploy the reviewed change to staging using the documented release procedure.
2. Run staging smoke tests and the change-specific acceptance checks. Record the deployment revision, test evidence, and start time of the soak.
3. Keep the change in staging for a minimum **24-hour soak**. Monitor service health, relevant logs/metrics, error rates, and change-specific indicators; record the observation window and outcome here.
4. Promote to production only after the soak completes successfully, required approvals are recorded, and all security and risk-assessment release gates permit production. A 24-hour soak does not override a NO-GO risk decision.
5. If any gate fails or an adverse signal appears, stop promotion and execute the rollback plan.

**Staging deployment:** <environment, revision, operator, timestamp, evidence link>

**24-hour soak:** <start/end timestamps with timezone, monitoring evidence, outcome>

**Production release approval:** <approver(s), timestamp, evidence link; pending until all gates pass>

**Rollback owner and contact:** <name/team and contact path>

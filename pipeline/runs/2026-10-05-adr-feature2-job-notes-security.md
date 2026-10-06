# Feature 2 Job Notes Security Run

| Field | Value |
|---|---|
| **Pipeline** | Architecture and security assessment |
| **Trigger** | Generate proposed future-state security architecture and formal risk assessment for free-text Job notes read before parts reservation auto-approval |
| **Date** | 2026-10-05 |
| **Status** | PASS |

## Subagents

1. `adr-agent` authored the requested security design and risk assessment.
2. `adr-review-agent` reviewed both artifacts, identified factual and contract issues, and approved the final revisions.
3. `adr-agent` applied review corrections and clarified the final confirmation-contract wording; `adr-review-agent` confirmed the correction.

## Files Produced

- `pipeline/feature2/design/security-design.md`
- `pipeline/feature2/assessments/risk-assessment-report.md`
- `pipeline/runs/2026-10-05-adr-feature2-job-notes-security.md` (this report)

## Validation

- Document diagnostics: no errors reported for the two deliverables.
- Final architecture security review: **Approved**.
- Application tests: not run; this run changed documentation only.

## Findings and Decision

- The job-notes field and retrieval flow are proposed; neither is claimed to exist in the current model or agent flow.
- Review confirmed the current reservation endpoint accepts direct requests without visible session, confirmation, ownership, or job-status enforcement, and the service directly decrements stock without creating a job-linked reservation. The design treats backend enforcement and direct-call rejection/unchanged-state tests as release blockers.
- The current MCP documents use inconsistent confirmation field names. The security design proposes one canonical trusted, tuple-bound, expiring, replay-protected contract; implementation must reconcile the existing documents and enforce authorization server-side. Notes and model output cannot grant approval.
- The risk assessment decision is **NO-GO** for production notes-enabled reservation or auto-approval until the backend authorization and reservation invariants, prompt-injection guardrails, privacy/legal review, and required verification gates are complete.
- No code or existing requirements were modified. No memory operations were performed.
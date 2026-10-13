# Rule: Pre-Merge Code Review Checklist

## Scope and authority

Before any generated code is shipped, `review-agent` must complete this checklist against the exact changes being proposed. Review implementation behavior and evidence, not only the presence of tests or design statements. Record the reviewed commit/change set and the evidence used for each result.

This file is the canonical source of the review checklist. Claude uses it directly; Copilot's instruction and prompt files point to it and must not duplicate its checklist content.

## Result protocol

- Complete every checklist item. For each, record exactly one result: **Pass**, **Fail**, or **Needs Human Decision**, plus concrete evidence (file, test/command result, trace, or approved decision).
- Use **Needs Human Decision** when requirements, security posture, an ADR, or policy leave a material choice unresolved. Do not infer approval from silence, a proposed design, or an agent's judgment.
- A **Pass** requires sufficient evidence. For a genuinely out-of-scope item, explain why it does not apply and what changed-scope evidence supports that conclusion.
- **Fail** or **Needs Human Decision** on any item blocks shipping. Resolve the issue or obtain and record the required human decision, update the review evidence, and rerun affected checks before changing the item to **Pass**.
- Tests are evidence, not a substitute for reviewing the intended behavior, changed code paths, and user-visible outcomes.

## 1. Acceptance Criteria Verification

- [ ] **1.1 Governing acceptance criteria and intent:** Identify the applicable committed feature specification. Read the relevant requirements specification and trace each applicable acceptance criterion to the intended user/system outcome; check the spirit of the criteria, not merely whether unit tests pass.
  - **Status:** Pass / Fail / Needs Human Decision
  - **Evidence:** Specification and criterion-to-code/evidence mapping.
- [ ] **1.2 End-to-end behavior:** Verify the relevant behavior through the actual affected path (API, service, UI, or agent/orchestrator integration). Confirm that tests exercise the contract and that an appropriate integration, UI, or manual check covers behavior unit tests cannot establish.
  - **Status:** Pass / Fail / Needs Human Decision
  - **Evidence:** Changed path and test/run result, including observed outcome.
- [ ] **1.3 Boundaries and rejection behavior:** Check applicable edge cases, authorization, invalid inputs, time/state boundaries, failure behavior, and the required no-mutation/no-side-effect outcome on rejection. Confirm the implementation does not satisfy a literal example while violating the surrounding acceptance intent.
  - **Status:** Pass / Fail / Needs Human Decision
  - **Evidence:** Criteria-to-edge-case mapping and observed state/results.

## 2. Quality Audit

- [ ] **2.1 Correctness and maintainability:** Review the generated code for logic defects, clear ownership of business rules, consistency with repository architecture/conventions, avoidable duplication, unsafe state mutation, and compatibility regressions. Confirm changes are limited to the approved scope.
  - **Status:** Pass / Fail / Needs Human Decision
  - **Evidence:** Relevant code paths and compatibility/regression findings.
- [ ] **2.2 Validation and test quality:** Confirm tests are meaningful, deterministic where required, cover success and failure paths, and would fail for the defect or regression being guarded against. Record the focused test, build, lint, or type-check commands and their results; do not treat a test name or stale report as proof.
  - **Status:** Pass / Fail / Needs Human Decision
  - **Evidence:** Exact commands, results, and any remaining test gap.
- [ ] **2.3 Operational and documentation impact:** Check error handling, concurrency/atomicity, resource lifecycle, observability without sensitive data, and required documentation/API updates for the changed behavior. Identify any operational risk that needs an owner or release gate.
  - **Status:** Pass / Fail / Needs Human Decision
  - **Evidence:** Code/documentation references and relevant checks.

## 3. OWASP Top 10 Security Review

- [ ] **3.1 Current Top 10 coverage:** Record the current official OWASP Top 10 edition used for this review and map all ten categories to the changed code, data flows, and trust boundaries. Do not skip a category: if it does not apply, record the reason and evidence. At minimum, explicitly consider broken access control, cryptographic failures, injection, insecure design, security misconfiguration, vulnerable/outdated components, identification/authentication failures, software/data integrity failures, security logging/monitoring failures, and SSRF (or their equivalents in the edition used).
  - **Status:** Pass / Fail / Needs Human Decision
  - **Evidence:** Completed ten-category mapping and applicable security test/scan results.
- [ ] **3.2 Security controls and abuse paths:** Verify authorization at the authoritative server boundary, input/schema validation, safe output and logging, secret handling, dependency/configuration exposure, and fail-closed behavior for relevant hostile, malformed, stale, replayed, or unavailable inputs. Ensure no model, client, or prompt is treated as an authorization authority.
  - **Status:** Pass / Fail / Needs Human Decision
  - **Evidence:** Trust-boundary review and negative/security test results.
- [ ] **3.3 Security claim evidence:** Distinguish implemented and tested controls from proposed documentation. Markdown, prompts, and agent decisions alone do not establish runtime enforcement; verify direct-call and integration paths where relevant.
  - **Status:** Pass / Fail / Needs Human Decision
  - **Evidence:** Runtime enforcement location and bypass/integration test results, or documented reason not applicable.

## 4. Compliance With Committed Rules

- [ ] **4.1 Complete rule inventory:** Enumerate every committed rule file in the repository for this review (including `pipeline/**/rules/*.md`, `.claude/rules/*.md`, and any other committed rule paths); assess each rule's scope and map every applicable constraint and verification requirement to code and evidence. Do not rely on a static baseline; include this checklist rule and discover any other newly committed rules.
  - **Status:** Pass / Fail / Needs Human Decision
  - **Evidence:** Rule inventory and per-rule applicability/compliance mapping.
- [ ] **4.2 Guardrail compliance:** Explicitly inspect any active injection guardrails and safety rules on every review. If user input, notes, candidates, MCP tools, authorization, or related execution paths are affected, verify all applicable mandatory constraints and verification criteria, including untrusted input treatment, immutable action tuples, strict schemas, trusted exact authorization, authoritative validation, fail-closed behavior, minimized output/logging, and independent backend enforcement. If none of these paths are in scope, explain why and confirm the changes introduce no new path that can bypass guardrails.
  - **Status:** Pass / Fail / Needs Human Decision
  - **Evidence:** Guardrail clause-to-code/test mapping or evidence-supported scope rationale. Never claim markdown documentation itself enforces runtime behavior.
- [ ] **4.3 Other applicable committed rules:** Explicitly check the applicable requirements of domain-specific rules (e.g. data retention, concurrency, and booking rules). Confirm rule-specific verification criteria are met where the change can affect them.
  - **Status:** Pass / Fail / Needs Human Decision
  - **Evidence:** Per-rule findings and verification results.

## 5. Design Alignment and Risk Mitigation

- [ ] **5.1 Architecture & ADR alignment:** Compare implementation and tests against the applicable architecture designs and ADRs. Record every deviation and verify it is approved and reflected in the relevant design/spec artifacts. A Proposed ADR or an ADR review marked Changes required is not approval; unresolved choices affecting the code require a human decision.
  - **Status:** Pass / Fail / Needs Human Decision
  - **Evidence:** Decision-to-implementation mapping, deviations, and recorded approvals.
- [ ] **5.2 Risk-mitigation coverage:** For each applicable risk and mitigation in the architecture risk assessment report, verify that the control is implemented at the correct boundary and that its stated test/evidence exists. Include relevant concurrency, boundary/time semantics, stale state, compatibility, authorization, privacy, logging, and operational failure risks; do not treat conditional residual ratings as completed mitigation.
  - **Status:** Pass / Fail / Needs Human Decision
  - **Evidence:** Risk-to-control-to-test/evidence matrix and residual-risk disposition.
- [ ] **5.3 Release gates and human decisions:** Confirm all applicable ADR open questions, ADR-review findings, risk-assessment release conditions, approvals, and rollback/monitoring gates are resolved before shipping. In particular, automated writes or auto-approval remain **NO-GO** until required controls and named approvals are evidenced.
  - **Status:** Pass / Fail / Needs Human Decision
  - **Evidence:** Recorded decisions, sign-offs, release-condition evidence, and rollback/monitoring owner.

## Final Verdict

- [ ] **Ship** — permitted only when every checklist item is **Pass**, evidence is recorded, and no applicable human decision or release gate is outstanding.
- [ ] **No-Ship** — required if any item is **Fail** or **Needs Human Decision**, evidence is missing, or a required release gate is unmet. State the blocking item(s) and the action/decision needed to reopen review.

**Final verdict:** Ship / No-Ship  
**Reviewer:**  
**Reviewed change/commit:**  
**Date:**  
**Blocking items or decision owner:**

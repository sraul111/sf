---
name: bug-fix-skill
description: Use whenever implementing a fix described in a rule file under pipeline/**/rules/*.md. Covers how to read a rule file's constraint/root-cause/verification table, how to confirm the root cause still matches the current code before patching, how to derive tests directly from the rule's Verification field, and how to close out the rule's "Artifacts this rule touches" section. Load this before touching any code from a rule file.
---

# Bug Fix Skill — turning a rule file into a verified fix

## Why this exists

Rule files under `pipeline/*/rules/*.md` are diagnosed bugs with a prescribed fix, not raw feature intents. They already contain the constraint being violated, the affected feature, the root cause, and the verification criteria — the job is to apply and prove the fix, not to redesign anything.

## The rule-file shape

Every rule file has:
- **Constraint** — the invariant that must hold, stated generically enough to test.
- **Affected feature** — the spec under `pipeline/features/` this bug lives in.
- **Related decision** — a fact from `pipeline/decisions/` the fix must not remove or contradict.
- **Root cause (diagnostic)** — a *hypothesis* about where and why the bug happens, written at diagnosis time. Treat it as a lead, not gospel.
- **Verification** — concrete before/after scenarios the fix must satisfy. This is your test list, already written for you.
- **Artifacts this rule touches** — the files expected to change; update this list to match what actually changed.

## What to do, in order

1. **Read the rule file in full**, plus the affected feature spec and any named decision — don't patch code before understanding what invariant is being restored and why it must coexist with the related decision.
2. **Re-verify the root cause against current code.** The rule file's root-cause line may be stale (line numbers, method names, even the exact bug shape can have drifted since diagnosis). Open the named file and confirm the described defect is still there in that form before changing anything. If it has drifted, adapt the fix to the code as it actually exists, not as the rule describes it.
3. **Apply the minimal fix** that satisfies the Constraint without violating the Related decision. Don't refactor beyond what's needed to close the gap.
4. **Derive tests directly from the Verification field** — one test per scenario listed (each rejected case, each still-accepted case). Don't invent extra scenarios the rule never asked for, and don't skip any it did.
5. **Run the tests** and confirm they pass, including the full existing suite for the affected file(s) — a fix that passes its own new tests but breaks an existing one isn't done.
6. **Update the rule file's "Artifacts this rule touches" section** to reflect what was actually changed, if it differs from what was originally listed.

## Boundaries

- This skill is generic across any rule file — it is not specific to any one bug.
- Do not expand scope beyond the rule's Constraint; a rule file is not a license to refactor the surrounding code.
- Do not touch `pipeline/features/*.md`'s Definition of Done — that belongs to the BA/spec, not to a bug fix.

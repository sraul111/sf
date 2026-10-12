---
mode: agent
description: "Review proposed code changes against the repository's mandatory pre-merge checklist"
---

You are acting as the review agent defined in `.claude/agents/review-agent.md`. Read that file and `.claude/rules/review-checklist-rule.md` in full before reviewing. The Claude rule is the sole source of truth for the checklist; do not recreate or abbreviate its criteria.

Review the exact proposed change set. Complete every applicable checklist item with exactly one status (**Pass**, **Fail**, or **Needs Human Decision**) and concrete evidence. End with **Ship** only if every item passes and every release gate is satisfied; otherwise return **No-Ship** with the blockers and required decisions/actions.
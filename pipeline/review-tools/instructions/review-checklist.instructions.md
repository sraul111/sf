---
applyTo: "**"
---

# Pre-Merge Review Requirement

Before shipping any generated code, require `review-agent` to review the exact proposed changes using the canonical checklist in `rules/review-checklist-rule.md`.

For Copilot, use the wrapper prompt `prompts/review-agent.prompt.md` or agent `agents/review-agent.agent.md`, which delegates to the canonical role in `agents/review-agent.md`. The checklist content lives only in the canonical rule; do not duplicate it across instruction files.

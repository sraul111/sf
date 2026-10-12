---
applyTo: "**"
---

# Pre-Merge Review Requirement

Before shipping any generated code, require `review-agent` to review the exact proposed changes using the canonical checklist in `.claude/rules/review-checklist-rule.md`.

For Copilot, use the wrapper prompt `.github/prompts/review-agent.prompt.md`, which delegates to the canonical role in `.claude/agents/review-agent.md`. The checklist content lives only in the canonical Claude rule; do not duplicate it here.
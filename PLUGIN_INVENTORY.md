# Plugin Inventory — ServiceForge Review Tools

This root manifest references the review tools plugin located in [`pipeline/review-tools/`](pipeline/review-tools/).

See the full inventory and audit in [`pipeline/review-tools/PLUGIN_INVENTORY.md`](pipeline/review-tools/PLUGIN_INVENTORY.md).

## Summary of Components in `pipeline/review-tools/`

| Component Name | Type | Relative Path | Description |
|---|---|---|---|
| `create-pr` | Skill | [`pipeline/review-tools/skills/create-pr/SKILL.md`](pipeline/review-tools/skills/create-pr/SKILL.md) | Guides agent to confirm branches, summarize changes and review findings, and invoke `create_pull_request` with `draft: true`. |
| `review-agent` | Agent | [`pipeline/review-tools/agents/review-agent.md`](pipeline/review-tools/agents/review-agent.md) | Canonical Pre-Merge Review Agent inspecting remote PR diffs via GitHub MCP with read-only tools and submitting pending reviews. |
| `review-agent.agent.md` | Agent Wrapper | [`pipeline/review-tools/agents/review-agent.agent.md`](pipeline/review-tools/agents/review-agent.agent.md) | Copilot workspace agent definition. |
| `review-agent.prompt.md` | Prompt | [`pipeline/review-tools/prompts/review-agent.prompt.md`](pipeline/review-tools/prompts/review-agent.prompt.md) | Copilot prompt wrapper for pre-merge review. |
| `review-checklist-rule` | Rule | [`pipeline/review-tools/rules/review-checklist-rule.md`](pipeline/review-tools/rules/review-checklist-rule.md) | Canonical 5-area pre-merge review checklist rule. |
| `review-checklist.instructions` | Instruction | [`pipeline/review-tools/instructions/review-checklist.instructions.md`](pipeline/review-tools/instructions/review-checklist.instructions.md) | Workspace instruction enforcing pre-merge code review. |

## Verification Check

- **No Application Source Code**: No `.java`, `.ts`, `.js`, or `.py` application code files.
- **No Feature Evidence**: No test execution logs (`.log`), test output reports, or telemetry dumps.
- **No `.env` Files**: No `.env` or local configuration environment files.
- **No Secrets**: No API keys, personal access tokens, or sensitive credentials.

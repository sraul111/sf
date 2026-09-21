---
mode: agent
description: "Bug fix agent — implement and self-test the fix described in a pipeline rule file"
---
You are acting as the Bug Fix agent defined in `.claude/agents/bug-fix-agent.md`, using `.claude/skills/bug-fix-skill/SKILL.md` (and `.claude/skills/build-code-skill/SKILL.md`, plus `.claude/skills/migration-safety-skill/SKILL.md` if this touches the data model). Read all relevant files in full before writing code.

Given a rule file path under `pipeline/**/rules/*.md`, read it along with its Affected feature spec and Related decision, re-verify its Root cause against the current code, apply the minimal fix that satisfies its Constraint, then write and run your own tests — one per scenario in its Verification field. Do this end to end yourself; do not delegate the code change or the tests to the developer-agent or tester-agent. Update the rule file's "Artifacts this rule touches" section with what actually changed.

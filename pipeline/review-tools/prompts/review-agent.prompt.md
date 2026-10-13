---
mode: agent
description: "Review proposed code changes on a remote pull request against the repository's mandatory pre-merge checklist"
---

You are acting as the review agent defined in `review-agent.md`.

1. Explicitly load `review-checklist-rule.md` by path.
2. Read the remote diff instead of local files by fetching PR metadata, changed files, and the unified diff using GitHub MCP (`pull_request_read` with `get`, `get_files`, `get_diff`).
3. Remain strictly read-only on repository code — do not edit local files or push commits.
4. Complete every checklist item with Pass, Fail, or Needs Human Decision and concrete evidence.
5. Submit a pending review on the remote PR containing the review summary and at least one inline comment anchored to a specific line in the diff.
